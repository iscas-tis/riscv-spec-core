package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

trait CExtensionInsts {
  val rdNotZero    = (inst: UInt, xlen: Int) => { inst(11, 7) =/= 0.U }
  val nzimmNotZero = (inst: UInt, xlen: Int) => { Cat(inst(12), inst(6, 2)) =/= 0.U }
  val shamtCheck = (inst: UInt, xlen: Int) => {
    (xlen match {
      case 32 => inst(12) === 0.U
      case 64 => true.B
    }) && Cat(inst(12), inst(6, 2)) =/= 0.U
  }

  // scalafmt: { maxColumn = 200 }
  // 00
  val C_Illegal  = Inst("b????????????????_000_00000000_000_00")
  val C_ADDI4SPN = Inst("b????????????????_000_????????_???_00", (inst, xlen) => { inst(12, 5) =/= 0.U })

  val C_FLD = Inst("b????????????????_001_???_???_??_???_00")
  val C_LQ  = Inst("b????????????????_001_???_???_??_???_00")
  val C_LW  = Inst("b????????????????_010_???_???_??_???_00")
  val C_FLW = Inst("b????????????????_011_???_???_??_???_00")
  val C_LD  = Inst("b????????????????_011_???_???_??_???_00")
  // Reserved
  val C_FSD = Inst("b????????????????_101_???_???_??_???_00")
  val C_SQ  = Inst("b????????????????_101_???_???_??_???_00")
  val C_SW  = Inst("b????????????????_110_???_???_??_???_00")
  val C_FSW = Inst("b????????????????_111_???_???_??_???_00")
  val C_SD  = Inst("b????????????????_111_???_???_??_???_00")

  // 01
  val C_NOP      = Inst("b????????????????_000_?_00000_?????_01")
  val C_ADDI     = Inst("b????????????????_000_?_?????_?????_01", (inst, xlen) => { rdNotZero(inst, xlen) && nzimmNotZero(inst, xlen) })
  val C_JAL      = Inst("b????????????????_001_???????????_01", (inst, xlen) => (xlen == 32).B) // RV32 only
  val C_ADDIW    = Inst("b????????????????_001_?_?????_?????_01", rdNotZero)
  val C_LI       = Inst("b????????????????_010_?_?????_?????_01", rdNotZero)
  val C_ADDI16SP = Inst("b????????????????_011_?_00010_?????_01", nzimmNotZero)
  val C_LUI      = Inst("b????????????????_011_?_?????_?????_01", (inst, xlen) => { rdNotZero(inst, xlen) && inst(11, 7) =/= 2.U && nzimmNotZero(inst, xlen) })

  val C_SRLI   = Inst("b????????????????_100_?_00_???_?????_01", shamtCheck)
  val C_SRLI64 = Inst("b????????????????_100_0_00_???_00000_01")
  val C_SRAI   = Inst("b????????????????_100_?_01_???_?????_01", shamtCheck)
  val C_SAI64  = Inst("b????????????????_100_0_01_???_00000_01")
  val C_ANDI   = Inst("b????????????????_100_?_10_???_?????_01")

  val C_SUB  = Inst("b????????????????_100_0_11_???_00_???_01")
  val C_XOR  = Inst("b????????????????_100_0_11_???_01_???_01")
  val C_OR   = Inst("b????????????????_100_0_11_???_10_???_01")
  val C_AND  = Inst("b????????????????_100_0_11_???_11_???_01")
  val C_SUBW = Inst("b????????????????_100_1_11_???_00_???_01")
  val C_ADDW = Inst("b????????????????_100_1_11_???_01_???_01")
  // Reserved
  // Reserved
  val C_J    = Inst("b????????????????_101_???????????_01")
  val C_BEQZ = Inst("b????????????????_110_???_???_?????_01")
  val C_BNEZ = Inst("b????????????????_111_???_???_?????_01")

  // 10
  val C_SLLI   = Inst("b????????????????_000_?_?????_?????_10", (inst, xlen) => { rdNotZero(inst, xlen) && shamtCheck(inst, xlen) })
  val C_SLLI64 = Inst("b????????????????_000_0_?????_00000_10", rdNotZero)
  val C_FLDSP  = Inst("b????????????????_001_?_?????_?????_10")
  val C_LQSP   = Inst("b????????????????_001_?_?????_?????_10", rdNotZero)
  val C_LWSP   = Inst("b????????????????_010_?_?????_?????_10", rdNotZero)
  val C_FLWSP  = Inst("b????????????????_011_?_?????_?????_10")
  val C_LDSP   = Inst("b????????????????_011_?_?????_?????_10", rdNotZero)
  val C_JR     = Inst("b????????????????_100_0_?????_00000_10", rdNotZero)
  val C_MV     = Inst("b????????????????_100_0_?????_?????_10", (inst, xlen) => { rdNotZero(inst, xlen) && inst(6, 2) =/= 0.U })
  val C_EBREAK = Inst("b????????????????_100_1_00000_00000_10")
  val C_JALR   = Inst("b????????????????_100_1_?????_00000_10", rdNotZero)
  val C_ADD    = Inst("b????????????????_100_1_?????_?????_10", (inst, xlen) => { rdNotZero(inst, xlen) && inst(6, 2) =/= 0.U })

  val C_FSDSP = Inst("b????????????????_101_??????_?????_10")
  val C_SQSP  = Inst("b????????????????_101_??????_?????_10")
  val C_SWSP  = Inst("b????????????????_110_??????_?????_10")
  val C_FSWSP = Inst("b????????????????_111_??????_?????_10")
  val C_SDSP  = Inst("b????????????????_111_??????_?????_10")
  // scalafmt: { maxColumn = 120 } (back to defaults)
}

/** Decode part
  *
  *   - riscv-spec-20191213
  *   - Chapter 16: “C” Standard Extension for Compressed Instructions, Version
  *     2.0
  *   - 16.2 Compressed Instruction Formats
  */
trait CDecode extends BaseCore with CommonDecode {
  val funct2 = WireInit(0.U(2.W))
  val funct4 = WireInit(0.U(4.W))
  val funct6 = WireInit(0.U(6.W))
  val op     = WireInit(0.U(2.W))
  val rdP    = WireInit(0.U(3.W))
  val rs1P   = WireInit(0.U(3.W))
  val rs2P   = WireInit(0.U(3.W))

  // ph for placeholder, should not be used after decode
  val ph1  = WireInit(0.U(1.W)); val ph5 = WireInit(0.U(5.W))
  val ph6  = WireInit(0.U(6.W))
  val ph8  = WireInit(0.U(8.W))
  val ph3  = WireInit(0.U(3.W)); val ph2 = WireInit(0.U(2.W))
  val ph11 = WireInit(0.U(11.W))

  // Table 16.1: Compressed 16-bit RVC instruction formats.
  // format: off
  //                                       / 15  13 | 12  | 11   7 | 6           2 | 1 0 \
  def decodeCR  = { decodeInit; unpack(List(    funct4    ,  rs1   ,      rs2      , op  ), inst(15, 0)); rd := rs1     }
  def decodeCI  = { decodeInit; unpack(List( funct3 , ph1 ,  rs1   ,      ph5      , op  ), inst(15, 0)); rd := rs1     }
  def decodeCSS = { decodeInit; unpack(List( funct3 ,     ph6      ,      rs2      , op  ), inst(15, 0))                }
  def decodeCIW = { decodeInit; unpack(List( funct3 ,         ph8           , rdP  , op  ), inst(15, 0))                }
  def decodeCL  = { decodeInit; unpack(List( funct3 ,  ph3  , rs1P ,  ph2   , rdP  , op  ), inst(15, 0))                }
  def decodeCS  = { decodeInit; unpack(List( funct3 ,  ph3  , rs1P ,  ph2   , rs2P , op  ), inst(15, 0))                }
  def decodeCA  = { decodeInit; unpack(List(     funct6     , rs1P , funct2 , rs2P , op  ), inst(15, 0)); rdP := rs1P }
  def decodeCB  = { decodeInit; unpack(List( funct3 ,  ph3  , rs1P ,      ph5      , op  ), inst(15, 0)); rdP := rs1P } // rdP := rs1P described in C.SRLI
  def decodeCJ  = { decodeInit; unpack(List( funct3 ,             ph11             , op  ), inst(15, 0))                }
  //                                       \ 15  13 | 12 10 | 9  7 | 6    5 | 4  2 | 1 0 /
  // format: on
}

/** “C” Standard Extension for Compressed Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 16: “C” Standard Extension for Compressed Instructions, Version
  *     2.0
  */
trait CExtension extends BaseCore with CDecode with CExtensionInsts { this: IBase =>

  def cat01(x: UInt): UInt = Cat("b01".U(2.W), x)

  def doRV32C: Unit = {
    // - 16.3 Load and Store Instructions
    // - Stack-Pointer-Based Loads and Stores
    when(C_LWSP(inst)) {
      decodeCI
      imm          := zeroExt(reorder(5, (4, 2), (7, 6))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := signExt(memRead(now.reg(2.U) + imm, 32.U)(31, 0), XLEN)
    }
    when(C_SWSP(inst)) {
      decodeCSS
      imm := zeroExt(reorder((5, 2), (7, 6))(inst, (12, 7)), XLEN)
      memWrite(now.reg(2.U) + imm, 32.U, now.reg(rs2)(31, 0))
    }
    // - Register-Based Loads and Stores
    when(C_LW(inst)) {
      decodeCL
      imm                  := zeroExt(reorder((5, 3), 2, (6, 6))(inst, (12, 10), (6, 5)), XLEN)
      next.reg(cat01(rdP)) := signExt(memRead(now.reg(cat01(rs1P)) + imm, 32.U)(31, 0), XLEN)
    }
    when(C_SW(inst)) {
      decodeCS
      imm := zeroExt(reorder((5, 3), 2, 6)(inst, (12, 10), (6, 5)), XLEN)
      memWrite(now.reg(cat01(rs1P)) + imm, 32.U, now.reg(cat01(rs2P)))
    }
    // - 16.4 Control Transfer Instructions
    when(C_J(inst)) {
      decodeCJ
      imm               := signExt(reorder(11, 4, (9, 8), 10, 6, 7, (3, 1), 5)(inst, (12, 2)), XLEN)
      global_data.setpc := true.B
      next.pc           := now.pc + imm
    }
    when(C_JAL(inst)) {
      decodeCJ
      imm               := signExt(reorder(11, 4, (9, 8), 10, 6, 7, (3, 1), 5)(inst, (12, 2)), XLEN)
      global_data.setpc := true.B
      next.pc           := now.pc + imm
      next.reg(1.U)     := now.pc + 2.U
    }
    when(C_JR(inst)) {
      decodeCR
      global_data.setpc := true.B
      // setting the least-significant to zero according to JALR in RVI
      next.pc := Cat(now.reg(rs1)(XLEN - 1, 1), 0.U(1.W))
    }
    when(C_JALR(inst)) {
      decodeCR
      global_data.setpc := true.B
      // setting the least-significant to zero according to JALR in RVI
      next.pc       := Cat(now.reg(rs1)(XLEN - 1, 1), 0.U(1.W))
      next.reg(1.U) := now.pc + 2.U
    }
    when(C_BEQZ(inst)) {
      decodeCB
      imm := signExt(reorder(8, (4, 3), (7, 6), (2, 1), 5)(inst, (12, 10), (6, 2)), XLEN)
      when(now.reg(cat01(rs1P)) === 0.U) {
        global_data.setpc := true.B
        next.pc           := now.pc + imm
      }
    }
    when(C_BNEZ(inst)) {
      decodeCB
      imm := signExt(reorder(8, (4, 3), (7, 6), (2, 1), 5)(inst, (12, 10), (6, 2)), XLEN)
      when(now.reg(cat01(rs1P)) =/= 0.U) {
        global_data.setpc := true.B
        next.pc           := now.pc + imm
      }
    }
    // - 16.5 Integer Computational Instructions
    // - Integer Constant-Generation Instructions
    when(C_LI(inst)) {
      decodeCI
      imm          := signExt(reorder(5, (4, 0))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := imm
    }
    when(C_LUI(inst)) {
      decodeCI
      val nzimm_C_LUI = signExt(reorder(17, (16, 12))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := nzimm_C_LUI
    }
    // - Integer Register-Immediate Operations
    when(C_ADDI(inst)) {
      decodeCI
      val nzimm_C_ADDI = signExt(reorder(5, (4, 0))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := now.reg(rd) + nzimm_C_ADDI
    }
    when(C_ADDI16SP(inst)) {
      decodeCI
      val nzimm_C_ADDI16SP = signExt(reorder(9, 4, 6, (8, 7), 5)(inst, 12, (6, 2)), XLEN)
      next.reg(2.U) := now.reg(2.U) + nzimm_C_ADDI16SP
    }
    when(C_ADDI4SPN(inst)) {
      decodeCIW
      val nzimm_C_ADDI4SPN = zeroExt(reorder((5, 4), (9, 6), 2, 3)(inst, (12, 5)), XLEN)
      next.reg(cat01(rdP)) := now.reg(2.U) + nzimm_C_ADDI4SPN
    }
    when(C_SLLI(inst)) {
      decodeCI
      imm          := reorder(5, (4, 0))(inst, 12, (6, 2))
      next.reg(rd) := now.reg(rd) << imm(5, 0)
    }
    when(C_SRLI(inst)) {
      decodeCB
      imm                  := reorder(5, (4, 0))(inst, 12, (6, 2))
      next.reg(cat01(rdP)) := now.reg(cat01(rdP)) >> imm(5, 0)
    }
    when(C_SRAI(inst)) {
      decodeCB
      imm                  := reorder(5, (4, 0))(inst, 12, (6, 2))
      next.reg(cat01(rdP)) := (now.reg(cat01(rdP)).asSInt >> imm(5, 0)).asUInt
    }
    when(C_ANDI(inst)) {
      decodeCB
      imm                  := signExt(reorder(5, (4, 0))(inst, 12, (6, 2)), XLEN)
      next.reg(cat01(rdP)) := now.reg(cat01(rdP)) & imm
    }
    // - Integer Register-Register Operations
    when(C_MV(inst))  { decodeCR; next.reg(rd) := now.reg(0.U) + now.reg(rs2) }
    when(C_ADD(inst)) { decodeCR; next.reg(rd) := now.reg(rd) + now.reg(rs2) }
    when(C_AND(inst)) { decodeCA; next.reg(cat01(rdP)) := now.reg(cat01(rdP)) & now.reg(cat01(rs2P)) }
    when(C_OR(inst))  { decodeCA; next.reg(cat01(rdP)) := now.reg(cat01(rdP)) | now.reg(cat01(rs2P)) }
    when(C_XOR(inst)) { decodeCA; next.reg(cat01(rdP)) := now.reg(cat01(rdP)) ^ now.reg(cat01(rs2P)) }
    when(C_SUB(inst)) { decodeCA; next.reg(cat01(rdP)) := now.reg(cat01(rdP)) - now.reg(cat01(rs2P)) }
    // - Defined Illegal Instruction
    // - NOP Instruction
    when(C_NOP(inst)) { decodeCI /* then do nothing */ }
    // - Breakpoint Instruction
  }
  def doRV64C: Unit = {
    doRV32C

    // - 16.3 Load and Store Instructions
    // - Stack-Pointer-Based Loads and Stores
    when(C_LDSP(inst)) {
      decodeCI
      imm          := zeroExt(reorder(5, (4, 3), (8, 6))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := signExt(memRead(now.reg(2.U) + imm, 64.U)(63, 0), XLEN)
    }
    when(C_SDSP(inst)) {
      decodeCSS
      imm := zeroExt(reorder((5, 3), (8, 6))(inst, (12, 7)), XLEN)
      memWrite(now.reg(2.U) + imm, 64.U, now.reg(rs2)(63, 0))
    }
    // - Register-Based Loads and Stores
    when(C_LD(inst)) {
      decodeCL
      imm                  := zeroExt(reorder((5, 3), (7, 6))(inst, (12, 10), (6, 5)), XLEN)
      next.reg(cat01(rdP)) := signExt(memRead(now.reg(cat01(rs1P)) + imm, 64.U)(63, 0), XLEN)
    }
    when(C_SD(inst)) {
      decodeCS
      imm := zeroExt(reorder((5, 3), (7, 6))(inst, (12, 10), (6, 5)), XLEN)
      memWrite(now.reg(cat01(rs1P)) + imm, 64.U, now.reg(cat01(rs2P)))
    }
    // - 16.5 Integer Computational Instructions
    // - Integer Register-Immediate Operations
    when(C_ADDIW(inst)) {
      decodeCI
      imm          := signExt(reorder(5, (4, 0))(inst, 12, (6, 2)), XLEN)
      next.reg(rd) := signExt((now.reg(rd) + imm)(31, 0), XLEN)
    }
    // C_SLLI C_SRLI C_SRAI defined in RV32C
    // - Integer Register-Register Operations
    when(C_ADDW(inst)) {
      decodeCA
      next.reg(cat01(rdP)) := signExt(now.reg(cat01(rdP))(31, 0) + now.reg(cat01(rs2P))(31, 0), XLEN)
    }
    when(C_SUBW(inst)) {
      decodeCA
      next.reg(cat01(rdP)) := signExt(now.reg(cat01(rdP))(31, 0) - now.reg(cat01(rs2P))(31, 0), XLEN)
    }
  }

  def doRVC: Unit = {
    config.XLEN match {
      case 32 => doRV32C
      case 64 => doRV64C
    }
  }
}
