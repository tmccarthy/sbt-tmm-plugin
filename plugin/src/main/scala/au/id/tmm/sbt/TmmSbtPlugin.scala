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
      sonatypeProfileNameSetting,
    )

    def settingsForSubprojectCalled(name: String) = Seq(
      Keys.name := s"${(ThisBuild / baseProjectName).value}-$name",
      ScalacSettings.scalacSetting,
      publishConfiguration := publishConfiguration.value.withOverwrite(true),
      publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
      sonatypeProfileNameSetting,
    ) ++ DependencySettings.commonDependencies

  }

  import autoImport._

  override def trigger = PluginTrigger.AllRequirements

  override def requires: Plugins =
    xerial.sbt.Sonatype &&
      ch.epfl.scala.sbt.release.ReleaseEarlyPlugin &&
      org.scalafmt.sbt.ScalafmtPlugin &&
      sbtghactions.GitHubActionsPlugin

  private def defaultsForSettings = List(
    githubUser := "tmccarthy",
    githubProjectName := (ThisBuild / baseProjectName).value,
    githubUserFullName := "Timothy McCarthy",
    githubUserEmail := "ebh042@gmail.com",
    githubUserWebsite := "http://tmm.id.au",
    primaryScalaVersion := "2.13.5",
    otherScalaVersions := List(),
  )

  private def commandAliases = addCommandAlias("ci-release", ";releaseEarly") ++
    addCommandAlias("check", ";+test;scalafmtCheckAll;scalafmtSbtCheck;githubWorkflowCheck")

  // TODO this is probably being used in too many places but it has fixed it so 🤷
  private def sonatypeProfileNameSetting = Sonatype.SonatypeKeys.sonatypeProfileName := sonatypeProfile.value

  private def sonatypeSettings = List(
    releaseEarly / Keys.aggregate := false, // Workaround for https://github.com/scalacenter/sbt-release-early/issues/30
    sonatypeProfileNameSetting,
  ) ++ sbt.inThisBuild(
    List(
      sonatypeProfileNameSetting,
      organization := (ThisBuild / sonatypeProfile).value + "." + (ThisBuild / baseProjectName).value,
      publishMavenStyle := true,
      sonatypeProjectHosting := Some(
        GitHubHosting(
          (ThisBuild / githubUser).value,
          (ThisBuild / githubProjectName).value,
          (ThisBuild / githubUserFullName).value,
          (ThisBuild / githubUserEmail).value,
        ),
      ),
      homepage := Some(
        url(s"https://github.com/${(ThisBuild / githubUser).value}/${(ThisBuild / githubProjectName).value}"),
      ),
      startYear := Some(2019),
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      developers := List(
        Developer(
          (ThisBuild / githubUser).value,
          (ThisBuild / githubUserFullName).value,
          (ThisBuild / githubUserEmail).value,
          url((ThisBuild / githubUserWebsite).value),
        ),
      ),
      scmInfo := Some(
        ScmInfo(
          url(s"https://github.com/${(ThisBuild / githubUser).value}/${(ThisBuild / githubProjectName).value}"),
          s"scm:git:https://github.com/${(ThisBuild / githubUser).value}/${(ThisBuild / githubProjectName).value}.git",
        ),
      ),
      pgpPublicRing := file("/tmp/secrets/pubring.kbx"),
      pgpSecretRing := file("/tmp/secrets/secring.gpg"),
      releaseEarlyWith := ReleaseEarlyPlugin.autoImport.SonatypePublisher,
      releaseEarlyEnableInstantReleases := false,
    ),
  )

  private def compilerPlugins =
    kindProjectorSettings ++
      List(
        addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      )

  private def kindProjectorSettings = List(
    ThisBuild / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _))                  => List("-Ykind-projector:underscores")
        case Some((2, 13)) | Some((2, 12)) => List("-Xsource:3", "-P:kind-projector:underscore-placeholders")
        case _                             => List.empty
      }
    },
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => List.empty
        case Some((2, 13)) | Some((2, 12)) =>
          List(compilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full))
        case _ => List.empty
      }
    },
  )

  private def scalaVersionSettings = List(
    scalaVersion := (ThisBuild / primaryScalaVersion).value,
    crossScalaVersions := Seq((ThisBuild / primaryScalaVersion).value) ++ (ThisBuild / otherScalaVersions).value,
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

  override def globalSettings: Seq[Def.Setting[_]] =
    defaultsForSettings ++
      commandAliases ++
      scalaVersionSettings

  override def buildSettings: Seq[Def.Setting[_]] =
    sonatypeSettings ++
      compilerPlugins ++
      githubWorkflowSettings

}
