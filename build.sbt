import sbt.Keys._

lazy val commonSettings = Seq(
  organization := "com.nulabinc",
  version := "0.11.0b4-SNAPSHOT",
  scalaVersion := "2.11.6",
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
  resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo),
  libraryDependencies ++= Seq(
    "com.osinka.i18n"               % "scala-i18n_2.11"    % "1.0.0",
    "ch.qos.logback"                % "logback-classic"    % "1.1.3",
    "com.typesafe.akka"             % "akka-actor_2.11"    % "2.3.11",
    "com.typesafe.akka"             % "akka-slf4j_2.11"    % "2.3.11",
    "io.spray"                      % "spray-json_2.11"    % "1.3.2",
    "com.github.scala-incubator.io" % "scala-io-core_2.11" % "0.4.3",
    "com.github.scala-incubator.io" % "scala-io-file_2.11" % "0.4.3",
    "com.typesafe"                  % "config"             % "1.3.0",
    "joda-time"                     % "joda-time"          % "2.3",
    "org.joda"                      % "joda-convert"       % "1.6",
    "com.google.inject"             % "guice"              % "4.1.0",
    "com.netaporter"                %% "scala-uri"         % "0.4.16",
    "org.fusesource.jansi"          % "jansi"              % "1.11",
    "com.mixpanel"                  % "mixpanel-java"      % "1.4.4"
  ),
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val common = (project in file("common"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-migration-common",
    libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.0.1" % "test"),
    unmanagedBase := baseDirectory.value / "libs",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  )

lazy val importer = (project in file("importer"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-importer",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  )
  .dependsOn(common % "test->test;compile->compile")
  .aggregate(common)

lazy val redmine = (project in file("redmine"))
  .settings(commonSettings: _*)
  .settings(
    name := "redmine",
    libraryDependencies ++= Seq("com.taskadapter" % "redmine-java-api" % "2.4.0"),
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(common % "test->test;compile->compile")
  .aggregate(common)

lazy val exporter = (project in file("exporter"))
  .settings(commonSettings: _*)
  .settings(
    name := "redmine-exporter",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(common % "test->test;compile->compile", redmine, mappingConverter)
  .aggregate(common, redmine)

lazy val mappingBase = (project in file("mapping-base"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-redmine-mapping-base",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(common % "test->test;compile->compile", redmine)


lazy val mappingConverter = (project in file("mapping-converter"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-redmine-mapping-converter",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(mappingBase)

lazy val mappingCollector = (project in file("mapping-collector"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-redmine-mapping-collector",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(mappingBase)

lazy val mappingFile = (project in file("mapping-file"))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-redmine-mapping-file",
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq(
      "NullParameter",
      "CatchThrowable",
      "NoOpOverride"
    )
  )
  .dependsOn(mappingBase)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "backlog-migration-redmine",
    libraryDependencies ++= Seq(
      "com.taskadapter" % "redmine-java-api" % "2.4.0",
      "org.scalatest"   %% "scalatest"       % "3.0.1" % "test",
      "org.rogach"      % "scallop_2.11"     % "2.0.5"
    ),
    assemblyJarName in assembly := {
      s"${name.value}-${version.value}.jar"
    },
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"),
      Tests.Argument(TestFrameworks.ScalaTest, "-f", "target/test-reports/output.txt")
    ),
    test in assembly := {},
    scapegoatVersion := "1.1.0",
    scapegoatDisabledInspections := Seq("NullParameter", "CatchThrowable", "NoOpOverride")
  )
  .dependsOn(common % "test->test;compile->compile", importer, exporter, mappingFile, mappingCollector)
  .aggregate(common, importer, exporter)
