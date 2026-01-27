package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.looker.droidify.R
import com.looker.droidify.network.percentBy
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById

class DownloadStatusViewHolder(
    itemView: View
) : BaseViewHolder<AppDetailItem.DownloadStatusItem>(itemView) {
    private val statusText: TextView = itemView.compatRequireViewById(R.id.status)
    private val progress: LinearProgressIndicator = itemView.compatRequireViewById(R.id.progress)

    override fun bindImpl(item: AppDetailItem.DownloadStatusItem) {
        val status = item.status
        itemView.isVisible = status != AppDetailAdapter.Status.Idle
        statusText.isVisible = status != AppDetailAdapter.Status.Idle
        progress.isVisible = status != AppDetailAdapter.Status.Idle
        if (status != AppDetailAdapter.Status.Idle) {
            when (status) {
                is AppDetailAdapter.Status.Pending -> {
                    statusText.setText(R.string.waiting_to_start_download)
                    progress.isIndeterminate = true
                }

                is AppDetailAdapter.Status.Connecting -> {
                    statusText.setText(R.string.connecting)
                    progress.isIndeterminate = true
                }

                is AppDetailAdapter.Status.Downloading -> {
                    statusText.text = statusText.context.getString(
                        R.string.downloading_FORMAT,
                        if (status.total == null) {
                            status.read.toString()
                        } else {
                            "${status.read} / ${status.total}"
                        },
                    )
                    progress.isIndeterminate = status.total == null
                    if (status.total != null) {
                        progress.setProgressCompat(
                            status.read.value percentBy status.total.value,
                            true,
                        )
                    }
                }

                AppDetailAdapter.Status.Installing -> {
                    statusText.setText(R.string.installing)
                    progress.isIndeterminate = true
                }

                AppDetailAdapter.Status.PendingInstall -> {
                    statusText.setText(R.string.waiting_to_start_installation)
                    progress.isIndeterminate = true
                }

                AppDetailAdapter.Status.Idle -> {}
            }
        }
    }
}
