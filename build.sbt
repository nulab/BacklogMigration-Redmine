import sbt.Keys._
import de.johoop.cpd4sbt.CopyPasteDetector._
import de.johoop.findbugs4sbt.FindBugs._

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.10.0b1-SNAPSHOT",
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-language:reflectiveCalls",
    "-language:postfixOps",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yrangepos",
    //"-Xfatal-warnings",
    //"-Ywarn-value-discard",
    //"-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  ),
  resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
  libraryDependencies ++= Seq(
    "com.osinka.i18n" % "scala-i18n_2.11" % "1.0.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "com.typesafe.akka" % "akka-actor_2.11" % "2.3.11",
    "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.11",
    "io.spray" % "spray-json_2.11" % "1.3.2",
    "com.github.scala-incubator.io" % "scala-io-core_2.11" % "0.4.3",
    "com.github.scala-incubator.io" % "scala-io-file_2.11" % "0.4.3",
    "com.typesafe" % "config" % "1.3.0",
    "joda-time" % "joda-time" % "2.3",
    "org.joda" % "joda-convert" % "1.6",
    "com.google.inject" % "guice" % "4.0-beta",
    "net.codingwell" %% "scala-guice" % "4.0.0-beta5",
    "com.netaporter" %% "scala-uri" % "0.4.16"
  ),
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val common = (project in file("common")).
  settings(commonSettings: _*).
  settings(
    name := "backlog-migration-common",
    unmanagedBase := baseDirectory.value / "libs",
    cpdSettings,
    findbugsSettings,
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  )
lazy val importer = (project in file("importer")).
  settings(commonSettings: _*).
  settings(
    name := "backlog-importer",
    cpdSettings,
    findbugsSettings,
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  ).dependsOn(common).aggregate(common)
lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "backlog-migration-redmine",
    libraryDependencies ++= Seq(
      "com.taskadapter" % "redmine-java-api" % "2.4.0",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "org.rogach" % "scallop_2.11" % "2.0.5"
    ),
    assemblyJarName in assembly := {
      s"${name.value}-${version.value}.jar"
    },
    cpdSettings,
    findbugsSettings,
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  ).dependsOn(common, importer).aggregate(common, importer)
