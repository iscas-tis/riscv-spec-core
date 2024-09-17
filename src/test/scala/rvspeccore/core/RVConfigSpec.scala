package rvspeccore.core

import org.scalatest.flatspec.AnyFlatSpec

class RVConfigSpec extends AnyFlatSpec {
  behavior of "RVConfig"

  it should "be able to be created" in {
    val config = RVConfig(
      XLEN = 64,
      extensions = "IMCZifenceiU",
      fakeExtensions = "A",
      initValue = Map(
        "pc"    -> "h8000_0000",
        "mtvec" -> "h0000_01c0"
      ),
      functions = Seq("Privileged"),
      formal = Seq("ArbitraryRegFile")
    )
    assert(config.XLEN == 64)
    assert(config.extensions.I)
    assert(config.extensions.M)
    assert(config.extensions.C)
    assert(config.extensions.Zifencei)
    assert(config.extensions.Zicsr == false)
    assert(config.extensions.U)
    assert(config.extensions.S == false)
    assert(config.fakeExtensions.toString == "A")
    assert(config.csr.MisaExtList == "AIMCU")
    assert(config.initValue("pc") == "h8000_0000")
    assert(config.initValue("mtvec") == "h0000_01c0")
    assert(config.functions.privileged)
    assert(config.formal.arbitraryRegFile)

    val configs = Seq(
      RVConfig(64),
      RVConfig(64, "MC"),
      RVConfig(64, "MC", "ABXY"),
      // this is not recommended:
      RVConfig(
        "XLEN"           -> 64,
        "extensions"     -> Seq("I", "M", "C", "Zifencei", "U"),
        "fakeExtensions" -> Seq("A"),
        "functions"      -> Seq("Privileged"),
        "initValue" -> Map(
          "pc"    -> "h8000_0000",
          "mtvec" -> "h0000_01c0"
        ),
        "formal" -> Seq("ArbitraryRegFile")
      )
    )
  }
}
