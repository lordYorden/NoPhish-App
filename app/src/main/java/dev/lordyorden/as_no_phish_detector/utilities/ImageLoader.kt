package dev.lordyorden.tradely.utilities

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import dev.lordyorden.as_no_phish_detector.R
import java.lang.ref.WeakReference


class ImageLoader private constructor(context: Context) {

    companion object {

        @Volatile
        private var instance: ImageLoader? = null

        fun getInstance(): ImageLoader {
            return instance
                ?: throw IllegalStateException("ImageLoader must be initialized by calling init(context) before use")
        }

        fun init(context: Context): ImageLoader {
            return instance ?: synchronized(this) {
                instance ?: ImageLoader(context).also { instance = it }
            }
        }
    }

    private val contextRef = WeakReference(context)

    fun loadAppIcon(
        packageName: String,
        imageView: AppCompatImageView,
        placeholder: Int = R.drawable.ic_whatsapp
    ) {
        contextRef.get()?.let { context ->
            try {
                val icon: Drawable? = context.packageManager.getApplicationIcon(packageName)
                imageView.setImageDrawable(icon)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                imageView.setImageResource(placeholder)
            }
        }
    }

/*    fun loadImage(
        source: Drawable,
        imageView: AppCompatImageView,
        placeholder: Int = R.drawable.unavailable_photo
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .into(imageView)
        }
    }

    fun loadImage(
        source: String,
        imageView: AppCompatImageView,
        placeholder: Int = R.drawable.unavailable_photo
    ) {
        contextRef.get()?.let { context ->
            Glide
                .with(context)
                .load(source)
                .centerCrop()
                .placeholder(placeholder)
                .into(imageView)
        }
    }*/

}