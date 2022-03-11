package rvspeccore.core

import chisel3._
import chisel3.util._

sealed abstract class RVConfig(extensions: String) {

  /**   - riscv-spec-20191213
    *   - We use the term XLEN to refer to the width of an integer register in
    *     bits.
    */
  val XLEN: Int
  val M: Boolean = extensions.indexOf("M") != -1
  val C: Boolean = extensions.indexOf("C") != -1
}

case class RV32Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 32
}
case class RV64Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 64
}
