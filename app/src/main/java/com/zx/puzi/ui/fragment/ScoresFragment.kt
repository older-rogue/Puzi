package com.zx.puzi.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
 * 显示热门曲谱列表和搜索功能
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
        setupSearchControls()
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

    private fun setupSearchControls() {
        // 点击搜索框或搜索按钮，跳转到搜索页面
        binding.searchEditText.setOnClickListener {
            navigateToSearch()
        }
        
        binding.searchButton.setOnClickListener {
            navigateToSearch()
        }

        binding.ivRefresh.setOnClickListener {
            loadHotScores()
        }
    }

    private fun navigateToSearch() {
        val intent = Intent(requireContext(), SearchResultsActivity::class.java)
        startActivity(intent)
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