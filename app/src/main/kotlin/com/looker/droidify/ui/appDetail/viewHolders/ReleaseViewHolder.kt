package com.looker.droidify.ui.appDetail.viewHolders

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.format.DateFormat
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil3.load
import com.looker.droidify.R
import com.looker.droidify.data.local.model.Reproducible
import com.looker.droidify.model.Release
import com.looker.droidify.network.DataSize
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailAdapter.Status
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.copyLinkToClipboard
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.sdkName
import com.looker.droidify.utility.extension.android.Android
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ReleaseViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.ReleaseItem>(itemView), View.OnClickListener, View.OnLongClickListener {
    private val dateFormat: java.text.DateFormat = DateFormat.getDateFormat(itemView.context)!!

    private val version: TextView = itemView.compatRequireViewById(R.id.version)
    private val installationStatus: TextView = itemView.compatRequireViewById(R.id.installation_status)
    private val rbBadge: ImageView = itemView.compatRequireViewById(R.id.rb_badge)
    private val source: TextView = itemView.compatRequireViewById(R.id.source)
    private val added: TextView = itemView.compatRequireViewById(R.id.added)
    private val size: TextView = itemView.compatRequireViewById(R.id.size)
    private val signature: TextView = itemView.compatRequireViewById(R.id.signature)
    private val compatibility: TextView = itemView.compatRequireViewById(R.id.compatibility)
    private val sdkVer: TextView = itemView.compatRequireViewById(R.id.sdk_ver)

    private val statefulViews: Array<View>
        get() = arrayOf(
            itemView,
            version,
            installationStatus,
            source,
            added,
            size,
            signature,
            compatibility,
            sdkVer,
        )

    var status: Status = Status.Idle

    init {
        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            callbacks.onReleaseClick(currentItem.release)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        return if (v == itemView) {
            val releaseItem = currentItem
            itemView.copyLinkToClipboard(
                releaseItem.release.getDownloadUrl(releaseItem.repository),
            )
            true
        } else {
            false
        }
    }

    override fun bindImpl(item: AppDetailItem.ReleaseItem) {
        val context = itemView.context

        val release = item.release
        val incompatibility = item.incompatibility
        val singlePlatform = item.singlePlatform
        val installed = item.installed
        val suggested = item.suggested

        if (suggested) {
            itemView.apply {
                background = context.corneredBackground
                backgroundTintList =
                    itemView.context.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
            }
        } else {
            itemView.background = null
        }
        version.text = context.getString(R.string.version_FORMAT, release.version)

        with(installationStatus) {
            isVisible = installed || suggested
            setText(
                when {
                    installed -> R.string.installed
                    suggested -> R.string.suggested
                    else -> R.string.unknown
                },
            )
            background = context.corneredBackground
            setPadding(15, 15, 15, 15)
            if (installed) {
                backgroundTintList = context.getColorFromAttr(com.google.android.material.R.attr.colorSecondaryContainer)
                setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorOnSecondaryContainer))
            } else {
                backgroundTintList = context.getColorFromAttr(com.google.android.material.R.attr.colorPrimaryContainer)
                setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorOnPrimaryContainer))
            }
        }

        when (item.reproducible) {
            Reproducible.TRUE -> {
                rbBadge.isVisible = true
                rbBadge.load(R.drawable.ic_verified)
            }

            Reproducible.FALSE -> {
                rbBadge.isVisible = true
                rbBadge.load(R.drawable.ic_verified_off)
            }

            Reproducible.UNKNOWN -> {
                rbBadge.isVisible = true
                rbBadge.load(R.drawable.ic_verified_off)
                rbBadge.imageTintList = context.getColorFromAttr(com.google.android.material.R.attr.colorTertiary)
            }

            Reproducible.NO_DATA -> rbBadge.isGone = true
        }

        source.text = context.getString(R.string.provided_by_FORMAT, item.repository.name)
        added.text = formatAddedDate(release)
        size.text = DataSize(release.size).toString()

        val signatureIsVisible = item.showSignature && release.signature.isNotEmpty()
        signature.isVisible = signatureIsVisible
        signature.text = if (signatureIsVisible) {
            formatSignature(context, release.signature)
        } else {
            null
        }

        with(compatibility) {
            isVisible = incompatibility != null || singlePlatform != null
            if (incompatibility != null) {
                setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorErrorContainer))
                text = when (incompatibility) {
                    is Release.Incompatibility.MinSdk,
                    is Release.Incompatibility.MaxSdk,
                        -> context.getString(
                        R.string.incompatible_with_FORMAT,
                        Android.name,
                    )

                    is Release.Incompatibility.Platform -> context.getString(
                        R.string.incompatible_with_FORMAT,
                        Android.primaryPlatform ?: context.getString(R.string.unknown),
                    )

                    is Release.Incompatibility.Feature -> context.getString(
                        R.string.requires_FORMAT,
                        incompatibility.feature,
                    )
                }
            } else if (singlePlatform != null) {
                setTextColor(context.getColorFromAttr(android.R.attr.textColorSecondary))
                text = context.getString(
                    R.string.only_compatible_with_FORMAT,
                    singlePlatform,
                )
            }
        }

        sdkVer.text = formatRelease(context, release)
        val enabled = status == Status.Idle
        statefulViews.forEach { it.isEnabled = enabled }
    }

    private fun formatAddedDate(release: Release): String? {
        val instant = Instant.fromEpochMilliseconds(release.added)
        // FDroid uses UTC time
        val date = instant.toLocalDateTime(TimeZone.UTC)
        val dateFormat = try {
            date.toJavaLocalDateTime()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        } catch (e: Exception) {
            e.printStackTrace()
            dateFormat.format(release.added)
        }
        return dateFormat
    }

    private fun formatRelease(context: Context, release: Release): String {
        val release = release
        val targetSdkVersion = sdkName.getOrDefault(
            release.targetSdkVersion,
            context.getString(
                R.string.label_unknown_sdk,
                release.targetSdkVersion,
            ),
        )
        val minSdkVersion = sdkName.getOrDefault(
            release.minSdkVersion,
            context.getString(
                R.string.label_unknown_sdk,
                release.minSdkVersion,
            ),
        )
        return context.getString(
            R.string.label_sdk_version,
            targetSdkVersion,
            minSdkVersion,
        )
    }

    private fun formatSignature(context: Context, signature: String): CharSequence {
        val bytes = signature
            .uppercase(Locale.US)
            .windowed(2, 2, false)
            .take(8)
        val signature = bytes.joinToString(separator = " ")
        val builder = SpannableStringBuilder(
            context.getString(
                R.string.signature_FORMAT,
                signature,
            ),
        )
        val index = builder.indexOf(signature)
        if (index >= 0) {
            bytes.forEachIndexed { i, _ ->
                builder.setSpan(
                    TypefaceSpan("monospace"),
                    index + 3 * i,
                    index + 3 * i + 2,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        return builder
    }
}
