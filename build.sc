// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
import coursier.maven.MavenRepository
// support BSP
import mill.bsp._

object RiscvSpecCore extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion   = "2.12.15"
  override def scalacOptions = Seq(
    "-Xsource:2.11",
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
    // Enables autoclonetype2 in 3.4.x (on by default in 3.5)
    // "-P:chiselplugin:useBundlePlugin"
  )
  override def repositories = super.repositories ++ Seq(
    MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
    MavenRepository("https://oss.sonatype.org/content/repositories/releases")
  )
  override def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.5-SNAPSHOT"
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:3.5-SNAPSHOT",
    ivy"org.scalamacros:::paradise:2.1.1"
  )
  object test extends Tests with ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"edu.berkeley.cs::chiseltest:0.5-SNAPSHOT"
    )
  }
}
