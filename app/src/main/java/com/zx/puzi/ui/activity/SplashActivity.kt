package com.zx.puzi.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zx.puzi.databinding.ActivitySplashBinding
import com.zx.puzi.local.LocalData
import com.zx.puzi.network.ApiService
import com.zx.puzi.utils.StatusBarUtil

/**
 * 启动页Activity
 * 负责检查应用授权和引导用户进入主页面
 */
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        StatusBarUtil.setWhiteStatusBar(this)
        checkLicenseAndNavigate()
    }

    /**
     * 检查授权并导航到相应页面
     */
    private fun checkLicenseAndNavigate() {
        ApiService.instance.checkLicense(
            onSuccess = { canUsed ->
                runOnUiThread {
                    if (canUsed) {
                        navigateToNextScreen()
                    } else {
                        Toast.makeText(this, "下架了，暂时不能用了", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, "加载失败: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    /**
     * 导航到下一个页面
     */
    private fun navigateToNextScreen() {
        val intent = if (LocalData.getIsFirst(this)) {
            Intent(this, AboutActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}