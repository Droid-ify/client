package com.looker.droidify.ui.repository

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.systemBarsPadding
import com.looker.droidify.model.Repository
import com.looker.droidify.databinding.RepositoryPageBinding
import com.looker.droidify.ui.Message
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.extension.screenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import com.google.android.material.R as MaterialR
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class RepositoryFragment() : ScreenFragment() {

    private var _binding: RepositoryPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RepositoryViewModel by viewModels()

    constructor(repositoryId: Long) : this() {
        arguments = bundleOf(RepositoryViewModel.ARG_REPO_ID to repositoryId)
    }

    private var layout: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = RepositoryPageBinding.inflate(inflater, container, false)
        viewModel.bindService(requireContext())
        screenActivity.onToolbarCreated(toolbar)
        toolbar.title = getString(stringRes.repository)
        val scroll = NestedScrollView(binding.root.context)
        scroll.addView(binding.root)
        scroll.systemBarsPadding()
        fragmentBinding.fragmentContent.addView(scroll)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collectLatest {
                    setupView(it.repo, it.appCount)
                }
            }
        }
        return fragmentBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        layout = null
        viewModel.unbindService(requireContext())
    }

    private fun setupView(repository: Repository?, appCount: Int) {
        with(binding) {
            address.title.setText(stringRes.address)
            if (repository == null) {
                address.text.text = getString(stringRes.unknown)
            } else {
                repoSwitch.isChecked = repository.enabled
                repoSwitch.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.enabledRepository(isChecked)
                }

                address.text.text = repository.address
                toolbar.title = repository.name
                repoName.title.setText(stringRes.name)
                repoName.text.text = repository.name

                repoDescription.title.setText(stringRes.description)
                repoDescription.text.text = repository.description.replace('\n', ' ').trim()

                recentlyUpdated.title.setText(stringRes.recently_updated)
                recentlyUpdated.text.text = run {
                    val lastUpdated = repository.updated
                    if (lastUpdated > 0L) {
                        val date = Date(repository.updated)
                        val format =
                            if (DateUtils.isToday(date.time)) {
                                DateUtils.FORMAT_SHOW_TIME
                            } else {
                                DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
                            }
                        DateUtils.formatDateTime(requireContext(), date.time, format)
                    } else {
                        getString(stringRes.unknown)
                    }
                }

                numberOfApps.title.setText(stringRes.number_of_applications)
                numberOfApps.text.text = appCount.toString()

                repoFingerprint.title.setText(stringRes.fingerprint)
                if (repository.fingerprint.isEmpty()) {
                    if (repository.updated > 0L) {
                        val builder =
                            SpannableStringBuilder(getString(stringRes.repository_unsigned_DESC))
                        builder.setSpan(
                            ForegroundColorSpan(
                                requireContext()
                                    .getColorFromAttr(MaterialR.attr.colorError)
                                    .defaultColor
                            ),
                            0,
                            builder.length,
                            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        repoFingerprint.text.text = builder
                    }
                } else {
                    val fingerprint =
                        SpannableStringBuilder(
                            repository.fingerprint.windowed(2, 2, false)
                                .take(32).joinToString(separator = " ") { it.uppercase(Locale.US) }
                        )
                    fingerprint.setSpan(
                        TypefaceSpan("monospace"),
                        0,
                        fingerprint.length,
                        SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    repoFingerprint.text.text = fingerprint
                }
            }

            editRepoButton.setOnClickListener {
                screenActivity.navigateEditRepository(viewModel.id)
            }

            deleteRepoButton.setOnClickListener {
                MessageDialog(
                    Message.DeleteRepositoryConfirm
                ).show(childFragmentManager)
            }
        }
    }

    internal fun onDeleteConfirm() {
        viewModel.deleteRepository(
            onDelete = { requireActivity().onBackPressedDispatcher.onBackPressed() }
        )
    }
}
