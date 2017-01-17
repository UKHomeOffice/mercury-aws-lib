name := "aws-scala-lib"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

fork in run := true

fork in Test := true

publishArtifact in Test := true

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Kamon Repository" at "http://repo.kamon.io",
  "jitpack" at "https://jitpack.io",
  Resolver.bintrayRepo("hseeberger", "maven"),
  Resolver.bintrayRepo("findify", "maven")
)

val `akka-version` = "2.4.16"
val `akka-http-version` = "10.0.1"
val `play-version` = "2.5.10"
val `aws-java-version` = "1.11.76"
val `elasticmq-version` = "0.12.1"
val `akka-scala-lib-version` = "2.1.1"
val `io-scala-lib-version` = "1.9.3"
val `test-scala-lib-version` = "1.4.1"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % `aws-java-version`,
  "com.typesafe.akka" %% "akka-actor" % `akka-version` withSources(),
  "com.typesafe.akka" %% "akka-http" % `akka-http-version` withSources(),
  "com.typesafe.play" %% "play-ws" % `play-version` withSources(),
  "org.elasticmq" %% "elasticmq-core" % `elasticmq-version` withSources(),
  "org.elasticmq" %% "elasticmq-rest-sqs" % `elasticmq-version` withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` withSources(),
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` withSources()
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test withSources(),
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test withSources(),
  "com.typesafe.play" %% "play-server" % `play-version` % Test withSources(),
  "com.typesafe.play" %% "play-test" % `play-version` % Test withSources(),
  "io.findify" %% "s3mock" % "0.1.5" % Test withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` % Test classifier "tests" withSources()
)

assemblyExcludedJars in assembly := {
  val testDependencies = (fullClasspath in Test).value
    .sortWith((f1, f2) => f1.data.getName < f2.data.getName)

  val compileDependencies = (fullClasspath in Compile).value
    .filterNot(_.data.getName.endsWith("-tests.jar"))
    .filterNot(_.data.getName.startsWith("mockito-"))
    .filterNot(_.data.getName.startsWith("specs2-"))
    .filterNot(_.data.getName.startsWith("scalatest"))
    .sortWith((f1, f2) => f1.data.getName < f2.data.getName)

  val testOnlyDependencies = testDependencies.diff(compileDependencies).sortWith((f1, f2) => f1.data.getName < f2.data.getName)
  testOnlyDependencies
}

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case "application.conf" => MergeStrategy.first
  case "application.test.conf" => MergeStrategy.discard
  case "version.conf" => MergeStrategy.concat
  case PathList("org", "mozilla", _*) => MergeStrategy.first
  case PathList("javax", "xml", _*) => MergeStrategy.first
  case PathList("com", "sun", _*) => MergeStrategy.first
  case PathList("org", "w3c", "dom", _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", "logging", _*) => MergeStrategy.first
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".java" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".so" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".jnilib" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".dll" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".tooling" => MergeStrategy.discard
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}