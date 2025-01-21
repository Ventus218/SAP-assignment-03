package users.domain.errors

import shared.domain.EventSourcing.CommandId

final case class CommandNotFound(id: CommandId)
