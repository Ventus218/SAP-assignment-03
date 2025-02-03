package smartcity

import smartcity.domain.SmartCityServiceImpl
import smartcity.domain.model.JunctionId

object Main extends App:
  val service = SmartCityServiceImpl()
  println:
    service.bestPath(JunctionId("J1"), JunctionId("J4")) match
      case Left(value)  => value.toString()
      case Right(value) => value.map(_.id.value).mkString(", ")
