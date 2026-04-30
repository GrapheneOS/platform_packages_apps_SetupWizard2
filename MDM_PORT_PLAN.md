# Port Headwind MDM QR-provisioning patch to GrapheneOS SetupWizard2 (Android 16)

## Context

The Android-15-based fork at `MoChahadeh/grapheneos-setup-wizard` carries the Headwind MDM patch (originally proposed as upstream PR https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2/pull/40, still open and unmerged). The patch turns the GrapheneOS setup wizard into an enrollment entry point: tapping the Welcome screen 6 times reveals a hidden QR scanner, and a properly-formatted QR provisions the device as device-owner under an MDM (Headwind, MobileIron, etc.). End-to-end the flow is functional and uses the standard Android `ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE` intent — no GrapheneOS-specific bypasses.
GrapheneOS upstream has since moved to the Android-16 branch `16-qpr2` at https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2/tree/16-qpr2. We need to re-apply the MDM patch on top of that branch, while:

1. **Fixing bugs** discovered in the Android-15 implementation (NPE crash, BroadcastReceiver leak, unsafe PendingIntent flag, missing HTTPS validation, dead/commented code).

2. **Modernizing deprecated APIs** (`onActivityResult` → `ActivityResultContract`, raw `Executor` → `lifecycleScope`/`viewModelScope`, singleton ViewModel → activity-scoped).

3. **Adapting to Android-16 upstream drift** — upstream added `UpdaterSecurityPreviewActivity`, three permissions, and refactored `Android.bp` to put the privapp xml under `etc/permissions/`. The patch must layer on top without disturbing those.
   
   ## Sources of truth
- **Android-16 base (target):** `https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2` branch `16-qpr2`

- **Android-15 fork with patch (source of patch content):** `https://github.com/MoChahadeh/grapheneos-setup-wizard` — every NEW file listed below should be copied from here verbatim except where this plan says otherwise

- **Upstream PR for reference only:** `https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2/pull/40`
  
  ## Plan structure
  
  This plan is organized in five phases. Execute them in order:
1. Add third-party libraries (ZXing, Jackson) to the build
2. Copy NEW files from the Android-15 fork (with edits noted)
3. Modify existing upstream files (`WelcomeActivity`, `WelcomeActions`, `AndroidManifest.xml`, `Android.bp`, `res/values/strings.xml`)
4. Apply bug fixes and modernizations
5. Verify end-to-end
   Each phase below lists exact file paths, line-anchored edits, and what to copy.

---

## Phase 1 — Add third-party libraries

The patch needs ZXing (QR scan UI + decoder) and Jackson (JSON parsing of the QR payload). The Android-15 fork ships them as prebuilt JAR/AAR under `libs/`. Reviewers on PR #40 flagged this as a maintainability concern but it is the only practical option short of building from source — keep the prebuilt approach.

### 1.1 Copy library binaries

From the Android-15 fork copy these files verbatim into a new `libs/` directory at the repo root:

- `libs/zxing-android-embedded-4.3.0.aar`

- `libs/zxing-core-3.4.1.jar`

- `libs/jackson-core-2.18.2.jar`

- `libs/jackson-databind-2.18.2.jar`

- `libs/jackson-annotations-2.18.2.jar`
  
  ### 1.2 Create `libs/Android.bp`
  
  Copy `libs/Android.bp` from the fork verbatim. It declares five Soong imports:
  
  ```
  android_library_import {
  name: "setupwizard2-zxing-android",
  aars: ["zxing-android-embedded-4.3.0.aar"],
  sdk_version: "current",
  }
  java_import {
  name: "setupwizard2-zxing-core",
  jars: ["zxing-core-3.4.1.jar"],
  sdk_version: "current",
  }
  java_import {
  name: "setupwizard2-jackson-core",
  jars: ["jackson-core-2.18.2.jar"],
  sdk_version: "current",
  }
  java_import {
  name: "setupwizard2-jackson-databind",
  jars: ["jackson-databind-2.18.2.jar"],
  sdk_version: "current",
  }
  java_import {
  name: "setupwizard2-jackson-annotations",
  jars: ["jackson-annotations-2.18.2.jar"],
  sdk_version: "current",
  }
  ```
  
  ### 1.3 Wire libs into the main `Android.bp`
  
  Edit the upstream root `Android.bp`. Inside the `static_libs:` array of the `android_app { name: "SetupWizard2", ... }` block, append:
  
  ```
  "setupwizard2-jackson-core",
  "setupwizard2-jackson-databind",
  "setupwizard2-jackson-annotations",
  "setupwizard2-zxing-android",
  "setupwizard2-zxing-core",
  ```
  
  **Do not** modify the `required:` entry or the `prebuilt_etc` block — upstream-16 has refactored those to use `etc_permissions_app.grapheneos.setupwizard` with `src: "etc/permissions/app.grapheneos.setupwizard.xml"`. Leave them as-is.

---

## Phase 2 — Add new source files

All paths below are relative to the SetupWizard2 repo root. Copy each file verbatim from the Android-15 fork unless this section calls out a fix to apply during the copy. **Bug fixes from Phase 4 should be folded in as you copy** — the per-file callouts below summarise which fixes apply where, and Phase 4 lists the diff to apply.

### 2.1 New utility — gesture detection

- **`java/app/grapheneos/setupwizard/android/ConsecutiveTapsGestureDetector.kt`** — copy verbatim. Counts consecutive `ACTION_UP` touches within the platform's double-tap slop and fires a callback per tap. No fixes needed.
  
  ### 2.2 New activities

- **`java/app/grapheneos/setupwizard/view/activity/MdmInstallActivity.kt`** — copy with Phase 4 fixes:
  
  - Replace deprecated `onActivityResult` override with the WiFi-result `ActivityResultLauncher` already used elsewhere in upstream (or with a `registerForActivityResult(StartActivityForResult())` field).
  - Bind to an activity-scoped `MdmInstallViewModel` rather than the singleton object (see 2.5).

- **`java/app/grapheneos/setupwizard/view/activity/ProvisionActivity.kt`** — copy with Phase 4 fixes:
  
  - Replace deprecated `onActivityResult(int, int, Intent)` with two `registerForActivityResult(StartActivityForResult())` launchers (one for the provisioning intent, one for the finalization intent).
    
    ### 2.3 New action layer

- **`java/app/grapheneos/setupwizard/action/MdmInstallActions.kt`** — copy with Phase 4 fixes:
  
  - Add `return` after the `qrContent == null` error branch (NPE fix).
  - Validate `downloadLocation` is `https://` before opening the connection.
  - Move `BroadcastReceiver` registration/unregistration into a `try/finally` and use `Context.RECEIVER_NOT_EXPORTED` (the install-status broadcast comes from `system_server` to the receiver's own action; not exporting it eliminates the spoofing surface).
  - Replace the singleton `Executor` with `lifecycleScope`/`Dispatchers.IO` driven from `MdmInstallActivity`, or — simpler — keep the executor but track the `Future<*>` so the activity can cancel it in `onDestroy`.
  - Add a recursion-depth guard to `jsonToPersistableBundle` (cap at e.g. 8 levels) to bound DoS via deeply nested QR payloads.

- **`java/app/grapheneos/setupwizard/action/ProvisionActions.kt`** — copy with Phase 4 fixes:
  
  - Delete the commented-out `disableSelfAndFinish` block and the commented profile-owner factory-reset block. Either implement them properly or remove. Recommendation: keep a real `disableSelfAndFinish()` that disables the wizard's own component after `setProvisioningState()` succeeds, so the device doesn't try to re-run setup after MDM hands off.
  - Convert callers from `startActivityForResult` to `ActivityResultLauncher`-based equivalents wired in `ProvisionActivity` (see 2.2).

- **`java/app/grapheneos/setupwizard/action/AppInstaller.kt`** — copy with Phase 4 fixes:
  
  - **Critical:** Drop `FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT`. Make the install-callback `Intent` explicit by setting `intent.setPackage(context.packageName)`. Keep `FLAG_MUTABLE` (PackageInstaller requires it to inject `EXTRA_STATUS`). The combination "explicit + mutable" is the documented safe pattern.
  
  - Increase the read buffer in the install loop to a single constant (e.g. `64 * 1024`) — match the download buffer for consistency.
    
    ### 2.4 New data layer

- **`java/app/grapheneos/setupwizard/data/MdmInstallData.kt`** — **rewrite as `MdmInstallViewModel.kt`**. The fork ships this as a Kotlin `object` (singleton) inheriting `ViewModel`, which leaks observers across activity recreations. Replace with a regular `class MdmInstallViewModel : ViewModel()` and acquire it in `MdmInstallActivity` via `by viewModels()`. Move the `init { … }` side-effect into an explicit `start(intent: Intent)` method called from the activity's `onCreate`. Field shape (`spinnerVisible`, `progressVisible`, `message`, `downloadProgress`, `downloadProgressLegend`, `error`, `complete` as `MutableLiveData`) stays the same.
  
  ### 2.5 New resources

- **`res/layout/activity_mdm_install.xml`** — copy verbatim. GlifLayout wrapping a header (`Powered by Headwind MDM`), a spinner ProgressBar, a status TextView, a horizontal ProgressBar, and a legend TextView.

- **`res/drawable/baseline_provisioning.xml`** — copy verbatim.

- **`res/drawable/baseline_provisioning_glif.xml`** — copy verbatim.

---

## Phase 3 — Modify existing upstream files

These are surgical additions on top of the Android-16 base. **Do not** wholesale-replace upstream files — apply only the diff described.

### 3.1 `AndroidManifest.xml`

After the existing `<uses-permission>` lines (the last upstream entry is `app.seamlessupdate.client.OPEN_SECURITY_PREVIEW_SETTINGS`), add four new permissions:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<!-- To start the device owner provisioning workflow -->
<uses-permission android:name="android.permission.DISPATCH_PROVISIONING_MESSAGE"/>
<!-- To install the admin app -->
<uses-permission android:name="android.permission.INSTALL_PACKAGES"/>
<!-- To factory reset if provisioning failed -->
<uses-permission android:name="android.permission.MASTER_CLEAR"/>
```

On the `<application>` tag, add `android:hardwareAccelerated="true"` (required by the ZXing camera `SurfaceView`).
After the existing activity entries (the last upstream one is `FinishActivity`; **preserve `UpdaterSecurityPreviewActivity` which is new in 16-qpr2**), add:

```xml
<activity android:name=".view.activity.MdmInstallActivity" />
<activity android:name=".view.activity.ProvisionActivity" />
<activity
 android:name="com.journeyapps.barcodescanner.CaptureActivity"
 android:screenOrientation="portrait"
 tools:replace="screenOrientation"
 android:exported="false"/>
```

### 3.2 `java/app/grapheneos/setupwizard/view/activity/WelcomeActivity.kt`

Apply the following additions (the upstream file is the standard 16-qpr2 `WelcomeActivity`):

1. Add imports:
   
   ```kotlin
   import android.view.MotionEvent
   import app.grapheneos.setupwizard.android.ConsecutiveTapsGestureDetector
   ```

2. Add a nullable field next to `letsSetupText`:
   
   ```kotlin
   private var consecutiveTapsGestureDetector: ConsecutiveTapsGestureDetector? = null
   ```

3. Add an `onResume` override that calls `consecutiveTapsGestureDetector?.resetCounter()` after `super.onResume()`.

4. At the end of `bindViews()`, instantiate the detector against the root layout:
   
   ```kotlin
   consecutiveTapsGestureDetector = ConsecutiveTapsGestureDetector(
   onConsecutiveTapsListener,
   requireViewById(R.id.root_layout)
   )
   ```

5. Add `dispatchTouchEvent` override that forwards `ACTION_UP` events to the detector after `super.dispatchTouchEvent(ev)`.

6. Add the listener field that calls `WelcomeActions.handleConsecutiveTap(welcomeTapCounter, this@WelcomeActivity)`.
   Reference: the fork's `java/app/grapheneos/setupwizard/view/activity/WelcomeActivity.kt` lines 6, 20, 34, 47–50, 71–74, 89–95, 97–102.
   
   ### 3.3 `java/app/grapheneos/setupwizard/action/WelcomeActions.kt`
   
   Apply the following additions to upstream's `WelcomeActions`:

7. Add imports:
   
   ```kotlin
   import android.widget.Toast
   import androidx.activity.ComponentActivity
   import androidx.activity.result.ActivityResultLauncher
   import app.grapheneos.setupwizard.view.activity.MdmInstallActivity
   import com.journeyapps.barcodescanner.ScanContract
   import com.journeyapps.barcodescanner.ScanOptions
   ```
   
   **Note:** the fork imports `androidx.appcompat.app.AppCompatActivity`, but `ComponentActivity` is the minimum needed for `registerForActivityResult` and avoids a dependency on AppCompat. Use `ComponentActivity` and cast accordingly.

8. Add two private fields next to `simLocaleApplied`:
   
   ```kotlin
   private var qrToast: Toast? = null
   private var barcodeLauncher: ActivityResultLauncher<ScanOptions>? = null
   ```

9. In `handleEntry(context: Activity)`, after the existing body, add:
   
   ```kotlin
   initQrProvisioning(context as ComponentActivity)
   ```

10. Append four new methods at the end of the `object`:
    
    - `fun handleConsecutiveTap(welcomeTapCounter: Int, activity: ComponentActivity)` — at `>= 6` taps call `startQrProvisioning()`; at 3–5 show a `Toast` built from `R.plurals.qr_provision_toast` with `tapsRemaining = 6 - welcomeTapCounter`; below 3, no-op. Cancel any prior `qrToast` first.
    
    - `private fun initQrProvisioning(activity: ComponentActivity)` — `activity.registerForActivityResult(ScanContract()) { result -> if (result.contents == null) Toast.makeText(...).show(); else launchQrProvisioning(activity, result.contents) }`.
    
    - `fun startQrProvisioning()` — build `ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false)` and `barcodeLauncher?.launch(options)`.
    
    - `fun launchQrProvisioning(activity: ComponentActivity, contents: String)` — build an explicit `Intent(activity, MdmInstallActivity::class.java)` with `MdmInstallActions.EXTRA_QR_CONTENTS` and dispatch via `SetupWizard.startActivity(activity, intent)`.
      Reference: the fork's `java/app/grapheneos/setupwizard/action/WelcomeActions.kt` lines 13–14, 21, 26–27, 35–36, 48, 157–200.
      
      ### 3.4 `res/values/strings.xml`
      
      Append the strings the patch introduces. From the fork (`res/values/strings.xml` lines 59–82) copy the following keys verbatim:
- `qr_provision_toast` (plurals, with `one` and `other` forms describing the remaining-taps countdown)
- `qr_provisioning_cancelled`
- `provisioning_title`
- `provisioning_desc`
- `provisioning_powered` (the `Powered by Headwind MDM` string)
- All `provisioning_*` workflow strings (preparing, parse_failed, validating_params, wifi_setup, downloading, validating_checksum, installing, install_failed, success, etc.)
  Do not modify any existing strings.

---

## Phase 4 — Bug fixes and modernization (consolidated)

These fixes are referenced from Phase 2/3 but listed here so reviewers can see the security/correctness diff at a glance. Each is anchored to the fork's source so you can see what the original code looks like before fixing it.

### 4.1 Critical security: explicit + immutable PendingIntent (where possible)

**File:** the new `AppInstaller.kt` (Phase 2.3). Fork reference: `createIntentSender()` builds the `PendingIntent` with `FLAG_MUTABLE or FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT`.
`PackageInstaller.commit(IntentSender)` requires `FLAG_MUTABLE` because `system_server` injects `EXTRA_STATUS` and friends. The unsafe part isn't the mutability — it's the **implicit** intent. Fix:

```kotlin
val intent = Intent(ACTION_INSTALL_COMPLETE).apply {
 setPackage(context.packageName) // make it explicit
}
val pi = PendingIntent.getBroadcast(
 context, 0, intent,
 PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
)
```

This satisfies `PendingIntent`'s explicit-target requirement on Android 14+ without needing `FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT`.

### 4.2 Critical security: HTTPS validation on APK download

**File:** `MdmInstallActions.kt` `downloadAdminAppSync`. Before opening the connection:

```kotlin
val url = URL(downloadLocation)
require(url.protocol.equals("https", ignoreCase = true)) {
 "Refusing non-HTTPS download URL"
}
```

On failure, post a localized error to `MdmInstallViewModel.error` and return — do not fall back to HTTP.

### 4.3 Critical correctness: NPE in `handleEntry`

**File:** `MdmInstallActions.kt` `handleEntry`. Fork's current code posts an error then falls through to `parseProvisioningQr(context, qrContent!!)`, which crashes. Fix:

```kotlin
val qrContent = context.intent.getStringExtra(EXTRA_QR_CONTENTS)
if (qrContent == null) {
 MdmInstallViewModel.error.postValue(context.getString(R.string.qr_parse_failed))
 return
}
```

### 4.4 Lifecycle: BroadcastReceiver leak

**File:** `MdmInstallActions.kt` `installAdminApp`. Today the receiver is registered before the install but only unregistered on the error path. Fix:

- Move `registerReceiver` and `unregisterReceiver` calls into `MdmInstallActivity` lifecycle (`onStart` / `onStop`), exposing the receiver as a property of the ViewModel.

- Register with `Context.RECEIVER_NOT_EXPORTED` (the install-status broadcast originates from `system_server` to a private action; not exporting eliminates third-party-app spoofing).

- Drop the bare `try { unregisterReceiver(...) } catch (_: Exception) {}` in `handleError` once registration moves to lifecycle methods.
  
  ### 4.5 Lifecycle: cancel background work on activity destroy
  
  **File:** `MdmInstallActions.kt`. The fork uses a singleton `Executors.newSingleThreadExecutor()` whose tasks call `postValue` on shared `LiveData` regardless of whether the activity is alive. Fix:

- Drive download/install from `MdmInstallViewModel.viewModelScope.launch(Dispatchers.IO) { ... }`.

- ViewModel scope is automatically cancelled when the activity is finished (not just rotated), which is the correct semantics here.

- Inside the coroutine, use `withContext(Dispatchers.Main)` for `LiveData.setValue` if needed; `postValue` from IO is also fine.
  
  ### 4.6 Modernization: `ActivityResultContract` everywhere
  
  **Files:** `MdmInstallActivity.kt`, `ProvisionActivity.kt`, `ProvisionActions.kt`.
  Replace deprecated `onActivityResult` overrides and `startActivityForResult` calls with `registerForActivityResult(StartActivityForResult())` launchers held as activity fields. `ProvisionActivity` needs **two** launchers — one for the provisioning intent (returns `RESULT_CODE_DEVICE_OWNER_SET` = 123), one for the finalization intent. `MdmInstallActivity` needs one launcher for the WiFi result.
  
  ### 4.7 Hygiene: remove dead code
  
  **File:** `ProvisionActions.kt`.

- Delete the commented-out profile-owner factory-reset block (around fork lines 75–78).

- Decide on `disableSelfAndFinish()`: either implement it (call `PackageManager.setComponentEnabledSetting(WelcomeActivity, COMPONENT_ENABLED_STATE_DISABLED)` on success) or remove it. **Recommendation:** implement it so the wizard doesn't relaunch after MDM hands off.
  
  ### 4.8 Hardening: bound recursion in `jsonToPersistableBundle`
  
  **File:** `MdmInstallActions.kt`. Add a `depth: Int = 0` parameter, bail out (return empty bundle + error) at `depth > 8`, and pass `depth + 1` on recursive calls. Prevents a malicious/malformed QR from exhausting stack via deeply nested objects.
  
  ### 4.9 Hygiene: tap threshold as a resource
  
  **File:** `WelcomeActions.kt`. Move the literal `6` to `res/values/integers.xml` as `qr_provision_tap_threshold` and read via `activity.resources.getInteger(...)`. Cheap to do, makes future tuning a one-line change.

---

## Phase 5 — Verification

After implementation, validate in this order. Type-checking and the test suite verify code correctness, not feature correctness — final validation requires a real device or emulator.

### 5.1 Build

- `m SetupWizard2` from an Android-16 GrapheneOS build tree, or whichever build target the upstream README documents. Resolve any Soong errors first; ZXing/Jackson should resolve via the new `libs/Android.bp`.

- Confirm the resulting APK is signed with the platform certificate (`aapt dump badging` → `application-label` plus `apksigner verify --print-certs` showing the platform cert).
  
  ### 5.2 Static checks

- Run the upstream lint/format pipeline (the repo README documents this; typically `m`-based or `lint` invocations under the build tree).

- Inspect the manifest with `aapt dump permissions` to confirm only the four new permissions were added.
  
  ### 5.3 Device flow (golden path)
  
  On an Android-16 GrapheneOS build, factory-reset and reach the Welcome screen:
1. Tap the screen 3 times → toast appears: "3 more taps to start QR code setup".

2. Tap a 4th and 5th time → toast updates to "2 more" / "1 more".

3. Tap a 6th time → ZXing camera opens.

4. Scan a QR encoding the JSON payload (admin component name, HTTPS download URL, base64 SHA-256 checksum, optional `extrasBundle`).

5. WiFi setup → APK download with progress → silent install → device-owner provisioning intent → finalization → `FinishActivity`.

6. Reboot. Confirm `dpm dump-policy` (or `adb shell dpm list-owners`) reports the MDM admin as device owner.
   
   ### 5.4 Edge cases
- QR scanner cancelled (back button): wizard returns to Welcome with a toast, no crash.

- QR with non-HTTPS URL: error dialog, factory-reset prompt or graceful exit.

- QR with bad JSON: error dialog, no NPE.

- QR with missing required field: error dialog with `validating_params_failed` string.

- Download checksum mismatch: error dialog with `validating_checksum_failed` string.

- APK download interrupted (airplane mode mid-download): error dialog, retry path.

- Re-running the wizard after provisioning succeeded: `WizardManagerHelper.isUserSetupComplete` should already short-circuit `WelcomeActivity.onCreate`.
  
  ### 5.5 Reference materials

- Original Headwind PR (for QR JSON schema and reviewer feedback): https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2/pull/40

- Source-of-truth fork for new files: https://github.com/MoChahadeh/grapheneos-setup-wizard

- Upstream Android-16 base: https://github.com/GrapheneOS/platform_packages_apps_SetupWizard2/tree/16-qpr2

---

## Critical files summary

| File                                                                        | Action                                                        |
| --------------------------------------------------------------------------- | ------------------------------------------------------------- |
| `Android.bp`                                                                | **Modify** — append 5 static_libs                             |
| `libs/Android.bp`                                                           | **New**                                                       |
| `libs/*.aar`, `libs/*.jar`                                                  | **New** (5 binaries)                                          |
| `AndroidManifest.xml`                                                       | **Modify** — 4 perms, `hardwareAccelerated`, 3 activities     |
| `res/values/strings.xml`                                                    | **Modify** — append patch strings                             |
| `res/layout/activity_mdm_install.xml`                                       | **New**                                                       |
| `res/drawable/baseline_provisioning*.xml`                                   | **New** (2 files)                                             |
| `java/app/grapheneos/setupwizard/view/activity/WelcomeActivity.kt`          | **Modify**                                                    |
| `java/app/grapheneos/setupwizard/action/WelcomeActions.kt`                  | **Modify**                                                    |
| `java/app/grapheneos/setupwizard/android/ConsecutiveTapsGestureDetector.kt` | **New**                                                       |
| `java/app/grapheneos/setupwizard/view/activity/MdmInstallActivity.kt`       | **New** (with fixes)                                          |
| `java/app/grapheneos/setupwizard/view/activity/ProvisionActivity.kt`        | **New** (with fixes)                                          |
| `java/app/grapheneos/setupwizard/action/MdmInstallActions.kt`               | **New** (with fixes)                                          |
| `java/app/grapheneos/setupwizard/action/ProvisionActions.kt`                | **New** (with fixes)                                          |
| `java/app/grapheneos/setupwizard/action/AppInstaller.kt`                    | **New** (with fixes)                                          |
| `java/app/grapheneos/setupwizard/data/MdmInstallViewModel.kt`               | **New** (rewritten from fork's `MdmInstallData.kt` singleton) |
