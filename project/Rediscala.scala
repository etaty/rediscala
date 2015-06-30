import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import sbt.LocalProject
import sbt.Tests.{InProcess, Group}

object Resolvers {
  val typesafe = Seq(
    "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
  val resolversList = typesafe
}

object Dependencies {
  val akkaVersion = "2.3.6"

  import sbt._

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val specs2 = "org.specs2" %% "specs2" % "2.3.13"

  val stm = "org.scala-stm" %% "scala-stm" % "0.7"

  //val scalameter = "com.github.axel22" %% "scalameter" % "0.4"

  val rediscalaDependencies = Seq(
    akkaActor,
    stm,
    akkaTestkit % "test",
  //  scalameter % "test",
    specs2 % "test"
  )
}

object RediscalaBuild extends Build {
  val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"


  lazy val standardSettings = Defaults.defaultSettings ++
    Seq(
      name := "rediscala",
      organization := "com.github.etaty",
      scalaVersion := "2.11.7",
      crossScalaVersions := Seq("2.11.7", "2.10.4"),
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url("https://github.com/non/rediscala")),
      scmInfo := Some(ScmInfo(url("https://github.com/etaty/rediscala"), "scm:git:git@github.com:etaty/rediscala.git")),
      apiURL := Some(url("http://etaty.github.io/rediscala/latest/api/")),
      pomExtra := (
        <developers>
          <developer>
            <id>etaty</id>
            <name>Valerian Barbot</name>
            <url>http://github.com/etaty/</url>
          </developer>
        </developers>
        ),
      resolvers ++= Resolvers.resolversList,

      publishMavenStyle := true,
      git.gitRemoteRepo := "git@github.com:etaty/rediscala.git",

      scalacOptions ++= Seq(
        "-encoding", "UTF-8",
        "-Xlint",
        "-deprecation",
        "-Xfatal-warnings",
        "-feature",
        "-language:postfixOps",
        "-unchecked"
      ),
      scalacOptions in (Compile, doc) <++= baseDirectory in LocalProject("rediscala") map { bd =>
        Seq(
          "-sourcepath", bd.getAbsolutePath
        )
      },
      autoAPIMappings := true,
      apiURL := Some(url("http://etaty.github.io/rediscala/")),
      scalacOptions in (Compile, doc) <++= version in LocalProject("rediscala") map { version =>
        val branch = if(version.trim.endsWith("SNAPSHOT")) "master" else version
        Seq[String](
          "-doc-source-url", baseSourceUrl + branch +"€{FILE_PATH}.scala"
        )
      }
  ) ++ site.settings ++ site.includeScaladoc(version +"/api") ++ site.includeScaladoc("latest/api") ++ ghpages.settings

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
    //.settings(benchTestSettings: _* )

  def partitionTests(tests: Seq[TestDefinition]) = {
    Seq(new Group("inProcess", tests, InProcess))
  }
}
