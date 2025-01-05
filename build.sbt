ThisBuild / scalaVersion := "3.5.2"
ThisBuild / libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
ThisBuild / scalacOptions += "-deprecation"

// AKKA HTTP
ThisBuild / resolvers += "Akka library repository".at(
  "https://repo.akka.io/maven"
)
val AkkaVersion = "2.9.3"
val AkkaHttpVersion = "10.6.3"
lazy val akkaHttpSettings = Seq(
  libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion
)

lazy val commonFrontendSettings = Seq(
  libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M19", // for sttp
  libraryDependencies += "com.lihaoyi" %% "upickle" % "4.0.2"
)

lazy val sharedFrontend = project
  .in(file("SharedFrontend"))
  .settings(
    name := "Shared Frontend",
    version := "0.1.0",
    commonFrontendSettings
  )

lazy val userFrontend = project
  .in(file("UserFrontend"))
  .settings(
    name := "User Frontend",
    version := "0.1.0",
    commonFrontendSettings,
    assembly / assemblyOutputPath := file("./UserFrontend/executable.jar")
  )
  .dependsOn(sharedFrontend)

lazy val adminFrontend = project
  .in(file("AdminFrontend"))
  .settings(
    name := "Admin Frontend",
    version := "0.1.0",
    commonFrontendSettings,
    assembly / assemblyOutputPath := file("./AdminFrontend/executable.jar")
  )
  .dependsOn(sharedFrontend)

lazy val shared = project
  .in(file("Shared"))
  .settings(
    name := "Shared",
    version := "0.1.0",
    akkaHttpSettings,
    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.9.0",
    libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M19", // for sttp
    libraryDependencies += "com.lihaoyi" %% "upickle" % "4.0.2"
  )

lazy val rides = project
  .in(file("Rides"))
  .settings(
    name := "Rides",
    version := "0.1.0",
    akkaHttpSettings,
    assembly / assemblyOutputPath := file("./Rides/executable.jar")
  )
  .dependsOn(shared)

lazy val users = project
  .in(file("Users"))
  .settings(
    name := "Users",
    version := "0.1.0",
    akkaHttpSettings,
    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.9.0",
    assembly / assemblyOutputPath := file("./Users/executable.jar")
  )
  .dependsOn(shared)

lazy val eBikes = project
  .in(file("EBikes"))
  .settings(
    name := "EBikes",
    version := "0.1.0",
    akkaHttpSettings,
    libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.2" % Test,
    assembly / assemblyOutputPath := file("./EBikes/executable.jar")
  )
  .dependsOn(shared)

import scala.sys.process.*

val allProjectsFilter = ScopeFilter(projects = inAnyProject)

lazy val composeUp =
  taskKey[Any]("Builds the docker images and runs compose up")
composeUp := {
  assembly.all(allProjectsFilter).value
  composeUpProcess("production.env") !
}

lazy val composeUpDev = taskKey[Any](
  "Builds the docker images and runs compose up (also loads the docker-compose.dev.yml)"
)
composeUpDev := {
  assembly.all(allProjectsFilter).value
  composeUpProcess(
    "development.env",
    "docker-compose.yml",
    "docker-compose.dev.yml"
  ) !
}

def composeUpProcess(envFile: String, composeFiles: String*): ProcessBuilder = {
  val ymlFilesOptions = composeFiles.map("-f " + _).mkString(" ")
  s"docker compose $ymlFilesOptions --env-file $envFile build" #&& s"docker compose $ymlFilesOptions --env-file $envFile up --force-recreate"
}
