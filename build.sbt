name := "My Project"
 
version := "1.0"
 
scalaVersion := "2.10.1"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2-M2",
  "org.specs2" %% "specs2" % "1.14" % "test"
)