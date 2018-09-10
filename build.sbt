import sbt.Keys._

scapegoatVersion in ThisBuild := "1.3.3"

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.13.0b6-SNAPSHOT",
  scalaVersion := "2.12.6",
  scalacOptions ++= Seq(
    "-language:reflectiveCalls",
    "-language:postfixOps",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yrangepos",
    "-Ywarn-dead-code",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  ),
  scapegoatVersion := "1.3.4",
  scapegoatDisabledInspections := Seq(
    "NullParameter",
    "CatchThrowable",
    "NoOpOverride"
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
      val catsVersion = "1.2.0"
      Seq(
        "org.typelevel"     %% "cats-core"      % catsVersion,
        "org.typelevel"     %% "cats-free"      % catsVersion,
        "io.monix"          %% "monix-reactive" % "3.0.0-RC1",
        "com.typesafe.akka" %% "akka-actor"     % "2.5.9",
        "com.typesafe.akka" %% "akka-slf4j"     % "2.5.9",
        "org.rogach"        %% "scallop"        % "3.1.2",
        "org.scalatest"     %% "scalatest"      % "3.0.5"       % "test"
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
