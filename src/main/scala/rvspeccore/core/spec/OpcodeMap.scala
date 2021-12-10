package rvspeccore.core.spec

import chisel3._
import chisel3.util._

object OpcodeMap {

  /** RISC-V base opcode map
    *
    *   - riscv-spec-20191213
    *   - Chapter 24: RV32/64G Instruction Set Listings
    *   - Table 24.1: RISC-V base opcode map
    */
  private val rawTable = List(
    // format: off
    // inst[1:0] = 11, inst[4:2] / inst[6:5]
    //  |    000 |       001 |       010 |       011 |     100 |       101 |        110 |  111 |
    List("LOAD"  , "LOAD-FP" , "custom-0", "MISC-MEM", "OP-IMM", "AUIPC"   , "OP-IMM-32", "48b"), // 00
    List("STORE" , "STORE-FP", "custom-1", "AMO"     , "OP"    , "LUI"     , "OP-32"    , "64b"), // 01
    List("MADD"  , "MSUB"    , "NMSUB"   , "NMADD"   , "OP-FP" , "reserved", "custom-2" , "48b"), // 10
    List("BRANCH", "JALR"    , "reserved", "JAL"     , "SYSTEM", "reserved", "custom-3" , "80b")  // 11
    // format: on
  )

  private def getOpcode(i: Int, j: Int): UInt = {
    require(0 <= i && i < (1 << 2))
    require(0 <= j && j < (1 << 3))
    // inst[6:5] | inst[4:2] | inst[1:0]
    ((i * 8 + j) * 4 + 3).U(7.W)
  }

  val opcodeMap: Map[String, UInt] = {
    for (i <- 0 until rawTable.size; j <- 0 until rawTable(i).size)
      yield (rawTable(i)(j) -> getOpcode(i, j))
  }.toMap

  def apply(opcodeName: String): UInt = {
    opcodeMap(opcodeName)
  }
}
