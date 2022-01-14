package rvspeccore.core.spec.behavior

import chisel3._
import chisel3.util._

import rvspeccore.core._
import rvspeccore.core.spec.code._
import rvspeccore.core.tool.BitTool._

trait Execute extends BaseCore { this: Decode =>
  val setPc = WireInit(false.B)

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
          is(Funct3Map("LB"))  { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 8.U; next.reg(rd) := signExt(mem.read.data(7, 0), XLEN) }
          is(Funct3Map("LH"))  { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 16.U; next.reg(rd) := signExt(mem.read.data(15, 0), XLEN) }
          is(Funct3Map("LW"))  { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 32.U; next.reg(rd) := signExt(mem.read.data(31, 0), XLEN) }
          is(Funct3Map("LBU")) { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 8.U; next.reg(rd) := zeroExt(mem.read.data(7, 0), XLEN) }
          is(Funct3Map("LHU")) { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 16.U; next.reg(rd) := zeroExt(mem.read.data(16, 0), XLEN) }
        }
      }
      is(OpcodeMap("STORE")) {
        sTypeDecode
        // STORE
        switch(funct3) {
          is(Funct3Map("SB")) { mem.write.valid := true.B; mem.write.addr := now.reg(rs1) + imm; mem.write.memWidth := 8.U; mem.write.data := now.reg(rs2)(7, 0) }
          is(Funct3Map("SH")) { mem.write.valid := true.B; mem.write.addr := now.reg(rs1) + imm; mem.write.memWidth := 16.U; mem.write.data := now.reg(rs2)(15, 0) }
          is(Funct3Map("SW")) { mem.write.valid := true.B; mem.write.addr := now.reg(rs1) + imm; mem.write.memWidth := 32.U; mem.write.data := now.reg(rs2)(31, 0) }
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
          is(Funct3Map("LWU")) { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 32.U; next.reg(rd) := zeroExt(mem.read.data(31, 0), XLEN) }
          is(Funct3Map("LD"))  { mem.read.valid := true.B; mem.read.addr := now.reg(rs1) + imm; mem.read.memWidth := 64.U; next.reg(rd) := signExt(mem.read.data(63, 0), XLEN) }
        }
      }
      is(OpcodeMap("STORE")) {
        sTypeDecode
        // STORE
        switch(funct3) {
          is(Funct3Map("SD")) { mem.write.valid := true.B; mem.write.addr := now.reg(rs1) + imm; mem.write.memWidth := 64.U; mem.write.data := now.reg(rs2)(63, 0) }
        }
      }
    }
    // scalafmt: { maxColumn = 120 } (back to defaults)
  }
}
