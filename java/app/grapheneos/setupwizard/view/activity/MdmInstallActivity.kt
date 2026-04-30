package app.grapheneos.setupwizard.view.activity

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.action.AppInstaller
import app.grapheneos.setupwizard.action.DateTimeActions
import app.grapheneos.setupwizard.action.MdmInstallActions
import app.grapheneos.setupwizard.data.MdmInstallViewModel

class MdmInstallActivity : SetupWizardActivity(
    R.layout.activity_mdm_install,
    R.drawable.baseline_provisioning_glif,
    R.string.provisioning_title,
    R.string.provisioning_desc,
) {
    companion object {
        private const val TAG = "MdmInstallActivity"
    }

    val viewModel: MdmInstallViewModel by viewModels()

    private lateinit var spinner: ProgressBar
    private lateinit var message: TextView
    private lateinit var linearProgress: ProgressBar
    private lateinit var progressLegend: TextView

    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(this, intent)
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            // RECEIVER_NOT_EXPORTED: the install-status broadcast is sent from system_server to
            // our own private action (with setPackage(this.packageName)), so no other app needs
            // to reach this receiver. Not exporting eliminates the spoofing surface.
            registerReceiver(
                viewModel.appInstallReceiver,
                IntentFilter(AppInstaller.ACTION_INSTALL_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (receiverRegistered) {
            unregisterReceiver(viewModel.appInstallReceiver)
            receiverRegistered = false
        }
    }

    override fun onResume() {
        super.onResume()
        DateTimeActions.handleEntry()
    }

    override fun onPause() {
        super.onPause()
        DateTimeActions.handleExit()
    }

    override fun onActivityResult(resultCode: Int, data: Intent?) {
        super.onActivityResult(resultCode, data)
        MdmInstallActions.handleActivityResult(this, resultCode)
    }

    override fun bindViews() {
        spinner = requireViewById(R.id.spinning_progress)
        message = requireViewById(R.id.text_message)
        linearProgress = requireViewById(R.id.linear_progress)
        progressLegend = requireViewById(R.id.progress_legend)
        secondaryButton.visibility = View.GONE
        primaryButton.visibility = View.GONE

        viewModel.message.observe(this) {
            this.message.text = it
        }
        viewModel.spinnerVisible.observe(this) {
            this.spinner.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.progressVisible.observe(this) {
            val visibility = if (it) View.VISIBLE else View.GONE
            this.linearProgress.visibility = visibility
            this.progressLegend.visibility = visibility
        }
        viewModel.downloadProgress.observe(this) {
            this.linearProgress.progress = it
        }
        viewModel.downloadProgressLegend.observe(this) {
            this.progressLegend.text = it
        }
        viewModel.error.observe(this) {
            if (it != null) {
                MdmInstallActions.handleError(this, it)
            }
        }
        viewModel.complete.observe(this) {
            primaryButton.setText(this, R.string.next)
            primaryButton.visibility = View.VISIBLE
        }
    }

    override fun setupActions() {
        primaryButton.setOnClickListener {
            MdmInstallActions.provisionDeviceOwner(this)
        }
    }
}
