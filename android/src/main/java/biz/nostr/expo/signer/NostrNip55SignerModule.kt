package biz.nostr.expo.signer

import android.content.Context
import android.content.Intent
import biz.nostr.android.nip55.AppInfo
import biz.nostr.android.nip55.IntentBuilder
import biz.nostr.android.nip55.Signer
import biz.nostr.expo.signer.exceptions.ActivityAlreadyStartedException
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.exception.toCodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class NostrNip55SignerModule : Module() {

    private val context: Context
        get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

    private var signerPackageName: String? = null

    // region: Fields for fallback approach
    private var pendingPromise: Promise? = null
    private var pendingRequestCode: Int = 0
    private val callsMap: MutableMap<Int, CallData> = mutableMapOf()

    override fun definition() = ModuleDefinition {
        Name("ExpoNostrSignerModule")

        Events("onChange")

        AsyncFunction("isExternalSignerInstalled") { packageName: String ->
            val resolveInfoList: List<Any> = Signer.isExternalSignerInstalled(context, packageName)
            // Return true if the list isn't empty, false otherwise
            resolveInfoList.isNotEmpty()
        }

        AsyncFunction("getInstalledSignerApps") {
            val signerAppInfos: List<AppInfo> = Signer.getInstalledSignerApps(context)
            signerAppInfos.map { info ->
                mapOf(
                        "name" to info.name,
                        "packageName" to info.packageName,
                        "iconData" to info.iconData,
                        "iconUrl" to info.iconUrl
                )
            }
        }

        AsyncFunction("setPackageName") { packageName: String ->
            if (packageName.isBlank()) {
                throw IllegalArgumentException("Missing or empty packageName parameter")
            }
            signerPackageName = packageName
        }

        AsyncFunction("getPublicKey") { pkgName: String?, permissions: String?, promise: Promise ->
            val packageName = getPackageNameFromCall(pkgName)
            val publicKey: String? = Signer.getPublicKey(context, packageName)
            if (publicKey != null) {
                // Direct approach success
                val resultMap = mapOf("npub" to publicKey, "package" to packageName)
                promise.resolve(resultMap)
            } else {
                // Fallback approach
                launchFallbackIntent(
                        requestCode = REQUEST_GET_PUBLIC_KEY,
                        intent = IntentBuilder.getPublicKeyIntent(packageName, permissions),
                        promise = promise
                )
            }
        }

        AsyncFunction("signEvent") {
                pkgName: String?,
                eventJson: String,
                eventId: String,
                npub: String,
                promise: Promise ->
            val packageName = getPackageNameFromCall(pkgName)
            val signedEvent: Array<String>? =
                Signer.signEvent(context, packageName, eventJson, npub)
            if (signedEvent != null) {
                val resultMap =
                        mapOf(
							"signature" to signedEvent[0],
							"id" to eventId,
							"event" to signedEvent[1]
                        )
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.signEventIntent(packageName, eventJson, eventId, npub)
                launchFallbackIntent(REQUEST_SIGN_EVENT, intent, promise)
            }
        }

        AsyncFunction("nip04Encrypt") {
                packageName: String?,
                plainText: String,
                id: String,
                pubKey: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val encryptedText = Signer.nip04Encrypt(context, pkg, plainText, npub, pubKey)
            if (encryptedText != null) {
                val resultMap = mapOf("result" to encryptedText, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.nip04EncryptIntent(pkg, plainText, id, npub, pubKey)
                launchFallbackIntent(REQUEST_NIP_04_ENCRYPT, intent, promise)
            }
        }

        AsyncFunction("nip04Decrypt") {
                packageName: String?,
                encryptedText: String,
                id: String,
                pubKey: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val decryptedText = Signer.nip04Decrypt(context, pkg, encryptedText, npub, pubKey)
            if (decryptedText != null) {
                val resultMap = mapOf("result" to decryptedText, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.nip04DecryptIntent(pkg, encryptedText, id, npub, pubKey)
                launchFallbackIntent(REQUEST_NIP_04_DECRYPT, intent, promise)
            }
        }

        AsyncFunction("nip44Encrypt") {
                packageName: String?,
                plainText: String,
                id: String,
                pubKey: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val encryptedText = Signer.nip44Encrypt(context, pkg, plainText, npub, pubKey)
            if (encryptedText != null) {
                val resultMap = mapOf("result" to encryptedText, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.nip44EncryptIntent(pkg, plainText, id, npub, pubKey)
                launchFallbackIntent(REQUEST_NIP_44_ENCRYPT, intent, promise)
            }
        }

        AsyncFunction("nip44Decrypt") {
                packageName: String?,
                encryptedText: String,
                id: String,
                pubKey: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val decryptedText = Signer.nip44Decrypt(context, pkg, encryptedText, npub, pubKey)
            if (decryptedText != null) {
                val resultMap = mapOf("result" to decryptedText, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.nip44DecryptIntent(pkg, encryptedText, id, npub, pubKey)
                launchFallbackIntent(REQUEST_NIP_44_DECRYPT, intent, promise)
            }
        }

        AsyncFunction("decryptZapEvent") {
                packageName: String?,
                eventJson: String,
                id: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val decryptedEventJson = Signer.decryptZapEvent(context, pkg, eventJson, npub)
            if (decryptedEventJson != null) {
                val resultMap = mapOf("result" to decryptedEventJson, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.decryptZapEventIntent(pkg, eventJson, id, npub)
                launchFallbackIntent(REQUEST_DECRYPT_ZAP_EVENT, intent, promise)
            }
        }

        AsyncFunction("getRelays") {
                packageName: String?,
                id: String,
                npub: String,
                promise: Promise ->
            val pkg = getPackageNameFromCall(packageName)
            val relayJson = Signer.getRelays(context, pkg, npub)
            if (relayJson != null) {
                val resultMap = mapOf("result" to relayJson, "id" to id)
                promise.resolve(resultMap)
            } else {
                val intent = IntentBuilder.getRelaysIntent(pkg, id, npub)
                launchFallbackIntent(REQUEST_GET_RELAYS, intent, promise)
            }
        }

        OnActivityResult { _, payload ->
            callsMap[payload.requestCode] ?: return@OnActivityResult
            callsMap.remove(payload.requestCode)

            // If the user canceled the operation
            if (payload.resultCode == android.app.Activity.RESULT_CANCELED) {
                pendingPromise?.reject("ActivityCancelled", "Activity Cancelled", null)
				pendingPromise = null
                return@OnActivityResult
            }

            // If we don't have an intent or extras, we can't proceed
            val dataIntent = payload.data
            if (dataIntent == null) {
                pendingPromise?.reject("ERROR", "No data returned from activity", null)
				pendingPromise = null
                return@OnActivityResult
            }

            when (payload.requestCode) {
                REQUEST_GET_PUBLIC_KEY -> {
                    val npub = dataIntent.getStringExtra("signature")
                    val packageName = dataIntent.getStringExtra("package")

                    val resultMap = mutableMapOf<String, Any?>()
                    resultMap["npub"] = npub
                    resultMap["package"] = packageName

                    pendingPromise?.resolve(resultMap)
                }
                REQUEST_SIGN_EVENT -> {
                    val signature = dataIntent.getStringExtra("signature")
                    val id = dataIntent.getStringExtra("id")
                    val signedEventJson = dataIntent.getStringExtra("event")

                    val resultMap =
						mutableMapOf<String, Any?>(
							"signature" to signature,
							"id" to id,
							"event" to signedEventJson
						)
                    pendingPromise?.resolve(resultMap)
                }
                REQUEST_NIP_04_ENCRYPT, REQUEST_NIP_44_ENCRYPT -> {
                    val signature = dataIntent.getStringExtra("signature")
                    val resultId = dataIntent.getStringExtra("id")

                    val resultMap =
						mutableMapOf<String, Any?>("result" to signature, "id" to resultId)
                    pendingPromise?.resolve(resultMap)
                }
                REQUEST_NIP_04_DECRYPT, REQUEST_NIP_44_DECRYPT -> {
                    val signature = dataIntent.getStringExtra("result")
                    val resultId = dataIntent.getStringExtra("id")

                    val resultMap =
						mutableMapOf<String, Any?>("result" to signature, "id" to resultId)
                    pendingPromise?.resolve(resultMap)
                }
                REQUEST_DECRYPT_ZAP_EVENT -> {
                    val decryptedEventJson = dataIntent.getStringExtra("result")
                    val resultId = dataIntent.getStringExtra("id")

                    val resultMap =
						mutableMapOf<String, Any?>(
							"result" to decryptedEventJson,
							"id" to resultId
						)
                    pendingPromise?.resolve(resultMap)
                }
				REQUEST_GET_RELAYS -> {
                    val eventJson = dataIntent.getStringExtra("result")
                    val resultId = dataIntent.getStringExtra("id")

                    val resultMap =
						mutableMapOf<String, Any?>(
							"result" to eventJson,
							"id" to resultId
						)
                    pendingPromise?.resolve(resultMap)
                }
                else -> {
                    // If the requestCode is unknown, reject
                    pendingPromise?.reject(
						"ERROR",
						"Unknown request code: $payload.requestCode",
						null
                    )
                }
            }
			pendingPromise = null
        }
    }

    /** Helper method to handle fallback launching */
    private fun launchFallbackIntent(requestCode: Int, intent: Intent, promise: Promise) {
        // If another fallback is pending, reject
        if (pendingPromise != null) {
            throw ActivityAlreadyStartedException()
        }
        pendingRequestCode = requestCode
        callsMap[requestCode] = CallData(promise, requestCode)

        try {
            appContext.throwingActivity.startActivityForResult(intent, pendingRequestCode)
            pendingPromise = promise
        } catch (e: Throwable) {
            // Clean up
            pendingPromise = null
            pendingRequestCode = 0
            promise.reject(e.toCodedException())
        }
    }

    /** Helper function to get the effective package name or fallback to a stored one */
    private fun getPackageNameFromCall(paramPackageName: String?): String {
        return if (!paramPackageName.isNullOrBlank()) {
            paramPackageName
        } else {
            signerPackageName
				?: throw IllegalArgumentException(
					"Signer package name not set. Call setPackageName first."
				)
        }
    }

    companion object {
        private const val REQUEST_GET_PUBLIC_KEY = 1001
        private const val REQUEST_SIGN_EVENT = 1002
        private const val REQUEST_NIP_04_ENCRYPT = 1003
        private const val REQUEST_NIP_04_DECRYPT = 1004
        private const val REQUEST_NIP_44_ENCRYPT = 1005
        private const val REQUEST_NIP_44_DECRYPT = 1006
        private const val REQUEST_DECRYPT_ZAP_EVENT = 1007
        private const val REQUEST_GET_RELAYS = 1008
    }

    private data class CallData(val promise: Promise, val operation: Int)

}
