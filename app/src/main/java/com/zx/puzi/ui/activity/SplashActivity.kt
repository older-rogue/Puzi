package com.zx.puzi.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zx.puzi.databinding.ActivitySplashBinding
import com.zx.puzi.local.LocalData
import com.zx.puzi.network.ApiService
import com.zx.puzi.utils.StatusBarUtil

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置状态栏为白色
        StatusBarUtil.setWhiteStatusBar(this)
        
        // 2秒后跳转到主页
        Handler(Looper.getMainLooper()).postDelayed({
            // 使用ApiService获取热门曲谱
            ApiService.instance.checkLicense(
                onSuccess = { canUsed ->
                    this.runOnUiThread {
                        if (canUsed) {
                            if (LocalData.getIsFirst(this)) {
                                startActivity(Intent(this, AboutActivity::class.java))
                                finish()
                            } else {
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            this.runOnUiThread {
                                Toast.makeText(this, "下架了，暂时不能用了", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                },
                onError = { errorMessage ->
                    this.runOnUiThread {
                        Toast.makeText(this, "加载失败: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            )

        }, 1000)
    }
}