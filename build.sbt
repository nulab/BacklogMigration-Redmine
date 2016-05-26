name := "backlog-migration-redmine"

organization := "com.nulabinc"

version := "0.9.0b28"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.rogach" %% "scallop" % "0.9.5",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "ch.qos.logback" % "logback-core" % "1.1.3",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.11",
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.11",
  "io.spray" %% "spray-json" % "1.3.2",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "com.h2database" % "h2" % "1.4.185",
  "com.osinka.i18n" %% "scala-i18n" % "1.0.0",
  "com.taskadapter" % "redmine-java-api" % "2.4.0",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.6",
  "com.typesafe" % "config" % "1.3.0",
  "com.nulabinc" %% "backlog-importer" % "0.0.1"
)

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".types" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".class" => MergeStrategy.last
  case "application.conf" => MergeStrategy.concat
  case "messages.txt" => MergeStrategy.concat
  case "messages_ja.txt" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

scalacOptions ++= Seq("-Yrangepos", "-Ywarn-unused", "-Ywarn-unused-import", "-Xlint")

// Read here for optional dependencies:
// http://etorreborre.github.io/specs2/guide/org.specs2.guide.Runners.html#Dependencies

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)
resolvers ++= Seq(
  "LocalRepo" at file(Path.userHome.absolutePath + "/.ivy2/local").getAbsolutePath
)

initialCommands := "import com.nulabinc.r2b._"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

jarName in assembly := { s"${name.value}-${version.value}.jar" }

import de.johoop.cpd4sbt.CopyPasteDetector._
import de.johoop.findbugs4sbt.FindBugs._

cpdSettings
findbugsSettings