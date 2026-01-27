package com.looker.droidify.ui.repository

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.looker.droidify.R
import com.looker.droidify.model.Repository
import com.looker.droidify.databinding.RepositoryItemBinding
import com.looker.droidify.ui.repository.RepositoriesAdapter.ViewHolder

class RepositoriesAdapter(
    private val navigate: RepositoryNavigateListener,
    private val onSwitch: RepositorySwitchListener
) : PagingDataAdapter<Repository, ViewHolder>(ItemCallback()) {

    fun interface RepositoryNavigateListener {
        fun onRepositoryNavigate(repository: Repository)
    }

    fun interface RepositorySwitchListener {
        fun onRepositorySwitch(repository: Repository, isEnabled: Boolean): Boolean
    }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    class ViewHolder(
        binding: RepositoryItemBinding,
        private val navigate: RepositoryNavigateListener,
        private val onSwitch: RepositorySwitchListener
    ) : RecyclerView.ViewHolder(binding.root) {
        private val repoIcon = binding.repositoryIcon
        private val repoName = binding.repositoryName
        private val repoDesc = binding.repositoryDescription
        private val repoState = binding.repositoryState

        private var isChecked = false

        private val colorOnSurface = ColorStateList.valueOf(
            MaterialColors.getColor(
                itemView,
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK,
            )
        )

        private val colorSurfaceContainer = ColorStateList.valueOf(
            MaterialColors.getColor(
                itemView,
                com.google.android.material.R.attr.colorSurfaceContainer,
                Color.WHITE,
            )
        )

        private lateinit var repository: Repository

        init {
            itemView.setOnClickListener {
                navigate.onRepositoryNavigate(repository)
            }

            repoState.setOnClickListener {
                isChecked = !isChecked
                onSwitch.onRepositorySwitch(repository, isChecked)
            }
        }

        fun bind(repository: Repository) {
            this.repository = repository

            repoName.text = repository.name
            repoDesc.text = repository.description.trim()

            isChecked = repository.enabled
            val repoState = repoState
            if (isChecked) {
                repoState.setImageResource(R.drawable.ic_check)
                repoState.imageTintList = colorSurfaceContainer
                repoState.backgroundTintList = colorOnSurface
            } else {
                repoState.setImageResource(R.drawable.ic_cancel)
                repoState.imageTintList = colorOnSurface
                repoState.backgroundTintList = colorSurfaceContainer
            }

            // TODO: fetch repo icon
            repoIcon.setImageIcon(null)
            repoIcon.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            binding = RepositoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            navigate = navigate,
            onSwitch = onSwitch,
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    private class ItemCallback: DiffUtil.ItemCallback<Repository>() {
        override fun areItemsTheSame(
            oldItem: Repository,
            newItem: Repository,
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Repository,
            newItem: Repository,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
