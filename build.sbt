ThisBuild / sonatypeProfile := "au.id.tmm"
ThisBuild / baseProjectName := "sbt"
ThisBuild / githubProjectName := "sbt-tmm-plugin"

ThisBuild / primaryScalaVersion := "2.12.13"

lazy val root = project
  .in(file("."))
  .settings(settingsForRootProject)
  .settings(console := (console in Compile in plugin).value)
  .aggregate(
    plugin,
  )

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(settingsForSubprojectCalled("tmm-plugin"))
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype"       % "3.9.5"),
    addSbtPlugin("com.github.sbt" % "sbt-pgp"            % "2.1.2"),
    addSbtPlugin("org.scalameta"  % "sbt-scalafmt"       % "2.4.6"),
    addSbtPlugin("ch.epfl.scala"  % "sbt-release-early"  % "2.1.1"),
    addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.14.2"),
  )
