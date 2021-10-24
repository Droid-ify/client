package com.looker.droidify.screen

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.inflate
import com.looker.droidify.utility.extension.resources.sizeScaled
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*

class RepositoryFragment() : ScreenFragment() {
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
    private var repositoryDisposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncConnection.bind(requireContext())
        repositoryDisposable = Observable.just(Unit)
            .concatWith(Database.observable(Database.Subject.Repository(repositoryId)))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { updateRepositoryView() }

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)!!
        screenActivity.onToolbarCreated(toolbar)
        toolbar.setTitle(R.string.repository)

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

        val content = view.findViewById<FrameLayout>(R.id.fragment_content)!!
        val scroll = NestedScrollView(content.context)
        scroll.id = android.R.id.list
        scroll.isFillViewport = true
        content.addView(
            scroll,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
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
        syncConnection.unbind(requireContext())
        repositoryDisposable?.dispose()
        repositoryDisposable = null
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
                layout.addTitleText(R.string.name, repository.name)
                layout.addTitleText(R.string.description, repository.description.replace('\n', ' '))
                layout.addTitleText(R.string.last_update, run {
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

    private fun LinearLayout.addTitleText(titleResId: Int, text: CharSequence) {
        if (text.isNotEmpty()) {
            val layout = inflate(R.layout.title_text_item)
            val titleView = layout.findViewById<TextView>(R.id.title)!!
            titleView.setText(titleResId)
            val textView = layout.findViewById<TextView>(R.id.text)!!
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
