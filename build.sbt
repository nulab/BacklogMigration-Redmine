import sbt.Keys._

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.14.0b3-SNAPSHOT",
  scalaVersion := "2.13.1",
  scalacOptions ++= Seq(
    "-language:reflectiveCalls",
    "-language:postfixOps",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yrangepos",
    "-Ywarn-dead-code",
    "-Xlint:unused"
  ),
  resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)

lazy val importer = (project in file("importer"))
  .settings(commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val redmine = (project in file("redmine"))
  .settings(commonSettings: _*)
  .dependsOn(common % "test->test;compile->compile")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-migration-redmine",
    libraryDependencies ++= {
      val catsVersion = "2.0.0"
      val akkaVersion = "2.5.26"
      Seq(
        "org.typelevel"     %% "cats-core"      % catsVersion,
        "org.typelevel"     %% "cats-free"      % catsVersion,
        "io.monix"          %% "monix-reactive" % "3.0.0",
        "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion,
        "org.rogach"        %% "scallop"        % "3.3.1",
        "org.scalatest"     %% "scalatest"      % "3.0.8"       % "test"
      )
    },
    assemblyJarName in assembly := {
      s"${name.value}-${version.value}.jar"
    },
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-f", "target/test-reports/output.txt")
    ),
    test in assembly := {}
  )
  .dependsOn(common % "test->test;compile->compile")
  .dependsOn(redmine)
  .dependsOn(importer)
