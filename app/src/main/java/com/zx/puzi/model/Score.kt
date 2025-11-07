package com.zx.puzi.model

/**
 * 曲谱数据模型类
 *
 * @property title 曲谱标题
 * @property url 曲谱URL
 * @property name 作者名称
 * @property time 收藏时间戳
 * @property isClick 是否已点击
 * @property isLove 是否喜欢
 */
data class Score(
    val title: String,
    val url: String,
    val name: String = "",
    var time: Long = 0L,
    var isClick: Boolean = false,
    var isLove: Boolean = false
)