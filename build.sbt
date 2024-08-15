// See README.md for license details.
ThisBuild / version      := "1.1.1"
ThisBuild / organization := "cn.ac.ios.tis"
ThisBuild / scalaVersion := "2.13.7"

lazy val riscvSpecCore = (project in file("."))
  .settings(
    name := "RiscvSpecCore",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3"    % "3.6.0",
      "edu.berkeley.cs" %% "chiseltest" % "0.6.2" % "test"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.6.0" cross CrossVersion.full)
  )