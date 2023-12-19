package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.RVConfig
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import csr._

trait PrivilegedInsts {
  // - Privileged Insts Volume II
  val SRET       = Inst("b000100000010_00000_000_00000_1110011")
  val MRET       = Inst("b001100000010_00000_000_00000_1110011")
  val WFI        = Inst("b0001000_00101_00000_000_00000_1110011")
  val SFANCE_VMA = Inst("b0001001_?????_?????_000_00000_1110011")
  // FIXME: need to remove
  // val TEST_ILLEGAL=Inst("b0000000_00000_00000_000_00000_1111011")
  val TEST_TLBLW = Inst("b0000000_00000_00011_010_111010_000011")

  // The above are instructions for Nutshell

  val SINVAL_VMA      = Inst("b0001011_?????_?????_000_00000_1110011")
  val SFANCE_W_INVAL  = Inst("b0001100_00000_00000_000_00000_1110011")
  val SFANCE_INVAL_IR = Inst("b0001100_00001_00000_000_00000_1110011")

  val HFANCE_VVMA = Inst("b0010001_?????_?????_000_00000_1110011")
  val HFANCE_GVMA = Inst("b0110001_?????_?????_000_00000_1110011")
  val HINVAL_VVMA = Inst("b0010011_?????_?????_000_00000_1110011")
  val HINVAL_GVMA = Inst("b0110011_?????_?????_000_00000_1110011")
  val NOP         = Inst("b0000000_00000_00000_000_00000_0000000")
  // TODO: For more insts
  // ......
}

/** “Privileged” Instruction-Fetch Fence Volume II Insts
  */
trait PrivilegedExtension
    extends BaseCore
    with CommonDecode
    with PrivilegedInsts
    with CSRSupport
    with ExceptionSupport {
  def doRVPrivileged()(implicit config: RVConfig): Unit = {
    // FIXME: need to decode more insts & clearify there actions(not do nothing....)
    when(SRET(inst)) {
      // printf("Is SRET:%x\n",inst)
      decodeI
      Sret()
      /* then do nothing for now */
    }
    when(MRET(inst)) {
      // printf("Is MRET:%x\n",inst)
      decodeI
      Mret()
      /* then do nothing for now */
    }
    when(WFI(inst)) { decodeI /* then do nothing for now */ }
    when(NOP(inst)) {
      // printf("Is NOP:%x\n",inst)
      tryRaiseException()
      /* then do nothing for now */
    }
    when(SFANCE_VMA(inst)) { decodeI /* then do nothing for now */ }
  }
}
