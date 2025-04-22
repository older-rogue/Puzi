package com.zx.puzi.local

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.zx.puzi.model.Score
import com.zx.puzi.utils.DownloadFileUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 收藏管理类，负责收藏数据的本地存储和读取
 */
class FavoritesManager private constructor(private val context: Context) {

    companion object {
        private const val PREF_NAME = "favorites"
        private const val KEY_FAVORITES = "favorites_list"


        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 获取所有收藏的曲谱
     */
    fun getFavorites(): List<Score> {
        val favoritesList = mutableListOf<Score>()
        val favoritesJson = sharedPreferences.getString(KEY_FAVORITES, "[]")

        try {
            val jsonArray = JSONArray(favoritesJson)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val url = jsonObject.getString("url")
                val name = jsonObject.optString("name", "")
                val time = jsonObject.optLong("time", 0L)
                val isLove = jsonObject.optBoolean("love",false)
                favoritesList.add(Score(title, url, name, time, isLove = isLove))
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return favoritesList
    }

    /**
     * 添加曲谱到收藏
     */
    fun addFavorite(score: Score): Boolean {
        val currentFavorites = getFavorites().toMutableList()

        // 检查是否已存在
        currentFavorites.removeAll(currentFavorites.filter { it.url == score.url })
        score.time = System.currentTimeMillis()
        currentFavorites.add(score)
        return saveFavorites(currentFavorites)
    }

    /**
     * 从收藏中移除曲谱
     */
    fun removeFavorite(url: String): Boolean {
        val currentFavorites = getFavorites().toMutableList()
        val initialSize = currentFavorites.size

        currentFavorites.removeAll { it.url == url }

        if (currentFavorites.size != initialSize) {
            return saveFavorites(currentFavorites)
        }

        return false
    }

    /**
     * 检查曲谱是否已收藏
     */
    fun isFavorite(url: String): Boolean {
        return getFavorites().any { it.url == url }
    }

    /**
     * 检查曲谱是否喜欢
     */
    fun isLove(url: String): Boolean {
        return getFavorites().any { it.url == url && it.isLove }
    }

    /**
     * 保存收藏列表到本地
     */
    private fun saveFavorites(favorites: List<Score>): Boolean {
        val jsonArray = JSONArray()

        favorites.forEach { score ->
            val jsonObject = JSONObject().apply {
                put("title", score.title)
                put("url", score.url)
                put("name", score.name)
                put("time", score.time)
                put("love", score.isLove)
            }
            jsonArray.put(jsonObject)
        }

        return sharedPreferences.edit()
            .putString(KEY_FAVORITES, jsonArray.toString())
            .commit()
    }


    fun saveToLocal() {
        try {
            val favoritesJson = sharedPreferences.getString(KEY_FAVORITES, "[]") ?: "[]"
            DownloadFileUtils.saveFileToDownload(context, "puzi", "puzi", favoritesJson)
            Toast.makeText(context, "备份成功", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(context, "备份失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadFromLocal(content: String) {
        try {
            if (content.isEmpty()) return
            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val url = jsonObject.getString("url")
                val name = jsonObject.optString("name", "")
                val time = jsonObject.optLong("time", 0L)
                val isLove = jsonObject.optBoolean("love",false)
                addFavorite(Score(title, url, name, time, isLove = isLove))
            }
            Toast.makeText(context, "同步成功", Toast.LENGTH_SHORT).show()
        } catch (e: Throwable) {
            Toast.makeText(context, "同步失败", Toast.LENGTH_SHORT).show()
        }
    }
} 