package com.zx.puzi.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivitySearchResultsBinding
import com.zx.puzi.model.Score
import com.zx.puzi.network.ApiService
import com.zx.puzi.ui.adapter.ScoreAdapter
import com.zx.puzi.utils.StatusBarUtil

/**
 * 搜索结果页面
 */
class SearchResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var adapter: ScoreAdapter
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_results)

        // 设置状态栏为白色
        StatusBarUtil.setWhiteStatusBar(this)

        // 获取搜索查询
        searchQuery = intent.getStringExtra("query") ?: ""

        setupViews()
        performSearch()
    }
    
    private fun setupViews() {
        // 设置返回按钮
        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 设置RecyclerView和适配器
        adapter = ScoreAdapter { score ->
            navigateToScoreDetail(score)
        }
        
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SearchResultsActivity.adapter
        }
    }
    
    private fun navigateToScoreDetail(score: Score) {
        val intent = Intent(this, ScoreDetailActivity::class.java).apply {
            putExtra("url", score.url)
            putExtra("title", score.title)
            putExtra("name", score.name)
        }
        startActivity(intent)
    }

    private fun performSearch() {
        if (searchQuery.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
        
        // 使用ApiService搜索曲谱
        ApiService.instance.searchScores(
            query = searchQuery,
            onSuccess = { scores ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(scores)
                    
                    // 更新UI显示
                    if (scores.isEmpty()) {
                        binding.emptyView.visibility = View.VISIBLE
                        binding.searchResultsRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyView.visibility = View.GONE
                        binding.searchResultsRecyclerView.visibility = View.VISIBLE
                    }
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "搜索失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
} 