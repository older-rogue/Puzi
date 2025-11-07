package com.zx.puzi.network

import com.zx.puzi.model.Score
import com.zx.puzi.model.UpdateInfo
import com.zx.puzi.utils.HtmlParserUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 网络请求服务类，专门处理网络相关操作
 */
class ApiService private constructor() {

    companion object {
        private const val BASE_URL = "https://www.qupu123.com"
        private const val LICENSE_CHECK_URL = "https://www.pgyer.com/4zY5oxqe"
        private const val UPDATE_CHECK_URL = "https://api.pgyer.com/apiv2/app/check"
        private const val API_KEY = "56eac30dc043da146b8b834b88ab1ff8"
        private const val APP_KEY = "951ecb48ae4ab9b21932f8451c1ed2f0"
        private const val TIMEOUT_SECONDS = 10L

        val instance: ApiService by lazy { ApiService() }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 检查应用授权状态
     * 通过检查蒲公英上的空白app是否存在来判断应用是否可用
     */
    fun checkLicense(onSuccess: (Boolean) -> Unit, onError: (String) -> Unit) {
        val request = Request.Builder()
            .url(LICENSE_CHECK_URL)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    onSuccess(it.code != 404)
                }
            }
        })
    }

    /**
     * 检查应用更新
     */
    fun checkUpdate(callback: (UpdateInfo) -> Unit) {
        val formBody = FormBody.Builder()
            .add("_api_key", API_KEY)
            .add("appKey", APP_KEY)
            .build()

        val request = Request.Builder()
            .url(UPDATE_CHECK_URL)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 静默失败，不影响主流程
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.code == 200) {
                        val data = it.body?.string() ?: return
                        if (data.isNotEmpty()) {
                            try {
                                val jsonObject = JSONObject(data)
                                val finalData = jsonObject.getJSONObject("data")
                                val updateInfo = UpdateInfo(
                                    buildVersionNo = finalData.optString("buildVersionNo", "0"),
                                    buildVersion = finalData.optString("buildVersion", ""),
                                    appUrl = finalData.optString("appURl", ""),
                                    buildUpdateDescription = finalData.optString("buildUpdateDescription", "无")
                                )
                                callback(updateInfo)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * 获取热门曲谱
     */
    fun getHotScores(onSuccess: (List<Score>) -> Unit, onError: (String) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/tongsu/${Random.nextInt(1, 728)}.html")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val html = it.body?.string() ?: ""
                    val scores = HtmlParserUtil.parseHotScores(html)
                    onSuccess(scores)
                }
            }
        })
    }

    /**
     * 搜索曲谱
     */
    fun searchScores(query: String, onSuccess: (List<Score>) -> Unit, onError: (String) -> Unit) {
        try {
            val url = "$BASE_URL/Search?keys=$query&cid="
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError(e.message ?: "搜索请求失败")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val html = it.body?.string() ?: ""
                        val results = parseSearchResults(html)
                        onSuccess(results)
                    }
                }
            })
        } catch (e: Exception) {
            onError(e.message ?: "搜索请求异常")
        }
    }

    /**
     * 解析搜索结果
     */
    private fun parseSearchResults(html: String): List<Score> {
        val results = mutableListOf<Score>()

        try {
            val regex = """<td class="f1">.*?<a href="([^"]+)"[^>]*>(.*?)</a>.*?</td>\s*<td class="f3">(.*?)</td>""".toRegex()
            regex.findAll(html).forEach { matchResult ->
                val (url, title, artist) = matchResult.destructured
                results.add(
                    Score(
                        title = title,
                        url = "$BASE_URL$url",
                        name = artist.ifBlank { "未知" }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }
} 