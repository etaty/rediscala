import sbt._
import Keys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import sbtunidoc.Plugin._

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
  val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"

  val v = "0.1-SNAPSHOT"

  lazy val standardSettings = Defaults.defaultSettings ++
    Seq(
      name := "rediscala",
      version := v,
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
      publishMavenStyle := true,
      git.gitRemoteRepo := "git@github.com:etaty/rediscala.git",

      scalacOptions in (Compile, doc) <++= baseDirectory in LocalProject("rediscala") map { bd =>
        Seq(
          "-sourcepath", bd.getAbsolutePath
        )
      },
      scalacOptions in (Compile, doc) <++= version in LocalProject("rediscala") map { version =>
        val branch = if(version.trim.endsWith("SNAPSHOT")) "master" else version
        Seq[String](
          "-doc-source-url", baseSourceUrl + branch +"€{FILE_PATH}.scala",
          "-doc-title", "Rediscala API",
          "-doc-version", version
        )
      },
      testOptions in Test += Tests.Argument("exclude", "benchmark")
  ) ++ site.settings ++ site.includeScaladoc(v +"/api") ++ ghpages.settings ++
    ScctPlugin.instrumentSettings ++
    com.github.theon.coveralls.CoverallsPlugin.coverallsSettings

  lazy val BenchTest = config("bench") extend(Test)



  lazy val root = Project(id = "rediscala",
    base = file("."),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= Dependencies.rediscalaDependencies
    )
  ).configs(BenchTest)
  .settings(inConfig(BenchTest)(Defaults.testTasks): _*)
  .settings(testOptions in BenchTest := Seq(Tests.Argument("include", "benchmark"))
  )

}