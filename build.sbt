import Dependencies._

ThisBuild / scalaVersion := "2.12.12"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "jsoft.flow4s"
ThisBuild / organizationName := "jsoft"
ThisBuild / scalacOptions := Seq("-language:implicitConversions")
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

Global / onChangedBuildSource := ReloadOnSourceChanges
bintrayReleaseOnPublish in ThisBuild := false

lazy val disablingPublishingSettings = Seq(skip in publish := true, publishArtifact := false)

lazy val enablingPublishingSettings = Seq(
  publishArtifact := true,
  publishMavenStyle := true,
  // http://www.scala-sbt.org/0.12.2/docs/Detailed-Topics/Artifacts.html
  publishArtifact in Test := false,
  // Bintray
  bintrayPackageLabels := Seq("scala", "sbt"),
  bintrayRepository := "maven",
  bintrayVcsUrl := Option("https://github.com/joacovela16/modux"),
  bintrayOrganization := Option("jsoft"),
)

lazy val root = (project in file("."))
  .settings(
    name := "flow4s",
    libraryDependencies += scalaTest % Test,
    enablingPublishingSettings
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
