package org.grapheneos.setupwizard.view.activity

import android.app.Activity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.google.android.material.color.DynamicColors
import com.google.android.material.elevation.SurfaceColors
import com.google.android.setupdesign.util.ThemeHelper
import org.grapheneos.setupwizard.action.SetupWizard


/**
 * This is the base activity for all setup wizard activities.
 */
abstract class SetupWizardActivity(@LayoutRes val layoutResID: Int) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(ThemeHelper.getSuwDefaultTheme(applicationContext))
        ThemeHelper.trySetDynamicColor(this)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(layoutResID)
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom)
            insets
        }
        bindViews()
        setupActions()
    }

    @MainThread
    abstract fun bindViews()

    @MainThread
    abstract fun setupActions()
}