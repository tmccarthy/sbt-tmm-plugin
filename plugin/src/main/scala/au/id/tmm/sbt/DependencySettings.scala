package au.id.tmm.sbt

import sbt.Keys.{libraryDependencies, testFrameworks}
import sbt._

object DependencySettings {

  val mUnitVersion = "0.7.27"

  val commonDependencies = Seq(
    libraryDependencies += "org.scalameta" %% "munit" % mUnitVersion % Test,
    testFrameworks += new TestFramework("munit.Framework"),
  )

}
