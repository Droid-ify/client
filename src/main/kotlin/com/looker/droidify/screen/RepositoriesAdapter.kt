package com.looker.droidify.screen

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.entity.Repository
import com.looker.droidify.utility.extension.resources.inflate
import com.looker.droidify.widget.CursorRecyclerAdapter

class RepositoriesAdapter(
    private val onClick: (Repository) -> Unit,
    private val onSwitch: (repository: Repository, isEnabled: Boolean) -> Boolean,
) :
    CursorRecyclerAdapter<RepositoriesAdapter.ViewType, RecyclerView.ViewHolder>() {
    enum class ViewType { REPOSITORY }

    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<MaterialTextView>(R.id.name)!!
        val enabled = itemView.findViewById<SwitchMaterial>(R.id.enabled)!!

        var listenSwitch = true
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
            itemView.setOnClickListener { onClick(getRepository(adapterPosition)) }
            enabled.setOnCheckedChangeListener { _, isChecked ->
                if (listenSwitch) {
                    if (!onSwitch(getRepository(adapterPosition), isChecked)) {
                        listenSwitch = false
                        enabled.isChecked = !isChecked
                        listenSwitch = true
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        val repository = getRepository(position)
        val lastListenSwitch = holder.listenSwitch
        holder.listenSwitch = false
        holder.enabled.isChecked = repository.enabled
        holder.listenSwitch = lastListenSwitch
        holder.name.text = repository.name
    }
}
