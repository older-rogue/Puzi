package com.zx.puzi.ui.activity

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
 */
class ScoreDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScoreDetailBinding
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private lateinit var favoritesManager: FavoritesManager
    private var scoreUrl: String = ""
    private var scoreTitle: String = ""
    private var name: String = ""
    private var isFavorite = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_score_detail)
        favoritesManager = FavoritesManager.getInstance(this)

        // 设置状态栏为白色
        StatusBarUtil.setWhiteStatusBar(this)

        // 获取传递的参数并初始化界面
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

        if (name.isNotEmpty()) {
            binding.scoreAuthor.visibility = View.VISIBLE
            binding.scoreAuthor.text = name
        } else {
            binding.scoreAuthor.visibility = View.GONE
        }

        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 检查是否已收藏
        isFavorite = favoritesManager.isFavorite(scoreUrl)
        updateFavoriteButtonAppearance()

        // 设置收藏按钮点击事件
        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        binding.ivAction.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.ivAction.setImageResource(R.drawable.start)
            } else {
                mediaPlayer?.start()
                binding.ivAction.setImageResource(R.drawable.stop)
                updateSeekBar()
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
                val html = response.body?.string() ?: ""
                val imageUrls = extractImageUrlsFromHtml(html)
                val music = extractMusicUrlsFromHtml(html)
                runOnUiThread {
                    if (music.isNotEmpty()) {
                        initMediaPlayer(music)
                        binding.llMusic.isVisible = true
                    }
                    displayScoreImages(imageUrls)
                }
            }
        })
    }

    private fun initMediaPlayer(music: String) {
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.setDataSource(music) // 替换为真实的音频URL
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener {
                binding.sbProgress.max = mediaPlayer?.duration ?: Int.MAX_VALUE
                updateSeekBar()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun extractImageUrlsFromHtml(html: String): List<String> {
        val imageUrls = mutableListOf<String>()

        try {
            // 查找曲谱图片区域
            val regex = """href=['"]((/Public/Uploads/|/data2/uploads/)[^'"]+)['"]""".toRegex()
            regex.findAll(html).forEach {
                imageUrls.add("https://www.qupu123.com${it.groupValues[1]}")
            }
            val regex1 = """showopern\(([^,]+),\s*"([^"]+)"\)""".toRegex()
            regex1.findAll(html).forEach {
                val key = it.groupValues[1]
                val value = it.groupValues[2]
                val regex3 = """var\s+${key}\s*=\s*"([^"]+)"""".toRegex()
                regex3.findAll(html).forEach { it2 ->
                    val image = DecodeUtils.showdown(it2.groupValues[1], value)
                    imageUrls.add(1, "https://www.qupu123.com${image}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageUrls
    }

    private fun extractMusicUrlsFromHtml(html: String): String {
        var music = ""
        try {
            // 查找曲谱图片区域  <source src="/Public/Uploads/mp3s/282550.mp3">
            val regex = """source\s+src=['"](/Public/Uploads/[^'"]+)['"]""".toRegex()
            regex.find(html)?.let {
                music = "https://www.qupu123.com${it.groupValues[1]}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return music
    }

    private fun displayScoreImages(imageUrls: List<String>) {
        binding.scoreImagesContainer.removeAllViews()

        for (url in imageUrls) {
            val imageView = ImageView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                Glide.with(this@ScoreDetailActivity).load(url)
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
                            binding.favoriteButton.visibility = View.VISIBLE
                            return false
                        }

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.progressBar.visibility = View.GONE
                            binding.favoriteButton.visibility = View.VISIBLE
                            return false
                        }
                    }).into(this)
            }
            binding.scoreImagesContainer.addView(imageView)
        }
    }

    private fun toggleFavorite() {
        if (isFavorite) {
            // 移除收藏
            favoritesManager.removeFavorite(scoreUrl)
            isFavorite = false
            Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show()
        } else {
            // 添加收藏
            val score = Score(scoreTitle, scoreUrl, name)
            favoritesManager.addFavorite(score)
            isFavorite = true
            Toast.makeText(this, "已添加到收藏", Toast.LENGTH_SHORT).show()
        }
        updateFavoriteButtonAppearance()
    }

    private fun updateFavoriteButtonAppearance() {
        binding.favoriteButton.setBackgroundResource(
            if (isFavorite) R.drawable.bg_round
            else R.drawable.bg_grew
        )
    }

    private fun updateSeekBar() {
        lifecycleScope.launch {
            delay(500)
            if (mediaPlayer?.isPlaying == true) {
                binding.sbProgress.progress = mediaPlayer?.currentPosition ?: 0
                binding.tvTime.text = formatTime(mediaPlayer?.currentPosition ?: 0)
                updateSeekBar()
            }
        }

    }

    private fun formatTime(milliseconds: Int): String {
        val format = SimpleDateFormat("mm:ss", Locale.getDefault())
        return format.format(Date(milliseconds.toLong()))
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
} 