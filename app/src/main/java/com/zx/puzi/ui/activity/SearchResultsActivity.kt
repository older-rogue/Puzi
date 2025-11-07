package com.zx.puzi.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
 * 搜索结果页面Activity
 */
class SearchResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var adapter: ScoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search_results)

        StatusBarUtil.setWhiteStatusBar(this)

        setupViews()
        
        // 如果从其他页面传入了搜索关键词，则自动搜索
        val initialQuery = intent.getStringExtra("query")
        if (!initialQuery.isNullOrEmpty()) {
            binding.searchEditText.setText(initialQuery)
            performSearch()
        }
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = ScoreAdapter { score ->
            navigateToScoreDetail(score)
        }

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SearchResultsActivity.adapter
        }

        // 设置搜索按钮点击事件
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        // 设置输入框回车搜索
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch()
                true
            } else {
                false
            }
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
        val query = binding.searchEditText.text.toString().trim()
        
        if (query.isEmpty()) {
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show()
            return
        }

        // 隐藏软键盘
        binding.searchEditText.clearFocus()
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)

        showLoading()

        ApiService.instance.searchScores(
            query = query,
            onSuccess = { scores ->
                runOnUiThread {
                    hideLoading()
                    adapter.submitList(scores)
                    updateEmptyView(scores.isEmpty())
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(this, "搜索失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.searchResultsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.searchResultsRecyclerView.visibility = View.VISIBLE
        }
    }
} 