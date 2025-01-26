import { registerWebModule, NativeModule } from 'expo';

import { NostrNip55SignerModuleEvents } from './NostrNip55Signer.types';

class NostrNip55SignerModule extends NativeModule<NostrNip55SignerModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(NostrNip55SignerModule);
