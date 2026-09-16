package com.example.mybudgettree

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

/**
 * Shows a single receipt/proof image full-screen, read-only
 */
class ViewImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        setContentView(R.layout.activity_view_image)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.viewImageRoot)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val path = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val bitmap = path?.let { BitmapFactory.decodeFile(it) }
        if (bitmap == null) {
            finish()
            return
        }
        findViewById<ImageView>(R.id.ivFullImage).setImageBitmap(bitmap)
    }
}
