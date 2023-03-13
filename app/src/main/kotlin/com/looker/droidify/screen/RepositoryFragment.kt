package com.looker.droidify.screen

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.systemBarsPadding
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.RepositoryPageBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.extension.screenActivity
import java.util.*
import com.google.android.material.R as MaterialR
import com.looker.core.common.R.string as stringRes

class RepositoryFragment() : ScreenFragment() {

	private var _binding: RepositoryPageBinding? = null
	private val binding get() = _binding!!

	companion object {
		private const val EXTRA_REPOSITORY_ID = "repositoryId"
	}

	constructor(repositoryId: Long) : this() {
		arguments = Bundle().apply {
			putLong(EXTRA_REPOSITORY_ID, repositoryId)
		}
	}

	private val repositoryId: Long
		get() = requireArguments().getLong(EXTRA_REPOSITORY_ID)

	private var layout: LinearLayout? = null

	private val syncConnection = Connection(SyncService::class.java)

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		_binding = RepositoryPageBinding.inflate(inflater, container, false)
		syncConnection.bind(requireContext())
		screenActivity.onToolbarCreated(toolbar)
		setupView()
		toolbar.title = getString(stringRes.repository)
		val scroll = NestedScrollView(binding.root.context)
		scroll.addView(binding.root)
		scroll.systemBarsPadding()
		fragmentBinding.fragmentContent.addView(scroll)
		return fragmentBinding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()

		layout = null
		syncConnection.unbind(requireContext())
	}

	private fun setupView() {
		val repository = Database.RepositoryAdapter.get(repositoryId)
		with(binding) {

			address.title.setText(stringRes.address)
			if (repository == null) {
				address.text.text = getString(stringRes.unknown)
			} else {
				repoSwitch.isChecked = repository.enabled
				repoSwitch.setOnCheckedChangeListener { _, isChecked ->
					syncConnection.binder?.setEnabled(repository, isChecked)
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
							if (DateUtils.isToday(date.time)) DateUtils.FORMAT_SHOW_TIME else
								DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
						DateUtils.formatDateTime(requireContext(), date.time, format)
					} else getString(stringRes.unknown)
				}

				numberOfApps.title.setText(stringRes.number_of_applications)
				numberOfApps.text.text = Database.ProductAdapter.getCount(repository.id).toString()

				repoFingerprint.title.setText(stringRes.fingerprint)
				if (repository.fingerprint.isEmpty()) {
					if (repository.updated > 0L) {
						val builder =
							SpannableStringBuilder(getString(stringRes.repository_unsigned_DESC))
						builder.setSpan(
							ForegroundColorSpan(requireContext().getColorFromAttr(MaterialR.attr.colorError).defaultColor),
							0, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
						)
						repoFingerprint.text.text = builder
					}
				} else {
					val fingerprint =
						SpannableStringBuilder(repository.fingerprint.windowed(2, 2, false)
							.take(32).joinToString(separator = " ") { it.uppercase(Locale.US) })
					fingerprint.setSpan(
						TypefaceSpan("monospace"), 0, fingerprint.length,
						SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					repoFingerprint.text.text = fingerprint
				}
			}

			editRepoButton.setOnClickListener {
				screenActivity.navigateEditRepository(repositoryId)
			}

			deleteRepoButton.setOnClickListener {
				MessageDialog(
					MessageDialog.Message.DeleteRepositoryConfirm
				).show(childFragmentManager)
			}
		}
	}

	internal fun onDeleteConfirm() {
		if (syncConnection.binder?.deleteRepository(repositoryId) == true) {
			requireActivity().onBackPressed()
		}
	}
}
