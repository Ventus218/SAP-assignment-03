ThisBuild / scalaVersion := "3.5.2"
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
    assembly / assemblyOutputPath := file("./Users/executable.jar")
  )
  .dependsOn(shared)

lazy val eBikes = project
  .in(file("EBikes"))
  .settings(
    name := "EBikes",
    version := "0.1.0",
    akkaHttpSettings,
    assembly / assemblyOutputPath := file("./EBikes/executable.jar")
  )
  .dependsOn(shared)

lazy val aBikesSimulator = project
  .in(file("ABikesSimulator"))
  .settings(
    name := "ABikesSimulator",
    version := "0.1.0",
    akkaHttpSettings,
    assembly / assemblyOutputPath := file("./ABikesSimulator/executable.jar")
  )
  .dependsOn(shared)

lazy val smartCity = project
  .in(file("SmartCity"))
  .settings(
    name := "SmartCity",
    version := "0.1.0",
    akkaHttpSettings,
    assembly / assemblyOutputPath := file("./SmartCity/executable.jar")
  )
  .dependsOn(shared)

import scala.sys.process.*

val allProjectsFilter = ScopeFilter(projects = inAnyProject)

// DOCKER COMPOSE BUILD

lazy val composeBuild =
  taskKey[Any]("Builds the docker images")
composeBuild := {
  assembly.all(allProjectsFilter).value
  composeBuildProcess("production.env") !
}

lazy val composeBuildDev = taskKey[Any](
  "Builds the docker images (also loads the docker-compose.dev.yml)"
)
composeBuildDev := {
  assembly.all(allProjectsFilter).value
  composeBuildProcess(
    "development.env",
    "docker-compose.yml",
    "docker-compose.dev.yml"
  ) !
}

def composeBuildProcess(
    envFile: String,
    composeFiles: String*
): ProcessBuilder = {
  val ymlFilesOptions = composeFiles.map("-f " + _).mkString(" ")
  s"docker compose $ymlFilesOptions --env-file $envFile build"
}

// DOCKER COMPOSE PUBLISH

lazy val composePublish =
  taskKey[Any]("Builds and publishes the docker images, it may take 20 minutes")
composePublish := {
  assembly.all(allProjectsFilter).value
  composePublishProcess("production.env") !
}

lazy val composePublishDev = taskKey[Any](
  "Builds and publishes the docker images (also loads the docker-compose.dev.yml), it may take 20 minutes"
)
composePublishDev := {
  assembly.all(allProjectsFilter).value
  composePublishProcess(
    "development.env",
    "docker-compose.yml",
    "docker-compose.dev.yml"
  ) !
}

def composePublishProcess(
    envFile: String,
    composeFiles: String*
): ProcessBuilder = {
  val ymlFilesOptions = composeFiles.map("-f " + _).mkString(" ")
  composeBuildProcess(
    envFile,
    composeFiles*
  ) #&& s"docker compose $ymlFilesOptions --env-file $envFile push"
}

// DOCKER COMPOSE UP

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
  composeBuildProcess(envFile, composeFiles*) #&&
    s"docker compose $ymlFilesOptions --env-file $envFile up --force-recreate"
}
