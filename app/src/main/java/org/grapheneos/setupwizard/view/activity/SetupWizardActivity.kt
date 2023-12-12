package org.grapheneos.setupwizard.view.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity // TODO: better to use Activity directly

/**
 * This is the base activity for all setup wizard activities.
 */
abstract class SetupWizardActivity(@LayoutRes val layoutResID: Int) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResID)
        bindViews()
        setupActions()
    }

    @MainThread
    abstract fun bindViews()

    @MainThread
    abstract fun setupActions()
}