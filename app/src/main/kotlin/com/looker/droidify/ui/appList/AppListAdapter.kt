package com.looker.droidify.ui.appList

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import com.looker.droidify.network.percentBy
import com.looker.droidify.ui.DownloadStatus
import com.looker.droidify.utility.common.extension.authentication
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.inflate
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.common.log
import com.looker.droidify.utility.common.nullIfEmpty
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import com.looker.droidify.widget.CursorRecyclerAdapter
import kotlin.system.measureTimeMillis
import com.google.android.material.R as MaterialR

class AppListAdapter(
    private val source: AppListFragment.Source,
    private val onClick: (packageName: String) -> Unit,
) : CursorRecyclerAdapter<AppListAdapter.ViewType, RecyclerView.ViewHolder>() {

    enum class ViewType { PRODUCT, LOADING, EMPTY }

    private inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.name)!!
        val status = itemView.findViewById<TextView>(R.id.status)!!
        val summary = itemView.findViewById<TextView>(R.id.summary)!!
        val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)!!
        val progress = itemView.findViewById<LinearProgressIndicator>(R.id.progress)!!

        init {
            itemView.setOnClickListener {
                log(measureTimeMillis { onClick(getPackageName(absoluteAdapterPosition)) }, "Bench")
            }
        }
    }

    private class LoadingViewHolder(context: Context) :
        RecyclerView.ViewHolder(FrameLayout(context)) {
        init {
            with(itemView as FrameLayout) {
                val progressBar = CircularProgressIndicator(context)
                progressBar.isIndeterminate = true
                addView(
                    progressBar,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { gravity = Gravity.CENTER }
                )
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    private class EmptyViewHolder(context: Context) :
        RecyclerView.ViewHolder(TextView(context)) {
        val text: TextView
            get() = itemView as TextView

        init {
            with(itemView as TextView) {
                gravity = Gravity.CENTER
                setPadding(20.dp, 20.dp, 20.dp, 20.dp)
                typeface = TypefaceExtra.light
                setTextColor(context.getColorFromAttr(android.R.attr.colorPrimary))
                setTextSizeScaled(20)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    private val repositories: HashMap<Long, Repository> = HashMap()

    /**
     * Download status tracking for each package.
     *
     * Tracks states: [DownloadStatus.Pending], [DownloadStatus.Connecting], [DownloadStatus.Downloading].
     * Updated by [AppListFragment.updateDownloadState] from [DownloadService].
     */
    private val downloadStatuses: HashMap<String, DownloadStatus> = HashMap()

    /**
     * Install status tracking for each package.
     *
     * Tracks states: [DownloadStatus.PendingInstall], [DownloadStatus.Installing].
     * Updated by [AppListFragment.updateInstallStates] from [InstallManager].
     *
     * Kept separate from [downloadStatuses] because install status should take priority
     * when both are present (e.g., download completes and sets Idle while install is starting).
     */
    private val installStatuses: HashMap<String, DownloadStatus> = HashMap()

    fun updateRepos(repos: List<Repository>) {
        repos.forEach {
            repositories[it.id] = it
        }
        notifyDataSetChanged()
    }

    /**
     * Updates the download status for a package.
     *
     * Called from [AppListFragment] when [DownloadService] state changes.
     * Setting [DownloadStatus.Idle] removes the package from tracking.
     */
    fun updateDownloadStatus(packageName: String, status: DownloadStatus) {
        val oldStatus = downloadStatuses[packageName]
        if (oldStatus == status) return
        if (status == DownloadStatus.Idle) {
            downloadStatuses.remove(packageName)
        } else {
            downloadStatuses[packageName] = status
        }
        notifyStatusChange(packageName)
    }

    /**
     * Updates the install status for a package.
     *
     * Called from [AppListFragment] when [InstallManager] state changes.
     * Setting [DownloadStatus.Idle] removes the package from tracking.
     */
    fun updateInstallStatus(packageName: String, status: DownloadStatus) {
        val oldStatus = installStatuses[packageName]
        if (oldStatus == status) return
        if (status == DownloadStatus.Idle) {
            installStatuses.remove(packageName)
        } else {
            installStatuses[packageName] = status
        }
        notifyStatusChange(packageName)
    }

    /**
     * Returns the effective status for a package, considering both download and install states.
     *
     * Install status takes priority over download status. This handles the race condition
     * where download completes (setting Idle) just as installation begins (setting Installing).
     * Without this priority, the UI would briefly flash to Idle before showing Installing.
     */
    private fun getEffectiveStatus(packageName: String): DownloadStatus {
        val installStatus = installStatuses[packageName]
        if (installStatus != null) return installStatus
        return downloadStatuses[packageName] ?: DownloadStatus.Idle
    }

    /**
     * Notifies the RecyclerView of a status change using payload-based partial binding.
     *
     * Uses [DownloadStatus] as payload so [onBindViewHolder] can update only the progress
     * indicator without rebinding the entire item (icon, name, etc.).
     */
    private fun notifyStatusChange(packageName: String) {
        val position = findPositionByPackageName(packageName)
        if (position >= 0) {
            val effectiveStatus = getEffectiveStatus(packageName)
            notifyItemChanged(position, effectiveStatus)
        }
    }

    /**
     * Finds the adapter position for a package by iterating through the cursor.
     *
     * @return Position of the package, or -1 if not found in the current list.
     */
    private fun findPositionByPackageName(packageName: String): Int {
        if (isEmpty) return -1
        for (i in 0 until super.getItemCount()) {
            if (getPackageName(i) == packageName) return i
        }
        return -1
    }

    var emptyText: String = ""
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                if (isEmpty) {
                    notifyDataSetChanged()
                }
            }
        }

    override val viewTypeClass: Class<ViewType>
        get() = ViewType::class.java

    private val isEmpty: Boolean
        get() = super.getItemCount() == 0

    override fun getItemCount(): Int = if (isEmpty) 1 else super.getItemCount()
    override fun getItemId(position: Int): Long = if (isEmpty) -1 else super.getItemId(position)

    override fun getItemEnumViewType(position: Int): ViewType {
        return when {
            !isEmpty -> ViewType.PRODUCT
            cursor == null -> ViewType.LOADING
            else -> ViewType.EMPTY
        }
    }

    private fun getPackageName(position: Int): String {
        return Database.ProductAdapter.transformPackageName(moveTo(position.coerceAtLeast(0)))
    }

    private fun getProductItem(position: Int): ProductItem {
        return Database.ProductAdapter.transformItem(moveTo(position.coerceAtLeast(0)))
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PRODUCT -> ProductViewHolder(parent.inflate(R.layout.product_item))
            ViewType.LOADING -> LoadingViewHolder(parent.context)
            ViewType.EMPTY -> EmptyViewHolder(parent.context)
        }
    }

    private var updateBackground: ColorStateList? = null
    private var updateForeground: ColorStateList? = null
    private var installedBackground: ColorStateList? = null
    private var installedForeground: ColorStateList? = null
    private var defaultForeground: ColorStateList? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemEnumViewType(position)) {
            ViewType.PRODUCT -> {
                holder as ProductViewHolder
                val productItem = getProductItem(position)
                holder.name.text = productItem.name
                holder.summary.text = productItem.summary
                holder.summary.isVisible = productItem.summary.isNotEmpty()
                    && productItem.name != productItem.summary
                val repository = repositories[productItem.repositoryId]
                if (repository != null) {
                    val iconUrl = productItem.icon(view = holder.icon, repository = repository)
                    holder.icon.load(iconUrl) {
                        authentication(repository.authentication)
                    }
                }
                with(holder.status) {
                    val versionText = if (source == AppListFragment.Source.UPDATES) {
                        productItem.version
                    } else {
                        productItem.installedVersion.nullIfEmpty() ?: productItem.version
                    }
                    text = versionText
                    val isInstalled = productItem.installedVersion.nullIfEmpty() != null
                    when {
                        productItem.canUpdate -> {
                            if (updateBackground == null) {
                                updateBackground =
                                    context.getColorFromAttr(MaterialR.attr.colorTertiaryContainer)
                            }
                            if (updateForeground == null) {
                                updateForeground =
                                    context.getColorFromAttr(MaterialR.attr.colorOnTertiaryContainer)
                            }
                            backgroundTintList = updateBackground
                            setTextColor(updateForeground)
                        }

                        isInstalled -> {
                            if (installedBackground == null) {
                                installedBackground =
                                    context.getColorFromAttr(MaterialR.attr.colorSecondaryContainer)
                            }
                            if (installedForeground == null) {
                                installedForeground =
                                    context.getColorFromAttr(MaterialR.attr.colorOnSecondaryContainer)
                            }
                            backgroundTintList = installedBackground
                            setTextColor(installedForeground)
                        }

                        else -> {
                            setPadding(0, 0, 0, 0)
                            if (defaultForeground == null) {
                                defaultForeground =
                                    context.getColorFromAttr(MaterialR.attr.colorOnBackground)
                            }
                            setTextColor(defaultForeground)
                            background = null
                            return@with
                        }
                    }
                    background = context.corneredBackground
                    6.dp.let { setPadding(it, it, it, it) }
                }
                val enabled = productItem.compatible || productItem.installedVersion.isNotEmpty()
                holder.name.isEnabled = enabled
                holder.status.isEnabled = enabled
                holder.summary.isEnabled = enabled

                // Bind download/install status
                val effectiveStatus = getEffectiveStatus(productItem.packageName)
                bindDownloadStatus(holder, effectiveStatus)
            }

            ViewType.LOADING -> {
                // Do nothing
            }

            ViewType.EMPTY -> {
                holder as EmptyViewHolder
                holder.text.text = emptyText
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if (holder is ProductViewHolder) {
                for (payload in payloads) {
                    if (payload is DownloadStatus) {
                        bindDownloadStatus(holder, payload)
                    }
                }
            }
        }
    }

    private fun bindDownloadStatus(holder: ProductViewHolder, status: DownloadStatus) {
        when (status) {
            is DownloadStatus.Idle -> {
                holder.progress.isVisible = false
            }
            is DownloadStatus.Pending,
            is DownloadStatus.Connecting,
            is DownloadStatus.PendingInstall -> {
                holder.progress.isVisible = true
                holder.progress.isIndeterminate = true
            }
            is DownloadStatus.Downloading -> {
                holder.progress.isVisible = true
                if (status.total != null) {
                    val percent = status.read.value percentBy status.total.value
                    if (percent >= 0) {
                        holder.progress.isIndeterminate = false
                        holder.progress.setProgressCompat(percent, true)
                    } else {
                        holder.progress.isIndeterminate = true
                    }
                } else {
                    holder.progress.isIndeterminate = true
                }
            }
            is DownloadStatus.Installing -> {
                holder.progress.isVisible = true
                holder.progress.isIndeterminate = true
            }
        }
    }
}
