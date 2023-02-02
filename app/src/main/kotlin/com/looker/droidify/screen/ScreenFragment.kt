package com.looker.droidify.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.looker.core.common.extension.setCollapsable
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.droidify.databinding.FragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
open class ScreenFragment : Fragment() {
	private var _fragmentBinding: FragmentBinding? = null
	val fragmentBinding get() = _fragmentBinding!!

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository
	private val userPreferencesFlow get() = userPreferencesRepository.userPreferencesFlow

	lateinit var appBarLayout: AppBarLayout
	lateinit var toolbar: MaterialToolbar
	lateinit var collapsingToolbar: CollapsingToolbarLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		_fragmentBinding = FragmentBinding.inflate(layoutInflater)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View? {
		this.appBarLayout = fragmentBinding.appbarLayout
		this.toolbar = fragmentBinding.toolbar
		this.collapsingToolbar = fragmentBinding.collapsingToolbar
		lifecycleScope.launch {
			userPreferencesFlow
				.distinctMap { it.allowCollapsingToolbar }
				.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
				.stateIn(
					scope = this,
					started = SharingStarted.WhileSubscribed(5000),
					initialValue = false
				)
				.collect {
					appBarLayout.setCollapsable(it)
				}
		}
		return fragmentBinding.root
	}

	open fun onBackPressed(): Boolean = false

	override fun onDestroyView() {
		super.onDestroyView()
		_fragmentBinding = null
	}
}
