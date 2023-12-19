package rvspeccore.core

import chisel3._
import chisel3.util._

sealed abstract class RVConfig(extensions: String) {

  /**   - riscv-spec-20191213
    *   - We use the term XLEN to refer to the width of an integer register in
    *     bits.
    */
  val XLEN: Int

  // ISA Extensions
  val M: Boolean = extensions.indexOf("M") != -1
  val C: Boolean = extensions.indexOf("C") != -1
  // Priviledge Levels
  val S: Boolean = extensions.indexOf("S") != -1
  val U: Boolean = extensions.indexOf("U") != -1
  // CSRs Config

  // Misa
  var CSRMisaExtList = Seq(
    Some('I'),
    if (M) Some('M') else None,
    if (C) Some('C') else None,
    if (S) Some('S') else None,
    if (U || S) Some('U') else None
  ).flatten
}

case class RV32Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 32
}
case class RV64Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 64
}
