package biz.nostr.expo.signer.exceptions

import expo.modules.core.errors.CodedException

class ActivityAlreadyStartedException :
  CodedException("Another fallback activity is already started. You need to wait for its result before starting another activity.") {
  override fun getCode(): String {
    return "ACTIVITY_IN_PROGRESS"
  }
}