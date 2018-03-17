name := "rediscala"

organization := "com.github.Ma27"

scalaVersion := "2.12.4"

crossScalaVersions := Seq(scalaVersion.value, "2.11.11")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/Ma27/rediscala"))

scmInfo := Some(ScmInfo(url("https://github.com/Ma27/rediscala"), "scm:git:git@github.com:Ma27/rediscala.git"))

apiURL := Some(url("http://etaty.github.io/rediscala/latest/api/"))

pomExtra :=
  <developers>
    <developer>
      <id>Ma27</id>
      <name>Valerian Barbot, The Rediscala community</name>
      <url>http://github.com/Ma27/</url>
    </developer>
  </developers>


resolvers ++= Seq(
  "Typesafe repository snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
)

publishMavenStyle := true

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-Xlint",
  "-deprecation",
  "-Xfatal-warnings",
  "-feature",
  "-language:postfixOps",
  "-unchecked"
)

libraryDependencies ++= {
  val akkaVersion = "2.5.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "org.specs2" %% "specs2-core" % "3.8.6" % Test,
    "org.scala-stm" %% "scala-stm" % "0.8",
    "org.scalacheck" %% "scalacheck" % "1.13.4",
    "com.storm-enroute" %% "scalameter" % "0.9"
  )
}

autoAPIMappings := true

// TODO create new github pages target
apiURL := Some(url("http://etaty.github.io/rediscala/"))

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

logBuffered := false
