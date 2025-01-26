import * as React from 'react';

import { NostrNip55SignerViewProps } from './NostrNip55Signer.types';

export default function NostrNip55SignerView(props: NostrNip55SignerViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
