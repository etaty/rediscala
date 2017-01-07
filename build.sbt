import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
//import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
//import microsites.ExtraMdFileConfig

lazy val buildSettings = Seq(
  organization := "com.github.etaty",
  licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.11.8", scalaVersion.value)
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding", "UTF-8", // 2 args
    "-feature",
    "-deprecation",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-value-discard"
  ) ++ (
    if (scalaVersion.value startsWith "2.12") Seq("-Ypartial-unification")
    else Nil
    ),
  scalacOptions in(Compile, doc) ++= Seq(
    "-groups",
    "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url", "https://github.com/etaty/rediscala/tree/" + version.value + "€{FILE_PATH}.scala",
    "-skip-packages", "scalaz"
  )
)


lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  homepage := Some(url("https://github.com/etaty/rediscala")),
  pomIncludeRepository := Function.const(false),
  pomExtra := (
    <scm>
      <url>git@github.com:etaty/rediscala.git</url>
      <connection>scm:git:git@github.com:etaty/rediscala.git</connection>
    </scm>
      <developers>
        <developer>
          <id>etaty</id>
          <name>Valerian Barbot</name>
          <url>https://github.com/etaty</url>
        </developer>
      </developers>
    ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("package", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges),
  mappings in(Compile, packageSrc) ++= (managedSources in Compile).value pair relativeTo(sourceManaged.value / "main" / "scala")
)

lazy val rediscalaSettings = buildSettings ++ commonSettings


lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

val akkaVersion = "2.4.12"

lazy val rediscala = project.in(file("."))
  .settings(rediscalaSettings)
  .settings(publishSettings)
  .settings(
    fork in Test := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "org.scala-stm" %% "scala-stm" % "0.8",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
      "org.specs2" %% "specs2-core" % "3.8.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
    )
  )

lazy val benchmark = {
  import pl.project13.scala.sbt.JmhPlugin

  Project(
    id = "benchmark",
    base = file("benchmark")
  )
    .settings(rediscalaSettings)
    .settings(noPublishSettings)
    .settings(Seq(
      fork in Test := true,
      libraryDependencies += "net.debasishg" %% "redisclient" % "3.3"
    ))
    .enablePlugins(JmhPlugin)
    .dependsOn(rediscala)
}