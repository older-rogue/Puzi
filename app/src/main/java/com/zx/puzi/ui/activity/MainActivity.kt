package com.zx.puzi.ui.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

/**
 * 主Activity
 * 包含曲谱列表和收藏列表两个Fragment
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val scoresFragment by lazy { ScoresFragment() }
    private val favoritesFragment by lazy { FavoritesFragment() }
    private var activeFragment: Fragment? = null

    private var backPressedTime = 0L
    private val exitInterval = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        StatusBarUtil.setWhiteStatusBar(this)
        initFragments()
        setupBottomNavigation()
        setupBackPressHandler()
        checkForUpdates()
    }

    /**
     * 初始化Fragment
     */
    private fun initFragments() {
        if (supportFragmentManager.findFragmentByTag("scores") == null) {
            activeFragment = scoresFragment
            supportFragmentManager.commit {
                add(R.id.fragment_container, scoresFragment, "scores")
            }
        }
    }

    /**
     * 设置底部导航
     */
    private fun setupBottomNavigation() {
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
    }

    /**
     * 切换Fragment
     */
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

    /**
     * 设置返回键处理
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                if (currentTime - backPressedTime < exitInterval) {
                    finish()
                } else {
                    Toast.makeText(this@MainActivity, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                    backPressedTime = currentTime
                }
            }
        })
    }

    /**
     * 检查应用更新
     */
    private fun checkForUpdates() {
        ApiService.instance.checkUpdate { updateInfo ->
            lifecycleScope.launch {
                try {
                    if (getAppCode() < updateInfo.buildVersionNo.toInt()) {
                        showUpdateDialog(updateInfo)
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 显示更新对话框
     */
    @SuppressLint("SetTextI18n")
    private fun showUpdateDialog(info: UpdateInfo) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_update, null)
        dialog.setContentView(view)

        val tvInfo = view.findViewById<TextView>(R.id.tv_info)
        tvInfo.text = "当前版本：${getAppVersion()}\n" +
                "最新版本：${info.buildVersion}\n" +
                "更新内容：\n${info.buildUpdateDescription}"

        view.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.tv_update).setOnClickListener {
            dialog.dismiss()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.appUrl))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "未安装可用浏览器", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}