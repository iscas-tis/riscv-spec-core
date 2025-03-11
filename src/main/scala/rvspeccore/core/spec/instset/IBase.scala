package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.tool.LoadStore
import rvspeccore.core.spec.instset.csr._

/** Base Integer Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 24: RV32/64G Instruction Set Listings
  *     - Table 24.2: Instruction listing for RISC-V
  */
trait IBaseInsts {
  // - RV32I Base Instruction Set
  val LUI   = Inst("b????????????????????_?????_0110111")
  val AUIPC = Inst("b????????????????????_?????_0010111")
  val JAL   = Inst("b????????????????????_?????_1101111")
  val JALR  = Inst("b????????????_?????_000_?????_1100111")

  val BEQ  = Inst("b???????_?????_?????_000_?????_1100011")
  val BNE  = Inst("b???????_?????_?????_001_?????_1100011")
  val BLT  = Inst("b???????_?????_?????_100_?????_1100011")
  val BGE  = Inst("b???????_?????_?????_101_?????_1100011")
  val BLTU = Inst("b???????_?????_?????_110_?????_1100011")
  val BGEU = Inst("b???????_?????_?????_111_?????_1100011")

  val LB  = Inst("b????????????_?????_000_?????_0000011")
  val LH  = Inst("b????????????_?????_001_?????_0000011")
  val LW  = Inst("b????????????_?????_010_?????_0000011")
  val LBU = Inst("b????????????_?????_100_?????_0000011")
  val LHU = Inst("b????????????_?????_101_?????_0000011")
  val SB  = Inst("b???????_?????_?????_000_?????_0100011")
  val SH  = Inst("b???????_?????_?????_001_?????_0100011")
  val SW  = Inst("b???????_?????_?????_010_?????_0100011")

  val ADDI  = Inst("b????????????_?????_000_?????_0010011")
  val SLTI  = Inst("b????????????_?????_010_?????_0010011")
  val SLTIU = Inst("b????????????_?????_011_?????_0010011")
  val XORI  = Inst("b????????????_?????_100_?????_0010011")
  val ORI   = Inst("b????????????_?????_110_?????_0010011")
  val ANDI  = Inst("b????????????_?????_111_?????_0010011")

  val SLLI = Inst(
    32 -> "b0000000_?????_?????_001_?????_0010011",
    64 -> "b000000_??????_?????_001_?????_0010011"
  )
  val SRLI = Inst(
    32 -> "b0000000_?????_?????_101_?????_0010011",
    64 -> "b000000_??????_?????_101_?????_0010011"
  )
  val SRAI = Inst(
    32 -> "b0100000_?????_?????_101_?????_0010011",
    64 -> "b010000_??????_?????_101_?????_0010011"
  )

  val ADD  = Inst("b0000000_?????_?????_000_?????_0110011")
  val SUB  = Inst("b0100000_?????_?????_000_?????_0110011")
  val SLL  = Inst("b0000000_?????_?????_001_?????_0110011")
  val SLT  = Inst("b0000000_?????_?????_010_?????_0110011")
  val SLTU = Inst("b0000000_?????_?????_011_?????_0110011")
  val XOR  = Inst("b0000000_?????_?????_100_?????_0110011")
  val SRL  = Inst("b0000000_?????_?????_101_?????_0110011")
  val SRA  = Inst("b0100000_?????_?????_101_?????_0110011")
  val OR   = Inst("b0000000_?????_?????_110_?????_0110011")
  val AND  = Inst("b0000000_?????_?????_111_?????_0110011")

  val FENCE = Inst("b????????????_?????_000_?????_0001111")

  val ECALL  = Inst("b000000000000_00000_000_00000_1110011")
  val EBREAK = Inst("b000000000001_00000_000_00000_1110011")

  // - RV64I Base Instruction Set (in addition to RV32I)
  val LWU = Inst("b???????_?????_?????_110_?????_0000011")
  val LD  = Inst("b???????_?????_?????_011_?????_0000011")
  val SD  = Inst("b???????_?????_?????_011_?????_0100011")

  // SLLI, SRLI, SRAI defined earlier

  val ADDIW = Inst("b????????????_?????_000_?????_0011011")
  val SLLIW = Inst("b0000000_?????_?????_001_?????_0011011")
  val SRLIW = Inst("b0000000_?????_?????_101_?????_0011011")
  val SRAIW = Inst("b0100000_?????_?????_101_?????_0011011")

  val ADDW = Inst("b0000000_?????_?????_000_?????_0111011")
  val SUBW = Inst("b0100000_?????_?????_000_?????_0111011")
  val SLLW = Inst("b0000000_?????_?????_001_?????_0111011")
  val SRLW = Inst("b0000000_?????_?????_101_?????_0111011")
  val SRAW = Inst("b0100000_?????_?????_101_?????_0111011")
}

// scalafmt: { maxColumn = 200 }
object SizeOp {
  def b = "b00".U
  def h = "b01".U
  def w = "b10".U
  def d = "b11".U
}
trait IBase extends BaseCore with CommonDecode with IBaseInsts with ExceptionSupport with LoadStore {
  // val setPc = WireInit(false.B)

  def alignedException(method: String, size: UInt, addr: UInt): Unit = {
    when(!addrAligned(size, addr)) {
      method match {
        case "Store" => {
          raiseException(MExceptionCode.storeOrAMOAddressMisaligned)
        }
        case "Load" => {
          raiseException(MExceptionCode.loadAddressMisaligned)
        }
        case "Instr" => {
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }

  }
  def addrAligned(size: UInt, addr: UInt): Bool = {
    MuxLookup(size, false.B)(
      Seq(
        "b00".U -> true.B,               // b
        "b01".U -> (addr(0) === 0.U),    // h
        "b10".U -> (addr(1, 0) === 0.U), // w
        "b11".U -> (addr(2, 0) === 0.U)  // d
      )
    )
  }

  def getfetchSize(): UInt = {
    MuxLookup(now.csr.misa(CSR.getMisaExtInt('C')), SizeOp.w)(
      Seq(
        "b0".U -> SizeOp.w,
        "b1".U -> SizeOp.h
      )
    )
  }

  /** RV32I Base Integer Instruction Set
    *
    *   - riscv-spec-20191213
    *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
    */
  def doRV32I: Unit = {
    // - 2.4 Integer Computational Instructions
    // - Integer Register-Immediate Instructions
    // ADDI/SLTI[U]
    when(ADDI(inst))  { decodeI; next.reg(rd) := now.reg(rs1) + imm }
    when(SLTI(inst))  { decodeI; next.reg(rd) := Mux(now.reg(rs1).asSInt < imm.asSInt, 1.U, 0.U) }
    when(SLTIU(inst)) { decodeI; next.reg(rd) := Mux(now.reg(rs1) < imm, 1.U, 0.U) }
    // ANDI/ORI/XORI
    when(ANDI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) & imm }
    when(ORI(inst))  { decodeI; next.reg(rd) := now.reg(rs1) | imm }
    when(XORI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) ^ imm }
    // SLLI/SRLI/SRAI
    when(SLLI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) << imm(4, 0) }
    when(SRLI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) >> imm(4, 0) }
    when(SRAI(inst)) { decodeI; next.reg(rd) := (now.reg(rs1).asSInt >> imm(4, 0)).asUInt }
    // LUI
    when(LUI(inst)) { decodeU; next.reg(rd) := imm }
    // AUIPC
    when(AUIPC(inst)) { decodeU; next.reg(rd) := now.pc + imm }
    // - Integer Register-Register Operations
    // ADD/SLT/SLTU
    when(ADD(inst))  { decodeR; next.reg(rd) := now.reg(rs1) + now.reg(rs2) }
    when(SLT(inst))  { decodeR; next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, 1.U, 0.U) }
    when(SLTU(inst)) { decodeR; next.reg(rd) := Mux(now.reg(rs1) < now.reg(rs2), 1.U, 0.U) }
    // AND/OR/XOR
    when(AND(inst)) { decodeR; next.reg(rd) := now.reg(rs1) & now.reg(rs2) }
    when(OR(inst))  { decodeR; next.reg(rd) := now.reg(rs1) | now.reg(rs2) }
    when(XOR(inst)) { decodeR; next.reg(rd) := now.reg(rs1) ^ now.reg(rs2) }
    // SLL/SRL
    when(SLL(inst)) { decodeR; next.reg(rd) := now.reg(rs1) << now.reg(rs2)(4, 0) }
    when(SRL(inst)) { decodeR; next.reg(rd) := now.reg(rs1) >> now.reg(rs2)(4, 0) }
    // SUB/SRA
    when(SUB(inst)) { decodeR; next.reg(rd) := now.reg(rs1) - now.reg(rs2) }
    when(SRA(inst)) { decodeR; next.reg(rd) := (now.reg(rs1).asSInt >> now.reg(rs2)(4, 0)).asUInt }
    // - NOP Instruction
    // NOP is encoded as ADDI x0, x0, 0.

    // - 2.5 Control Transfer Instructions
    // - Unconditional Jumps
    // JAL
    when(JAL(inst)) {
      decodeJ;
      when(addrAligned(getfetchSize(), now.pc + imm)) {
        global_data.setpc := true.B;
        next.pc           := now.pc + imm;
        next.reg(rd)      := now.pc + 4.U;
      }.otherwise {
        next.csr.mtval := now.pc + imm;
        raiseException(MExceptionCode.instructionAddressMisaligned)
      }
    }
    // JALR
    when(JALR(inst)) {
      decodeI;
      when(addrAligned(getfetchSize(), Cat((now.reg(rs1) + imm)(XLEN - 1, 1), 0.U(1.W)))) {
        global_data.setpc := true.B;
        next.pc           := Cat((now.reg(rs1) + imm)(XLEN - 1, 1), 0.U(1.W));
        next.reg(rd)      := now.pc + 4.U;
      }.otherwise {
        next.csr.mtval := Cat((now.reg(rs1) + imm)(XLEN - 1, 1), 0.U(1.W))
        raiseException(MExceptionCode.instructionAddressMisaligned)
      }
    }
    // - Conditional Branches
    // BEQ/BNE
    when(BEQ(inst)) {
      decodeB;
      when(now.reg(rs1) === now.reg(rs2)) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm;
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    when(BNE(inst)) {
      decodeB;
      when(now.reg(rs1) =/= now.reg(rs2)) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm;
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    // BLT[U]
    when(BLT(inst)) {
      decodeB;
      when(now.reg(rs1).asSInt < now.reg(rs2).asSInt) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    when(BLTU(inst)) {
      decodeB;
      when(now.reg(rs1) < now.reg(rs2)) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    // BGE[U]
    when(BGE(inst)) {
      decodeB;
      when(now.reg(rs1).asSInt >= now.reg(rs2).asSInt) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    when(BGEU(inst)) {
      decodeB;
      when(now.reg(rs1) >= now.reg(rs2)) {
        when(addrAligned(getfetchSize(), now.pc + imm)) {
          global_data.setpc := true.B;
          next.pc           := now.pc + imm
        }.otherwise {
          next.csr.mtval := now.pc + imm;
          raiseException(MExceptionCode.instructionAddressMisaligned)
        }
      }
    }
    // - 2.6 Load and Store Instructions
    // LOAD
    when(LB(inst)) {
      decodeI;
      when(addrAligned(SizeOp.b, now.reg(rs1) + imm)) {
        next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 8.U)(7, 0), XLEN)
      }.otherwise {
        // TODO: LB doesn't seem to get an exception for unaligned access
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    when(LH(inst)) {
      decodeI;
      when(addrAligned(SizeOp.h, now.reg(rs1) + imm)) {
        next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 16.U)(15, 0), XLEN)
      }.otherwise {
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    when(LW(inst)) {
      decodeI;
      when(addrAligned(SizeOp.w, now.reg(rs1) + imm)) {
        next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 32.U)(31, 0), XLEN)
      }.otherwise {
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    when(LBU(inst)) { decodeI; alignedException("Load", SizeOp.b, rs2); next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 8.U)(7, 0), XLEN) }
    when(LHU(inst)) {
      decodeI;
      when(addrAligned(SizeOp.h, now.reg(rs1) + imm)) {
        next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 16.U)(15, 0), XLEN)
      }.otherwise {
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    // STORE
    when(SB(inst)) { decodeS; alignedException("Store", SizeOp.b, rs2); memWrite(now.reg(rs1) + imm, 8.U, now.reg(rs2)(7, 0)) }
    when(SH(inst)) {
      decodeS;
      when(addrAligned(SizeOp.h, now.reg(rs1) + imm)) {
        memWrite(now.reg(rs1) + imm, 16.U, now.reg(rs2)(15, 0))
      }.otherwise {
        mem.write.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.storeOrAMOAddressMisaligned)
      }
    }
    when(SW(inst)) {
      decodeS;
      when(addrAligned(SizeOp.w, now.reg(rs1) + imm)) {
        memWrite(now.reg(rs1) + imm, 32.U, now.reg(rs2)(31, 0))
      }.otherwise {
        mem.write.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.storeOrAMOAddressMisaligned)
      }
    }
    when(EBREAK(inst)) {
      decodeI;
      raiseException(MExceptionCode.breakpoint)
      // printf("IS EBREAK\n")
    }

    when(ECALL(inst)) {
      decodeI;
      switch(now.internal.privilegeMode) {
        is(0x3.U) { raiseException(MExceptionCode.environmentCallFromMmode) }
        is(0x1.U) { raiseException(MExceptionCode.environmentCallFromSmode) }
        is(0x0.U) { raiseException(MExceptionCode.environmentCallFromUmode) }
      }
    }
    when(FENCE(inst)) {
      decodeI /* then do nothing for now */
    }

    // - 2.7 Memory Ordering Instructions
    // - 2.8 Environment Call and Breakpoints
    // - 2.9 HINT Instructions
  }

  /** RV64I Base Integer Instruction Set
    *
    *   - riscv-spec-20191213
    *   - Chapter 5: RV64I Base Integer Instruction Set, Version 2.1
    */
  def doRV64I: Unit = {
    doRV32I
    // RV64I will override same inst in RV32I

    // - 5.2 Integer Computational Instructions
    // - Integer Register-Immediate Instructions
    // ADDIW
    when(ADDIW(inst)) { decodeI; next.reg(rd) := signExt((now.reg(rs1) + imm)(31, 0), XLEN) }
    // SLLI/SRLI/SRAI
    when(SLLI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) << imm(5, 0) }                 // override RV32
    when(SRLI(inst)) { decodeI; next.reg(rd) := now.reg(rs1) >> imm(5, 0) }                 // override RV32
    when(SRAI(inst)) { decodeI; next.reg(rd) := (now.reg(rs1).asSInt >> imm(5, 0)).asUInt } // override RV32
    // SLLIW/SRLIW/SRAIW
    when(SLLIW(inst)) { decodeI; next.reg(rd) := signExt((now.reg(rs1)(31, 0) << imm(4, 0))(31, 0), XLEN) }
    when(SRLIW(inst)) { decodeI; next.reg(rd) := signExt(now.reg(rs1)(31, 0) >> imm(4, 0), XLEN) }
    when(SRAIW(inst)) { decodeI; next.reg(rd) := signExt((now.reg(rs1)(31, 0).asSInt >> imm(4, 0)).asUInt, XLEN) }
    // LUI/AUIPC not changed
    // - Integer Register-Register Operations
    // SLL/SRL
    when(SLL(inst)) { decodeR; next.reg(rd) := now.reg(rs1) << now.reg(rs2)(5, 0) } // override RV32
    when(SRL(inst)) { decodeR; next.reg(rd) := now.reg(rs1) >> now.reg(rs2)(5, 0) } // overried RV32
    // SRA
    when(SRA(inst)) { decodeR; next.reg(rd) := (now.reg(rs1).asSInt >> now.reg(rs2)(5, 0)).asUInt }
    // ADDW
    when(ADDW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0) + now.reg(rs2)(31, 0))(31, 0), XLEN) }
    // SLLW/SRLW
    when(SLLW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0) << now.reg(rs2)(4, 0))(31, 0), XLEN) }
    when(SRLW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0) >> now.reg(rs2)(4, 0))(31, 0), XLEN) }
    // SUBW/SRAW
    when(SUBW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0) - now.reg(rs2)(31, 0))(31, 0), XLEN) }
    when(SRAW(inst)) { decodeR; next.reg(rd) := signExt((now.reg(rs1)(31, 0).asSInt >> now.reg(rs2)(4, 0)).asUInt, XLEN) }

    // - 5.3 Load and Store Instructions RV64
    // - LOAD
    // FIXME: Not all of them have added the exception access limit, which needs to be reorganized and added.
    when(LWU(inst)) {
      decodeI;
      when(addrAligned(SizeOp.w, now.reg(rs1) + imm)) {
        next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 32.U)(31, 0), XLEN)
      }.otherwise {
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    when(LD(inst)) {
      decodeI;
      when(addrAligned(SizeOp.d, now.reg(rs1) + imm)) {
        next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 64.U)(63, 0), XLEN)
      }.otherwise {
        mem.read.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.loadAddressMisaligned)
      }
    }
    // - STORE
    when(SD(inst)) {
      decodeS;
      when(addrAligned(SizeOp.d, now.reg(rs1) + imm)) {
        memWrite(now.reg(rs1) + imm, 64.U, now.reg(rs2)(63, 0))
      }.otherwise {
        mem.write.addr := now.reg(rs1) + imm
        raiseException(MExceptionCode.storeOrAMOAddressMisaligned)
      }
    }

    // - 5.4 HINT Instructions
  }

  def doRVI: Unit = {
    config.XLEN match {
      case 32 => doRV32I
      case 64 => doRV64I
    }
  }
}

// scalafmt: { maxColumn = 120 } (back to defaults)
