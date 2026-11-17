package app.grapheneos.setupwizard.provisioning

import android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_LOCAL_TIME
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SKIP_OWNERSHIP_DISCLAIMER
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_SUPPORTED_MODES
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_TRIGGER
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_WIFI_SSID
import android.app.admin.DevicePolicyManager.FLAG_SUPPORTED_MODES_DEVICE_OWNER
import android.app.admin.DevicePolicyManager.FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED
import android.app.admin.DevicePolicyManager.FLAG_SUPPORTED_MODES_PERSONALLY_OWNED
import android.app.admin.DevicePolicyManager.PROVISIONING_TRIGGER_QR_CODE
import android.content.ComponentName
import android.content.Intent
import android.os.PersistableBundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProvisioningQrParserTest {

    @Test
    fun minimalPayload_buildsQrProvisioningIntent() {
        val intent = parse(payload())

        assertThat(intent.action).isEqualTo(ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
        assertThat(intent.componentExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME))
            .isEqualTo(ADMIN)
        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_TRIGGER, -1))
            .isEqualTo(PROVISIONING_TRIGGER_QR_CODE)
        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_SUPPORTED_MODES, -1))
            .isEqualTo(FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED)
    }

    @Test
    fun typedExtras_areForwarded() {
        val intent = parse(
            payload()
                .put(EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION, "https://dpc/x.apk")
                .put(EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE, 42)
                .put(EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
                .put(EXTRA_PROVISIONING_LOCAL_TIME, 1_700_000_000_000L)
        )

        assertThat(intent.getStringExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION))
            .isEqualTo("https://dpc/x.apk")
        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE, -1))
            .isEqualTo(42)
        assertThat(intent.getBooleanExtra(EXTRA_PROVISIONING_SKIP_ENCRYPTION, false)).isTrue()
        assertThat(intent.getLongExtra(EXTRA_PROVISIONING_LOCAL_TIME, -1))
            .isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun scalarsInStringForm_areCoerced() {
        val intent = parse(
            payload()
                .put(EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE, "42")
                .put(EXTRA_PROVISIONING_SKIP_ENCRYPTION, "true")
        )

        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE, -1))
            .isEqualTo(42)
        assertThat(intent.getBooleanExtra(EXTRA_PROVISIONING_SKIP_ENCRYPTION, false)).isTrue()
    }

    @Test
    fun unknownKeys_areIgnored() {
        val intent = parse(payload().put("com.example.UNKNOWN", "value"))

        assertThat(intent.hasExtra("com.example.UNKNOWN")).isFalse()
    }

    @Test
    fun initiatorResponsibilityExtras_areNotForwarded() {
        val intent = parse(
            payload()
                .put(EXTRA_PROVISIONING_SKIP_OWNERSHIP_DISCLAIMER, true)
                .put(EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true)
        )

        assertThat(intent.hasExtra(EXTRA_PROVISIONING_SKIP_OWNERSHIP_DISCLAIMER)).isFalse()
        assertThat(intent.hasExtra(EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS)).isFalse()
    }

    @Test
    fun returnBeforePolicyCompliance_isForcedOn() {
        val intent = parse(payload().put(EXTRA_PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE, false))

        assertThat(intent.getBooleanExtra(EXTRA_PROVISIONING_RETURN_BEFORE_POLICY_COMPLIANCE, false))
            .isTrue()
    }

    @Test
    fun personallyOwnedOnly_isRejected() {
        assertRejected(
            payload().put(EXTRA_PROVISIONING_SUPPORTED_MODES, FLAG_SUPPORTED_MODES_PERSONALLY_OWNED)
        )
    }

    @Test
    fun organizationAndPersonallyOwned_isReducedToOrganizationOwned() {
        val modes = FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED or FLAG_SUPPORTED_MODES_PERSONALLY_OWNED

        val intent = parse(payload().put(EXTRA_PROVISIONING_SUPPORTED_MODES, modes))

        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_SUPPORTED_MODES, -1))
            .isEqualTo(FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED)
    }

    @Test
    fun deviceOwnerMode_isKept() {
        val intent = parse(
            payload().put(EXTRA_PROVISIONING_SUPPORTED_MODES, FLAG_SUPPORTED_MODES_DEVICE_OWNER)
        )

        assertThat(intent.getIntExtra(EXTRA_PROVISIONING_SUPPORTED_MODES, -1))
            .isEqualTo(FLAG_SUPPORTED_MODES_DEVICE_OWNER)
    }

    @Test
    fun missingAdminComponent_isRejected() {
        assertRejected(JSONObject().put(EXTRA_PROVISIONING_WIFI_SSID, "net"))
    }

    @Test
    fun malformedAdminComponent_isRejected() {
        assertRejected(JSONObject().put(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, "no-slash"))
    }

    @Test
    fun invalidJson_isRejected() {
        assertThat(ProvisioningQrParser.parse("not json")).isNull()
        assertThat(ProvisioningQrParser.parse("[]")).isNull()
    }

    @Test
    fun wrongExtraTypes_areRejected() {
        assertRejected(payload().put(EXTRA_PROVISIONING_WIFI_SSID, JSONObject()))
        assertRejected(payload().put(EXTRA_PROVISIONING_WIFI_SSID, JSONArray().put("net")))
        assertRejected(payload().put(EXTRA_PROVISIONING_WIFI_SSID, JSONObject.NULL))
        assertRejected(payload().put(EXTRA_PROVISIONING_SKIP_ENCRYPTION, "yes"))
        assertRejected(payload().put(EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE, "x"))
        assertRejected(payload().put(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, "not an object"))
        assertRejected(
            JSONObject().put(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, JSONObject())
        )
    }

    @Test
    fun adminExtras_areConvertedRecursively() {
        val extras = JSONObject()
            .put("string", "value")
            .put("boolean", true)
            .put("int", 7)
            .put("long", 1L shl 40)
            .put("double", 1.5)
            .put("array", JSONArray().put("a").put("b"))
            .put("nested", JSONObject().put("inner", "x"))

        val intent = parse(payload().put(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, extras))

        val bundle = intent.persistableBundleExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        assertThat(bundle.getString("string")).isEqualTo("value")
        assertThat(bundle.getBoolean("boolean")).isTrue()
        assertThat(bundle.getInt("int")).isEqualTo(7)
        assertThat(bundle.getLong("long")).isEqualTo(1L shl 40)
        assertThat(bundle.getDouble("double")).isEqualTo(1.5)
        assertThat(bundle.getStringArray("array")).asList().containsExactly("a", "b").inOrder()
        assertThat(bundle.getPersistableBundle("nested")?.getString("inner")).isEqualTo("x")
    }

    @Test
    fun adminExtras_atMaximumDepth_areAccepted() {
        val intent = parse(payload().put(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, nested(8)))

        var bundle = intent.persistableBundleExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        repeat(8) {
            bundle = checkNotNull(bundle.getPersistableBundle("child"))
        }
        assertThat(bundle.getString("leaf")).isEqualTo("x")
    }

    @Test
    fun adminExtras_nestedTooDeeply_areRejected() {
        assertRejected(payload().put(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, nested(9)))
    }

    private fun payload(): JSONObject {
        return JSONObject().put(EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, ADMIN.flattenToString())
    }

    private fun nested(levels: Int): JSONObject {
        var json = JSONObject().put("leaf", "x")
        repeat(levels) {
            json = JSONObject().put("child", json)
        }
        return json
    }

    private fun parse(payload: JSONObject): Intent {
        return checkNotNull(ProvisioningQrParser.parse(payload.toString())) {
            "payload rejected: $payload"
        }
    }

    private fun assertRejected(payload: JSONObject) {
        assertThat(ProvisioningQrParser.parse(payload.toString())).isNull()
    }

    private fun Intent.componentExtra(key: String): ComponentName? {
        return getParcelableExtra(key, ComponentName::class.java)
    }

    private fun Intent.persistableBundleExtra(key: String): PersistableBundle {
        return checkNotNull(getParcelableExtra(key, PersistableBundle::class.java))
    }

    private companion object {
        val ADMIN = ComponentName("com.example.dpc", "com.example.dpc.AdminReceiver")
    }
}
