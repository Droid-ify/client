package com.looker.droidify.screen

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.TitleTextItemBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.extension.screenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class RepositoryFragment() : ScreenFragment() {

	private var titleBinding: TitleTextItemBinding? = null

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

	private var layout: LinearLayoutCompat? = null

	private val syncConnection = Connection(SyncService::class.java)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		titleBinding = TitleTextItemBinding.inflate(layoutInflater)
		syncConnection.bind(requireContext())

		lifecycleScope.launch(Dispatchers.Main) { updateRepositoryView() }

		screenActivity.onToolbarCreated(toolbar)
		collapsingToolbar.title = getString(R.string.repository)

		toolbar.menu.apply {
			add(R.string.edit_repository)
				.setIcon(Utils.getToolbarIcon(toolbar.context, R.drawable.ic_edit))
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
				.setOnMenuItemClickListener {
					view.post { screenActivity.navigateEditRepository(repositoryId) }
					true
				}

			add(R.string.delete)
				.setIcon(Utils.getToolbarIcon(toolbar.context, R.drawable.ic_delete))
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
				.setOnMenuItemClickListener {
					MessageDialog(MessageDialog.Message.DeleteRepositoryConfirm).show(
						childFragmentManager
					)
					true
				}
		}

		val content = fragmentBinding.fragmentContent
		val scroll = NestedScrollView(content.context)
		scroll.id = android.R.id.list
		scroll.isFillViewport = true
		content.addView(scroll)
		val layout = LinearLayoutCompat(scroll.context)
		layout.orientation = LinearLayoutCompat.VERTICAL
		resources.sizeScaled(8).let { layout.setPadding(0, it, 0, it) }
		this.layout = layout
		scroll.addView(
			layout,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		layout = null
		titleBinding = null
		syncConnection.unbind(requireContext())
	}

	private fun updateRepositoryView() {
		val repository = Database.RepositoryAdapter.get(repositoryId)
		val layout = layout!!
		layout.removeAllViews()
		if (repository == null) {
			layout.addTitleText(R.string.address, getString(R.string.unknown))
		} else {
			layout.addTitleText(R.string.address, repository.address)
			if (repository.updated > 0L) {
				collapsingToolbar.title = repository.name
				layout.addTitleText(R.string.name, repository.name)
				layout.addTitleText(R.string.description, repository.description.replace('\n', ' ').trim())
				layout.addTitleText(R.string.recently_updated, run {
					val lastUpdated = repository.updated
					if (lastUpdated > 0L) {
						val date = Date(repository.updated)
						val format =
							if (DateUtils.isToday(date.time)) DateUtils.FORMAT_SHOW_TIME else
								DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
						DateUtils.formatDateTime(layout.context, date.time, format)
					} else {
						getString(R.string.unknown)
					}
				})
				if (repository.enabled && (repository.lastModified.isNotEmpty() || repository.entityTag.isNotEmpty())) {
					layout.addTitleText(
						R.string.number_of_applications,
						Database.ProductAdapter.getCount(repository.id).toString()
					)
				}
			} else {
				layout.addTitleText(
					R.string.description,
					getString(R.string.repository_not_used_DESC)
				)
			}
			if (repository.fingerprint.isEmpty()) {
				if (repository.updated > 0L) {
					val builder =
						SpannableStringBuilder(getString(R.string.repository_unsigned_DESC))
					builder.setSpan(
						ForegroundColorSpan(layout.context.getColorFromAttr(R.attr.colorError).defaultColor),
						0, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					layout.addTitleText(R.string.fingerprint, builder)
				}
			} else {
				val fingerprint =
					SpannableStringBuilder(repository.fingerprint.windowed(2, 2, false)
						.take(32).joinToString(separator = " ") { it.uppercase(Locale.US) })
				fingerprint.setSpan(
					TypefaceSpan("monospace"), 0, fingerprint.length,
					SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				layout.addTitleText(R.string.fingerprint, fingerprint)
			}
		}
	}

	private fun LinearLayoutCompat.addTitleText(titleResId: Int, text: CharSequence) {
		if (text.isNotEmpty()) {
			val binding = TitleTextItemBinding.inflate(layoutInflater)
			val layout = binding.root
			val titleView = binding.title
			val textView = binding.text
			titleView.setText(titleResId)
			textView.text = text
			addView(layout)
		}
	}

	internal fun onDeleteConfirm() {
		if (syncConnection.binder?.deleteRepository(repositoryId) == true) {
			requireActivity().onBackPressed()
		}
	}
}
