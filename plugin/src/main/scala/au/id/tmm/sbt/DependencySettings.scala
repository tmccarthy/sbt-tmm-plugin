package au.id.tmm.sbt

import sbt.Keys.{libraryDependencies, testFrameworks}
import sbt._

object DependencySettings {

  val mUnitVersion = "1.0.0-M3"

  val commonDependencies = Seq(
    libraryDependencies += "org.scalameta" %% "munit" % mUnitVersion % Test,
    testFrameworks += new TestFramework("munit.Framework"),
  )

}
