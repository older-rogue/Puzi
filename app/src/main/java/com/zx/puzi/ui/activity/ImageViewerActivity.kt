package com.zx.puzi.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityImageViewerBinding
import com.zx.puzi.ui.adapter.ImageViewerAdapter

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
                updatePageIndicator(position)
            }
        })
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
