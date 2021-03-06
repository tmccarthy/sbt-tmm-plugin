val settingsHelper = ProjectSettingsHelper("au.id.tmm", "sbt-tmm")(
  githubProjectName = "sbt-tmm",
)

settingsHelper.settingsForBuild

lazy val root = project
  .in(file("."))
  .settings(settingsHelper.settingsForRootProject)
  .settings(console := (console in Compile in plugin).value)
  .aggregate(
    plugin,
  )

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "tmm-sbt-plugin",
    ScalacSettings.scalacSetting,
    publishConfiguration := publishConfiguration.value.withOverwrite(true),
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
  )
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype"      % "2.4"),
    addSbtPlugin("com.jsuereth"   % "sbt-pgp"           % "1.1.2"),
    addSbtPlugin("org.scalameta"  % "sbt-scalafmt"      % "2.4.2"),
    addSbtPlugin("ch.epfl.scala"  % "sbt-release-early" % "2.1.1"),
  )
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0-M4" % "test",
  )

addCommandAlias("check", ";+test;scalafmtCheckAll")
