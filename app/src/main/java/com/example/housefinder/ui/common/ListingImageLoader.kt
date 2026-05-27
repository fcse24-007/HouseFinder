package com.example.housefinder.ui.common

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.load
import com.example.housefinder.R
import com.facebook.shimmer.ShimmerFrameLayout

object ListingImageLoader {
    fun bind(
        imageView: ImageView,
        imagePath: String?,
        @DrawableRes fallbackResId: Int = R.drawable.listing_photo_1,
        shimmerLayout: ShimmerFrameLayout? = null
    ): Boolean {
        showShimmer(shimmerLayout)
        if (imagePath.isNullOrBlank()) {
            imageView.setImageResource(fallbackResId)
            hideShimmer(shimmerLayout)
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
                    listener(
                        onSuccess = { _, _ -> hideShimmer(shimmerLayout) },
                        onError = { _, _ -> hideShimmer(shimmerLayout) }
                    )
                }
                return true
            }
        } else {
            // URI (content:// or file://)
            imageView.load(parsedUri) {
                crossfade(true)
                placeholder(fallbackResId)
                error(fallbackResId)
                listener(
                    onSuccess = { _, _ -> hideShimmer(shimmerLayout) },
                    onError = { _, _ -> hideShimmer(shimmerLayout) }
                )
            }
            return true
        }

        imageView.setImageResource(fallbackResId)
        hideShimmer(shimmerLayout)
        return false
    }

    private fun showShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.let {
            it.visibility = View.VISIBLE
            it.startShimmer()
        }
    }

    private fun hideShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.let {
            it.stopShimmer()
            it.visibility = View.GONE
        }
    }
}
