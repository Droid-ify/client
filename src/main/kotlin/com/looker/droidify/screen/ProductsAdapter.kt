package com.looker.droidify.screen

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.entity.ProductItem
import com.looker.droidify.entity.Repository
import com.looker.droidify.network.PicassoDownloader
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.*
import com.looker.droidify.utility.extension.text.*
import com.looker.droidify.widget.CursorRecyclerAdapter
import com.looker.droidify.widget.DividerItemDecoration

class ProductsAdapter(private val onClick: (ProductItem) -> Unit):
  CursorRecyclerAdapter<ProductsAdapter.ViewType, RecyclerView.ViewHolder>() {
  enum class ViewType { PRODUCT, LOADING, EMPTY }

  private class ProductViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    val name = itemView.findViewById<TextView>(R.id.name)!!
    val status = itemView.findViewById<TextView>(R.id.status)!!
    val summary = itemView.findViewById<TextView>(R.id.summary)!!
    val icon = itemView.findViewById<ImageView>(R.id.icon)!!

    val progressIcon: Drawable
    val defaultIcon: Drawable

    init {
      val (progressIcon, defaultIcon) = Utils.getDefaultApplicationIcons(icon.context)
      this.progressIcon = progressIcon
      this.defaultIcon = defaultIcon
    }
  }

  private class LoadingViewHolder(context: Context): RecyclerView.ViewHolder(FrameLayout(context)) {
    init {
      itemView as FrameLayout
      val progressBar = ProgressBar(itemView.context)
      itemView.addView(progressBar, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER })
      itemView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
        RecyclerView.LayoutParams.MATCH_PARENT)
    }
  }

  private class EmptyViewHolder(context: Context): RecyclerView.ViewHolder(TextView(context)) {
    val text: TextView
      get() = itemView as TextView

    init {
      itemView as TextView
      itemView.gravity = Gravity.CENTER
      itemView.resources.sizeScaled(20).let { itemView.setPadding(it, it, it, it) }
      itemView.typeface = TypefaceExtra.light
      itemView.setTextColor(context.getColorFromAttr(android.R.attr.textColorPrimary))
      itemView.setTextSizeScaled(20)
      itemView.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
        RecyclerView.LayoutParams.MATCH_PARENT)
    }
  }

  fun configureDivider(context: Context, position: Int, configuration: DividerItemDecoration.Configuration) {
    val currentItem = if (getItemEnumViewType(position) == ViewType.PRODUCT) getProductItem(position) else null
    val nextItem = if (position + 1 < itemCount && getItemEnumViewType(position + 1) == ViewType.PRODUCT)
      getProductItem(position + 1) else null
    when {
      currentItem != null && nextItem != null && currentItem.matchRank != nextItem.matchRank -> {
        configuration.set(true, false, 0, 0)
      }
      else -> {
        configuration.set(true, false, context.resources.sizeScaled(72), 0)
      }
    }
  }

  var repositories: Map<Long, Repository> = emptyMap()
    set(value) {
      field = value
      notifyDataSetChanged()
    }

  var emptyText: String = ""
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

  private fun getProductItem(position: Int): ProductItem {
    return Database.ProductAdapter.transformItem(moveTo(position))
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: ViewType): RecyclerView.ViewHolder {
    return when (viewType) {
      ViewType.PRODUCT -> ProductViewHolder(parent.inflate(R.layout.product_item)).apply {
        itemView.setOnClickListener { onClick(getProductItem(adapterPosition)) }
      }
      ViewType.LOADING -> LoadingViewHolder(parent.context)
      ViewType.EMPTY -> EmptyViewHolder(parent.context)
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (getItemEnumViewType(position)) {
      ViewType.PRODUCT -> {
        holder as ProductViewHolder
        val productItem = getProductItem(position)
        holder.name.text = productItem.name
        holder.summary.text = if (productItem.name == productItem.summary) "" else productItem.summary
        holder.summary.visibility = if (holder.summary.text.isNotEmpty()) View.VISIBLE else View.GONE
        val repository: Repository? = repositories[productItem.repositoryId]
        if ((productItem.icon.isNotEmpty() || productItem.metadataIcon.isNotEmpty()) && repository != null) {
          holder.icon.load(PicassoDownloader.createIconUri(holder.icon, productItem.packageName,
            productItem.icon, productItem.metadataIcon, repository)) {
            placeholder(holder.progressIcon)
            error(holder.defaultIcon)
          }
        } else {
          holder.icon.clear()
          holder.icon.setImageDrawable(holder.defaultIcon)
        }
        holder.status.apply {
          if (productItem.canUpdate) {
            text = productItem.version
            if (background == null) {
              resources.sizeScaled(4).let { setPadding(it, 0, it, 0) }
              setTextColor(holder.status.context.getColorFromAttr(android.R.attr.colorBackground))
              background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, null).apply {
                color = holder.status.context.getColorFromAttr(android.R.attr.colorAccent)
                cornerRadius = holder.status.resources.sizeScaled(2).toFloat()
              }
            }
          } else {
            text = productItem.installedVersion.nullIfEmpty() ?: productItem.version
            if (background != null) {
              setPadding(0, 0, 0, 0)
              setTextColor(holder.status.context.getColorFromAttr(android.R.attr.textColorPrimary))
              background = null
            }
          }
        }
        val enabled = productItem.compatible || productItem.installedVersion.isNotEmpty()
        sequenceOf(holder.name, holder.status, holder.summary).forEach { it.isEnabled = enabled }
      }
      ViewType.LOADING -> {
        // Do nothing
      }
      ViewType.EMPTY -> {
        holder as EmptyViewHolder
        holder.text.text = emptyText
      }
    }::class
  }
}
