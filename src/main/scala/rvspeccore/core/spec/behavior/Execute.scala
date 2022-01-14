package rvspeccore.core.spec.behavior

import chisel3._
import chisel3.util._

import rvspeccore.core._
import rvspeccore.core.spec.code._
import rvspeccore.core.tool.BitTool._

trait Execute extends BaseCore { this: Decode =>
  val setPc = WireInit(false.B)

  def memRead(addr: UInt, memWidth: UInt): UInt = {
    mem.read.valid    := true.B
    mem.read.addr     := addr
    mem.read.memWidth := memWidth
    mem.read.data
  }
  def memWrite(addr: UInt, memWidth: UInt, data: UInt): Unit = {
    mem.write.valid    := true.B
    mem.write.addr     := addr
    mem.write.memWidth := memWidth
    mem.write.data     := data
  }

  def deRV32I: Unit = {
    // scalafmt: { maxColumn = 200 }
    switch(inst(6, 0)) {
      // riscv-spec-20191213
      // Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
      // 2.4 Integer Computational Instructions
      // Integer Register-Immediate Instructions
      is(OpcodeMap("OP-IMM")) {
        iTypeDecode
        switch(funct3) {
          // ADDI/SLTI[U]
          is(Funct3Map("ADDI"))  { next.reg(rd) := now.reg(rs1) + imm }
          is(Funct3Map("SLTI"))  { next.reg(rd) := Mux(now.reg(rs1).asSInt < imm.asSInt, 1.U, 0.U) }
          is(Funct3Map("SLTIU")) { next.reg(rd) := Mux(now.reg(rs1) < imm, 1.U, 0.U) }
          // ANDI/ORI/XORI
          is(Funct3Map("ANDI")) { next.reg(rd) := now.reg(rs1) & imm }
          is(Funct3Map("ORI"))  { next.reg(rd) := now.reg(rs1) | imm }
          is(Funct3Map("XORI")) { next.reg(rd) := now.reg(rs1) ^ imm }
        }
        switch(Cat(imm(11, 5), funct3)) {
          // SLLI/SRLI/SRAI
          is(catLit("b000_0000".U(7.W), Funct3Map("SLLI"))) { next.reg(rd) := now.reg(rs1) << imm(4, 0) }
          is(catLit("b000_0000".U(7.W), Funct3Map("SRLI"))) { next.reg(rd) := now.reg(rs1) >> imm(4, 0) }
          is(catLit("b010_0000".U(7.W), Funct3Map("SRAI"))) { next.reg(rd) := (now.reg(rs1).asSInt >> imm(4, 0)).asUInt }
        }
      }
      is(OpcodeMap("LUI")) {
        uTypeDecode
        // LUI
        next.reg(rd) := imm
      }
      is(OpcodeMap("AUIPC")) {
        uTypeDecode
        // AUIPC
        next.reg(rd) := now.pc + imm
      }
      // Integer Register-Register Operations
      is(OpcodeMap("OP")) {
        rTypeDecode
        switch(Cat(funct7, funct3)) {
          // ADD/SLT/SLTU
          is(catLit(Funct7Map("ADD"), Funct3Map("ADD")))   { next.reg(rd) := now.reg(rs1) + now.reg(rs2) }
          is(catLit(Funct7Map("SLT"), Funct3Map("SLT")))   { next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, 1.U, 0.U) }
          is(catLit(Funct7Map("SLTU"), Funct3Map("SLTU"))) { next.reg(rd) := Mux(now.reg(rs1) < now.reg(rs2), 1.U, 0.U) }
          // AND/OR/XOR
          is(catLit(Funct7Map("AND"), Funct3Map("AND"))) { next.reg(rd) := now.reg(rs1) & now.reg(rs2) }
          is(catLit(Funct7Map("OR"), Funct3Map("OR")))   { next.reg(rd) := now.reg(rs1) | now.reg(rs2) }
          is(catLit(Funct7Map("XOR"), Funct3Map("XOR"))) { next.reg(rd) := now.reg(rs1) ^ now.reg(rs2) }
          // SLL/SRL
          is(catLit(Funct7Map("SLL"), Funct3Map("SLL"))) { next.reg(rd) := now.reg(rs1) << now.reg(rs2)(4, 0) }
          is(catLit(Funct7Map("SRL"), Funct3Map("SRL"))) { next.reg(rd) := now.reg(rs1) >> now.reg(rs2)(4, 0) }
          // SUB/SRA
          is(catLit(Funct7Map("SUB"), Funct3Map("SUB"))) { next.reg(rd) := now.reg(rs1) - now.reg(rs2) }
          is(catLit(Funct7Map("SRA"), Funct3Map("SRA"))) { next.reg(rd) := (now.reg(rs1).asSInt >> now.reg(rs2)(4, 0)).asUInt }
        }
      }
      // NOP Instruction
      // NOP is encoded as ADDI x0, x0, 0.

      // 2.5 Control Transfer Instructions
      // Unconditional Jumps
      is(OpcodeMap("JAL")) {
        jTypeDecode
        // JAL
        setPc        := true.B
        next.pc      := now.pc + imm
        next.reg(rd) := now.pc + 4.U
      }
      is(OpcodeMap("JALR")) {
        iTypeDecode
        // JALR
        setPc        := true.B
        next.pc      := Cat((now.reg(rs1) + imm)(XLEN - 1, 1), 0.U(1.W))
        next.reg(rd) := now.pc + 4.U
      }
      // Conditional Branches
      is(OpcodeMap("BRANCH")) {
        bTypeDecode
        switch(funct3) {
          // BEQ/BNE
          is(Funct3Map("BEQ")) { when(now.reg(rs1) === now.reg(rs2)) { setPc := true.B; next.pc := now.pc + imm } }
          is(Funct3Map("BNE")) { when(now.reg(rs1) =/= now.reg(rs2)) { setPc := true.B; next.pc := now.pc + imm } }
          // BLT[U]
          is(Funct3Map("BLT"))  { when(now.reg(rs1).asSInt < now.reg(rs2).asSInt) { setPc := true.B; next.pc := now.pc + imm } }
          is(Funct3Map("BLTU")) { when(now.reg(rs1) < now.reg(rs2)) { setPc := true.B; next.pc := now.pc + imm } }
          // BGE[U]
          is(Funct3Map("BGE"))  { when(now.reg(rs1).asSInt >= now.reg(rs2).asSInt) { setPc := true.B; next.pc := now.pc + imm } }
          is(Funct3Map("BGEU")) { when(now.reg(rs1) >= now.reg(rs2)) { setPc := true.B; next.pc := now.pc + imm } }
        }
      }
      // 2.6 Load and Store Instructions
      is(OpcodeMap("LOAD")) {
        iTypeDecode
        // LOAD
        switch(funct3) {
          is(Funct3Map("LB"))  { next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 8.U)(7, 0), XLEN) }
          is(Funct3Map("LH"))  { next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 16.U)(15, 0), XLEN) }
          is(Funct3Map("LW"))  { next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 32.U)(31, 0), XLEN) }
          is(Funct3Map("LBU")) { next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 8.U)(7, 0), XLEN) }
          is(Funct3Map("LHU")) { next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 16.U)(15, 0), XLEN) }
        }
      }
      is(OpcodeMap("STORE")) {
        sTypeDecode
        // STORE
        switch(funct3) {
          is(Funct3Map("SB")) { memWrite(now.reg(rs1) + imm, 8.U, now.reg(rs2)(7, 0)) }
          is(Funct3Map("SH")) { memWrite(now.reg(rs1) + imm, 16.U, now.reg(rs2)(15, 0)) }
          is(Funct3Map("SW")) { memWrite(now.reg(rs1) + imm, 32.U, now.reg(rs2)(31, 0)) }
        }
      }
    }
    // scalafmt: { maxColumn = 120 } (back to defaults)
  }
  def deRV64I: Unit = {
    deRV32I
    // RV64I will override same inst in RV32I
    // scalafmt: { maxColumn = 200 }
    switch(inst(6, 0)) {
      // riscv-spec-20191213
      // RV64I Base Integer Instruction Set, Version 2.1
      // 5.2 Integer Computational Instructions
      // Integer Register-Immediate Instructions
      is(OpcodeMap("OP-IMM-32")) {
        iTypeDecode
        switch(funct3) {
          // ADDIW
          is(Funct3Map("ADDIW")) { next.reg(rd) := signExt((now.reg(rs1) + imm)(31, 0), XLEN) }
        }
        switch(Cat(imm(11, 5), funct3)) {
          // SLLIW/SRLIW/SRAIW
          is(catLit("b000_0000".U(7.W), Funct3Map("SLLIW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) << imm(4, 0))(31, 0), XLEN) }
          is(catLit("b000_0000".U(7.W), Funct3Map("SRLIW"))) { next.reg(rd) := signExt(now.reg(rs1)(31, 0) >> imm(4, 0), XLEN) }
          is(catLit("b010_0000".U(7.W), Funct3Map("SRAIW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0).asSInt >> imm(4, 0)).asUInt, XLEN) }
        }
      }
      is(OpcodeMap("OP-IMM")) {
        iTypeDecode
        switch(Cat(imm(11, 6), funct3)) {
          // SLLI/SRLI/SRAI
          is(catLit("b00_0000".U(6.W), Funct3Map("SLLI"))) { next.reg(rd) := now.reg(rs1) << imm(5, 0) }
          is(catLit("b00_0000".U(6.W), Funct3Map("SRLI"))) { next.reg(rd) := now.reg(rs1) >> imm(5, 0) }
          is(catLit("b01_0000".U(6.W), Funct3Map("SRAI"))) { next.reg(rd) := (now.reg(rs1).asSInt >> imm(5, 0)).asUInt }
        }
      }
      // LUI/AUIPC not changed
      // Integer Register-Register Operations
      is(OpcodeMap("OP")) {
        rTypeDecode
        switch(Cat(funct7, funct3)) {
          // SLL/SRL
          is(catLit(Funct7Map("SLL"), Funct3Map("SLL"))) { next.reg(rd) := now.reg(rs1) << now.reg(rs2)(5, 0) }
          is(catLit(Funct7Map("SRL"), Funct3Map("SRL"))) { next.reg(rd) := now.reg(rs1) >> now.reg(rs2)(5, 0) }
          // SRA
          is(catLit(Funct7Map("SRA"), Funct3Map("SRA"))) { next.reg(rd) := (now.reg(rs1).asSInt >> now.reg(rs2)(5, 0)).asUInt }
        }
      }
      is(OpcodeMap("OP-32")) {
        rTypeDecode
        switch(Cat(funct7, funct3)) {
          // ADDW
          is(catLit(Funct7Map("ADDW"), Funct3Map("ADDW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) + now.reg(rs2)(31, 0))(31, 0), XLEN) }
          // SLLW/SRLW
          is(catLit(Funct7Map("SLLW"), Funct3Map("SLLW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) << now.reg(rs2)(4, 0))(31, 0), XLEN) }
          is(catLit(Funct7Map("SRLW"), Funct3Map("SRLW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) >> now.reg(rs2)(4, 0))(31, 0), XLEN) }
          // SUBW/SRAW
          is(catLit(Funct7Map("SUBW"), Funct3Map("SUBW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0) - now.reg(rs2)(31, 0))(31, 0), XLEN) }
          is(catLit(Funct7Map("SRAW"), Funct3Map("SRAW"))) { next.reg(rd) := signExt((now.reg(rs1)(31, 0).asSInt >> now.reg(rs2)(4, 0)).asUInt, XLEN) }
        }
      }
      // 5.3 Load and Store Instructions
      is(OpcodeMap("LOAD")) {
        iTypeDecode
        // LOAD
        switch(funct3) {
          is(Funct3Map("LWU")) { next.reg(rd) := zeroExt(memRead(now.reg(rs1) + imm, 32.U)(31, 0), XLEN) }
          is(Funct3Map("LD"))  { next.reg(rd) := signExt(memRead(now.reg(rs1) + imm, 64.U)(63, 0), XLEN) }
        }
      }
      is(OpcodeMap("STORE")) {
        sTypeDecode
        // STORE
        switch(funct3) {
          is(Funct3Map("SD")) { memWrite(now.reg(rs1) + imm, 64.U, now.reg(rs2)(63, 0)) }
        }
      }
    }
    // scalafmt: { maxColumn = 120 } (back to defaults)
  }
}
