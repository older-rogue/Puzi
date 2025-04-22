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
 */
class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ScoreAdapter
    private lateinit var favoritesManager: FavoritesManager

    private val list = mutableListOf<Score>()

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
        initView()
    }

    private fun initView() {
        binding.tvInOrOut.setOnClickListener {
            showCustomDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = ScoreAdapter(true) { score ->
            navigateToScoreDetail(score)
        }

        binding.favoritesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoritesFragment.adapter
        }
        adapter.submitList(list)
    }

    private fun navigateToScoreDetail(score: Score) {
//        favoritesManager.addFavorite(score)
        val intent = Intent(requireContext(), ScoreDetailActivity::class.java).apply {
            putExtra("url", score.url)
            putExtra("title", score.title)
            putExtra("name", score.name)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // 每次页面可见时重新加载收藏，以更新列表
        loadFavorites()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFavorites() {
        val favorites = favoritesManager.getFavorites()
        val deleteList = mutableListOf<Score>()
        list.forEach { song ->
            if (favorites.find { it.url == song.url } == null) {
                deleteList.add(song)
            }
        }
        list.removeAll(deleteList)
        favorites.forEach { song ->
            if (list.find { it.url == song.url } == null) {
                list.add(song)
            }
        }
        list.sortByDescending { it.time }
        list.sortBy { !it.isLove }
        adapter.notifyDataSetChanged()

        // 显示空视图或列表
        if (favorites.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.favoritesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.favoritesRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingInflatedId")
    fun showCustomDialog() {
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
            openFile()
            loadFavorites()
        }

        dialog.show()
    }

    // 调用文件选择器
    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*" // 可以选择任何文件类型，例如 "image/*", "text/plain"
        startActivityForResult(intent, 1001)
    }

    // 处理文件选择的结果
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // 使用 SAF 获取文件 URI
                readFile(uri)
            }
        }
    }

    // 读取文件内容
    private fun readFile(uri: Uri) {
        try {
            val inputStream = requireActivity().contentResolver.openInputStream(uri)
            inputStream?.use {
                val content = BufferedReader(InputStreamReader(it)).readText()
                favoritesManager.loadFromLocal(content)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
} 