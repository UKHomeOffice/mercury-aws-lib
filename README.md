AWS Library for Scala
=====================
Scala library to interact with AWS e.g. easier coding and testing against SQS and S3.
ElasticMQ can be used for embedded testing of the SQS messaging system and S3 Mock for AWS S3 testing.

Application built with the following (main) technologies:

- Scala

- SBT

- Akka

- AWS SQS / ElasticMQ

- AWS S3

Application
-----------
The application is configured as per a typical Scala application, where the default configuration file is "application.conf" (or reference.conf).
This default file can be overridden with other "conf" files and then given to the application upon boot with the following example Java option:
```
-Dconfig.file=test-classes/application.test.conf
```

Individual configuration properties can be overridden again by Java options e.g. to override which Mongodb to connect:
```bash
-Dmongo.db=some-other-mongo
```

where this overrides the default in application.conf.

Build and Deploy
----------------
The project is built with SBT. On a Mac (sorry everyone else) do:
```
brew install sbt
```

It is also a good idea to install Typesafe Activator (which sits on top of SBT) for when you need to create new projects - it also has some SBT extras, so running an application with Activator instead of SBT can be useful. On Mac do:
```
brew install typesafe-activator
```

To compile:
```
sbt compile
```

or
```
activator compile
```

To run the specs:
```
sbt test
```

To actually run the application, you can simply:
```
sbt run
```

or first "assemble" it:
```
sbt assembly
```

This packages up an executable JAR - Note that "assembly" will first compile and test.

Then just run as any executable JAR, with any extra Java options for overriding configurations.

For example, to use a config file (other than the default application.conf) which is located on the file system (in this case in the boot directory)
```
java -Dconfig.file=test-classes/my-application.conf -jar <jar name>.jar
```

Note that the log configuration file could also be included e.g.
```
-Dlogback.configurationFile=path/to/my-logback.xml
```

So a more indepth startup with sbt itself could be:
```
sbt test:run -Dconfig.file=target/scala-2.11/test-classes/application.test.conf -Dlogback.configurationFile=target/scala-2.11/test-classes/logback.test.xml
```

Note the use of test:run. Usually we would only use "run", but as this is a library, there is no default "main" class, but we do have an example test "main" class.

And another example:

running from directory of the executable JAR using a config that is within said JAR:
```
java -Dconfig.resource=application.uat.conf -jar <jar name>.jar
```

SBT - Revolver (keep things going while developing/testing)
-----------------------------------------------------------
sbt-revolver is a plugin for SBT enabling a super-fast development turnaround for your Scala applications:

See https://github.com/spray/sbt-revolver

For development, you can use ~re-start to go into "triggered restart" mode.
Your application starts up and SBT watches for changes in your source (or resource) files.
If a change is detected SBT recompiles the required classes and sbt-revolver automatically restarts your application. 
When you press &lt;ENTER&gt; SBT leaves "triggered restart" and returns to the normal prompt keeping your application running.

Gatling - Performance (Integration) Testing
-------------------------------------------
Performance tests are under src/it, and test reports are written to the "target" directory.

To execute Gatling performance integration tests from withing SBT:
```
gatling-it:test
```

Example Usage
-------------
Example of booting an application to publish/subscribe to an Amazon SQS instance.

1) Start up an instance of ElasticMQ (to run an instance of Amazon SQS locally) - From the root of this project:
```bash
java -jar bin/elasticmq-server-0.12.1.jar
```
which starts up a working server that binds to localhost:9324

or with a custom configuration that could create queues:
```
java -Dconfig.file=src/test/resources/application.test.conf -jar bin/elasticmq-server-0.12.1.jar
```
   
2) Boot this application:
```
sbt test:run
```
   
where the example application can be found under the "test" directory and is also show here:
```scala
object ExampleBoot extends App {
  val system = ActorSystem("amazon-sqs-actor-system")

  implicit val sqsClient = new SQSClient(new URL("http://localhost:9324"), new BasicAWSCredentials("x", "x"))

  val queue = new Queue("test-queue")

  system actorOf Props {
    new SubscriberActor(new Subscriber(queue)) with ExampleSubscription
  }

  new Publisher(queue) publish compact(render("input" -> "blah"))
}

trait ExampleSubscription extends JsonSubscription with Exit {
  this: SubscriberActor =>

  def receive: Receive = {
    case m: Message => exitAfter {
      val result = s"Well Done! Processed given message $m"
      println(result)
      result
    }
  }
}
```

Releasing
---------
Version control of this library can be achieved through the SBT Release plugin e.g.
```
sbt release
```

where the following default value will be chosen:
- Continue with snapshots dependencies: no
- Release Version: current version without the qualifier (eg. 1.2.0-SNAPSHOT -> 1.2.0)
- Next Version: increase the minor version segment of the current version and set the qualifier to '-SNAPSHOT' (eg. 1.2.1-SNAPSHOT -> 1.3.0-SNAPSHOT)
- VCS tag: abort if the tag already exists
- VCS push:
    - Abort if no remote tracking branch is set up.
    - Abort if remote tracking branch cannot be checked (eg. via git fetch).
    - Abort if the remote tracking branch has unmerged commits.
    - Set release version and next version as command arguments

You can set the release version using the argument release-version and next version with next-version.

Example (within sbt):
```
release release-version 1.0.99 next-version 1.2.0-SNAPSHOT
```

or
```
release with-defaults
```