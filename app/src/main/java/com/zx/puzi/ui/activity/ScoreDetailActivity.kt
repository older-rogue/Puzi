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
import java.io.File
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

        // 先尝试从本地缓存读取 HTML，加快页面展示速度
        val cachedHtml = loadHtmlFromCache(scoreUrl)
        if (!cachedHtml.isNullOrEmpty()) {
            processHtml(cachedHtml)
            // 如果已有缓存，根据需求这里就不再发起网络请求
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val request = Request.Builder()
            .url(scoreUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (isFinishing || isDestroyed) {
                    return
                }
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
                response.use { resp ->
                    // 仅在 HTTP 成功时继续处理
                    if (!resp.isSuccessful) {
                        if (isFinishing || isDestroyed) {
                            return
                        }
                        runOnUiThread {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@ScoreDetailActivity,
                                "加载曲谱失败: ${resp.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return
                    }

                    val html = resp.body?.string().orEmpty()
                    // 网络成功后写入缓存
                    if (html.isNotEmpty()) {
                        saveHtmlToCache(scoreUrl, html)
                    }
                    processHtml(html)
                }
            }
        })
    }

    /**
     * 统一处理 HTML 内容：提取图片、音频并更新 UI
     */
    private fun processHtml(html: String) {
        val extractedUrls = extractImageUrlsFromHtml(html)
        val musicUrl = extractMusicUrlsFromHtml(html)

        runOnUiThread {
            if (isFinishing || isDestroyed) {
                return@runOnUiThread
            }
            if (musicUrl.isNotEmpty()) {
                initMediaPlayer(musicUrl)
                binding.llMusic.isVisible = true
            }
            imageUrls.clear()
            imageUrls.addAll(extractedUrls)
            displayScoreImages(extractedUrls)
        }
    }

    /**
     * 从本地缓存读取 HTML
     */
    private fun loadHtmlFromCache(url: String): String? {
        return try {
            val file = File(cacheDir, "score_${url.hashCode()}.html")
            if (file.exists()) file.readText() else null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将 HTML 写入本地缓存
     */
    private fun saveHtmlToCache(url: String, html: String) {
        try {
            val file = File(cacheDir, "score_${url.hashCode()}.html")
            file.writeText(html)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun initMediaPlayer(musicUrl: String) {
        // 避免重复创建导致资源泄漏
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(musicUrl)
                setOnPreparedListener {
                    binding.sbProgress.max = duration
                    binding.tvTotalTime.text = formatTime(duration)
                }
                setOnCompletionListener {
                    binding.ivAction.setImageResource(R.drawable.start)
                    binding.sbProgress.progress = 0
                    binding.tvTime.text = formatTime(0)
                }
                prepareAsync()
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
        try {
            val imagePairs = mutableListOf<Pair<Int, String>>()

            // 提取普通图片链接，记录在HTML中的位置
            val normalRegex = """href=['"]((/Public/Uploads/|/data2/uploads/)[^'"]+)['"]""".toRegex()
            normalRegex.findAll(html).forEach { match ->
                val url = "https://www.qupu123.com${match.groupValues[1]}"
                imagePairs.add(match.range.first to url)
            }

            // 提取加密图片链接：按 showopern 在 HTML 中出现的位置排序
            val showopernRegex = """showopern\(([^,]+),\s*"([^"]+)"\)""".toRegex()
            showopernRegex.findAll(html).forEach { showMatch ->
                val key = showMatch.groupValues[1]
                val value = showMatch.groupValues[2]
                val varRegex = """var\s+$key\s*=\s*"([^"]+)"""".toRegex()
                varRegex.findAll(html).forEach { varMatch ->
                    val image = DecodeUtils.showdown(varMatch.groupValues[1], value)
                    val url = "https://www.qupu123.com$image"
                    // 这里使用 showopern(...) 在 HTML 中的位置作为排序依据，
                    // 更贴近页面实际展示顺序，而不是变量定义的位置
                    imagePairs.add(showMatch.range.first to url)
                }
            }

            return imagePairs
                .sortedBy { it.first }
                .map { it.second }
                .distinct()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
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
            while (true) {
                val player = mediaPlayer ?: break
                if (!player.isPlaying) {
                    break
                }
                binding.sbProgress.progress = player.currentPosition
                binding.tvTime.text = formatTime(player.currentPosition)
                delay(500)
            }
        }
    }

    /**
     * 格式化时间显示
     */
    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
} 