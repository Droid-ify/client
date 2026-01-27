package com.looker.droidify.ui.repository

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.looker.droidify.R
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.systemBarsMargin
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.utility.extension.mainActivity
import com.looker.droidify.widget.addDivider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RepositoriesFragment : ScreenFragment() {

    private val viewModel: RepositoriesViewModel by viewModels()

    private var _binding: RecyclerViewWithFabBinding? = null
    private val binding get() = _binding!!

    private var reposAdapter: RepositoriesAdapter? = null

    private val syncConnection = Connection(SyncService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)
        _binding = binding

        val reposAdapter = RepositoriesAdapter(
            navigate = { mainActivity.navigateRepository(it.id) }
        ) { repository, isEnabled ->
            repository.enabled != isEnabled &&
                syncConnection.binder?.setEnabled(repository, isEnabled) == true
        }
        this.reposAdapter = reposAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.listFlow.collect {
                    reposAdapter.submitData(it)
                }
            }
        }

        val view = fragmentBinding.root.apply {
            binding.scrollUp.apply {
                setIconResource(R.drawable.ic_add)
                setText(R.string.add_repository)
                setOnClickListener { mainActivity.navigateAddRepository() }
                systemBarsMargin(16.dp)
            }
            binding.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                isMotionEventSplittingEnabled = false
                setHasFixedSize(true)
                adapter = reposAdapter
                addDivider { _, _, configuration ->
                    configuration.set(
                        needDivider = true,
                        toTop = false,
                        paddingStart = 16.dp,
                        paddingEnd = 16.dp
                    )
                }
                systemBarsPadding()
            }
            fragmentBinding.fragmentContent.addView(binding.root)
        }
        handleFab()
        return view
    }

    private fun handleFab() {
        binding.recyclerView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                binding.scrollUp.shrink()
            } else {
                binding.scrollUp.extend()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncConnection.bind(requireContext())
        mainActivity.onToolbarCreated(toolbar)
        toolbar.title = getString(R.string.repositories)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        syncConnection.unbind(requireContext())
    }
}
