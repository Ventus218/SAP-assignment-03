package ebikes.domain.model

final case class JunctionId(value: String)
final case class StreetId(value: String)

enum EBikeLocation:
  case Junction(id: JunctionId)
  case Street(id: StreetId)
