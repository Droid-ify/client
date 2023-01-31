package com.looker.droidify.screen

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.looker.core.common.extension.inflate
import com.looker.core.model.Repository
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.widget.CursorRecyclerAdapter

class RepositoriesAdapter(
	private val navigate: (Repository) -> Unit,
	private val onSwitch: (repository: Repository, isEnabled: Boolean) -> Boolean,
) : CursorRecyclerAdapter<RepositoriesAdapter.ViewType, RecyclerView.ViewHolder>() {
	enum class ViewType { REPOSITORY }

	private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val checkMark = itemView.findViewById<ShapeableImageView>(R.id.repository_state)!!
		val repoName = itemView.findViewById<TextView>(R.id.repository_name)!!
		val repoDesc = itemView.findViewById<TextView>(R.id.repository_description)!!

		var isEnabled = true
	}

	override val viewTypeClass: Class<ViewType>
		get() = ViewType::class.java

	override fun getItemEnumViewType(position: Int): ViewType {
		return ViewType.REPOSITORY
	}

	private fun getRepository(position: Int): Repository {
		return Database.RepositoryAdapter.transform(moveTo(position))
	}

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: ViewType,
	): RecyclerView.ViewHolder {
		return ViewHolder(parent.inflate(R.layout.repository_item)).apply {
			itemView.setOnLongClickListener {
				navigate(getRepository(absoluteAdapterPosition))
				true
			}
			itemView.setOnClickListener {
				isEnabled = !isEnabled
				onSwitch(getRepository(absoluteAdapterPosition), isEnabled)
			}
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		holder as ViewHolder
		val repository = getRepository(position)

		holder.isEnabled = repository.enabled
		holder.repoName.text = repository.name
		holder.repoDesc.text = repository.description.trim()

		if (repository.enabled) holder.checkMark.visibility = View.VISIBLE
		else holder.checkMark.visibility = View.INVISIBLE
	}
}
