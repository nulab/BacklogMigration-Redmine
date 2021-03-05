import sbt.Keys._

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.16.1",
  scalaVersion := "2.13.5",
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

lazy val common = (project in file("common")).settings(commonSettings: _*)

lazy val redmine = (project in file("redmine")).settings(commonSettings: _*).dependsOn(common)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-migration-redmine",
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "3.3.1"
    ),
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
  .dependsOn(redmine % "test->test;compile->compile")
