package com.looker.droidify.ui.appDetail.viewHolders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.google.android.material.R
import com.google.android.material.button.MaterialButton
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.dpToPx
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.open
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class EmptyViewHolder(
    context: Context,
    callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.EmptyItem>(LinearLayout(context)) {
    private val packageName: TextView = TextView(context)
    private val repoTitle: TextView = TextView(context)
    private val repoAddress: TextView = TextView(context)
    private val copyRepoAddress: MaterialButton = MaterialButton(context)

    init {
        copyRepoAddress.setOnClickListener {
            repoAddress.text?.let { link ->
                callbacks.onRequestAddRepository(link.toString())
            }
        }

        val colorPrimary = context.getColorFromAttr(R.attr.colorPrimary)
        val colorOutLine = context.getColorFromAttr(R.attr.colorOutline)

        with(itemView as LinearLayout) {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT,
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20.dp, 20.dp, 20.dp, 20.dp)
            val imageView = ImageView(context)
            val bitmap = createBitmap(
                width = 64.dp.dpToPx.roundToInt(),
                height = 32.dp.dpToPx.roundToInt(),
            )
            val canvas = Canvas(bitmap)
            val title = TextView(context)
            with(title) {
                gravity = Gravity.CENTER
                typeface = TypefaceExtra.medium
                setTextColor(colorPrimary)
                setTextSizeScaled(20)
                setText(com.looker.droidify.R.string.application_not_found)
                setPadding(0, 12.dp, 0, 12.dp)
            }
            with(packageName) {
                gravity = Gravity.CENTER
                setTextColor(colorOutLine)
                typeface = Typeface.DEFAULT_BOLD
                setTextSizeScaled(16)
                background = context.corneredBackground
                setPadding(0, 12.dp, 0, 12.dp)
            }
            val waveHeight = 2.dp.dpToPx
            val waveWidth = 12.dp.dpToPx
            with(canvas) {
                val linePaint = Paint().apply {
                    color = colorOutLine.defaultColor
                    strokeWidth = 8f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                for (x in 12..(width - 12)) {
                    val yValue =
                        (
                            (
                                sin(x * (2f * PI / waveWidth)) *
                                    (waveHeight / (2)) +
                                    (waveHeight / 2)
                                ).toFloat() +
                                (0 - (waveHeight / 2))
                            ) + height / 2
                    drawPoint(x.toFloat(), yValue, linePaint)
                }
            }
            imageView.load(bitmap)
            with(repoTitle) {
                gravity = Gravity.CENTER
                typeface = TypefaceExtra.medium
                setTextColor(colorPrimary)
                setTextSizeScaled(20)
                setPadding(0, 0, 0, 12.dp)
            }
            with(repoAddress) {
                gravity = Gravity.CENTER
                setTextColor(colorOutLine)
                typeface = Typeface.DEFAULT_BOLD
                setTextSizeScaled(16)
                background = context.corneredBackground
                setPadding(0, 12.dp, 0, 12.dp)
            }
            with(copyRepoAddress) {
                icon = context.open
                setText(com.looker.droidify.R.string.add_repository)
                setBackgroundColor(context.getColor(android.R.color.transparent))
                setTextColor(colorPrimary)
                iconTint = colorPrimary
            }
            addView(
                title,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            addView(
                packageName,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            addView(
                imageView,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            addView(
                repoTitle,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            addView(
                repoAddress,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            addView(
                copyRepoAddress,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    override fun bindImpl(item: AppDetailItem.EmptyItem) {
        packageName.text = item.packageName
        if (item.repoAddress != null) {
            repoTitle.setText(com.looker.droidify.R.string.repository_not_found)
            repoAddress.text = item.repoAddress
        }
    }
}
