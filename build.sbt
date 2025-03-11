// use `-DChiselVersion=3.x.x/6.x.x/7.x.x/CHA` to specify the version of Chisel
// use `-DHashId=true` to include git hash id in the version number
// use `-DScalaVersion=2.12.17/orOther` to specify the version of Scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.3-8bb84f4-SNAPSHOT"

lazy val chiselVersion: Map[String, ModuleID] = {
  sys.props.getOrElse("ChiselVersion", "3.6.0") match {
    case x if x.startsWith("3") =>
      Map(
        "chisel"     -> "edu.berkeley.cs" %% "chisel3"        % x,
        "chiseltest" -> "edu.berkeley.cs" %% "chiseltest"     % "0.6.0",
        "plugin"     -> "edu.berkeley.cs"  % "chisel3-plugin" % x
      )
    case x if x.startsWith("6") || x.startsWith("7") =>
      Map(
        "chisel"     -> "org.chipsalliance" %% "chisel"        % x,
        "chiseltest" -> "edu.berkeley.cs"   %% "chiseltest"    % "6.0.0",
        "plugin"     -> "org.chipsalliance"  % "chisel-plugin" % x
      )
    case "CHA" =>
      Map(
        "chisel"     -> "cn.ac.ios.tis" %% "chisel3"        % "3.7-SNAPSHOT",
        "chiseltest" -> "cn.ac.ios.tis" %% "chiseltest"     % "0.7-SNAPSHOT",
        "plugin"     -> "cn.ac.ios.tis"  % "chisel3-plugin" % "3.7-SNAPSHOT"
      )
    case _ => throw new Exception("chiselVersion should be one of 3.x.x, 6.x.x, 7.x.x, CHA")
  }
}

ThisBuild / version := {
  val versionNumber = "1.3"
  val snapshot      = "-SNAPSHOT"

  val chiselVersionTag: String = {
    sys.props.getOrElse("ChiselVersion", "").toLowerCase match {
      case ""    => ""
      case "cha" => "-cha"
      case x     => s"-chisel$x"
    }
  }
  val hashId = {
    val useHashId = sys.props.getOrElse("HashId", "false").toLowerCase == "true"
    val hash      = git.gitHeadCommit.value.map(_.take(7)).getOrElse("unknown")
    if (useHashId)
      if (git.gitUncommittedChanges.value) s"-$hash+"
      else s"-$hash"
    else ""
  }

  versionNumber + chiselVersionTag + hashId + snapshot
}
ThisBuild / organization := "cn.ac.ios.tis"
ThisBuild / scalaVersion := sys.props.getOrElse("ScalaVersion", "2.12.17")
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
      Seq(
        chiselVersion("chisel"),
        chiselVersion("chiseltest") % "test"
      )
    },
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin(
      chiselVersion("plugin") cross CrossVersion.full
    )
  )
