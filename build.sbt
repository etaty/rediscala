name := "Rediscala"
 
version := "0.1-SNAPSHOT"

scalaVersion := "2.10.1"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.0-RC1",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

publishTo <<= version { (version: String) =>
  val localPublishRepo = "/Users/valerian/Projects/rediscala/dist"
  if(version.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
  else Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
}

publishMavenStyle := true