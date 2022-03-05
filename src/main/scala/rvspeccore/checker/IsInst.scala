package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core.spec._

object IsInst {
  def apply(instName: String, inst: UInt): Bool = {
    require(inst.getWidth == 32)

    if (InstOpcodeMap.contains(instName)) {
      if (Funct3Map.contains(instName)) {
        if (Funct7Map.contains(instName)) {
          (inst(31, 25) === Funct7Map(instName) && inst(14, 12) === Funct3Map(instName) &&
          inst(6, 0) === InstOpcodeMap(instName))
        } else {
          (inst(14, 12) === Funct3Map(instName) &&
          inst(6, 0) === InstOpcodeMap(instName))
        }
      } else {
        inst(6, 0) === InstOpcodeMap(instName)
      }
    } else {
      false.B
    }
  }
}
