package com.looker.droidify.ui.appDetail

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.compose.settings.components.toDrawableRes
import com.looker.droidify.datastore.model.CustomButton
import com.looker.droidify.datastore.model.CustomButtonIcon
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import com.google.android.material.R as MaterialR

class CustomButtonsAdapter(
    private val onButtonClick: (url: String) -> Unit,
) : RecyclerView.Adapter<CustomButtonsAdapter.ViewHolder>() {

    private var buttons: List<CustomButton> = emptyList()
    private var packageName: String = ""

    fun setButtons(buttons: List<CustomButton>, packageName: String) {
        this.buttons = buttons
        this.packageName = packageName
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = buttons.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.context as android.content.Context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val button = buttons[position]
        holder.bind(button, packageName, onButtonClick)
    }

    class ViewHolder(context: android.content.Context) : RecyclerView.ViewHolder(
        LinearLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                72.dp,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(4.dp, 8.dp, 4.dp, 8.dp)

            isClickable = true
            isFocusable = true

            val outValue = android.util.TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true
            )
            setBackgroundResource(outValue.resourceId)
        }
    ) {
        private val iconContainer: LinearLayout
        private val iconView: ImageView
        private val textView: TextView
        private val labelView: TextView

        init {
            val context = itemView.context
            val layout = itemView as LinearLayout

            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColorFromAttr(MaterialR.attr.colorPrimaryContainer).defaultColor)
            }

            iconContainer = LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
                gravity = Gravity.CENTER
                background = circleDrawable
            }

            iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp)
                imageTintList = context.getColorFromAttr(MaterialR.attr.colorOnPrimaryContainer)
            }
            iconContainer.addView(iconView)

            textView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
                setTextSizeScaled(14)
                typeface = TypefaceExtra.medium
                setTextColor(context.getColorFromAttr(MaterialR.attr.colorOnPrimaryContainer))
            }

            labelView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dp
                }
                gravity = Gravity.CENTER
                setTextSizeScaled(11)
                maxLines = 2
                setTextColor(context.getColorFromAttr(MaterialR.attr.colorOnSurfaceVariant))
            }

            layout.addView(iconContainer)
            layout.addView(labelView)
        }

        fun bind(
            button: CustomButton,
            packageName: String,
            onButtonClick: (url: String) -> Unit,
        ) {
            val context = itemView.context

            if (button.icon == CustomButtonIcon.TEXT_ONLY) {
                iconView.visibility = View.GONE
                textView.visibility = View.VISIBLE
                textView.text = button.label.take(2).uppercase()
                if (textView.parent == null) {
                    iconContainer.addView(textView)
                }
            } else {
                iconView.visibility = View.VISIBLE
                textView.visibility = View.GONE
                iconView.setImageResource(button.icon.toDrawableRes())
            }

            labelView.text = button.label

            itemView.setOnClickListener {
                val resolvedUrl = button.resolveUrl(packageName)
                onButtonClick(resolvedUrl)
            }
        }
    }
}
