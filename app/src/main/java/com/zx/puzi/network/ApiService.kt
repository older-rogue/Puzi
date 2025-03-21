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
        val instance: ApiService by lazy { ApiService() }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 这个地址是我蒲公英一个空白app的地址
     * 如果有特殊情况（侵权问题），这个app将会被删掉，来达到停用的目的
     * 没办法，不想自己搭个服务器，也没有想到好方法
     */
    fun checkLicense(onSuccess: (Boolean) -> Unit, onError: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://www.pgyer.com/4zY5oxqe")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "网络请求失败")
            }

            override fun onResponse(call: Call, response: Response) {
                onSuccess(response.code != 404)
            }
        })
    }

    fun checkUpdate(callback: (UpdateInfo) -> Unit) {
// 构建表单请求体
        val formBody = FormBody.Builder()
            .add("_api_key", "56eac30dc043da146b8b834b88ab1ff8")
            .add("appKey", "951ecb48ae4ab9b21932f8451c1ed2f0")
            .build();

        val request = Request.Builder()
            .url("https://api.pgyer.com/apiv2/app/check")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 200 && response.body != null) {
                    val data = response.body?.string()?:""
                    if(data.isNotEmpty()){
                        val jsonObject = JSONObject(data)
                        val finalData = jsonObject.getJSONObject("data")
                        val buildVersionNo = finalData.optString("buildVersionNo", "")
                        val buildVersion = finalData.optString("buildVersion", "")
                        val appURl = finalData.optString("appURl", "")
                        val buildUpdateDescription = finalData.optString("buildUpdateDescription", "")
                        callback(UpdateInfo(buildVersionNo,buildVersion,appURl,buildUpdateDescription))
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
            .url("https://www.qupu123.com/tongsu/${Random.nextInt(1, 728)}.html")
//            .url("https://www.pgyer.com/4zY5oxqe")
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "网络请求失败")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val html = response.body?.string() ?: ""
                val scores = HtmlParserUtil.parseHotScores(html)
                onSuccess(scores)
            }
        })
    }
    
    /**
     * 搜索曲谱
     */
    fun searchScores(query: String, onSuccess: (List<Score>) -> Unit, onError: (String) -> Unit) {
        try {
            val url = "https://www.qupu123.com/Search?keys=${query}&cid="
            val request = Request.Builder()
                .url(url)
                .build()
                
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    onError(e.message ?: "搜索请求失败")
                }
                
                override fun onResponse(call: Call, response: Response) {
                    val html = response.body?.string() ?: ""
                    val results = parseSearchResults(html)
                    onSuccess(results)
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
                results.add(Score(title, "https://www.qupu123.com$url", artist.ifBlank { "未知" }))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results
    }
} 