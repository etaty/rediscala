import com.typesafe.sbt.SbtGit.{GitKeys => git}
import sbt.Tests.{InProcess, Group}

val akkaVersion = "2.6.3"

val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion

val akkaTestkit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion

val specs2 = "org.specs2" %% "specs2-core" % "4.8.0"

val stm = "org.scala-stm" %% "scala-stm" % "0.9.1"

val scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.3"

//val scalameter = "com.github.axel22" %% "scalameter" % "0.4"

val rediscalaDependencies = Seq(
  akkaActor,
  stm,
  akkaTestkit % "test",
  //scalameter % "test",
  specs2 % "test",
  scalacheck % "test"
)


val baseSourceUrl = "https://github.com/etaty/rediscala/tree/"

val Scala213 = "2.13.1"

lazy val standardSettings = Def.settings(
  name := "rediscala",
  organization := "com.github.etaty",
  scalaVersion := Scala213,
  crossScalaVersions := Seq("2.12.10", Scala213),
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
  publishTo := sonatypePublishTo.value,
  publishMavenStyle := true,
  git.gitRemoteRepo := "git@github.com:etaty/rediscala.git",

  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xlint",
    "-deprecation",
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
  },
  siteMappings ++= {
    for((f, d) <- (mappings in packageDoc in Compile).value) yield (f, (version in LocalProject("rediscala")).value + "/api/" + d)
  },
  ghpagesCleanSite := {
    val dir = ghpagesUpdatedRepository.value
    val v = (version in LocalProject("rediscala")).value
    val toClean = IO.listFiles(dir).filter{ f =>
      val p = f.getName
      p.startsWith("latest") || p.startsWith(v)
    }.map(_.getAbsolutePath).toList
    val log = streams.value.log
    val gitRunner = git.gitRunner.value
    if(toClean.nonEmpty)
      gitRunner.apply(("rm" :: "-r" :: "-f" :: "--ignore-unmatch" :: toClean) :_*)(dir, log)
    ()
  },
  ghpagesSynchLocal := {
    val clean = ghpagesCleanSite.value
    val repo = ghpagesUpdatedRepository.value
    // TODO - an sbt.Synch with cache of previous mappings to make this more efficient. */
    val betterMappings = ghpagesPrivateMappings.value map { case (file, target) => (file, repo / target) }
    // First, remove 'stale' files.
    //cleanSite.
    // Now copy files.
    IO.copy(betterMappings)
    if(ghpagesNoJekyll.value) IO.touch(repo / ".nojekyll")
    repo
  },
  siteSubdirName in SiteScaladoc := "latest/api"
)

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
  libraryDependencies ++= rediscalaDependencies
).configs(
  BenchTest
).enablePlugins(
  SiteScaladocPlugin, GhpagesPlugin
)

lazy val benchmark = {
  import pl.project13.scala.sbt.JmhPlugin

  Project(
    id = "benchmark",
    base = file("benchmark")
  ).settings(Seq(
    scalaVersion := Scala213,
    libraryDependencies += "net.debasishg" %% "redisclient" % "3.0"
  ))
    .enablePlugins(JmhPlugin)
    .dependsOn(root)
}

def partitionTests(tests: Seq[TestDefinition]) = {
  Seq(new Group("inProcess", tests, InProcess))
}
