package com.looker.droidify.ui.appDetail

import android.net.Uri
import android.os.Parcelable
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.collection.ArraySet
import androidx.recyclerview.widget.DiffUtil
import com.looker.droidify.R
import com.looker.droidify.model.ProductPreference
import com.looker.droidify.model.Release
import com.looker.droidify.network.DataSize
import com.looker.droidify.ui.appDetail.viewHolders.AppInfoViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.BaseViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.DownloadStatusViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.EmptyViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.ExpandViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.InstallButtonViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.LinkViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.PermissionsViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.ReleaseViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.ScreenShotViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.SectionViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.SwitchViewHolder
import com.looker.droidify.ui.appDetail.viewHolders.TextViewHolder
import com.looker.droidify.utility.common.extension.inflate
import com.looker.droidify.widget.EnumRecyclerAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.looker.droidify.R.drawable as drawableRes
import com.looker.droidify.R.string as stringRes

private const val DUMMY_PAYLOAD = "PAYLOAD"

class AppDetailAdapter(
    private val coroutineScope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher,
    private val callbacks: Callbacks,
) : EnumRecyclerAdapter<AppDetailAdapter.AppDetailsViewType, BaseViewHolder<AppDetailItem>>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    interface Callbacks: ScreenshotsAdapter.ScreenshotClickListener {
        fun onActionClick(action: Action)
        fun onFavouriteClicked()
        fun onPreferenceChanged(preference: ProductPreference)
        fun onPermissionsClick(group: String?, permissions: List<String>)
        fun onReleaseClick(release: Release)
        fun onRequestAddRepository(address: String)
        fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean
    }

    enum class Action(@StringRes val titleResId: Int, @DrawableRes val iconResId: Int) {
        INSTALL(stringRes.install, drawableRes.ic_download),
        UPDATE(stringRes.update, drawableRes.ic_download),
        LAUNCH(stringRes.launch, drawableRes.ic_launch),
        DETAILS(stringRes.details, drawableRes.ic_tune),
        UNINSTALL(stringRes.uninstall, drawableRes.ic_delete),
        CANCEL(stringRes.cancel, drawableRes.ic_cancel),
        SHARE(stringRes.share, drawableRes.ic_share),
        SOURCE(stringRes.source_code, drawableRes.ic_source_code),
    }

    sealed interface Status {
        data object Idle : Status
        data object Pending : Status
        data object Connecting : Status
        data class Downloading(val read: DataSize, val total: DataSize?) : Status
        data object PendingInstall : Status
        data object Installing : Status
    }

    enum class AppDetailsViewType {
        APP_INFO,
        DOWNLOAD_STATUS,
        INSTALL_BUTTON,
        SCREENSHOT,
        SWITCH,
        SECTION,
        EXPAND,
        TEXT,
        LINK,
        PERMISSIONS,
        RELEASE,
        EMPTY,
    }

    enum class ExpandType {
        NOTHING, DESCRIPTION, CHANGES,
        LINKS, DONATES, PERMISSIONS, VERSIONS
    }

    enum class TextType { DESCRIPTION, ANTI_FEATURES, CHANGES }

    private var items: MutableList<AppDetailItem> = ArrayList()
    private val expanded: ArraySet<ExpandType> = ArraySet<ExpandType>()

    suspend fun setState(
        state: AppDetailListState,
    ) {
        val newItems = state.items.mapTo(ArrayList()) {
            when (it) {
                is AppDetailItem.InstallButtonItem -> {
                    AppDetailItem.InstallButtonItem(action)
                }

                is AppDetailItem.DownloadStatusItem -> {
                    AppDetailItem.DownloadStatusItem(status)
                }

                else -> {
                    it
                }
            }
        }

        expandPreviouslyExpandedItems(newItems)

        val oldItems = items.toList()
        val diffResult = withContext(defaultDispatcher) {
            DiffUtil.calculateDiff(AppDetailDiffCallback(oldItems, newItems))
        }

        items = newItems

        diffResult.dispatchUpdatesTo(this)
    }

    private fun expandPreviouslyExpandedItems(newList: MutableList<AppDetailItem>) {
        for (position in newList.indices.reversed()) {
            when (val item = newList[position]) {
                is AppDetailItem.SectionItem if item.expandType in expanded -> {
                    newList.expandSectionItemAtPosition(position)
                }

                is AppDetailItem.ExpandItem if item.expandType in expanded -> {
                    newList.expandExpandItemAtPosition(position)
                }

                else -> {
                    // do nothing
                }
            }
        }
    }

    var action: Action? = null
        set(value) {
            if (field != value) {
                val items = items
                val index = items.indexOfFirst { it is AppDetailItem.InstallButtonItem }
                val progressBarIndex = items.indexOfFirst { it is AppDetailItem.DownloadStatusItem }
                if (index > 0 && progressBarIndex > 0) {
                    items[index] = AppDetailItem.InstallButtonItem(value)
                    notifyItemChanged(index, DUMMY_PAYLOAD)
                    notifyItemChanged(progressBarIndex, DUMMY_PAYLOAD)
                }
                field = value
            }
        }

    var status: Status = Status.Idle
        set(value) {
            if (field != value) {
                val items = items
                val index = items.indexOfFirst { it is AppDetailItem.DownloadStatusItem }
                if (index > 0) {
                    items[index] = AppDetailItem.DownloadStatusItem(value)
                    notifyItemChanged(index, DUMMY_PAYLOAD)
                }
                items.forEachIndexed { index, item ->
                    if (item is AppDetailItem.ReleaseItem) {
                        notifyItemChanged(index, DUMMY_PAYLOAD)
                    }
                }
                field = value
            }
        }

    override val viewTypeClass: Class<AppDetailsViewType>
        get() = AppDetailsViewType::class.java

    override fun getItemCount(): Int = items.size
    override fun getItemEnumViewType(position: Int): AppDetailsViewType = items[position].viewType

    private val expandRowLickListener = ExpandViewHolder.OnRowLickListener { item, position ->
        onExpandClick(item, position)
    }

    private val sectionRowLickListener = SectionViewHolder.OnRowLickListener { item, position ->
        onSectionClick(item, position)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: AppDetailsViewType,
    ): BaseViewHolder<AppDetailItem> {
        return when (viewType) {
            AppDetailsViewType.APP_INFO -> AppInfoViewHolder(
                itemView = parent.inflate(R.layout.app_detail_header),
                callbacks = callbacks
            )
            AppDetailsViewType.DOWNLOAD_STATUS -> DownloadStatusViewHolder(
                itemView = parent.inflate(R.layout.download_status),
            )
            AppDetailsViewType.INSTALL_BUTTON -> InstallButtonViewHolder(
                itemView = parent.inflate(R.layout.install_button),
                callbacks = callbacks,
            )
            AppDetailsViewType.SCREENSHOT -> ScreenShotViewHolder(
                context = parent.context,
                coroutineScope = coroutineScope,
                defaultDispatcher = defaultDispatcher,
                callbacks = callbacks
            )
            AppDetailsViewType.SWITCH -> SwitchViewHolder(
                itemView = parent.inflate(R.layout.switch_item),
                callbacks = callbacks
            )
            AppDetailsViewType.SECTION -> SectionViewHolder(
                itemView = parent.inflate(R.layout.section_item),
                onRowClick = sectionRowLickListener
            )
            AppDetailsViewType.EXPAND -> ExpandViewHolder(
                itemView = parent.inflate(R.layout.expand_view_button),
                onRowClick = expandRowLickListener,
            )
            AppDetailsViewType.TEXT -> TextViewHolder(
                context = parent.context,
                callbacks = callbacks
            )
            AppDetailsViewType.LINK -> LinkViewHolder(
                itemView = parent.inflate(R.layout.link_item),
                callbacks = callbacks
            )
            AppDetailsViewType.PERMISSIONS -> PermissionsViewHolder(
                parent.inflate(R.layout.permissions_item),
                callbacks
            )
            AppDetailsViewType.RELEASE -> ReleaseViewHolder(
                itemView = parent.inflate(R.layout.release_item),
                callbacks = callbacks
            )
            AppDetailsViewType.EMPTY -> EmptyViewHolder(
                context = parent.context,
                callbacks = callbacks
            )
        } as BaseViewHolder<AppDetailItem>
    }

    override fun onBindViewHolder(holder: BaseViewHolder<AppDetailItem>, position: Int) {
        val items = items
        val item = items[position]

        when (holder) {
            is LinkViewHolder -> {
                holder.hasTopMargin = position > 0 && items[position - 1] !is AppDetailItem.LinkItem
            }

            is PermissionsViewHolder -> {
                holder.hasTopMargin = position > 0 && items[position - 1] !is AppDetailItem.PermissionsItem
            }

            is ExpandViewHolder -> {
                holder.isExpanded = (item as AppDetailItem.ExpandItem).expandType !in expanded
            }

            is ReleaseViewHolder -> {
                holder.status = status
            }
        }

        holder.bind(item)
    }

    private fun onSectionClick(sectionItem: AppDetailItem.SectionItem, position: Int) {
        val items = items
        if (sectionItem.items.isNotEmpty()) {
            expanded += sectionItem.expandType
            items.expandSectionItemAtPosition(position)
            notifyItemChanged(position)
            notifyItemRangeInserted(position + 1, sectionItem.items.size)
        } else if (sectionItem.collapseCount > 0) {
            expanded -= sectionItem.expandType
            items.collapseSectionItemAtPosition(position)
            notifyItemChanged(position)
            notifyItemRangeRemoved(position + 1, sectionItem.collapseCount)
        }
    }

    private fun MutableList<AppDetailItem>.expandSectionItemAtPosition(position: Int) {
        val sectionItem = get(position) as AppDetailItem.SectionItem

        set(
            position,
            AppDetailItem.SectionItem(
                sectionType = sectionItem.sectionType,
                expandType = sectionItem.expandType,
                items = emptyList(),
                collapseCount = sectionItem.items.size + sectionItem.collapseCount,
            )
        )
        addAll(position + 1, sectionItem.items)
    }

    private fun MutableList<AppDetailItem>.collapseSectionItemAtPosition(position: Int) {
        val sectionItem = get(position) as AppDetailItem.SectionItem

        set(
            position,
            AppDetailItem.SectionItem(
                sectionType = sectionItem.sectionType,
                expandType = sectionItem.expandType,
                items = subList(position + 1, position + 1 + sectionItem.collapseCount)
                    .toList(),
                collapseCount = 0,
            )
        )
        repeat(sectionItem.collapseCount) { removeAt(position + 1) }
    }

    private fun onExpandClick(expandItem: AppDetailItem.ExpandItem, position: Int) {
        val items = items
        if (expandItem.expandType !in expanded) {
            expanded += expandItem.expandType
            items.expandExpandItemAtPosition(position)
            if (expandItem.replace) {
                notifyItemRangeChanged(position - 1, 2)
            } else {
                if (position > 0) {
                    notifyItemRangeInserted(position, expandItem.items.size)
                    notifyItemChanged(position + expandItem.items.size)
                }
            }
        } else {
            expanded -= expandItem.expandType
            items.collapseExpandItemAtPosition(position)
            if (expandItem.replace) {
                notifyItemRangeChanged(position - 1, 2)
            } else {
                if (position > 0) {
                    notifyItemRangeRemoved(
                        position - expandItem.items.size,
                        expandItem.items.size,
                    )
                    notifyItemChanged(position - expandItem.items.size)
                }
            }
        }
    }

    private fun MutableList<AppDetailItem>.expandExpandItemAtPosition(position: Int) {
        val expandItem = get(position) as AppDetailItem.ExpandItem
        if (expandItem.replace) {
            set(position - 1, expandItem.items[0])
        } else {
            addAll(position, expandItem.items)
        }
    }

    private fun MutableList<AppDetailItem>.collapseExpandItemAtPosition(position: Int) {
        val expandItem = get(position) as AppDetailItem.ExpandItem
        if (expandItem.replace) {
            set(position - 1, expandItem.items[1])
        } else {
            removeAll(expandItem.items)
        }
    }

    @Parcelize
    class SavedState internal constructor(internal val expanded: Set<ExpandType>) : Parcelable

    fun saveState(): SavedState? {
        return if (expanded.isNotEmpty()) {
            SavedState(expanded)
        } else {
            null
        }
    }

    fun restoreState(savedState: SavedState) {
        expanded.clear()
        expanded += savedState.expanded
    }

    private class AppDetailDiffCallback(
        private val oldList: List<AppDetailItem>,
        private val newList: List<AppDetailItem>,
    ): DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return when (oldItem) {
                is AppDetailItem.AppInfoItem if newItem is AppDetailItem.AppInfoItem -> {
                    true
                }

                is AppDetailItem.DownloadStatusItem if newItem is AppDetailItem.DownloadStatusItem -> {
                    true
                }

                is AppDetailItem.InstallButtonItem if newItem is AppDetailItem.InstallButtonItem -> {
                    true
                }

                is AppDetailItem.ScreenshotItem if newItem is AppDetailItem.ScreenshotItem -> {
                    true
                }

                is AppDetailItem.ReleaseItem if newItem is AppDetailItem.ReleaseItem -> {
                    oldItem.repository == newItem.repository && oldItem.release == newItem.release
                }

                is AppDetailItem.PermissionsItem if newItem is AppDetailItem.PermissionsItem -> {
                    oldItem.group == newItem.group
                }

                is AppDetailItem.LinkItem.Donate if newItem is AppDetailItem.LinkItem.Donate -> {
                    true
                }

                else -> {
                    oldItem == newItem
                }
            }
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int,
        ): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
            return DUMMY_PAYLOAD
        }
    }
}
