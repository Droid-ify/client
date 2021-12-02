package com.looker.droidify.screen

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.entity.Repository
import com.looker.droidify.utility.extension.resources.clear
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.inflate
import com.looker.droidify.widget.CursorRecyclerAdapter

class RepositoriesAdapter(
    private val onClick: (Repository) -> Unit,
    private val onSwitch: (repository: Repository, isEnabled: Boolean) -> Boolean,
) :
    CursorRecyclerAdapter<RepositoriesAdapter.ViewType, RecyclerView.ViewHolder>() {
    enum class ViewType { REPOSITORY }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val item = itemView.findViewById<MaterialCardView>(R.id.repository_item)!!
        val checkMark = itemView.findViewById<ShapeableImageView>(R.id.repository_state)!!
        val repoName = itemView.findViewById<MaterialTextView>(R.id.repository_name)!!
        val repoDesc = itemView.findViewById<MaterialTextView>(R.id.repository_description)!!

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
                onClick(getRepository(adapterPosition))
                true
            }
            itemView.setOnClickListener {
                isEnabled = !isEnabled
                onSwitch(getRepository(adapterPosition), isEnabled)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        val repository = getRepository(position)
        holder.repoName.text = repository.name
        holder.repoDesc.text = repository.description.trim()
        if (repository.enabled) {
            holder.isEnabled = true
            holder.item.setCardBackgroundColor(
                holder.item.context.getColorFromAttr(R.attr.colorPrimaryContainer)
            )
            holder.repoName.setTextColor(holder.repoName.context.getColorFromAttr(R.attr.colorOnPrimaryContainer))
            holder.repoDesc.setTextColor(holder.repoDesc.context.getColorFromAttr(R.attr.colorOnPrimaryContainer))
            holder.checkMark.load(R.drawable.ic_check)
            holder.checkMark.imageTintList =
                holder.checkMark.context.getColorFromAttr(R.attr.colorOnPrimaryContainer)
        } else {
            holder.isEnabled = false
            holder.item.setCardBackgroundColor(holder.item.context.getColorFromAttr(android.R.attr.colorBackground))
            holder.repoName.setTextColor(holder.repoName.context.getColorFromAttr(R.attr.colorOnBackground))
            holder.repoDesc.setTextColor(holder.repoDesc.context.getColorFromAttr(R.attr.colorOnBackground))
            holder.checkMark.clear()
            holder.checkMark.imageTintList =
                holder.checkMark.context.getColorFromAttr(R.attr.colorOnBackground)
        }
    }
}
