package com.looker.droidify.ui.favourites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.nullIfEmpty
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.R
import com.looker.droidify.databinding.ProductItemBinding
import com.looker.droidify.utility.extension.icon
import com.looker.droidify.utility.extension.resources.sizeScaled

class FavouriteFragmentAdapter(
	private val onProductClick: (String) -> Unit
) : RecyclerView.Adapter<FavouriteFragmentAdapter.ViewHolder>() {

	inner class ViewHolder(binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root) {
		val icon = binding.icon
		val name = binding.name
		val summary = binding.summary
		val version = binding.status
	}

	var apps: List<List<Product>> = emptyList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	var repositories: Map<Long, Repository> = emptyMap()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
		ViewHolder(
			ProductItemBinding.inflate(
				LayoutInflater.from(parent.context),
				parent,
				false
			)
		).apply {
			itemView.setOnClickListener {
				onProductClick(apps[absoluteAdapterPosition].first().packageName)
			}
		}

	override fun getItemCount(): Int = apps.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = apps[position].first().item()
		val repository: Repository? = repositories[item.repositoryId]
		holder.name.text = item.name
		holder.summary.isVisible = holder.summary.text.isNotEmpty()
		holder.summary.text = item.summary
		repository?.let { repo ->
			holder.icon.load(
				item.packageName.icon(
					view = holder.icon,
					icon = item.icon,
					metadataIcon = item.metadataIcon,
					repository = repo
				)
			)
		}
		holder.version.apply {
			text = item.installedVersion.nullIfEmpty() ?: item.version
			val isInstalled = item.installedVersion.nullIfEmpty() != null
			when {
				item.canUpdate -> {
					backgroundTintList =
						context.getColorFromAttr(R.attr.colorSecondaryContainer)
					setTextColor(context.getColorFromAttr(R.attr.colorOnSecondaryContainer))
				}
				isInstalled -> {
					backgroundTintList =
						context.getColorFromAttr(R.attr.colorPrimaryContainer)
					setTextColor(context.getColorFromAttr(R.attr.colorOnPrimaryContainer))
				}
				else -> {
					setPadding(0, 0, 0, 0)
					setTextColor(context.getColorFromAttr(R.attr.colorOnBackground))
					background = null
					return@apply
				}
			}
			background = ResourcesCompat.getDrawable(
				holder.itemView.resources,
				R.drawable.background_border,
				context.theme
			)
			resources.sizeScaled(6).let { setPadding(it, it, it, it) }
		}
	}
}