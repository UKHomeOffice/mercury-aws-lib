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

transitiveClassifiers := Seq("sources")

val `akka-version` = "2.4.17"
val `akka-http-version` = "10.0.3"
val `play-version` = "2.5.12"
val `aws-java-version` = "1.11.104"
val `elasticmq-version` = "0.13.1"
val `mercury-akka-lib-version` = "2.1.17"
val `mercury-io-lib-version` = "1.9.12"
val `mercury-test-lib-version` = "1.4.4"
val `bouncy-castle-version` = "1.55"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % `aws-java-version`,
  "com.typesafe.akka" %% "akka-actor" % `akka-version`,
  "com.typesafe.akka" %% "akka-http" % `akka-http-version`,
  "com.typesafe.play" %% "play-ws" % `play-version`,
  "org.elasticmq" %% "elasticmq-core" % `elasticmq-version`,
  "org.elasticmq" %% "elasticmq-rest-sqs" % `elasticmq-version`,
  "com.github.UKHomeOffice" %% "mercury-akka-lib" % `mercury-akka-lib-version`,
  "com.github.UKHomeOffice" %% "mercury-io-lib" % `mercury-io-lib-version`,
  "com.github.UKHomeOffice" %% "mercury-test-lib" % `mercury-test-lib-version`,
  "org.bouncycastle" % "bcprov-ext-jdk15on" % `bouncy-castle-version`
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % `akka-version` % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % `akka-http-version` % Test,
  "com.typesafe.play" %% "play-server" % `play-version` % Test,
  "com.typesafe.play" %% "play-test" % `play-version` % Test,
  "io.findify" %% "s3mock" % "0.1.6" % Test,
  "com.github.UKHomeOffice" %% "mercury-akka-lib" % `mercury-akka-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "mercury-io-lib" % `mercury-io-lib-version` % Test classifier "tests",
  "com.github.UKHomeOffice" %% "mercury-test-lib" % `mercury-test-lib-version` % Test classifier "tests"
)
