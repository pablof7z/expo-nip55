package biz.nostr.expo.signer

import android.content.Intent

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.toCodedException

import java.net.URL

import biz.nostr.android.nip55.AppInfo
import biz.nostr.android.nip55.Signer
import biz.nostr.android.nip55.IntentBuilder

class NostrNip55SignerModule : Module() {

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
	  // Call the Java method that returns List<ResolveInfo>
	  val resolveInfoList: List<Any> = Signer.isExternalSignerInstalled(appContext.reactContext, packageName)
  
	  // Return true if the list isn't empty, false otherwise
	  resolveInfoList.isNotEmpty()
	}

    /********************************************************
     * getInstalledSignerApps
     ********************************************************/
    AsyncFunction("getInstalledSignerApps") {
      val signerAppInfos: List<AppInfo> = Signer.getInstalledSignerApps(appContext.reactContext)
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
    AsyncFunction("getPublicKey") { pkgName: String?, promise: Promise ->
		val packageName = getPackageNameFromCall(pkgName)
		val publicKey: String? = Signer.getPublicKey(appContext.reactContext, packageName)
		if (publicKey != null) {
		  // Direct approach success
		  val resultMap = mapOf("npub" to publicKey, "package" to packageName)
		  promise.resolve(resultMap)
		} else {
		  // Fallback approach
		  launchFallbackIntent(
			requestCode = REQUEST_GET_PUBLIC_KEY,
			intent = IntentBuilder.getPublicKeyIntent(packageName, /*permissions*/ null),
			promise = promise
		  )
		}
	  }
  
    /********************************************************
     * signEvent
     ********************************************************/
    AsyncFunction("signEvent") { packageName: String?, eventJson: String, eventId: String, npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val signedEvent: Array<String>? = Signer.signEvent(appContext.reactContext, pkg, eventJson, npub)
      if (signedEvent != null) {
        // [0]: signature, [1]: event
        mapOf(
          "signature" to signedEvent[0],
          "id" to eventId,
          "event" to signedEvent[1]
        )
      } else {
        throw UnsupportedOperationException("Intent fallback not supported yet in an Expo module.")
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
        npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val encryptedText = Signer.nip04Encrypt(appContext.reactContext, pkg, plainText, pubKey, npub)
      encryptedText?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("Fallback encryption via Intent not supported here.")
    }

    /********************************************************
     * nip04Decrypt
     ********************************************************/
    AsyncFunction("nip04Decrypt") {
        packageName: String?,
        encryptedText: String,
        id: String,
        pubKey: String,
        npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val decryptedText = Signer.nip04Decrypt(appContext.reactContext, pkg, encryptedText, pubKey, npub)
      decryptedText?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("Fallback decrypt via Intent not supported.")
    }

    /********************************************************
     * nip44Encrypt
     ********************************************************/
    AsyncFunction("nip44Encrypt") {
        packageName: String?,
        plainText: String,
        id: String,
        pubKey: String,
        npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val encryptedText = Signer.nip44Encrypt(appContext.reactContext, pkg, plainText, pubKey, npub)
      encryptedText?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("NIP-44 fallback encryption not supported.")
    }

    /********************************************************
     * nip44Decrypt
     ********************************************************/
    AsyncFunction("nip44Decrypt") {
        packageName: String?,
        encryptedText: String,
        id: String,
        pubKey: String,
        npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val decryptedText = Signer.nip44Decrypt(appContext.reactContext, pkg, encryptedText, pubKey, npub)
      decryptedText?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("NIP-44 fallback decryption not supported.")
    }

    /********************************************************
     * decryptZapEvent
     ********************************************************/
    AsyncFunction("decryptZapEvent") {
        packageName: String?,
        eventJson: String,
        id: String,
        npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val decryptedEventJson = Signer.decryptZapEvent(appContext.reactContext, pkg, eventJson, npub)
      decryptedEventJson?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("Fallback Intent approach not supported.")
    }

    /********************************************************
     * getRelays
     ********************************************************/
    AsyncFunction("getRelays") { packageName: String?, id: String, npub: String ->
      val pkg = getPackageNameFromCall(packageName)
      val relayJson = Signer.getRelays(appContext.reactContext, pkg, npub)
      relayJson?.let {
        mapOf("result" to it, "id" to id)
      } ?: throw UnsupportedOperationException("Fallback intent approach not supported.")
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
  }

}
