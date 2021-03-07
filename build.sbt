val settingsHelper = ProjectSettingsHelper("au.id.tmm", "sbt")(
  githubProjectName = "sbt-tmm-plugin",
)

addCommandAlias("ci-release", ";releaseEarly")

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowJavaVersions := List("adopt@1.8", "adopt@1.11")
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scalafmtCheckAll")))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("ci-release")))

ThisBuild / githubWorkflowPublishPreamble := List(
  WorkflowStep.Run(
    commands = List("""./.secrets/decrypt.sh "${AES_KEY}""""),
    name = Some("Decrypt secrets"),
  ),
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
    name := "sbt-tmm-plugin",
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
