import sbt.Keys._

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version      := "0.17.0",
  scalaVersion := "2.13.6",
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
  javacOptions ++= Seq("-encoding", "UTF-8"),
  // scalafix
  addCompilerPlugin(scalafixSemanticdb),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  assembly / assemblyMergeStrategy := {
    case x if x.endsWith("module-info.class") => MergeStrategy.discard
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val common = (project in file("common")).settings(commonSettings: _*)

lazy val redmine =
  (project in file("redmine")).settings(commonSettings: _*).dependsOn(common)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-migration-redmine",
    libraryDependencies ++= Seq(
      "org.rogach" %% "scallop" % "4.1.0"
    ),
    assembly / assemblyJarName := {
      s"${name.value}-${version.value}.jar"
    },
    Test / testOptions ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(
        TestFrameworks.ScalaTest,
        "-f",
        "target/test-reports/output.txt"
      )
    ),
    assembly / test := {}
  )
  .dependsOn(common % "test->test;compile->compile")
  .dependsOn(redmine % "test->test;compile->compile")

addCommandAlias(
  "fixAll",
  "all compile:scalafix; test:scalafix; scalafmt; test:scalafmt; scalafmtSbt"
)
addCommandAlias(
  "checkAll",
  "compile:scalafix --check; test:scalafix --check; scalafmtCheck; test:scalafmtCheck; scalafmtSbtCheck"
)

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

Global / onChangedBuildSource := ReloadOnSourceChanges
