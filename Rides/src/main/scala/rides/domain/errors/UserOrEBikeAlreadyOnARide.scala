package rides.domain.errors

import rides.domain.model.*

enum UserOrEBikeAlreadyOnARide:
  case UserAlreadyOnARide(username: Username)
  case EBikeAlreadyOnARide(eBikeId: EBikeId)
