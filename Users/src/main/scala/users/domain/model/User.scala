package users.domain.model;

import shared.domain.EventSourcing.*

case class User(username: Username) extends Entity[Username]:
  def id: Username = username
