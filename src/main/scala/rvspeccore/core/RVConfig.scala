package rvspeccore.core

import chisel3._
import chisel3.util._

case class RVConfig(configs: (String, Any)*) {
  private val acceptKeys = Map(
    "XLEN"           -> Set("32", "64"),
    "extensions"     -> Set("I", "M", "C", "Zifencei", "Zicsr", "U", "S"),
    "fakeExtensions" -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ".map(_.toString).toSet,
    "initValue"      -> Set("pc", "mstatus", "mtvec"),
    "functions"      -> Set("Privileged", "TLB"),
    "formal"         -> Set("ArbitraryRegFile")
  )
  private val cfgs = configs.toMap

  require(
    cfgs.keySet.subsetOf(acceptKeys.keySet),
    s"Unknown keys in RVConfig: ${(cfgs.keySet -- acceptKeys.keySet).mkString(",")}"
  )

  // - riscv-spec-20191213
  // : We use the term XLEN to refer to the width of an integer register in
  //   bits.
  require(cfgs.contains("XLEN"), "XLEN is required in RVConfig")
  val XLEN: Int = cfgs("XLEN").asInstanceOf[Int]
  require(acceptKeys("XLEN").contains(XLEN.toString), s"not supported XLEN in RVConfig: ${XLEN}")

  object extensions {
    val raw = cfgs.getOrElse("extensions", Seq.empty[String]).asInstanceOf[Seq[String]]
    require(
      raw.toSet.subsetOf(acceptKeys("extensions")),
      s"Unknown extension in RVConfig: ${raw} | ${(raw.toSet -- acceptKeys("extensions")).mkString(",")}"
    )

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

  object fakeExtensions {
    val raw = cfgs.getOrElse("fakeExtensions", Seq.empty[String]).asInstanceOf[Seq[String]]
    require(
      raw.toSet.subsetOf(acceptKeys("fakeExtensions")),
      s"Unknown fake extension in RVConfig: ${raw.toSet -- acceptKeys("fakeExtensions")}"
    )

    override def toString: String = raw.mkString("")
  }

  // CSRs Config
  object csr {
    // Misa
    val MisaExtList: String = fakeExtensions.toString +
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
  require(
    initValue.keySet.subsetOf(acceptKeys("initValue")),
    s"Unknown initValue in RVConfig: ${(initValue.keySet -- acceptKeys("initValue")).mkString(",")}"
  )

  // Other Functions Config
  object functions {
    protected val raw = cfgs.getOrElse("functions", Seq[String]()).asInstanceOf[Seq[String]]
    require(
      raw.toSet.subsetOf(acceptKeys("functions")),
      s"Unknown functions in RVConfig: ${(raw.toSet -- acceptKeys("functions")).mkString(",")}"
    )

    val privileged: Boolean = raw.contains("Privileged")
    val tlb: Boolean        = raw.contains("TLB")
  }

  // Formal
  object formal {
    protected val raw = cfgs.getOrElse("formal", Seq[String]()).asInstanceOf[Seq[String]]
    require(
      raw.toSet.subsetOf(acceptKeys("formal")),
      s"Unknown formal in RVConfig: ${(raw.toSet -- acceptKeys("formal")).mkString(",")}"
    )

    val arbitraryRegFile: Boolean = raw.contains("ArbitraryRegFile")
  }
}

object RVConfig {
  def apply(
      XLEN: Int,
      extensions: Seq[String],
      fakeExtensions: Seq[String],
      initValue: Map[String, String],
      functions: Seq[String],
      formal: Seq[String]
  ): RVConfig = {
    RVConfig(
      "XLEN"           -> XLEN,
      "extensions"     -> extensions,
      "fakeExtensions" -> fakeExtensions,
      "initValue"      -> initValue,
      "functions"      -> functions,
      "formal"         -> formal
    )
  }

  def apply(
      XLEN: Int,
      extensions: String = "",
      fakeExtensions: String = "",
      initValue: Map[String, String] = Map.empty,
      functions: Seq[String] = Seq.empty,
      formal: Seq[String] = Seq.empty
  ): RVConfig = apply(
    XLEN,
    extensions.split("(?=[A-Z])").filter(_.nonEmpty),
    fakeExtensions.split("(?=[A-Z])").filter(_.nonEmpty),
    initValue,
    functions,
    formal
  )

}
