package com.zx.puzi.ui.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityMainBinding
import com.zx.puzi.model.UpdateInfo
import com.zx.puzi.network.ApiService
import com.zx.puzi.ui.fragment.FavoritesFragment
import com.zx.puzi.ui.fragment.ScoresFragment
import com.zx.puzi.utils.StatusBarUtil
import com.zx.puzi.utils.getAppCode
import com.zx.puzi.utils.getAppVersion
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    // 懒加载Fragment减少内存占用
    private val scoresFragment by lazy { ScoresFragment() }
    private val favoritesFragment by lazy { FavoritesFragment() }
    private var activeFragment: Fragment? = null
    private var backPressedTime = 0L
    private val exitInterval = 2000L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        
        // 设置状态栏为白色
        StatusBarUtil.setWhiteStatusBar(this)
        
        // 初始化所有Fragment
        initFragments()
        
        // 设置底部导航
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_scores -> {
                    switchFragment(scoresFragment)
                    true
                }
                R.id.tab_favorites -> {
                    switchFragment(favoritesFragment)
                    true
                }
                else -> false
            }
        }
        initData()

        // 注册返回键监听
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < exitInterval) {
                    finish()  // 退出当前 Activity
                } else {
                    Toast.makeText(this@MainActivity, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                    backPressedTime = currentTime
                }
            }
        })
    }

    private fun initData() {
        ApiService.instance.checkUpdate {
            lifecycleScope.launch {
                if (this@MainActivity.getAppCode() < it.buildVersionNo.toInt()) {
                    showCustomDialog(it)
                }
            }
        }
    }

    private fun initFragments() {
        // 第一次显示ScoresFragment
        if (supportFragmentManager.findFragmentByTag("scores") == null) {
            activeFragment = scoresFragment
            supportFragmentManager.commit {
                add(R.id.fragment_container, scoresFragment, "scores")
            }
        }
    }
    
    private fun switchFragment(fragment: Fragment) {
        activeFragment?.let { currentFragment ->
            supportFragmentManager.commit {
                hide(currentFragment)
                if (fragment.isAdded) {
                    show(fragment)
                } else {
                    add(R.id.fragment_container, fragment, fragment.javaClass.simpleName)
                }
            }
        }
        activeFragment = fragment
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    fun showCustomDialog(info: UpdateInfo) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_update, null)
        dialog.setContentView(view)

        val tvInfo = view.findViewById<TextView>(R.id.tv_info)
        tvInfo.text = "当前版本：${this.getAppVersion()}\n" +
                "最新版本：${info.buildVersion}\n" +
                "更新内容：\n${info.buildUpdateDescription}"
        val cancel = view.findViewById<TextView>(R.id.tv_cancel)
        val update = view.findViewById<TextView>(R.id.tv_update)
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        update.setOnClickListener {
            dialog.dismiss()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.appURl))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "未安装可用浏览器", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}