package com.looker.droidify.ui.repository

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.looker.droidify.R
import com.looker.droidify.model.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.RepositoryItemBinding
import com.looker.droidify.widget.CursorRecyclerAdapter

class RepositoriesAdapter(
    private val navigate: (Repository) -> Unit,
    private val onSwitch: (repository: Repository, isEnabled: Boolean) -> Boolean
) : CursorRecyclerAdapter<RepositoriesAdapter.ViewType, RecyclerView.ViewHolder>() {
    enum class ViewType { REPOSITORY }

    private class ViewHolder(itemView: RepositoryItemBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val repoIcon = itemView.repositoryIcon
        val repoName = itemView.repositoryName
        val repoDesc = itemView.repositoryDescription
        val repoState = itemView.repositoryState

        var isChecked = false
    }

    override val viewTypeClass: Class<ViewType>
        get() = ViewType::class.java

    override fun getItemEnumViewType(position: Int): ViewType {
        return ViewType.REPOSITORY
    }

    private fun getRepository(position: Int): Repository {
        return Database.RepositoryAdapter.transform(moveTo(position.takeUnless { it < 0 } ?: 0))
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: ViewType
    ): RecyclerView.ViewHolder {
        return ViewHolder(
            RepositoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ).apply {
            itemView.setOnClickListener {
                navigate(getRepository(absoluteAdapterPosition))
            }

            repoState.setOnClickListener {
                isChecked = !isChecked
                onSwitch(getRepository(absoluteAdapterPosition), isChecked)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        val repository = getRepository(position)

        holder.repoName.text = repository.name
        holder.repoDesc.text = repository.description.trim()

        val colorOnSurface = MaterialColors.getColor(
            holder.itemView, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        val colorSurfaceContainer = MaterialColors.getColor(
            holder.itemView, com.google.android.material.R.attr.colorSurfaceContainer, Color.WHITE)

        holder.isChecked = repository.enabled
        if (holder.isChecked) {
            holder.repoState.setImageResource(R.drawable.ic_check)
            holder.repoState.imageTintList = ColorStateList.valueOf(colorSurfaceContainer)
            holder.repoState.backgroundTintList = ColorStateList.valueOf(colorOnSurface)
        } else {
            holder.repoState.setImageResource(R.drawable.ic_cancel)
            holder.repoState.imageTintList = ColorStateList.valueOf(colorOnSurface)
            holder.repoState.backgroundTintList = ColorStateList.valueOf(colorSurfaceContainer)
        }

        // TODO: fetch repo icon
        holder.repoIcon.setImageIcon(null)
        holder.repoIcon.visibility = View.GONE
    }
}
