package com.zx.puzi.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.zx.puzi.R
import com.zx.puzi.databinding.FragmentFavoritesBinding
import com.zx.puzi.local.FavoritesManager
import com.zx.puzi.model.Score
import com.zx.puzi.ui.activity.ScoreDetailActivity
import com.zx.puzi.ui.adapter.ScoreAdapter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 收藏曲谱列表Fragment
 * 显示用户收藏的曲谱，支持备份和恢复
 */
class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ScoreAdapter
    private lateinit var favoritesManager: FavoritesManager

    private val list = mutableListOf<Score>()

    companion object {
        private const val REQUEST_CODE_OPEN_FILE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoritesManager = FavoritesManager.getInstance(requireContext())
        setupRecyclerView()
        setupBackupButton()
    }

    private fun setupBackupButton() {
        binding.tvInOrOut.setOnClickListener {
            showBackupDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ScoreAdapter(isFavoritesFragment = true) { score ->
            navigateToScoreDetail(score)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoritesFragment.adapter
        }
        adapter.submitList(list)
    }

    private fun navigateToScoreDetail(score: Score) {
        val intent = Intent(requireContext(), ScoreDetailActivity::class.java).apply {
            putExtra("url", score.url)
            putExtra("title", score.title)
            putExtra("name", score.name)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFavorites() {
        val favorites = favoritesManager.getFavorites()

        // 移除已删除的收藏
        list.removeAll { score ->
            favorites.none { it.url == score.url }
        }

        // 添加新的收藏
        favorites.forEach { score ->
            if (list.none { it.url == score.url }) {
                list.add(score)
            }
        }

        // 排序：喜欢的在前，然后按时间倒序
        list.sortWith(compareByDescending<Score> { it.isLove }.thenByDescending { it.time })
        adapter.notifyDataSetChanged()

        updateEmptyView(favorites.isEmpty())
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.favoritesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.favoritesRecyclerView.visibility = View.VISIBLE
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showBackupDialog() {
        val dialog = Dialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null)
        dialog.setContentView(view)

        val save = view.findViewById<TextView>(R.id.save)
        val load = view.findViewById<Button>(R.id.load)

        save.setOnClickListener {
            favoritesManager.saveToLocal()
            dialog.dismiss()
        }

        load.setOnClickListener {
            dialog.dismiss()
            openFilePicker()
        }

        dialog.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                readFileAndRestore(uri)
            }
        }
    }

    private fun readFileAndRestore(uri: Uri) {
        try {
            requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream)).readText()
                favoritesManager.loadFromLocal(content)
                loadFavorites()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 