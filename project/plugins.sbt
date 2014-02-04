resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.6.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.1.1")

addSbtPlugin("com.github.scct" % "sbt-scct" % "0.2")

addSbtPlugin("com.github.theon" %% "xsbt-coveralls-plugin" % "0.0.3")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")
