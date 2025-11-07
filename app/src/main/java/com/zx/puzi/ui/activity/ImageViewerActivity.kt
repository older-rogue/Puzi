package com.zx.puzi.ui.activity

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityImageViewerBinding
import com.zx.puzi.ui.adapter.ImageViewerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var adapter: ImageViewerAdapter
    private var imageUrls: ArrayList<String> = arrayListOf()
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_viewer)

        hideSystemUI()

        imageUrls = intent.getStringArrayListExtra("imageUrls") ?: arrayListOf()
        currentPosition = intent.getIntExtra("position", 0)

        setupViewPager()
        updatePageIndicator(currentPosition)
        setupDownloadButton()
    }

    private fun setupViewPager() {
        adapter = ImageViewerAdapter(imageUrls) {
            finish()
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updatePageIndicator(position)
            }
        })
    }

    private fun setupDownloadButton() {
        binding.downloadButton.setOnClickListener {
            downloadCurrentImage()
        }
    }

    private fun downloadCurrentImage() {
        if (imageUrls.isEmpty()) return

        val imageUrl = imageUrls[currentPosition]
        
        lifecycleScope.launch {
            try {
                binding.downloadButton.isEnabled = false
                Toast.makeText(this@ImageViewerActivity, "正在下载...", Toast.LENGTH_SHORT).show()

                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(this@ImageViewerActivity)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                }

                saveImageToGallery(bitmap)
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ImageViewerActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.downloadButton.isEnabled = true
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Puzi")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePageIndicator(position: Int) {
        binding.pageIndicator.text = "${position + 1}/${imageUrls.size}"
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
