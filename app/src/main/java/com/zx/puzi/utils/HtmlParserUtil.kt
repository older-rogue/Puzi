package com.zx.puzi.utils

import com.zx.puzi.model.Score

/**
 * HTML解析工具类
 * 用于处理HTML文件的编码转换和内容解析
 */
object HtmlParserUtil {
    // 预编译正则表达式以提高性能
    private val HOT_SCORES_REGEX = """<td class="f1"><a href=['"]([^'"]+)['"]title=['"]([^'"]+)['"]\s*target="_blank">.*?</td>\s*<td class="f2">.*?</td>\s*<td class="f3">\s*(.*?)\s*</td>""".toRegex()
    
    /**
     * 从HTML内容中解析热门歌谱部分
     * @param html HTML内容字符串
     * @return 解析出的热门歌谱列表
     */
    fun parseHotScores(html: String): List<Score> = try {
        HOT_SCORES_REGEX.findAll(html).map { matchResult ->
            val (url, title, author) = matchResult.destructured
            Score(
                title = title.substringBefore("（").trim(),
                url = "https://www.qupu123.com$url", 
                name = author.replace(Regex("\\s+"), " ").trim()
            )
        }.toList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}