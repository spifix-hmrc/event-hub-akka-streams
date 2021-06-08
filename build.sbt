import sbt.Resolver

name := """event-hub"""
organization := "uk.gov.hmrc"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.12"

libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
libraryDependencies += guice

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("hmrc", "releases"),
  Resolver.bintrayRepo("jetbrains", "markdown"),
  "bintray-djspiewak-maven" at "https://dl.bintray.com/djspiewak/maven"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "uk.gov.hmrc.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "uk.gov.hmrc.binders._"
