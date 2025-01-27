import NostrNip55SignerModule from "./NostrNip55SignerModule";

export async function isExternalSignerInstalled(
  packageName: string,
): Promise<boolean> {
  return NostrNip55SignerModule.isExternalSignerInstalled(packageName);
}

export async function getInstalledSignerApps() {
  return NostrNip55SignerModule.getInstalledSignerApps();
}

export async function setPackageName(packageName: string) {
  return NostrNip55SignerModule.setPackageName(packageName);
}

export async function getPublicKey(packageName?: string | null) {
  return NostrNip55SignerModule.getPublicKey(packageName ?? null);
}

export async function signEvent(
  packageName: string | null,
  eventJson: string,
  eventId: string,
  npub: string,
) {
  return NostrNip55SignerModule.signEvent(
    packageName,
    eventJson,
    eventId,
    npub,
  );
}

export async function nip04Encrypt(
  packageName: string | null,
  plainText: string,
  id: string,
  pubKey: string,
  npub: string,
) {
  return NostrNip55SignerModule.nip04Encrypt(
    packageName,
    plainText,
    id,
    pubKey,
    npub,
  );
}

export async function nip04Decrypt(
  packageName: string | null,
  encryptedText: string,
  id: string,
  pubKey: string,
  npub: string,
) {
  return NostrNip55SignerModule.nip04Decrypt(
    packageName,
    encryptedText,
    id,
    pubKey,
    npub,
  );
}

export async function nip44Encrypt(
  packageName: string | null,
  plainText: string,
  id: string,
  pubKey: string,
  npub: string,
) {
  return NostrNip55SignerModule.nip44Encrypt(
    packageName,
    plainText,
    id,
    pubKey,
    npub,
  );
}

export async function nip44Decrypt(
  packageName: string | null,
  encryptedText: string,
  id: string,
  pubKey: string,
  npub: string,
) {
  return NostrNip55SignerModule.nip44Decrypt(
    packageName,
    encryptedText,
    id,
    pubKey,
    npub,
  );
}

export async function decryptZapEvent(
  packageName: string | null,
  eventJson: string,
  id: string,
  npub: string,
) {
  return NostrNip55SignerModule.decryptZapEvent(
    packageName,
    eventJson,
    id,
    npub,
  );
}

export async function getRelays(
  packageName: string | null,
  id: string,
  npub: string,
) {
  return NostrNip55SignerModule.getRelays(packageName, id, npub);
}

export default {
  isExternalSignerInstalled,
  getInstalledSignerApps,
  setPackageName,
  getPublicKey,
  signEvent,
  nip04Encrypt,
  nip04Decrypt,
  nip44Encrypt,
  nip44Decrypt,
  decryptZapEvent,
  getRelays,
};
