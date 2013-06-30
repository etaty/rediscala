import sbt._
import Keys._

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/")
  val resolversList = typesafe
}

object Dependencies {
  val akkaVersion = "2.2.0-RC2"

  import sbt._

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val specs2 = "org.specs2" %% "specs2" % "1.14"

  val rediscalaDependencies = Seq(
    akkaActor,
    akkaTestkit % "test",
    specs2 % "test"
  )
}

object RediscalaBuild extends Build {
  override lazy val settings = super.settings ++
    Seq(
      name := "rediscala",
      version := "0.1-SNAPSHOT",
      organization := "com.etaty.rediscala",
      scalaVersion := "2.10.2",
      resolvers ++= Resolvers.resolversList,

      publishTo <<= version {
        (version: String) =>
          val localPublishRepo = "/Users/valerian/Projects/rediscala-mvn"
          if (version.trim.endsWith("SNAPSHOT"))
            Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
          else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
      },
      publishMavenStyle := true
    )

  lazy val root = Project(id = "rediscala",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.rediscalaDependencies
    )
  )
}