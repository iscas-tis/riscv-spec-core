package rvspeccore.core

import chisel3._
import chisel3.util._

sealed abstract class RVConfig {

  /**   - riscv-spec-20191213
    *   - We use the term XLEN to refer to the width of an integer register in
    *     bits.
    */
  val XLEN: Int
}

case class RV32Config() extends RVConfig {
  val XLEN = 32
}
case class RV64Config() extends RVConfig {
  val XLEN = 64
}
