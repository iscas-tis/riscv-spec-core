package rvspeccore.core

import chisel3._
import chisel3.util._

case class RVConfig(configs: (String, Any)*) {
  val cfgs = configs.toMap
  // - riscv-spec-20191213
  // : We use the term XLEN to refer to the width of an integer register in
  //   bits.
  require(cfgs.contains("XLEN"), "XLEN is required in RVConfig")
  val XLEN: Int = cfgs("XLEN").asInstanceOf[Int]
  require(XLEN == 32 || XLEN == 64, "RiscvCore only support 32 or 64 bits now")

  object extensions {
    protected val raw = cfgs.getOrElse("extensions", "").asInstanceOf[String]

    // - RISC-V ISA, Unprivileged, 20240411
    // - Preface
    val I        = true
    val M        = raw.contains("M")
    val C        = raw.contains("C")
    val Zifencei = raw.contains("Zifencei")
    val Zicsr    = raw.contains("Zicsr")

    // - RSIC-V ISA, Privileged, 20240411
    // - 1. Introduction
    // - 1.2. Privilege Levels
    //   - Table 1. RISC-V privilege levels.
    val U = raw.contains("U")
    val S = raw.contains("S")
    //   - Table 2. Supported combination of privilege modes.
    assert(
      (!U && !S) || (U && !S) || (U && S),
      "Supported combination of privilege modes are M, MU, MSU, " +
        s"not M${if (S) "S" else ""}${if (U) "U" else ""}"
    )
  }
  val fakeExtensions: String = cfgs.getOrElse("fakeExtensions", "").asInstanceOf[String]

  // CSRs Config
  object csr {
    // Misa
    val MisaExtList: String =
      cfgs.getOrElse("fakeExtensions", "").asInstanceOf[String] +
        Seq(
          if (extensions.I) 'I' else "",
          if (extensions.M) 'M' else "",
          if (extensions.C) 'C' else "",
          if (extensions.S) 'S' else "",
          if (extensions.U) 'U' else ""
        ).mkString("")
  }

  // Init Value
  val initValue = cfgs.getOrElse("initValue", Map[String, String]()).asInstanceOf[Map[String, String]]

  // Other Functions Config
  object functions {
    protected val raw       = cfgs.getOrElse("functions", Seq[String]()).asInstanceOf[Seq[String]]
    val privileged: Boolean = raw.contains("Privileged")
  }
}
