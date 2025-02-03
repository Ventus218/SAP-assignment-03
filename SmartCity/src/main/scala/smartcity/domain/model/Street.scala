package smartcity.domain.model

final case class StreetId(value: String)
final case class Street(id: StreetId, timeLengthMillis: Long)
