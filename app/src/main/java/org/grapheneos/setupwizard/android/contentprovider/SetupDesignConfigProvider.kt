package org.grapheneos.setupwizard.android.contentprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

// TODO: remove this class // only for testing
class SetupDesignConfigProvider : ContentProvider() {

    companion object {
        private val TAG = SetupDesignConfigProvider::class.java.simpleName
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException()
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException()
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        Log.d(
            TAG,
            "method: $method, caller: $callingPackage"
        )
        val res = Bundle()
        when (method) {
            "suwDefaultThemeString" -> res.putString(method, "glif_v4_light")
            "applyGlifThemeControlledTransition", "isDynamicColorEnabled",
            "isEmbeddedActivityOnePaneEnabled", "isFullDynamicColorEnabled",
            "IsMaterialYouStyleEnabled", "isNeutralButtonStyleEnabled",
            "isSuwDayNightEnabled" -> res.putBoolean(
                method,
                true
            )

            "getDeviceName" -> {
                var name = Settings.Global.getString(
                    context!!.contentResolver,
                    Settings.Global.DEVICE_NAME
                )
                if (TextUtils.isEmpty(name)) {
                    name = Build.MODEL
                }
                res.putCharSequence(method, name)
            }
        }
        return res
    }
}