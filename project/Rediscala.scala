import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtGhPages._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import com.typesafe.sbt.SbtGit.{GitKeys => git}
import com.typesafe.sbt.SbtSite._
import com.typesafe.sbt.SbtSite.SiteKeys.siteMappings
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
  val akkaVersion = "2.4.12"

  import sbt._

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

  val specs2 = "org.specs2" %% "specs2-core" % "3.8.6"

  val stm = "org.scala-stm" %% "scala-stm" % "0.8"

  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.13.4"

  //val scalameter = "com.github.axel22" %% "scalameter" % "0.4"

  val rediscalaDependencies = Seq(
    akkaActor,
    stm,
    akkaTestkit % "test",
    //scalameter % "test",
    specs2 % "test",
    scalacheck % "test"
  )
}

object RediscalaBuild extends Build {
  val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"


  lazy val standardSettings =
    Seq(
      name := "rediscala",
      organization := "com.github.etaty",
      scalaVersion := "2.11.8",
      crossScalaVersions := Seq(scalaVersion.value, "2.12.0"),
      licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
      homepage := Some(url("https://github.com/etaty/rediscala")),
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
      scalacOptions in (Compile, doc) ++= {
        Seq(
          "-sourcepath", (baseDirectory in LocalProject("rediscala")).value.getAbsolutePath
        )
      },
      autoAPIMappings := true,
      apiURL := Some(url("http://etaty.github.io/rediscala/")),
      scalacOptions in (Compile, doc) ++= {
        val v = (version in LocalProject("rediscala")).value
        val branch = if(v.trim.endsWith("SNAPSHOT")) "master" else v
        Seq[String](
          "-doc-source-url", baseSourceUrl + branch +"€{FILE_PATH}.scala"
        )
      }
  ) ++ site.settings ++ ghpages.settings ++ Seq(
      siteMappings ++= {
        for((f, d) <- (mappings in packageDoc in Compile).value) yield (f, (version in LocalProject("rediscala")).value + "/api/" + d)
      },
      cleanSite := {
        val dir = updatedRepository.value
        val v = (version in LocalProject("rediscala")).value
        val toClean = IO.listFiles(dir).filter{ f =>
          val p = f.getName
          p.startsWith("latest") || p.startsWith(v)
        }.map(_.getAbsolutePath).toList
        if(toClean.nonEmpty)
          git.gitRunner.value.apply(("rm" :: "-r" :: "-f" :: "--ignore-unmatch" :: toClean) :_*)(dir, streams.value.log)
        ()
      },
      synchLocal := {
        val clean = cleanSite.value
        val repo = updatedRepository.value
        // TODO - an sbt.Synch with cache of previous mappings to make this more efficient. */
        val betterMappings = privateMappings.value map { case (file, target) => (file, repo / target) }
        // First, remove 'stale' files.
        //cleanSite.
        // Now copy files.
        IO.copy(betterMappings)
        if(ghpagesNoJekyll.value) IO.touch(repo / ".nojekyll")
        repo
      }
  ) ++ site.includeScaladoc("latest/api")

  lazy val BenchTest = config("bench") extend Test

  lazy val benchTestSettings = inConfig(BenchTest)(Defaults.testSettings ++ Seq(
    sourceDirectory in BenchTest := baseDirectory.value / "src/benchmark",
    //testOptions in BenchTest += Tests.Argument("-preJDK7"),
    testFrameworks in BenchTest := Seq(new TestFramework("org.scalameter.ScalaMeterFramework")),

    //https://github.com/sbt/sbt/issues/539 => bug fixed in sbt 0.13.x
    testGrouping in BenchTest := (definedTests in BenchTest map partitionTests).value
  ))

  lazy val root = Project(id = "rediscala",
    base = file(".")
  ).settings(
    standardSettings,
    libraryDependencies ++= Dependencies.rediscalaDependencies
  ).configs(BenchTest)
    //.settings(benchTestSettings: _* )

  lazy val benchmark = {
    import pl.project13.scala.sbt.JmhPlugin

    Project(
      id = "benchmark",
      base = file("benchmark")
    ).settings(Seq(
      scalaVersion := "2.11.7",
      libraryDependencies += "net.debasishg" %% "redisclient" % "3.0"
    ))
      .enablePlugins(JmhPlugin)
      .dependsOn(root)
  }

  def partitionTests(tests: Seq[TestDefinition]) = {
    Seq(new Group("inProcess", tests, InProcess))
  }
}
