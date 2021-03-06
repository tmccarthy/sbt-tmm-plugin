package au.id.tmm.sbt

import sbt.Keys.{libraryDependencies, testFrameworks}
import sbt._

object DependencySettings {

  val silencerVersion = "1.7.3"
  val mUnitVersion    = "0.7.22"

  val commonDependencies = Seq(
    libraryDependencies += "org.scalameta"   %% "munit"        % mUnitVersion    % Test,
    libraryDependencies += "com.github.ghik" %% "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
    libraryDependencies += compilerPlugin(
      "com.github.ghik" %% "silencer-plugin" % silencerVersion cross CrossVersion.full,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )

}
