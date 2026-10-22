package com.example.mybudgettree

import android.content.Context
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat

/**
 * Populates a [GridLayout] with one tappable button per [IconCatalog] entry, highlighting
 * whichever key is currently selected. Shared by the category edit screen and the
 * new savings goal dialog so both icon pickers look and behave the same way.
 */
object IconPicker {
    /**
     * @param container The grid to fill
     * @param initialKey The icon that starts selected, or null for the default
     * @param rows Lay the icons out in this many rows and as many columns as that takes, for a grid that scrolls
     *   sideways. Null keeps four columns, however many rows that needs
     * @param onSelect Called with the key of the icon the user taps
     */
    fun populate(container: GridLayout, initialKey: String?, rows: Int? = null, onSelect: (String) -> Unit) {
        val context = container.context
        container.removeAllViews()
        container.columnCount = if (rows == null) 4 else (IconCatalog.icons.size + rows - 1) / rows
        var selected = initialKey ?: IconCatalog.DEFAULT_KEY
        val buttons = mutableMapOf<String, ImageButton>()

        fun highlight() {
            buttons.forEach { (key, button) ->
                val isSelected = key == selected
                button.setBackgroundResource(if (isSelected) R.drawable.bg_goal_tile_dark else R.drawable.bg_category_tile)
                button.setColorFilter(
                    ContextCompat.getColor(context, if (isSelected) android.R.color.white else R.color.green_text)
                )
            }
        }

        IconCatalog.icons.forEach { icon ->
            val size = dp(context, 60)
            val margin = dp(context, 6)
            val padding = dp(context, 14)
            val button = ImageButton(context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                setImageResource(icon.res)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(padding, padding, padding, padding)
                contentDescription = icon.key
            }
            buttons[icon.key] = button
            container.addView(button)
            button.setOnClickListener {
                selected = icon.key
                highlight()
                onSelect(icon.key)
            }
        }
        highlight()
    }

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
