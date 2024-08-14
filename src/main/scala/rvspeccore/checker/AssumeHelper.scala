package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core.spec

abstract class AssumeHelper {
  val partition: Seq[AssumeHelper]

  // these lists will be calculated automatically if not overrided
  lazy val list32: Seq[spec.Inst]   = partition.map(_.list32).flatten
  lazy val append64: Seq[spec.Inst] = partition.map(_.append64).flatten

  /** Instruction number in this set
    */
  def size(implicit XLEN: Int): Int = {
    require(XLEN == 32 || XLEN == 64)
    XLEN match {
      case 32 => list32.size
      case 64 => (list32 ++ append64).size
    }
  }

  /** Check if `inst` is a legal instruction in this set
    */
  def apply(inst: UInt)(implicit XLEN: Int): Bool = {
    require(XLEN == 32 || XLEN == 64)
    XLEN match {
      case 32 => list32.map(_(inst)(XLEN)).reduce(_ || _)
      case 64 => (list32 ++ append64).map(_(inst)(XLEN)).reduce(_ || _)
    }
  }
}
object AssumeHelper {
  def apply(list32A: Seq[spec.Inst], append64A: Seq[spec.Inst]): AssumeHelper = {
    new AssumeHelper {
      val partition              = List()
      override lazy val list32   = list32A
      override lazy val append64 = append64A
    }
  }
  def apply(list32A: Seq[spec.Inst]): AssumeHelper = {
    new AssumeHelper {
      val partition              = List()
      override lazy val list32   = list32A
      override lazy val append64 = List()
    }
  }
}

object RV extends AssumeHelper with spec.RVInsts {
  val partition: Seq[AssumeHelper] = List(RVG, RVC)
}
object RVG extends AssumeHelper with spec.GSetInsts {
  // : we use the abbreviation G for the IMAFDZicsr_Zifencei combination of
  // : instruction-set extensions
  val partition = List(RVI, RVM)
}
object RVI extends AssumeHelper with spec.instset.IBaseInsts {
  // classification by
  // - riscv-spec-20191213
  // - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
  // - Chapter 5: RV64I Base Integer Instruction Set, Version 2.1
  // or refer to IBase.scala
  val regImm = AssumeHelper(
    List(ADDI, SLTI, SLTIU, ANDI, ORI, XORI, SLLI, SRLI, SRAI, LUI, AUIPC),
    List(ADDIW, SLLIW, SRLIW, SRLIW, SRAIW)
  )
  val regReg = AssumeHelper(
    List(ADD, SLT, SLTU, AND, OR, XOR, SLL, SRL, SUB, SRA)
  )
  val control = AssumeHelper(
    List(JAL, JALR, BEQ, BNE, BLT, BLTU, BGE, BGEU)
  )
  val loadStore = AssumeHelper(
    List(LB, LH, LW, LBU, LHU, SB, SH, SW),
    List(LWU, LD, SD)
  )
  val other = AssumeHelper(
    // List(FENCE, ECALL, EBREAK)
    List(ECALL, EBREAK) // FIXME: FENCE will occurs exception when verify
  )

  val partition = List(regImm, regReg, control, loadStore, other)
}
object RVM extends AssumeHelper with spec.instset.MExtensionInsts {
  val mulOp = AssumeHelper(
    List(MUL, MULH, MULHSU, MULHU),
    List(MULW)
  )
  val divOp = AssumeHelper(
    List(DIV, DIVU, REM, REMU),
    List(DIVW, DIVUW, REMW, REMUW)
  )

  val partition: Seq[AssumeHelper] = List(mulOp, divOp)
}
object RVC extends AssumeHelper with spec.instset.CExtensionInsts {
  // RV128C, RVFC not included

  val loadStore = AssumeHelper(
    List(C_LWSP, C_SWSP, C_LW, C_SW),
    List(C_LDSP, C_SDSP, C_LD, C_SD)
  )
  val control = AssumeHelper(
    List(C_J, C_JAL, C_JR, C_JALR, C_BEQZ, C_BNEZ)
  )
  val regImm = AssumeHelper(
    List(C_LI, C_LUI, C_ADDI, C_ADDI16SP, C_ADDI4SPN, C_SLLI, C_SRLI, C_SRAI, C_ANDI),
    List(C_ADDIW)
  )
  val regReg = AssumeHelper(
    List(C_MV, C_ADD, C_AND, C_OR, C_XOR, C_SUB),
    List(C_ADDW, C_SUBW)
  )

  val partition: Seq[AssumeHelper] = List(loadStore, control, regImm, regReg)
}
object RVZicsr extends AssumeHelper with spec.instset.ZicsrExtensionInsts {
  val reg = AssumeHelper(
    List(CSRRW, CSRRS, CSRRC)
  )
  val imm = AssumeHelper(
    List(CSRRWI, CSRRSI, CSRRCI)
  )

  val partition: Seq[AssumeHelper] = List(reg, imm)
}
object RVZifencei extends AssumeHelper with spec.instset.ZifenceiExtensionInsts {
  val fence_i = AssumeHelper(
    List(FENCE_I)
  )

  val partition: Seq[AssumeHelper] = List(fence_i)
}
object RVPrivileged extends AssumeHelper with spec.instset.PrivilegedInsts {
  val trap_return = AssumeHelper(
    List(SRET, MRET)
  )
  // val illegal = AssumeHelper(
  //   List(TEST_ILLEGAL)
  // )
  val partition: Seq[AssumeHelper] = List(trap_return)
}
