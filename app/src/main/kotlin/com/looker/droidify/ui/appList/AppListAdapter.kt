package com.looker.droidify.ui.appList

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.R
import com.looker.droidify.database.AppListRow
import com.looker.droidify.database.AppListRowViewType
import com.looker.droidify.database.EmptyListRow
import com.looker.droidify.database.ProductRow
import com.looker.droidify.model.ProductItem
import com.looker.droidify.ui.appList.AppListAdapter.AppListRowViewHolder
import com.looker.droidify.utility.common.extension.authentication
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.inflate
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.common.log
import com.looker.droidify.utility.compatRequireViewById
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import kotlin.system.measureTimeMillis
import com.google.android.material.R as MaterialR

class AppListAdapter(
    context: Context,
    private val onClick: ProductItemClickListener,
) : PagingDataAdapter<AppListRow, AppListRowViewHolder<AppListRow>>(
    diffCallback = ItemCallback()
) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private class Colors(
        @JvmField
        val updateBackground: ColorStateList,
        @JvmField
        val updateForeground: ColorStateList,
        @JvmField
        val installedBackground: ColorStateList,
        @JvmField
        val installedForeground: ColorStateList,
        @JvmField
        val defaultForeground: ColorStateList
    )

    private val colors = Colors(
        updateBackground = context.getColorFromAttr(MaterialR.attr.colorTertiaryContainer),
        updateForeground = context.getColorFromAttr(MaterialR.attr.colorOnTertiaryContainer),
        installedBackground = context.getColorFromAttr(MaterialR.attr.colorSecondaryContainer),
        installedForeground = context.getColorFromAttr(MaterialR.attr.colorOnSecondaryContainer),
        defaultForeground = context.getColorFromAttr(MaterialR.attr.colorOnBackground),
    )

    fun interface ProductItemClickListener {
        fun onProductItemClick(productItemPackageName: String)
    }

    abstract class AppListRowViewHolder<T: AppListRow>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: T)
    }

    private class ProductViewHolder(
        private val colors: Colors,
        itemView: View
    ) : AppListRowViewHolder<ProductRow>(itemView), View.OnClickListener {
        private val name: TextView = itemView.compatRequireViewById(R.id.name)
        private val status: TextView = itemView.compatRequireViewById(R.id.status)
        private val summary: TextView = itemView.compatRequireViewById(R.id.summary)
        private val icon: ShapeableImageView = itemView.compatRequireViewById(R.id.icon)

        private lateinit var item: ProductItem

        lateinit var onClick: ProductItemClickListener

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (v == itemView) {
                log(measureTimeMillis { onClick.onProductItemClick(item.packageName) }, "Bench")
            }
        }

        override fun bind(item: ProductRow) {
            val productItem = item.productItem
            val repository = item.repository

            this.item = productItem

            name.text = productItem.name
            summary.text = item.summary
            summary.isVisible = item.summaryVisible

            if (repository != null) {
                val iconUrl = productItem.icon(view = icon, repository = repository)
                icon.load(iconUrl) {
                    authentication(repository.authentication)
                }
            }
            with(status) {
                text = item.versionText
                when {
                    productItem.canUpdate -> {
                        backgroundTintList = colors.updateBackground
                        setTextColor(colors.updateForeground)
                    }

                    item.isInstalled -> {
                        backgroundTintList = colors.installedBackground
                        setTextColor(colors.installedForeground)
                    }

                    else -> {
                        setPadding(0, 0, 0, 0)
                        setTextColor(colors.defaultForeground)
                        background = null
                        return@with
                    }
                }
                background = context.corneredBackground
                6.dp.let { setPadding(it, it, it, it) }
            }
            val enabled = item.enabled
            name.isEnabled = enabled
            status.isEnabled = enabled
            summary.isEnabled = enabled
        }
    }

    private class EmptyViewHolder(
        private val text: TextView,
    ) : AppListRowViewHolder<EmptyListRow>(text) {

        init {
            with(text) {
                gravity = Gravity.CENTER
                val dp20 = 20.dp
                setPadding(dp20, dp20, dp20, dp20)
                typeface = TypefaceExtra.light
                setTextColor(context.getColorFromAttr(android.R.attr.colorPrimary))
                setTextSizeScaled(20)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.MATCH_PARENT
                )
            }
        }

        override fun bind(item: EmptyListRow) {
            text.text = item.emptyText
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(
        parent: ViewGroup,
        @AppListRowViewType
        viewType: Int,
    ): AppListRowViewHolder<AppListRow> {
        return when (viewType) {
            AppListRowViewType.PRODUCT -> ProductViewHolder(
                itemView = parent.inflate(R.layout.product_item),
                colors = colors,
            )
            AppListRowViewType.EMPTY -> EmptyViewHolder(TextView(parent.context))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        } as AppListRowViewHolder<AppListRow>
    }

    private fun requireItem(position: Int): AppListRow {
        return getItem(position)!!
    }

    override fun getItemViewType(position: Int): Int {
        return requireItem(position).viewType
    }

    override fun onBindViewHolder(holder: AppListRowViewHolder<AppListRow>, position: Int) {
        holder.bind(requireItem(position))
        if (holder is ProductViewHolder) {
            holder.onClick = onClick
        }
    }

    private class ItemCallback: DiffUtil.ItemCallback<AppListRow>() {
        override fun areItemsTheSame(oldItem: AppListRow, newItem: AppListRow): Boolean {
            return when (oldItem) {
                is ProductRow if newItem is ProductRow -> oldItem.productItem.packageName == newItem.productItem.packageName
                is EmptyListRow if newItem is EmptyListRow -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: AppListRow, newItem: AppListRow): Boolean {
            return oldItem == newItem
        }
    }
}
