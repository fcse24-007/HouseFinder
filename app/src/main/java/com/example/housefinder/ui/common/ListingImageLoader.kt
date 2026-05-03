package com.example.housefinder.ui.common

import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import com.example.housefinder.R

object ListingImageLoader {
    fun bind(
        imageView: ImageView,
        imagePath: String?,
        @DrawableRes fallbackResId: Int = R.drawable.ic_home
    ): Boolean {
        if (imagePath.isNullOrBlank()) {
            imageView.setImageResource(fallbackResId)
            return false
        }

        val context = imageView.context
        val parsedUri = Uri.parse(imagePath)

        if (parsedUri.scheme.isNullOrBlank()) {
            // Likely a drawable resource name from seeding
            val drawableResId = context.resources.getIdentifier(
                imagePath,
                "drawable",
                context.packageName
            )
            if (drawableResId != 0) {
                imageView.load(drawableResId) {
                    crossfade(true)
                    placeholder(fallbackResId)
                    error(fallbackResId)
                }
                return true
            }
        } else {
            // URI (content:// or file://)
            imageView.load(parsedUri) {
                crossfade(true)
                placeholder(fallbackResId)
                error(fallbackResId)
            }
            return true
        }

        imageView.setImageResource(fallbackResId)
        return false
    }
}
