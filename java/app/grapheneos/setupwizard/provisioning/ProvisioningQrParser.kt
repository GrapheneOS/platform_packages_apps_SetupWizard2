package app.grapheneos.setupwizard.provisioning

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object ProvisioningQrParser {

    private const val TAG = "ProvisioningQrParser"
    private const val MAX_ADMIN_EXTRAS_DEPTH = 8

    private enum class ExtraType { STRING, BOOLEAN, INT, LONG, COMPONENT, PERSISTABLE_BUNDLE }

    // QR code keys forwarded to ManagedProvisioning, typed as its extras parser expects them.
    // Extras that are the initiator's responsibility, like skipping the ownership disclaimer,
    // are left out.
    private val allowedExtras = mapOf(
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME to ExtraType.COMPONENT,
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE to ExtraType.INT,
        DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE to ExtraType.PERSISTABLE_BUNDLE,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_HIDDEN to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SECURITY_TYPE to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PASSWORD to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_HOST to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_PORT to ExtraType.INT,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PROXY_BYPASS to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PAC_URL to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_EAP_METHOD to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_PHASE2_AUTH to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_CA_CERTIFICATE to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_USER_CERTIFICATE to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_IDENTITY to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_ANONYMOUS_IDENTITY to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_WIFI_DOMAIN to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_USE_MOBILE_DATA to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_ALLOW_OFFLINE to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_LOCALE to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_TIME_ZONE to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_LOCAL_TIME to ExtraType.LONG,
        DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_SENSORS_PERMISSION_GRANT_OPT_OUT to ExtraType.BOOLEAN,
        DevicePolicyManager.EXTRA_PROVISIONING_SUPPORTED_MODES to ExtraType.INT,
        DevicePolicyManager.EXTRA_PROVISIONING_ORGANIZATION_NAME to ExtraType.STRING,
        DevicePolicyManager.EXTRA_PROVISIONING_SUPPORT_URL to ExtraType.STRING,
    )

    fun parse(contents: String): Intent? {
        return try {
            toProvisioningIntent(JSONObject(contents))
        } catch (_: JSONException) {
            // exception messages can contain the payload, including Wi-Fi credentials
            Log.w(TAG, "invalid provisioning payload")
            null
        }
    }

    private fun toProvisioningIntent(payload: JSONObject): Intent {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)

        for (key in payload.keys()) {
            val type = allowedExtras[key]
            if (type == null) {
                Log.w(TAG, "ignoring unsupported key $key")
                continue
            }

            putExtra(intent, key, type, payload)
        }

        if (!intent.hasExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME)) {
            throw JSONException("no device admin component")
        }

        // personally-owned work profiles aren't finalized by this flow
        val requestedModes = intent.getIntExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_SUPPORTED_MODES,
            DevicePolicyManager.FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED,
        )
        val supportedModes =
            requestedModes and DevicePolicyManager.FLAG_SUPPORTED_MODES_PERSONALLY_OWNED.inv()
        if (supportedModes == 0) {
            throw JSONException("only personally-owned provisioning was requested")
        }
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SUPPORTED_MODES, supportedModes)

        // finalization, including the DPC's policy compliance screen, is done by
        // ProvisioningFinalizationActivity and requires ManagedProvisioning to return before it
        intent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE,
            true
        )

        intent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_TRIGGER,
            DevicePolicyManager.PROVISIONING_TRIGGER_QR_CODE,
        )

        return intent
    }

    private fun putExtra(intent: Intent, key: String, type: ExtraType, payload: JSONObject) {
        // org.json coerces between scalars, e.g. "true" to a boolean, but would also stringify
        // objects, arrays and null
        val value = payload.get(key)
        val isScalar = value != JSONObject.NULL && value !is JSONObject && value !is JSONArray
        if (type != ExtraType.PERSISTABLE_BUNDLE && !isScalar) {
            throw JSONException("$key must be a scalar")
        }

        when (type) {
            ExtraType.STRING -> {
                intent.putExtra(key, payload.getString(key))
            }

            ExtraType.BOOLEAN -> {
                intent.putExtra(key, payload.getBoolean(key))
            }

            ExtraType.INT -> {
                intent.putExtra(key, payload.getInt(key))
            }

            ExtraType.LONG -> {
                intent.putExtra(key, payload.getLong(key))
            }

            ExtraType.COMPONENT -> {
                val component = ComponentName
                    .unflattenFromString(payload.getString(key))
                    ?: throw JSONException("$key isn't a component name")

                intent.putExtra(key, component)
            }

            ExtraType.PERSISTABLE_BUNDLE -> {
                intent.putExtra(key, toPersistableBundle(payload.getJSONObject(key), depth = 0))
            }
        }
    }

    private fun toPersistableBundle(json: JSONObject, depth: Int): PersistableBundle {
        if (depth > MAX_ADMIN_EXTRAS_DEPTH) {
            throw JSONException("admin extras are nested too deeply")
        }

        val bundle = PersistableBundle()

        for (key in json.keys()) {
            when (val value = json.get(key)) {
                is String -> {
                    bundle.putString(key, value)
                }

                is Boolean -> {
                    bundle.putBoolean(key, value)
                }

                is Int -> {
                    bundle.putInt(key, value)
                }

                is Long -> {
                    bundle.putLong(key, value)
                }

                is Double -> {
                    bundle.putDouble(key, value)
                }

                is JSONObject -> {
                    bundle.putPersistableBundle(key, toPersistableBundle(value, depth + 1))
                }

                is JSONArray -> {
                    bundle.putStringArray(key, Array(value.length(), value::getString))
                }

                else -> {
                    Log.w(TAG, "ignoring admin extra $key of unsupported type")
                }
            }
        }

        return bundle
    }
}
