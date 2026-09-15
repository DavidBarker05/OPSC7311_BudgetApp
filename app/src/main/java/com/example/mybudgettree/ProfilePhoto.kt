package com.example.mybudgettree

import android.widget.ImageView

object ProfilePhoto {
    suspend fun bind(imageView: ImageView, path: String?, app: BudgetTreeApplication) {
        val bitmap = app.imageStorageSystem.loadImage(path)
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setPadding(0, 0, 0, 0)
        } else {
            val padding = (28 * imageView.resources.displayMetrics.density).toInt()
            imageView.setImageResource(R.drawable.ic_nav_profile)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setPadding(padding, padding, padding, padding)
        }
    }
}
