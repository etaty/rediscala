import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import sbt.LocalProject
import sbt.Tests.{InProcess, Group}
import ScoverageSbtPlugin.instrumentSettings
import CoverallsPlugin.coverallsSettings

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
    "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo"
  )
  val resolversList = typesafe
}

object Dependencies {
  val akkaVersion = "2.3.2"

  import sbt._

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val specs2 = "org.specs2" %% "specs2" % "2.1.1"

  val scalameter = "com.github.axel22" %% "scalameter" % "0.4"


  val rediscalaDependencies = Seq(
    akkaActor,
    akkaTestkit % "test",
    scalameter % "test",
    specs2 % "test"
  )
}

object RediscalaBuild extends Build {
  val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"

  val v = "1.3.1"

  lazy val standardSettings = Defaults.defaultSettings ++
    Seq(
      name := "rediscala",
      version := v,
      organization := "com.etaty.rediscala",
      scalaVersion := "2.10.4",
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
          "-doc-title", "Rediscala "+v+" API",
          "-doc-version", version
        )
      }
  ) ++ site.settings ++ site.includeScaladoc(v +"/api") ++ site.includeScaladoc("latest/api") ++ ghpages.settings ++
    instrumentSettings ++ coverallsSettings

  lazy val BenchTest = config("bench") extend Test

  lazy val benchTestSettings = inConfig(BenchTest)(Defaults.testSettings ++ Seq(
    sourceDirectory in BenchTest <<= baseDirectory / "src/benchmark",
    //testOptions in BenchTest += Tests.Argument("-preJDK7"),
    testFrameworks in BenchTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),

    //https://github.com/sbt/sbt/issues/539 => bug fixed in sbt 0.13.x
    testGrouping in BenchTest <<= definedTests in BenchTest map partitionTests
  ))

  lazy val root = Project(id = "rediscala",
    base = file("."),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= Dependencies.rediscalaDependencies
    )
  ).configs(BenchTest)
    .settings(benchTestSettings: _* )

  def partitionTests(tests: Seq[TestDefinition]) = {
    Seq(new Group("inProcess", tests, InProcess))
  }
}