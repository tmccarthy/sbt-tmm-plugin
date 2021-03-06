package au.id.tmm.sbt

import sbt.Keys.libraryDependencies
import sbt._

object DependencySettings {

  val silencerVersion = "1.7.1"
  val mUnitVersion    = "0.7.22"

  val commonDependencies: Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies += "org.scalameta"   %% "munit"        % mUnitVersion    % Test,
    libraryDependencies += "com.github.ghik" %% "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
    libraryDependencies += compilerPlugin(
      "com.github.ghik" %% "silencer-plugin" % silencerVersion cross CrossVersion.full,
    ),
  )

}
