package com.looker.droidify.screen

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.looker.core.common.extension.getColorFromAttr
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.TitleTextItemBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.extension.screenActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.looker.core.common.R.drawable as drawableRes
import com.looker.core.common.R.string as stringRes

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

	private var layout: LinearLayout? = null

	private val syncConnection = Connection(SyncService::class.java)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		titleBinding = TitleTextItemBinding.inflate(layoutInflater)
		syncConnection.bind(requireContext())

		lifecycleScope.launch(Dispatchers.Main) { updateRepositoryView() }

		screenActivity.onToolbarCreated(toolbar)
		collapsingToolbar.title = getString(stringRes.repository)

		toolbar.menu.apply {
			add(stringRes.edit_repository)
				.setIcon(Utils.getToolbarIcon(toolbar.context, drawableRes.ic_edit))
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
				.setOnMenuItemClickListener {
					view.post { screenActivity.navigateEditRepository(repositoryId) }
					true
				}

			add(stringRes.delete)
				.setIcon(Utils.getToolbarIcon(toolbar.context, drawableRes.ic_delete))
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
		val layout = LinearLayout(scroll.context)
		layout.orientation = LinearLayout.VERTICAL
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
			layout.addTitleText(stringRes.address, getString(stringRes.unknown))
		} else {
			layout.addTitleText(stringRes.address, repository.address)
			collapsingToolbar.title = repository.name
			layout.addTitleText(stringRes.name, repository.name)
			layout.addTitleText(
				stringRes.description,
				repository.description.replace('\n', ' ').trim()
			)
			layout.addTitleText(stringRes.recently_updated, run {
				val lastUpdated = repository.updated
				if (lastUpdated > 0L) {
					val date = Date(repository.updated)
					val format =
						if (DateUtils.isToday(date.time)) DateUtils.FORMAT_SHOW_TIME else
							DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE
					DateUtils.formatDateTime(layout.context, date.time, format)
				} else {
					getString(stringRes.unknown)
				}
			})
			if (repository.enabled && (repository.lastModified.isNotEmpty() || repository.entityTag.isNotEmpty())) {
				layout.addTitleText(
					stringRes.number_of_applications,
					Database.ProductAdapter.getCount(repository.id).toString()
				)
			}
			if (repository.fingerprint.isEmpty()) {
				if (repository.updated > 0L) {
					val builder =
						SpannableStringBuilder(getString(stringRes.repository_unsigned_DESC))
					builder.setSpan(
						ForegroundColorSpan(layout.context.getColorFromAttr(R.attr.colorError).defaultColor),
						0, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					layout.addTitleText(stringRes.fingerprint, builder)
				}
			} else {
				val fingerprint =
					SpannableStringBuilder(repository.fingerprint.windowed(2, 2, false)
						.take(32).joinToString(separator = " ") { it.uppercase(Locale.US) })
				fingerprint.setSpan(
					TypefaceSpan("monospace"), 0, fingerprint.length,
					SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				layout.addTitleText(stringRes.fingerprint, fingerprint)
			}
		}
	}

	private fun LinearLayout.addTitleText(titleResId: Int, text: CharSequence) {
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
