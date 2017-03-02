name := "aws-scala-lib"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

releaseIgnoreUntrackedFiles := true

scalacOptions ++= Seq(
  "-feature",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:reflectiveCalls",
  "-language:postfixOps",
  "-Yrangepos",
  "-Yrepl-sync"
)

lazy val IT = config("it") extend Test

lazy val root = project.in(file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .configs(IT)
  .settings(inConfig(IT)(Defaults.testSettings) : _*)
  .settings(Revolver.settings)