import { NativeModule, requireNativeModule } from "expo";
import { Platform } from "react-native";

import { SignerAppInfo } from ".";
import { NostrNip55SignerModuleEvents } from "./NostrNip55Signer.types";

declare class NostrNip55SignerModule extends NativeModule<NostrNip55SignerModuleEvents> {
  isExternalSignerInstalled(packageName: string): Promise<boolean>;
  getInstalledSignerApps(): Promise<SignerAppInfo[]>;
  setPackageName(packageName: string): Promise<void>;
  getPublicKey(packageName?: string | null): Promise<{ npub: string }>;
  signEvent(
    packageName: string | null,
    eventJson: string,
    eventId: string,
    npub: string,
  ): Promise<{ signature: string }>;
  nip04Encrypt(
    packageName: string | null,
    plainText: string,
    id: string,
    pubKey: string,
    npub: string,
  ): Promise<{ encryptedText: string }>;
  nip04Decrypt(
    packageName: string | null,
    encryptedText: string,
    id: string,
    pubKey: string,
    npub: string,
  ): Promise<{ plainText: string }>;
  nip44Encrypt(
    packageName: string | null,
    plainText: string,
    id: string,
    pubKey: string,
    npub: string,
  ): Promise<{ encryptedText: string }>;
  nip44Decrypt(
    packageName: string | null,
    encryptedText: string,
    id: string,
    pubKey: string,
    npub: string,
  ): Promise<{ plainText: string }>;
}

const val =
  Platform.OS === "android"
    ? requireNativeModule<NostrNip55SignerModule>("ExpoNostrSignerModule")
    : ({
        isExternalSignerInstalled: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        getInstalledSignerApps: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        setPackageName: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        getPublicKey: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        signEvent: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        nip04Encrypt: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        nip04Decrypt: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        nip44Encrypt: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        nip44Decrypt: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        addListener: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        removeListener: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        removeAllListeners: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        emit: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
        listenerCount: () => {
          throw new Error(
            "ExpoNostrSignerModule is not available on this platform.",
          );
        },
      } as NostrNip55SignerModule);

// This call loads the native module object from the JSI.
export default val;
