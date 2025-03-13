package com.zx.puzi.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.zx.puzi.R
import com.zx.puzi.databinding.ActivityMainBinding
import com.zx.puzi.ui.fragment.FavoritesFragment
import com.zx.puzi.ui.fragment.ScoresFragment
import com.zx.puzi.utils.StatusBarUtil

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    // 懒加载Fragment减少内存占用
    private val scoresFragment by lazy { ScoresFragment() }
    private val favoritesFragment by lazy { FavoritesFragment() }
    private var activeFragment: Fragment? = null
    
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
}