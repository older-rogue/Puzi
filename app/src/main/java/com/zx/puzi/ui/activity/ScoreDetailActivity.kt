package com.zx.puzi.ui.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityScoreDetailBinding
import com.zx.puzi.local.FavoritesManager
import com.zx.puzi.model.Score
import com.zx.puzi.utils.DecodeUtils
import com.zx.puzi.utils.StatusBarUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 曲谱详情页Activity
 * 显示曲谱图片、音频播放和收藏功能
 */
class ScoreDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScoreDetailBinding
    private lateinit var favoritesManager: FavoritesManager

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var scoreUrl: String = ""
    private var scoreTitle: String = ""
    private var name: String = ""
    private var isFavorite = false
    private var isLove = false
    private var mediaPlayer: MediaPlayer? = null
    private val imageUrls = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_score_detail)
        favoritesManager = FavoritesManager.getInstance(this)

        StatusBarUtil.setWhiteStatusBar(this)

        setupIntentData()
        setupUI()
        loadScoreDetail()
    }

    private fun setupIntentData() {
        scoreUrl = intent.getStringExtra("url") ?: ""
        name = intent.getStringExtra("name") ?: ""
        scoreTitle = intent.getStringExtra("title") ?: "曲谱详情"
    }

    private fun setupUI() {
        binding.scoreTitle.text = scoreTitle

        binding.scoreAuthor.apply {
            if (name.isNotEmpty()) {
                visibility = View.VISIBLE
                text = name
            } else {
                visibility = View.GONE
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        isFavorite = favoritesManager.isFavorite(scoreUrl)
        isLove = favoritesManager.isLove(scoreUrl)
        updateFavoriteButtonAppearance()
        updateLoveButtonAppearance()

        setupClickListeners()
        setupMediaPlayerControls()
    }

    private fun setupClickListeners() {
        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        binding.loveButton.setOnClickListener {
            toggleLove()
        }
    }

    private fun setupMediaPlayerControls() {
        binding.ivAction.setOnClickListener {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    binding.ivAction.setImageResource(R.drawable.start)
                } else {
                    player.start()
                    binding.ivAction.setImageResource(R.drawable.stop)
                    updateSeekBar()
                }
            }
        }

        binding.sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
                binding.tvTime.text = formatTime(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadScoreDetail() {
        if (scoreUrl.isEmpty()) {
            Toast.makeText(this, "无效的曲谱链接", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val request = Request.Builder()
            .url(scoreUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@ScoreDetailActivity,
                        "加载曲谱失败: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val html = it.body?.string() ?: ""
                    val extractedUrls = extractImageUrlsFromHtml(html)
                    val musicUrl = extractMusicUrlsFromHtml(html)

                    runOnUiThread {
                        if (musicUrl.isNotEmpty()) {
                            initMediaPlayer(musicUrl)
                            binding.llMusic.isVisible = true
                        }
                        imageUrls.clear()
                        imageUrls.addAll(extractedUrls)
                        displayScoreImages(extractedUrls)
                    }
                }
            }
        })
    }

    private fun initMediaPlayer(musicUrl: String) {
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(musicUrl)
                prepareAsync()
                setOnPreparedListener {
                    binding.sbProgress.max = duration
                    binding.tvTotalTime.text = formatTime(duration)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@ScoreDetailActivity, "音频加载失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 从HTML中提取图片URL
     */
    private fun extractImageUrlsFromHtml(html: String): List<String> {
        val imageUrls = mutableListOf<String>()

        try {
            // 提取普通图片链接
            val regex = """href=['"]((/Public/Uploads/|/data2/uploads/)[^'"]+)['"]""".toRegex()
            regex.findAll(html).forEach {
                imageUrls.add("https://www.qupu123.com${it.groupValues[1]}")
            }

            // 提取加密图片链接
            val showopernRegex = """showopern\(([^,]+),\s*"([^"]+)"\)""".toRegex()
            showopernRegex.findAll(html).forEach {
                val key = it.groupValues[1]
                val value = it.groupValues[2]
                val varRegex = """var\s+$key\s*=\s*"([^"]+)"""".toRegex()
                varRegex.findAll(html).forEach { match ->
                    val image = DecodeUtils.showdown(match.groupValues[1], value)
                    imageUrls.add(1, "https://www.qupu123.com$image")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageUrls
    }

    /**
     * 从HTML中提取音频URL
     */
    private fun extractMusicUrlsFromHtml(html: String): String {
        return try {
            val regex = """source\s+src=['"](/Public/Uploads/[^'"]+)['"]""".toRegex()
            regex.find(html)?.let {
                "https://www.qupu123.com${it.groupValues[1]}"
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 显示曲谱图片
     */
    private fun displayScoreImages(imageUrls: List<String>) {
        binding.scoreImagesContainer.removeAllViews()

        imageUrls.forEachIndexed { index, url ->
            val imageView = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            imageView.setOnClickListener {
                openImageViewer(index)
            }

            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        binding.llImg.visibility = View.VISIBLE
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progressBar.visibility = View.GONE
                        binding.llImg.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(imageView)

            binding.scoreImagesContainer.addView(imageView)
        }
    }

    /**
     * 打开图片查看器
     */
    private fun openImageViewer(position: Int) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putStringArrayListExtra("imageUrls", ArrayList(imageUrls))
            putExtra("position", position)
        }
        startActivity(intent)
    }

    /**
     * 切换收藏状态
     */
    private fun toggleFavorite() {
        if (isFavorite) {
            favoritesManager.removeFavorite(scoreUrl)
            isFavorite = false
            isLove = false
            Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
        } else {
            val score = Score(scoreTitle, scoreUrl, name)
            favoritesManager.addFavorite(score)
            isFavorite = true
            Toast.makeText(this, "已添加到收藏", Toast.LENGTH_SHORT).show()
        }
        updateFavoriteButtonAppearance()
        updateLoveButtonAppearance()
    }

    private fun updateFavoriteButtonAppearance() {
        binding.favoriteButton.setBackgroundResource(
            if (isFavorite) R.drawable.bg_round else R.drawable.bg_grew
        )
    }

    /**
     * 切换喜欢状态
     */
    private fun toggleLove() {
        val score = Score(scoreTitle, scoreUrl, name, isLove = !isLove)
        favoritesManager.addFavorite(score)

        if (isLove) {
            isLove = false
            Toast.makeText(this, "已不喜欢", Toast.LENGTH_SHORT).show()
        } else {
            isLove = true
            isFavorite = true
            Toast.makeText(this, "已设置为喜欢", Toast.LENGTH_SHORT).show()
        }

        updateLoveButtonAppearance()
        updateFavoriteButtonAppearance()
    }

    private fun updateLoveButtonAppearance() {
        binding.loveButton.setBackgroundResource(
            if (isLove) R.drawable.bg_round else R.drawable.bg_grew
        )
    }

    /**
     * 更新进度条
     */
    private fun updateSeekBar() {
        lifecycleScope.launch {
            delay(500)
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    binding.sbProgress.progress = player.currentPosition
                    binding.tvTime.text = formatTime(player.currentPosition)
                    updateSeekBar()
                }
            }
        }
    }

    /**
     * 格式化时间显示
     */
    private fun formatTime(milliseconds: Int): String {
        val format = SimpleDateFormat("mm:ss", Locale.getDefault())
        return format.format(Date(milliseconds.toLong()))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
} 