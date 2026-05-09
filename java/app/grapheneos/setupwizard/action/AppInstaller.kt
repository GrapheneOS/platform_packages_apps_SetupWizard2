package app.grapheneos.setupwizard.action

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.FileInputStream

object AppInstaller {
    private const val TAG = "AppInstaller"

    const val ACTION_INSTALL_COMPLETE = "app.grapheneos.setupwizard.INSTALL_COMPLETE"
    const val PACKAGE_NAME = "PACKAGE_NAME"
    const val IO_BUFFER_SIZE = 64 * 1024

    fun silentInstallApplication(
        context: Context,
        file: File
    ): String? {
        try {
            val packageManager: PackageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageArchiveInfo(file.path, 0)
                ?: throw Exception("Failed to parse the admin app package")

            val packageName = packageInfo.packageName

            Log.i(TAG, "Installing $packageName")
            val input = FileInputStream(file)
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            params.setAppPackageName(packageName)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            val out = session.openWrite("COSU", 0, -1)
            val buffer = ByteArray(IO_BUFFER_SIZE)
            var c: Int
            while (input.read(buffer).also { c = it } != -1) {
                out.write(buffer, 0, c)
            }
            session.fsync(out)
            input.close()
            out.close()
            session.commit(
                createIntentSender(
                    context,
                    sessionId,
                    packageName
                )
            )
            Log.i(TAG, "Installation session committed")
            return null
        } catch (e: Exception) {
            Log.w(TAG, "PackageInstaller error: ${e.message}", e)
            return e.message
        }
    }

    private fun createIntentSender(context: Context, sessionId: Int, packageName: String?): IntentSender {
        val intent = Intent(ACTION_INSTALL_COMPLETE).apply {
            // Make the broadcast explicit so PendingIntent's explicit-target requirement
            // is satisfied without needing FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT.
            setPackage(context.packageName)
            if (packageName != null) {
                putExtra(PACKAGE_NAME, packageName)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return pendingIntent.intentSender
    }

    fun getPackageInstallerStatusMessage(status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> "PENDING_USER_ACTION"
            PackageInstaller.STATUS_SUCCESS -> "SUCCESS"
            PackageInstaller.STATUS_FAILURE -> "FAILURE_UNKNOWN"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "BLOCKED"
            PackageInstaller.STATUS_FAILURE_ABORTED -> "ABORTED"
            PackageInstaller.STATUS_FAILURE_INVALID -> "INVALID"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "CONFLICT"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "STORAGE"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "INCOMPATIBLE"
            else -> "UNKNOWN"
        }
    }
}
