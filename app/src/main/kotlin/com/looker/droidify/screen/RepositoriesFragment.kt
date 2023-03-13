package com.looker.droidify.screen

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.systemBarsMargin
import com.looker.core.common.extension.systemBarsPadding
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.extension.screenActivity
import com.looker.droidify.widget.DividerItemDecoration
import com.looker.core.common.R as CommonR

class RepositoriesFragment : ScreenFragment(), CursorOwner.Callback {

	private var _binding: RecyclerViewWithFabBinding? = null
	private val binding get() = _binding!!

	private val syncConnection = Connection(SyncService::class.java)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		_binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)
		val view = fragmentBinding.root.apply {
			binding.scrollUp.apply {
				setIconResource(CommonR.drawable.ic_add)
				setText(CommonR.string.add_repository)
				setOnClickListener { screenActivity.navigateAddRepository() }
				systemBarsMargin(16.dp)
			}
			binding.recyclerView.apply {
				layoutManager = LinearLayoutManager(context)
				isMotionEventSplittingEnabled = false
				setHasFixedSize(true)
				adapter = RepositoriesAdapter(
					navigate = { screenActivity.navigateRepository(it.id) }
				) { repository, isEnabled ->
					repository.enabled != isEnabled &&
							syncConnection.binder?.setEnabled(repository, isEnabled) == true
				}
				addItemDecoration(
					DividerItemDecoration(context) { _, _, configuration ->
						val padding = context.resources.sizeScaled(16)
						configuration.set(
							needDivider = true,
							toTop = false,
							paddingStart = padding,
							paddingEnd = padding
						)
					}
				)
				systemBarsPadding()
			}
			fragmentBinding.fragmentContent.addView(binding.root)
		}
		handleFab()
		return view
	}

	private fun handleFab() {
		binding.recyclerView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
			if (scrollY > oldScrollY) binding.scrollUp.shrink()
			else binding.scrollUp.extend()
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		syncConnection.bind(requireContext())
		screenActivity.cursorOwner.attach(this, CursorOwner.Request.Repositories)
		screenActivity.onToolbarCreated(toolbar)
		toolbar.title = getString(CommonR.string.repositories)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
		syncConnection.unbind(requireContext())
		screenActivity.cursorOwner.detach(this)
	}

	override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
		(binding.recyclerView.adapter as RepositoriesAdapter).cursor = cursor
	}
}
