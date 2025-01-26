import { NativeModule, requireNativeModule } from 'expo';

import { NostrNip55SignerModuleEvents } from './NostrNip55Signer.types';

declare class NostrNip55SignerModule extends NativeModule<NostrNip55SignerModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<NostrNip55SignerModule>('NostrNip55Signer');
