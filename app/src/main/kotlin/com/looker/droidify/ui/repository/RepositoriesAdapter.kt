package com.looker.droidify.ui.repository

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        val checkMark = itemView.repositoryState
        val repoName = itemView.repositoryName
        val repoDesc = itemView.repositoryDescription

        var isEnabled = true
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

        if (repository.enabled) {
            holder.checkMark.visibility = View.VISIBLE
        } else {
            holder.checkMark.visibility = View.INVISIBLE
        }
    }
}
