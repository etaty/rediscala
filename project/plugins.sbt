
resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.6.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.1")

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.7")

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-coveralls" % "0.0.5")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")