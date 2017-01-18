name := "aws-scala-lib"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

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