package rides.domain.errors

import rides.domain.model.*

enum UserOrEBikeDoesNotExist:
  case UserDoesNotExist(username: Username)
  case EBikeDoesNotExist(eBikeId: EBikeId)
