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
val `akka-http-version` = "10.0.3"
val `play-version` = "2.5.12"
val `aws-java-version` = "1.11.76"
val `elasticmq-version` = "0.13.1"
val `akka-scala-lib-version` = "2.1.11"
val `io-scala-lib-version` = "1.9.10"
val `test-scala-lib-version` = "1.4.4"

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
  "io.findify" %% "s3mock" % "0.1.6" % Test withSources(),
  "com.github.UKHomeOffice" %% "akka-scala-lib" % `akka-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "io-scala-lib" % `io-scala-lib-version` % Test classifier "tests" withSources(),
  "com.github.UKHomeOffice" %% "test-scala-lib" % `test-scala-lib-version` % Test classifier "tests" withSources()
)