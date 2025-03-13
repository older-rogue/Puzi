package com.zx.puzi.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityAboutBinding
import com.zx.puzi.local.LocalData
import com.zx.puzi.utils.StatusBarUtil

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置状态栏为白色
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about)
        StatusBarUtil.setWhiteStatusBar(this)
        initView()
    }

    private fun initView() {
        binding.tvNext.setOnClickListener {
            LocalData.setIsFirst(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.tvClose.setOnClickListener {
            finish()
        }
    }

} 