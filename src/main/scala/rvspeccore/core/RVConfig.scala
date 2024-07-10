package rvspeccore.core

import chisel3._
import chisel3.util._

/** @param XLEN
  *   The width of an integer register in bits
  * @param extensions
  *   Supported extensions
  * @param fakeExtensions
  *   RiscvCore does not support these extensions, but they will appear in misa
  */
class RVConfig(val XLEN: Int, extensions: String, fakeExtensions: String) {
  // - riscv-spec-20191213
  // - We use the term XLEN to refer to the width of an integer register in
  //   bits.
  require(XLEN == 32 || XLEN == 64, "RiscvCore only support 32 or 64 bits now")

  // ISA Extensions
  val M: Boolean = extensions.indexOf("M") != -1
  val C: Boolean = extensions.indexOf("C") != -1
  // Priviledge Levels
  val S: Boolean = extensions.indexOf("S") != -1
  val U: Boolean = extensions.indexOf("U") != -1
  // CSRs Config

  // Misa
  val CSRMisaExtList = (fakeExtensions.toSeq ++
    Seq(
      Some('I'),
      if (M) Some('M') else None,
      if (C) Some('C') else None,
      if (S) Some('S') else None,
      if (U || S) Some('U') else None
    ).flatten).distinct
}

object RVConfig {

  /** Create a RVConfig
    * @param XLEN
    *   The width of an integer register in bits
    * @param extensions
    *   Supported extensions
    * @param fakeExtensions
    *   RiscvCore do not support this extensions, but will be appear in Misa
    */
  def apply(
      XLEN: Int,
      extensions: String = "",
      fakeExtensions: String = ""
  ): RVConfig =
    new RVConfig(XLEN, extensions, fakeExtensions)
}
