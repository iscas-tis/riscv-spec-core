// See README.md for license details.

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / version      := "0.1.0"
ThisBuild / organization := "com.github.liuyic00"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

lazy val root = (project in file("."))
  .settings(
    name := "RiscvSpecCore",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3"    % "3.5-SNAPSHOT",
      "edu.berkeley.cs" %% "chiseltest" % "0.5-SNAPSHOT" % "test"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
      // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
      // "-P:chiselplugin:useBundlePlugin"
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5-SNAPSHOT" cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise"       % "2.1.1" cross CrossVersion.full)
  )
