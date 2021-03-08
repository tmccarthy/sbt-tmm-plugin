package au.id.tmm.sbt

import ch.epfl.scala.sbt.release.AutoImported.{releaseEarly, releaseEarlyEnableInstantReleases, releaseEarlyWith}
import ch.epfl.scala.sbt.release.ReleaseEarlyPlugin
import com.typesafe.sbt.SbtPgp.autoImportImpl.{pgpPublicRing, pgpSecretRing}
import sbt.Keys._
import sbt.{Def, addCommandAlias, _}
import sbtghactions.GenerativePlugin.autoImport._
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.autoImport.sonatypeProjectHosting

object TmmSbtPlugin extends AutoPlugin {
  object autoImport {
    val sonatypeProfile = SettingKey[String]("sonatypeProfile")
    val baseProjectName = SettingKey[String]("baseProjectName")

    val githubUser          = SettingKey[String]("githubUser")
    val githubProjectName   = SettingKey[String]("githubProjectName")
    val githubUserFullName  = SettingKey[String]("githubUserFullName")
    val githubUserEmail     = SettingKey[String]("githubUserEmail")
    val githubUserWebsite   = SettingKey[String]("githubUserWebsite")
    val primaryScalaVersion = SettingKey[String]("primaryScalaVersion")
    val otherScalaVersions  = SettingKey[List[String]]("otherScalaVersions")

    def settingsForRootProject = Seq(
      publish / skip := true,
      name := (ThisBuild / baseProjectName).value,
      crossScalaVersions := Nil,
    )

    def settingsForSubprojectCalled(name: String) = Seq(
      Keys.name := s"${(ThisBuild / baseProjectName).value}-$name",
      ScalacSettings.scalacSetting,
      publishConfiguration := publishConfiguration.value.withOverwrite(true),
      publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    ) ++ DependencySettings.commonDependencies

  }

  import autoImport._

  override def requires: Plugins =
    xerial.sbt.Sonatype &&
      ch.epfl.scala.sbt.release.ReleaseEarlyPlugin &&
      org.scalafmt.sbt.ScalafmtPlugin &&
      sbtghactions.GitHubActionsPlugin

  private def defaultsForSettings = List(
    (ThisBuild / githubUser) := "tmccarthy",
    (ThisBuild / githubProjectName) := baseProjectName.value,
    (ThisBuild / githubUserFullName) := "Timothy McCarthy",
    (ThisBuild / githubUserEmail) := "ebh042@gmail.com",
    (ThisBuild / githubUserWebsite) := "http://tmm.id.au",
    (ThisBuild / primaryScalaVersion) := "2.13.5",
    (ThisBuild / otherScalaVersions) := List(),
  )

  private def commandAliases = addCommandAlias("ci-release", ";releaseEarly") ++
    addCommandAlias("check", ";+test;scalafmtCheckAll;scalafmtSbtCheck;githubWorkflowCheck")

  private def sonatypeSettings = List(
    releaseEarly / Keys.aggregate := false, // Workaround for https://github.com/scalacenter/sbt-release-early/issues/30
    Sonatype.SonatypeKeys.sonatypeProfileName := sonatypeProfile.value,
  ) ++ sbt.inThisBuild(
    List(
      organization := sonatypeProfile + "." + baseProjectName,
      publishMavenStyle := true,
      sonatypeProjectHosting := Some(
        GitHubHosting(
          githubUser.value,
          githubProjectName.value,
          githubUserFullName.value,
          githubUserEmail.value,
        ),
      ),
      homepage := Some(url(s"https://github.com/$githubUser/$githubProjectName")),
      startYear := Some(2019),
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      developers := List(
        Developer(
          githubUser.value,
          githubUserFullName.value,
          githubUserEmail.value,
          url(githubUserWebsite.value),
        ),
      ),
      scmInfo := Some(
        ScmInfo(
          url(s"https://github.com/$githubUser/$githubProjectName"),
          s"scm:git:https://github.com/$githubUser/$githubProjectName.git",
        ),
      ),
      pgpPublicRing := file("/tmp/secrets/pubring.kbx"),
      pgpSecretRing := file("/tmp/secrets/secring.gpg"),
      releaseEarlyWith := ReleaseEarlyPlugin.autoImport.SonatypePublisher,
      releaseEarlyEnableInstantReleases := false,
    ),
  )

  private def compilerPlugins =
    List(
      addCompilerPlugin("org.typelevel" % "kind-projector"     % "0.11.0" cross CrossVersion.full), // TODO upgrade
      addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1"),
    )

  private def scalaVersionSettings = List(
    scalaVersion := primaryScalaVersion.value,
    crossScalaVersions := Seq(primaryScalaVersion.value) ++ otherScalaVersions.value,
  )

  private def githubWorkflowSettings = List(
    ThisBuild / githubWorkflowTargetTags ++= Seq("v*"),
    ThisBuild / githubWorkflowPublishTargetBranches :=
      Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
    ThisBuild / githubWorkflowJavaVersions := List("adopt@1.8", "adopt@1.11"),
    ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scalafmtCheckAll", "scalafmtSbtCheck"))),
    ThisBuild / githubWorkflowPublish := List(
      WorkflowStep.Sbt(
        List("ci-release"),
        env = Map(
          "PGP_PASSWORD" -> "${{ secrets.PGP_PASSWORD }}",
          "SONA_PASS"    -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONA_USER"    -> "${{ secrets.SONATYPE_USER }}",
        ),
      ),
    ),
    ThisBuild / githubWorkflowPublishPreamble := List(
      WorkflowStep.Run(
        commands = List("""./.secrets/decrypt.sh"""), // TODO don't do this with an external script
        name = Some("Decrypt secrets"),
        env = Map(
          "AES_KEY" -> "${{ secrets.AES_KEY }}",
        ),
      ),
    ),
  )

  override def globalSettings: Seq[Def.Setting[_]] = defaultsForSettings ++ commandAliases

  override def buildSettings: Seq[Def.Setting[_]] =
      sonatypeSettings ++
      compilerPlugins ++
      scalaVersionSettings ++
      githubWorkflowSettings

}
