package com.zx.puzi.model

/**
 * 曲谱数据模型类
 *
 * @property title 曲谱标题
 * @property url 曲谱URL
 * @property name 作者名称
 */
data class Score(
    val title: String,
    val url: String,
    val name: String = "",
    var time: Long = 0L,
    var isClick: Boolean = false,
    var isLove: Boolean = false,
)