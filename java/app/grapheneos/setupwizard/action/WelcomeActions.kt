package app.grapheneos.setupwizard.action

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.service.oemlock.OemLockManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import app.grapheneos.setupwizard.APPLY_SIM_LANGUAGE_ON_ENTRY
import app.grapheneos.setupwizard.R
import app.grapheneos.setupwizard.appContext
import app.grapheneos.setupwizard.data.WelcomeData
import app.grapheneos.setupwizard.utils.DebugFlags
import app.grapheneos.setupwizard.view.activity.MdmInstallActivity
import app.grapheneos.setupwizard.view.activity.OemUnlockActivity
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocalePicker.LocaleInfo
import com.google.android.setupcompat.util.SystemBarHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale

object WelcomeActions {
    private const val TAG = "WelcomeActions"
    private const val ACTION_ACCESSIBILITY = "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW"
    private const val REBOOT_REASON_BOOTLOADER = "bootloader"
    private var simLocaleApplied = false
    private var qrToast: Toast? = null
    private var barcodeLauncher: ActivityResultLauncher<ScanOptions>? = null

    init {
        refreshCurrentLocale()
        refreshOemUnlockStatus()
        Log.d(TAG, "init: currentLocale = ${WelcomeData.selectedLanguage}")
    }

    fun handleEntry(context: Activity) {
        SetupWizard.setStatusBarHidden(true)
        SystemBarHelper.setBackButtonVisible(context.window, false)
        if (APPLY_SIM_LANGUAGE_ON_ENTRY) applySimLocale()
        initQrProvisioning(context as ComponentActivity)
    }

    fun showLanguagePicker(activity: Activity) {
        val adapter = constructLocaleAdapter(activity)
        AlertDialog.Builder(activity)
            .setTitle(R.string.choose_your_language)
            .setAdapter(adapter) { _, which ->
                updateLocale(
                    adapter.getItem(which)!!.locale
                )
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun applySimLocale() {
        if (simLocaleApplied) return
        val simLocale = getSimLocale() ?: return
        updateLocale(simLocale)
        simLocaleApplied = true
    }

    private fun updateLocale(locale: Locale) {
        LocalePicker.updateLocale(locale)
        refreshCurrentLocale()
    }

    private fun refreshCurrentLocale() {
        WelcomeData.selectedLanguage.value = LocalePicker.getLocales()[0]
    }

    fun accessibilitySettings(context: Activity) {
        SetupWizard.startActivity(context, Intent(ACTION_ACCESSIBILITY))
    }

    fun emergencyCall(context: Activity) {
        SetupWizard.startActivity(
            context,
            appContext.getSystemService(TelecomManager::class.java)!!
                .createLaunchEmergencyDialerIntent(null)
        )
    }

    private fun getSimLocale(): Locale? {
        return appContext.getSystemService(TelephonyManager::class.java)!!.simLocale
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun constructLocaleAdapter(activity: Activity): ArrayAdapter<LocaleInfo> {
        val adapter = LocalePicker.constructAdapter(activity)
        val simLocale = getSimLocale() ?: return adapter
        var localeInfo: LocaleInfo? = null
        for (index in 0..<adapter.count) {
            val item = adapter.getItem(index)
            if (!item?.locale?.toLanguageTag().equals(simLocale.toLanguageTag())) continue
            Log.d(TAG, "constructLocaleAdapter: found simLocale $simLocale")
            localeInfo = item
            adapter.remove(localeInfo)
            break
        }
        if (localeInfo != null) adapter.insert(localeInfo, 0)
        return adapter
    }

    private fun disableOemUnlockByUser() {
        getOemLockManager()?.isOemUnlockAllowedByUser = false
        refreshOemUnlockStatus()
    }

    private fun getOemLockManager(): OemLockManager? {
        return appContext.getSystemService(OemLockManager::class.java)
    }

    fun rebootToBootloader() {
        appContext.getSystemService(PowerManager::class.java)!!.reboot(REBOOT_REASON_BOOTLOADER)
    }

    fun next(activity: Activity) {
        if (Build.isDebuggable() && DebugFlags.getBool("enableUnlockedBootloaderHandling") != true) {
            // we allow free pass for development features on debug builds of the OS
            SetupWizard.next(activity)
            return
        }
        if (SetupWizard.isSecondaryUser) {
            // secondary users should not be bothered for this
            // the device setup (primary user setup) is already done at this point
            SetupWizard.next(activity)
            return
        }
        if (WelcomeData.oemUnlocked.value == true) {
            SetupWizard.startActivity(activity, OemUnlockActivity::class.java)
        } else {
            SetupWizard.next(activity)
        }
    }

    private fun refreshOemUnlockStatus() {
        if (Build.isDebuggable()) {
            val flag = DebugFlags.getBool("isDeviceOemUnlocked_override")
            if (flag != null) {
                WelcomeData.oemUnlocked.value = flag
                return
            }
        }

        WelcomeData.oemUnlocked.value = getOemLockManager()?.isDeviceOemUnlocked ?: false
    }

    fun handleConsecutiveTap(welcomeTapCounter: Int, activity: ComponentActivity) {
        qrToast?.cancel()
        val tapThreshold = activity.resources.getInteger(R.integer.qr_provision_tap_threshold)
        val toastFloor = (tapThreshold - 3).coerceAtLeast(1)
        if (welcomeTapCounter >= tapThreshold) {
            startQrProvisioning()
            return
        }
        if (welcomeTapCounter < toastFloor) return
        val tapsRemaining = tapThreshold - welcomeTapCounter
        val msg = activity.resources.getQuantityString(
            R.plurals.qr_provision_toast,
            tapsRemaining,
            tapsRemaining
        )
        qrToast = Toast.makeText(activity, msg, Toast.LENGTH_LONG).also { it.show() }
    }

    private fun initQrProvisioning(activity: ComponentActivity) {
        // Always re-register for the new activity instance. Holding a launcher in a singleton
        // across activity recreation would leave it bound to a destroyed activity and crash
        // when launched.
        barcodeLauncher = activity.registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(activity, R.string.qr_provisioning_cancelled, Toast.LENGTH_LONG).show()
            } else {
                launchQrProvisioning(activity, result.contents)
            }
        }
    }

    fun startQrProvisioning() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setBeepEnabled(false)
        barcodeLauncher?.launch(options)
    }

    fun launchQrProvisioning(activity: ComponentActivity, contents: String) {
        val intent = Intent(activity, MdmInstallActivity::class.java)
        intent.putExtra(MdmInstallActions.EXTRA_QR_CONTENTS, contents)
        SetupWizard.startActivity(activity, intent)
    }
}
