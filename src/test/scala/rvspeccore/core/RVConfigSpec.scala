package rvspeccore.core

import org.scalatest.flatspec.AnyFlatSpec

class RVConfigSpec extends AnyFlatSpec {
  behavior of "RVConfig"

  it should "be able to be created" in {
    val config = RVConfig(
      "XLEN"           -> 64,
      "extensions"     -> "IMCZifenceiU",
      "fakeExtensions" -> "A",
      "CSRs"           -> Seq("Misa"),
      "functions"      -> Seq("Privileged"),
      "initValue" -> Map(
        "pc" -> "h8000_0000"
      )
    )
    assert(config.XLEN == 64)
    assert(config.extensions.I)
    assert(config.extensions.M)
    assert(config.extensions.C)
    assert(config.extensions.Zifencei)
    assert(config.extensions.Zicsr == false)
    assert(config.extensions.U)
    assert(config.extensions.S == false)
    assert(config.fakeExtensions == "A")
    assert(config.csr.MisaExtList == "AIMCU")
    assert(config.initValue("pc") == "h8000_0000")
    assert(config.functions.privileged)
  }
}
