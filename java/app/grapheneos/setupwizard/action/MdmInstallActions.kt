package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.util.Base64
import androidx.lifecycle.viewModelScope
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.data.MdmInstallViewModel
import app.grapheneos.setupwizard.view.activity.MdmInstallActivity
import app.grapheneos.setupwizard.view.activity.ProvisionActivity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.setupcompat.util.SystemBarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object MdmInstallActions {
    private const val TAG = "MdmInstallActions"

    const val EXTRA_QR_CONTENTS = "EXTRA_QR_CONTENTS"

    private const val EXTRA_ADMIN_COMPONENT_NAME =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    private const val EXTRA_DOWNLOAD_LOCATION =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    private const val EXTRA_PACKAGE_CHECKSUM =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM"
    private const val EXTRA_SKIP_ENCRYPTION =
        "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    private const val EXTRA_SYSTEM_APPS_ENABLED =
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"
    private const val EXTRA_EXTRAS_BUNDLE =
        "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"

    private const val CONNECTION_TIMEOUT_MS = 10_000
    private const val ADMIN_APK_FILE_NAME = "deviceadmin.apk"

    // Bound recursion in jsonToPersistableBundle to prevent stack exhaustion via
    // a maliciously deeply-nested QR payload.
    private const val MAX_JSON_DEPTH = 8

    fun handleEntry(
        activity: MdmInstallActivity,
        viewModel: MdmInstallViewModel,
        intent: Intent
    ) {
        SystemBarHelper.setBackButtonVisible(activity.window, false)
        val qrContent = intent.getStringExtra(EXTRA_QR_CONTENTS)
        if (qrContent == null) {
            viewModel.error.postValue(activity.getString(R.string.qr_parse_failed))
            return
        }
        if (!parseProvisioningQr(activity, viewModel, qrContent)) {
            return
        }
        setupWiFi(activity)
    }

    fun handleError(activity: MdmInstallActivity, message: String) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.error_title)
            .setMessage(message)
            .setPositiveButton(R.string.button_ok) { dialog, _ ->
                dialog.dismiss()
                activity.viewModel.error.postValue(null)
                activity.finish()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    fun handleActivityResult(activity: MdmInstallActivity, resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) {
            handleError(activity, activity.getString(R.string.wifi_failed))
        } else {
            onWifiSetupComplete(activity)
        }
    }

    private fun parseProvisioningQr(
        activity: MdmInstallActivity,
        viewModel: MdmInstallViewModel,
        qrContent: String
    ): Boolean {
        val objectMapper = ObjectMapper()
        val jsonNode: JsonNode = try {
            objectMapper.readTree(qrContent)
        } catch (e: Exception) {
            viewModel.error.postValue(activity.getString(R.string.qr_parse_failed))
            return false
        }

        if (jsonNode.has(EXTRA_ADMIN_COMPONENT_NAME) && jsonNode[EXTRA_ADMIN_COMPONENT_NAME].isTextual) {
            viewModel.adminComponentName = jsonNode[EXTRA_ADMIN_COMPONENT_NAME].asText()
        } else {
            viewModel.error.postValue(
                activity.getString(R.string.qr_missing_parameter) + EXTRA_ADMIN_COMPONENT_NAME
            )
            return false
        }

        if (jsonNode.has(EXTRA_DOWNLOAD_LOCATION) && jsonNode[EXTRA_DOWNLOAD_LOCATION].isTextual) {
            viewModel.downloadLocation = jsonNode[EXTRA_DOWNLOAD_LOCATION].asText()
        } else {
            viewModel.error.postValue(
                activity.getString(R.string.qr_missing_parameter) + EXTRA_DOWNLOAD_LOCATION
            )
            return false
        }

        if (jsonNode.has(EXTRA_PACKAGE_CHECKSUM) && jsonNode[EXTRA_PACKAGE_CHECKSUM].isTextual) {
            viewModel.packageChecksum = jsonNode[EXTRA_PACKAGE_CHECKSUM].asText()
        } else {
            viewModel.error.postValue(
                activity.getString(R.string.qr_missing_parameter) + EXTRA_PACKAGE_CHECKSUM
            )
            return false
        }

        if (jsonNode.has(EXTRA_SKIP_ENCRYPTION) && jsonNode[EXTRA_SKIP_ENCRYPTION].isBoolean) {
            viewModel.skipEncryption = jsonNode[EXTRA_SKIP_ENCRYPTION].asBoolean()
        }

        if (jsonNode.has(EXTRA_SYSTEM_APPS_ENABLED) && jsonNode[EXTRA_SYSTEM_APPS_ENABLED].isBoolean) {
            viewModel.systemAppsEnabled = jsonNode[EXTRA_SYSTEM_APPS_ENABLED].asBoolean()
        }

        if (jsonNode.has(EXTRA_EXTRAS_BUNDLE) && jsonNode[EXTRA_EXTRAS_BUNDLE].isObject) {
            viewModel.extrasBundle = jsonToPersistableBundle(jsonNode[EXTRA_EXTRAS_BUNDLE], 0)
        }

        return true
    }

    /**
     * The QR payload may carry WiFi parameters (PROVISIONING_WIFI_SSID, etc.), but configuring
     * WiFi automatically requires elevated permissions (Device / Profile owner or
     * android.uid.system) which the wizard does not have at this point. Manual WiFi setup is the
     * only supported path.
     */
    private fun setupWiFi(activity: MdmInstallActivity) {
        WifiActions.launchSetup(activity)
    }

    private fun onWifiSetupComplete(activity: MdmInstallActivity) {
        downloadAdminApp(activity, activity.viewModel)
    }

    private fun downloadAdminApp(activity: MdmInstallActivity, viewModel: MdmInstallViewModel) {
        viewModel.message.postValue(activity.getString(R.string.downloading_admin_app))
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            if (downloadAdminAppSync(activity, viewModel)) {
                viewModel.progressVisible.postValue(false)
                if (!viewModel.calculatedPackageChecksum.equals(viewModel.packageChecksum, ignoreCase = true)) {
                    viewModel.error.postValue(activity.getString(R.string.checksum_failed))
                    return@launch
                }
                installAdminApp(activity, viewModel)
            }
        }
    }

    private fun downloadAdminAppSync(
        activity: MdmInstallActivity,
        viewModel: MdmInstallViewModel
    ): Boolean {
        val downloadLocation = viewModel.downloadLocation
        if (downloadLocation == null) {
            viewModel.error.postValue(activity.getString(R.string.download_failed))
            return false
        }

        val url: URL = try {
            URL(downloadLocation)
        } catch (e: Exception) {
            viewModel.error.postValue(activity.getString(R.string.download_failed) + e.message)
            return false
        }

        // Refuse non-HTTPS download URLs. The QR payload comes from a trusted MDM enrollment
        // source, but the URL itself must travel over a TLS-protected channel - otherwise an
        // attacker on path could swap the APK and the checksum check happens after we've already
        // written attacker-controlled bytes to disk.
        if (!url.protocol.equals("https", ignoreCase = true)) {
            viewModel.error.postValue(
                activity.getString(R.string.download_failed) + "non-HTTPS URL refused"
            )
            return false
        }

        val tempFile = File(activity.filesDir, ADMIN_APK_FILE_NAME)
        if (tempFile.exists()) {
            tempFile.delete()
        }
        try {
            try {
                tempFile.createNewFile()
            } catch (e: Exception) {
                viewModel.error.postValue("Failed to create " + tempFile.absolutePath)
                return false
            }
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.connectTimeout = CONNECTION_TIMEOUT_MS
            connection.readTimeout = CONNECTION_TIMEOUT_MS
            connection.connect()
            if (connection.responseCode != 200) {
                throw Exception("Bad server response for $downloadLocation: ${connection.responseCode}")
            }
            val lengthOfFile = connection.contentLength
            notifyDownloadStart(viewModel, lengthOfFile)
            val digest = MessageDigest.getInstance("SHA-256")
            DataInputStream(connection.inputStream).use { dis ->
                FileOutputStream(tempFile).use { fos ->
                    val buffer = ByteArray(AppInstaller.IO_BUFFER_SIZE)
                    var length: Int
                    var total: Long = 0
                    while (dis.read(buffer).also { length = it } > 0) {
                        digest.update(buffer, 0, length)
                        total += length.toLong()
                        notifyDownloadProgress(activity, viewModel, total.toInt(), lengthOfFile)
                        fos.write(buffer, 0, length)
                    }
                    fos.flush()
                }
            }
            viewModel.calculatedPackageChecksum =
                Base64.encodeToString(digest.digest(), Base64.NO_WRAP or Base64.URL_SAFE)
        } catch (e: Exception) {
            tempFile.delete()
            viewModel.error.postValue(activity.getString(R.string.download_failed) + e.message)
            return false
        }
        return true
    }

    private fun notifyDownloadStart(viewModel: MdmInstallViewModel, total: Int) {
        if (total == -1) {
            viewModel.spinnerVisible.postValue(true)
            viewModel.progressVisible.postValue(false)
        } else {
            viewModel.spinnerVisible.postValue(false)
            viewModel.progressVisible.postValue(true)
        }
    }

    private fun notifyDownloadProgress(
        activity: MdmInstallActivity,
        viewModel: MdmInstallViewModel,
        downloaded: Int,
        total: Int
    ) {
        if (total != -1) {
            val downloadedMb: Float = downloaded / 1048576.0f
            val totalMb: Float = total / 1048576.0f
            val progress = activity.getString(R.string.download_progress, downloadedMb, totalMb)
            viewModel.downloadProgressLegend.postValue(progress)
            viewModel.downloadProgress.postValue((downloadedMb * 100 / totalMb).toInt())
        }
    }

    private fun installAdminApp(activity: MdmInstallActivity, viewModel: MdmInstallViewModel) {
        viewModel.spinnerVisible.postValue(true)
        viewModel.message.postValue(activity.getString(R.string.installing_admin_app))
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val error = AppInstaller.silentInstallApplication(
                activity,
                File(activity.filesDir, ADMIN_APK_FILE_NAME)
            )
            if (error != null) {
                viewModel.error.postValue(activity.getString(R.string.install_failed) + error)
            }
            // Successful install completion is delivered via the BroadcastReceiver registered
            // by MdmInstallActivity in onStart/onStop.
        }
    }

    fun provisionDeviceOwner(activity: MdmInstallActivity) {
        val viewModel = activity.viewModel
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            handleError(
                activity,
                "Cannot set up device owner because device does not have the "
                        + PackageManager.FEATURE_DEVICE_ADMIN + " feature"
            )
            return
        }
        val dpm: DevicePolicyManager? = activity.getSystemService(DevicePolicyManager::class.java)
        if (dpm == null) {
            handleError(activity, "Cannot set up device owner because DevicePolicyManager can't be initialized")
            return
        }
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE)) {
            handleError(activity, "DeviceOwner provisioning is not allowed; the device may already be provisioned")
            return
        }

        val adminComponentNameRaw = viewModel.adminComponentName
        if (adminComponentNameRaw == null) {
            handleError(activity, "Missing admin component name")
            return
        }
        val adminComponentNameParts = adminComponentNameRaw.split("/")
        if (adminComponentNameParts.size != 2) {
            handleError(activity, "Wrong component name format: $adminComponentNameRaw")
            return
        }

        val intent = Intent(activity, ProvisionActivity::class.java)
        intent.putExtra(
            EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
            ComponentName(adminComponentNameParts[0], adminComponentNameParts[1])
        )
        intent.putExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM, viewModel.packageChecksum)
        if (viewModel.systemAppsEnabled) {
            intent.putExtra(EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)
        }
        if (viewModel.skipEncryption) {
            intent.putExtra(EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }
        viewModel.extrasBundle?.let {
            intent.putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, it)
        }
        activity.startActivity(intent)
    }

    private fun jsonToPersistableBundle(jsonNode: JsonNode, depth: Int): PersistableBundle {
        val bundle = PersistableBundle()
        if (depth > MAX_JSON_DEPTH) return bundle

        jsonNode.fields().forEach { (key, value) ->
            when {
                value.isTextual -> bundle.putString(key, value.asText())
                value.isInt -> bundle.putInt(key, value.asInt())
                value.isLong -> bundle.putLong(key, value.asLong())
                value.isBoolean -> bundle.putBoolean(key, value.asBoolean())
                value.isDouble -> bundle.putDouble(key, value.asDouble())
                value.isObject -> bundle.putPersistableBundle(
                    key,
                    jsonToPersistableBundle(value, depth + 1)
                )
                value.isArray -> {
                    val arrayElements = value.map { it.asText() }.toTypedArray()
                    bundle.putStringArray(key, arrayElements)
                }
            }
        }
        return bundle
    }
}
