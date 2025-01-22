package com.looker.droidify.ui.favourites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.database.Database
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.extension.screenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavouritesFragment : ScreenFragment() {

    private val viewModel: FavouritesViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: FavouriteFragmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = fragmentBinding.root.apply {
            val content = fragmentBinding.fragmentContent
            content.addView(
                RecyclerView(content.context).apply {
                    id = android.R.id.list
                    layoutManager = LinearLayoutManager(context)
                    isVerticalScrollBarEnabled = false
                    setHasFixedSize(true)
                    recyclerViewAdapter =
                        FavouriteFragmentAdapter { screenActivity.navigateProduct(it) }
                    this.adapter = recyclerViewAdapter
                    systemBarsPadding(includeFab = false)
                    recyclerView = this
                }
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.favouriteApps.collect { apps ->
                        recyclerViewAdapter.apps = apps
                    }
                }
                launch {
                    Database.RepositoryAdapter
                        .getAllStream()
                        .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
                        .collectLatest { repositories ->
                            recyclerViewAdapter.repositories = repositories.associateBy { it.id }
                        }
                }
            }
        }

        toolbar.title = getString(R.string.favourites)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screenActivity.onToolbarCreated(toolbar)
    }
}
