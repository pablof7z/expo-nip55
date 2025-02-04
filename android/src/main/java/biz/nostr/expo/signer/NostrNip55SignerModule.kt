package biz.nostr.expo.signer

import android.content.Intent
import android.content.Context

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.toCodedException
import expo.modules.kotlin.exception.Exceptions

import java.net.URL

import biz.nostr.android.nip55.AppInfo
import biz.nostr.android.nip55.Signer
import biz.nostr.android.nip55.IntentBuilder

class NostrNip55SignerModule : Module() {

  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  private var signerPackageName: String? = null

  // region: Fields for fallback approach
  private var pendingPromise: Promise? = null
  private var pendingRequestCode: Int = 0

  override fun definition() = ModuleDefinition {

    Name("ExpoNostrSignerModule")

    Events("onChange")

    /********************************************************
     * isExternalSignerInstalled
     ********************************************************/
    AsyncFunction("isExternalSignerInstalled") { packageName: String ->
	  val resolveInfoList: List<Any> = Signer.isExternalSignerInstalled(context, packageName)  
	  // Return true if the list isn't empty, false otherwise
	  resolveInfoList.isNotEmpty()
	}

    /********************************************************
     * getInstalledSignerApps
     ********************************************************/
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

    /********************************************************
     * setPackageName
     ********************************************************/
    AsyncFunction("setPackageName") { packageName: String ->
      if (packageName.isBlank()) {
        throw IllegalArgumentException("Missing or empty packageName parameter")
      }
      signerPackageName = packageName
    }

    /********************************************************
     * getPublicKey
     ********************************************************/
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
  
    /********************************************************
     * signEvent
     ********************************************************/
    AsyncFunction("signEvent") { pkgName: String?, eventJson: String, eventId: String, npub: String, promise: Promise ->
		val packageName = getPackageNameFromCall(pkgName)
		val signedEvent: Array<String>? = Signer.signEvent(context, packageName, eventJson, npub)
		if (signedEvent != null) {
		  val resultMap = mapOf(
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
  
    /********************************************************
     * nip04Encrypt
     ********************************************************/
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

    /********************************************************
     * nip04Decrypt
     ********************************************************/
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

    /********************************************************
     * nip44Encrypt
     ********************************************************/
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

    /********************************************************
     * nip44Decrypt
     ********************************************************/
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
        launchFallbackIntent(REQUEST_NIP_04_DECRYPT, intent, promise)
      }
    }

    /********************************************************
     * decryptZapEvent
     ********************************************************/
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

    /********************************************************
     * getRelays
     ********************************************************/
    AsyncFunction("getRelays") { packageName: String?, id: String, npub: String, promise: Promise ->
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
  }

  /**
   * Helper method to handle fallback launching
   */
  private fun launchFallbackIntent(
    requestCode: Int,
    intent: Intent,
    promise: Promise
  ) {
    // If another fallback is pending, reject
    if (pendingPromise != null) {
      //promise.reject("ACTIVITY_IN_PROGRESS", "Another fallback activity is already started.")
      return
    }
    pendingPromise = promise
    pendingRequestCode = requestCode

    try {
      val activity = appContext.activityProvider?.currentActivity
        ?: throw IllegalStateException("No current activity available")
      activity.startActivityForResult(intent, requestCode)
    } catch (e: Throwable) {
      // Clean up
      pendingPromise = null
      pendingRequestCode = 0
      promise.reject(e.toCodedException())
    }
  }

  /**
   * Helper function to get the effective package name or fallback to a stored one
   */
  private fun getPackageNameFromCall(paramPackageName: String?): String {
    // If paramPackageName is null/blank, fallback to a stored one, else throw an error
    // (assuming you have a stored 'signerPackageName' in your class, or adapt this logic)
    return if (!paramPackageName.isNullOrBlank()) {
      paramPackageName
    } else {
      signerPackageName ?: throw IllegalArgumentException("Signer package name not set. Call setPackageName first.")
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

}
