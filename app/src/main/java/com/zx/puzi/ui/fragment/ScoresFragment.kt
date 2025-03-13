package com.zx.puzi.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zx.puzi.databinding.FragmentScoresBinding
import com.zx.puzi.model.Score
import com.zx.puzi.network.ApiService
import com.zx.puzi.ui.activity.ScoreDetailActivity
import com.zx.puzi.ui.activity.SearchResultsActivity
import com.zx.puzi.ui.adapter.ScoreAdapter

/**
 * 曲谱列表Fragment
 */
class ScoresFragment : Fragment() {
    private var _binding: FragmentScoresBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ScoreAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchButton()
        loadHotScores()
    }
    
    private fun setupRecyclerView() {
        adapter = ScoreAdapter { score ->
            navigateToScoreDetail(score)
        }
        
        binding.scoresRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScoresFragment.adapter
        }
    }
    
    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(requireContext(), SearchResultsActivity::class.java).apply {
                    putExtra("query", query)
                }
                startActivity(intent)
            } else {
                Toast.makeText(context, "请输入搜索内容", Toast.LENGTH_SHORT).show()
            }
        }
        binding.ivRefresh.setOnClickListener {
            loadHotScores()
        }
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = binding.searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    val intent = Intent(requireContext(), SearchResultsActivity::class.java).apply {
                        putExtra("query", query)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(context, "请输入搜索内容", Toast.LENGTH_SHORT).show()
                }

                true // 表示事件已处理
            } else {
                false
            }
        }
    }
    
    private fun navigateToScoreDetail(score: Score) {
        val intent = Intent(requireContext(), ScoreDetailActivity::class.java).apply {
            putExtra("url", score.url)
            putExtra("title", score.title)
            putExtra("name", score.name)
        }
        startActivity(intent)
    }

    private fun loadHotScores() {
        binding.progressBar.visibility = View.VISIBLE
        
        // 使用ApiService获取热门曲谱
        ApiService.instance.getHotScores(
            onSuccess = { scores ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(scores)
                }
            },
            onError = { errorMessage ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "加载失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 