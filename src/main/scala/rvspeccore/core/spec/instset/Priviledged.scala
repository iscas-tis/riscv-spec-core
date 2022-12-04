package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import csr._

trait PriviledgedInsts {
  // - Priviledged Insts Volume II
  val SRET       = Inst("b000100000010_00000_000_00000_1110011")
  val MRET       = Inst("b001100000010_00000_000_00000_1110011")
  val WFI        = Inst("b0001000_00101_00000_000_00000_1110011") 
  val SFANCE_VMA = Inst("b0001001_?????_?????_000_00000_1110011")

  // The above are instructions for Nutshell

  val SINVAL_VMA = Inst("b0001011_?????_?????_000_00000_1110011")
  val SFANCE_W_INVAL  = Inst("b0001100_00000_00000_000_00000_1110011")
  val SFANCE_INVAL_IR = Inst("b0001100_00001_00000_000_00000_1110011")
  
  val HFANCE_VVMA     = Inst("b0010001_?????_?????_000_00000_1110011")
  val HFANCE_GVMA     = Inst("b0110001_?????_?????_000_00000_1110011")
  val HINVAL_VVMA     = Inst("b0010011_?????_?????_000_00000_1110011")
  val HINVAL_GVMA     = Inst("b0110011_?????_?????_000_00000_1110011")

  // TODO: For more insts
  // ......  
}
