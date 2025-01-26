package biz.nostr.expo.signer

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.net.URL

import biz.nostr.android.nip55.AppInfo
import biz.nostr.android.nip55.Signer
import biz.nostr.android.nip55.IntentBuilder

class NostrNip55SignerModule : Module() {
  // A default or user-set package name
  private var signerPackageName: String? = null

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
    AsyncFunction("getPublicKey") { packageName: String? ->
      val pkg = getPackageNameFromCall(packageName)
      // Attempt direct call first:
      val publicKey: String? = Signer.getPublicKey(appContext.reactContext, pkg)
      if (publicKey != null) {
        // Return directly
        mapOf("npub" to publicKey, "package" to pkg)
      } else {
        // If you want to replicate "startActivityForResult" fallback,
        // see the explanation below. For now, let's throw an exception or return a placeholder.
        throw UnsupportedOperationException("startActivityForResult-based fallback not supported in Expo modules")
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
   * Helper method to retrieve or fallback to previously set signerPackageName.
   */
  private fun getPackageNameFromCall(paramPackageName: String?): String {
    return if (paramPackageName.isNullOrBlank()) {
      signerPackageName ?: throw IllegalStateException("Signer package name not set. Call setPackageName first.")
    } else {
      paramPackageName
    }
  }
}
