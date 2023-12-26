package org.grapheneos.setupwizard.view.activity

import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import org.grapheneos.setupwizard.action.SetupWizard


/**
 * This is the base activity for all setup wizard activities.
 */
abstract class SetupWizardActivity(@LayoutRes val layoutResID: Int) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(layoutResID)
        bindViews()
        setupActions()
    }

    @MainThread
    abstract fun bindViews()

    @MainThread
    abstract fun setupActions()
}