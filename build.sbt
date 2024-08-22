// use `-DCHA=true` to use CHA version Chisel
lazy val useCHA: Boolean =
  sys.props.getOrElse("CHA", "false").toLowerCase == "true"

ThisBuild / version      := { if (useCHA) "1.3-cha-SNAPSHOT" else "1.3-SNAPSHOT" }
ThisBuild / organization := "cn.ac.ios.tis"
ThisBuild / scalaVersion := "2.12.17"
// Use Scala2.13 with ChiselTest0.6.0 will cause efficiency issues in the
// simulation. Here use Scala2.12 and ChiselTest0.6.0 to avoid this problem.

ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8")

resolvers ++= Resolver.sonatypeOssRepos("releases")
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val publishSettings = Seq(
  versionScheme := {
    if (version.value.contains("-")) Some("early-semver")
    else Some("semver-spec")
  },
  isSnapshot := version.value.endsWith("-SNAPSHOT"),

  // As of February 2021, all new projects began being provisioned on https://s01.oss.sonatype.org/
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
  // publishTo should be defined by sbt-sonatype

  homepage             := Some(url("https://github.com/iscas-tis/riscv-spec-core")),
  organizationHomepage := Some(url("https://tis.ios.ac.cn")),
  licenses             := List(License.Apache2),
  developers := List(
    Developer("liuyic00", "Yicheng Liu", "liuyic00@gmail.com", url("https://github.com/liuyic00")),
    Developer("SeddonShen", "Shidong Shen", "seddonshen2001@gmail.com", url("https://github.com/SeddonShen"))
  )
)

lazy val root = (project in file("."))
  .settings(publishSettings: _*)
  .settings(
    name := "RiscvSpecCore",
    libraryDependencies ++= {
      if (useCHA)
        Seq(
          "cn.ac.ios.tis" %% "chisel3"    % "3.7-SNAPSHOT",
          "cn.ac.ios.tis" %% "chiseltest" % "0.7-SNAPSHOT" % "test"
        )
      else
        Seq(
          "edu.berkeley.cs" %% "chisel3"    % "3.6.0",
          "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test"
        )
    },
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin(
      if (useCHA) "cn.ac.ios.tis" % "chisel3-plugin" % "3.7-SNAPSHOT" cross CrossVersion.full
      else "edu.berkeley.cs"      % "chisel3-plugin" % "3.6.0" cross CrossVersion.full
    )
  )
