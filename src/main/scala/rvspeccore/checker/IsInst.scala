package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core.spec._

object IsInst {
  def apply(instName: String, inst: UInt)(implicit XLEN: Int): Bool = {
    require(inst.getWidth == 32)
    require(XLEN == 32 || XLEN == 64)

    val sp7bitMap: Map[String, UInt] = Map(
      "SLLI"  -> "b000_0000".U(7.W), // RV32I
      "SRLI"  -> "b000_0000".U(7.W),
      "SRAI"  -> "b010_0000".U(7.W),
      "SLLIW" -> "b000_0000".U(7.W), // RV64I
      "SRLIW" -> "b000_0000".U(7.W),
      "SRAIW" -> "b010_0000".U(7.W)
    )
    val sp6bitMap: Map[String, UInt] = Map(
      "SLLI" -> "b000_000".U(6.W),
      "SRLI" -> "b000_000".U(6.W),
      "SRAI" -> "b010_000".U(6.W)
    )

    if (sp7bitMap.contains(instName)) {
      if (sp6bitMap.contains(instName)) {
        XLEN match {
          case 32 =>
            inst(31, 25) === sp7bitMap(instName) && inst(14, 12) === Funct3Map(instName) &&
            inst(6, 0) === InstOpcodeMap(instName)
          case 64 =>
            inst(31, 26) === sp6bitMap(instName) && inst(14, 12) === Funct3Map(instName) &&
            inst(6, 0) === InstOpcodeMap(instName)
        }
      } else {
        inst(31, 25) === sp7bitMap(instName) && inst(14, 12) === Funct3Map(instName) &&
        inst(6, 0) === InstOpcodeMap(instName)
      }
    } else if (InstOpcodeMap.contains(instName)) {
      (Funct7Map.contains(instName), Funct3Map.contains(instName)) match {
        case (true, true) => {
          inst(31, 25) === Funct7Map(instName) && inst(14, 12) === Funct3Map(instName) &&
          inst(6, 0) === InstOpcodeMap(instName)
        }
        case (false, true) =>
          inst(14, 12) === Funct3Map(instName) &&
          inst(6, 0) === InstOpcodeMap(instName)
        case (false, false) =>
          inst(6, 0) === InstOpcodeMap(instName)
        case _ => throw new Exception("inst not found: " + instName)
      }
    } else {
      throw new Exception("inst not found: " + instName)
    }
  }
}
