package com.looker.droidify.ui.favourites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.core.common.extension.systemBarsPadding
import com.looker.droidify.database.Database
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.extension.screenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import com.looker.core.common.R as CommonR

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
					systemBarsPadding()
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
					flowOf(Unit)
						.onCompletion { if (it == null) emitAll(Database.flowCollection(Database.Subject.Repositories)) }
						.map { Database.RepositoryAdapter.getAll(null).associateBy { it.id } }
						.collect { recyclerViewAdapter.repositories = it }
				}
			}
		}

		toolbar.title = getString(CommonR.string.favourites)
		return view
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		screenActivity.onToolbarCreated(toolbar)
	}
}