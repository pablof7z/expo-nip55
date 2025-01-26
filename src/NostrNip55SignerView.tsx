import { requireNativeView } from 'expo';
import * as React from 'react';

import { NostrNip55SignerViewProps } from './NostrNip55Signer.types';

const NativeView: React.ComponentType<NostrNip55SignerViewProps> =
  requireNativeView('NostrNip55Signer');

export default function NostrNip55SignerView(props: NostrNip55SignerViewProps) {
  return <NativeView {...props} />;
}
