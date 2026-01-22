package com.looker.droidify.ui.appDetail.viewHolders

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.ui.appDetail.ScreenshotsAdapter
import com.looker.droidify.utility.common.extension.dp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ScreenShotViewHolder private constructor(
    recyclerView: RecyclerView,
    coroutineScope: CoroutineScope,
    defaultDispatcher: CoroutineDispatcher,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.ScreenshotItem>(recyclerView) {

    constructor(
        context: Context,
        coroutineScope: CoroutineScope,
        defaultDispatcher: CoroutineDispatcher,
        callbacks: AppDetailAdapter.Callbacks,
    ): this(
        recyclerView = RecyclerView(context),
        coroutineScope = coroutineScope,
        defaultDispatcher = defaultDispatcher,
        callbacks = callbacks,
    )

    private val loadScope: CoroutineScope = coroutineScope + CoroutineName("Screenshots-LoadScope")

    private var updateJob: Job? = null

    private val listAdapter = ScreenshotsAdapter(
        defaultDispatcher = defaultDispatcher,
        onScreenshotClick = callbacks,
    )

    init {
        recyclerView.run {
            layoutParams = RecyclerView.LayoutParams(
                /* width = */ RecyclerView.LayoutParams.MATCH_PARENT,
                /* height = */ 166.dp,
            )

            setHasFixedSize(true)
            isNestedScrollingEnabled = false
            clipToPadding = false
            val dp8 = 8.dp
            setPadding(dp8, dp8, dp8, dp8)
            layoutManager = LinearLayoutManager(
                /* context = */ context,
                /* orientation = */ LinearLayoutManager.HORIZONTAL,
                /* reverseLayout = */ false,
            )
            adapter = listAdapter
        }
    }

    override fun bindImpl(item: AppDetailItem.ScreenshotItem) {
        updateJob?.cancel()

        updateJob = loadScope.launch {
            listAdapter.setScreenshots(item.screenshotItems)
        }
    }
}
