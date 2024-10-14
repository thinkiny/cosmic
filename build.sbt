lazy val commonSettings = Seq(
  scalaVersion := "3.3.3",
  Compile / run / fork := true,
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-new-syntax",
    "-rewrite",
    "-Wunused:all"
  )
)

lazy val main = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "org.typelevel" %% "toolkit" % "0.1.28",
      "com.github.mwiede" % "jsch" % "0.2.20"
    )
  )
  .settings(
    name := "cosmic",
    assembly / assemblyJarName := "cosmic.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _                        => MergeStrategy.first
    }
  )
