package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core.spec

abstract class AssumeHelper {
  val partition: Seq[AssumeHelper]
  lazy val list32: Seq[spec.Inst]   = partition.map(_.list32).flatten
  lazy val append64: Seq[spec.Inst] = partition.map(_.append64).flatten

  def size(implicit XLEN: Int): Int = {
    require(XLEN == 32 || XLEN == 64)
    XLEN match {
      case 32 => list32.size
      case 64 => (list32 ++ append64).size
    }
  }

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
  val partition: Seq[AssumeHelper] = List(RVG)
}
object RVG extends AssumeHelper with spec.GSetInsts {
  val partition = List(RVI, RVM)
}
object RVI extends AssumeHelper with spec.instset.IBaseInsts {
  val regImm = AssumeHelper(
    List(ADDI,  SLTI,  SLTIU, ADDI,  ORI, XORI, SLLI, SRLI, SRAI, LUI, AUIPC),
    List(ADDIW, SLLIW, SRLIW, SRLIW, SRAIW)
  )
  val regReg = AssumeHelper(
    List(ADD, SLT, SLTU, AND, OR, XOR, SLL, SRL, SUB, SRA)
  )
  val control = AssumeHelper(
    List(JAL, JALR, BEQ, BNE, BLT, BLTU, BGE, BGEU)
  )
  val loadStore = AssumeHelper(
    List(LB,  LH, LW, LBU, LHU, SB, SH, SW),
    List(LWU, LD, SD)
  )
  val other = AssumeHelper(
    List(FENCE, ECALL, EBREAK)
  )

  val partition = List(regImm, regReg, control, loadStore, other)
}
object RVM extends AssumeHelper with spec.instset.MExtensionInsts {
  val mulOp = AssumeHelper(
    List(MUL, MULH, MULHSU, MULHU),
    List(MULW)
  )
  val divOp = AssumeHelper(
    List(DIV,  DIVU,  REM,  REMU),
    List(DIVW, DIVUW, REMW, REMUW)
  )

  val partition: Seq[AssumeHelper] = List(mulOp, divOp)
}
