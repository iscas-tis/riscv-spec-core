package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

/** Base Integer Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
  *   - Chapter 5: RV64I Base Integer Instruction Set, Version 2.1
  *   - Chapter 24: RV32/64G Instruction Set Listings
  *     - Table 24.2: Instruction listing for RISC-V
  */
object IBaseInsts extends Insts {
  val table = List(
    // RV32I Base Instruction Set
    InstInfo(None, None, "LUI",   "LUI"),   // 0110111
    InstInfo(None, None, "AUIPC", "AUIPC"), // 0010111
    // Jump
    InstInfo(None, None,  "JAL",  "JAL"),  // 1101111
    InstInfo(None, "000", "JALR", "JALR"), // 1100111
    // Logic
    InstInfo(None, "000", "BEQ",  "BRANCH"), // 1100011
    InstInfo(None, "001", "BNE",  "BRANCH"),
    InstInfo(None, "100", "BLT",  "BRANCH"),
    InstInfo(None, "101", "BGE",  "BRANCH"),
    InstInfo(None, "110", "BLTU", "BRANCH"),
    InstInfo(None, "111", "BGEU", "BRANCH"),
    // Load Store
    InstInfo(None, "000", "LB",  "LOAD"),  // 0000011
    InstInfo(None, "001", "LH",  "LOAD"),
    InstInfo(None, "010", "LW",  "LOAD"),
    InstInfo(None, "100", "LBU", "LOAD"),
    InstInfo(None, "101", "LHU", "LOAD"),
    InstInfo(None, "000", "SB",  "STORE"), // 0100011
    InstInfo(None, "001", "SH",  "STORE"),
    InstInfo(None, "010", "SW",  "STORE"),
    // Arith with imm
    InstInfo(None, "000", "ADDI",  "OP-IMM"), // 0010011
    InstInfo(None, "010", "SLTI",  "OP-IMM"),
    InstInfo(None, "011", "SLTIU", "OP-IMM"),
    InstInfo(None, "100", "XORI",  "OP-IMM"),
    InstInfo(None, "110", "ORI",   "OP-IMM"),
    InstInfo(None, "111", "ANDI",  "OP-IMM"),
    InstInfo(None, "001", "SLLI",  "OP-IMM"),
    InstInfo(None, "101", "SRLI",  "OP-IMM"),
    InstInfo(None, "101", "SRAI",  "OP-IMM"),
    // Arith without imm
    InstInfo("0000000", "000", "ADD",  "OP"), // 0110011
    InstInfo("0100000", "000", "SUB",  "OP"),
    InstInfo("0000000", "001", "SLL",  "OP"),
    InstInfo("0000000", "010", "SLT",  "OP"),
    InstInfo("0000000", "011", "SLTU", "OP"),
    InstInfo("0000000", "100", "XOR",  "OP"),
    InstInfo("0000000", "101", "SRL",  "OP"),
    InstInfo("0100000", "101", "SRA",  "OP"),
    InstInfo("0000000", "110", "OR",   "OP"),
    InstInfo("0000000", "111", "AND",  "OP"),
    // Other
    InstInfo(None, "000", "FENCE",  "MISC-MEM"), // 0001111
    InstInfo(None, "001", "ECALL",  "SYSTEM"),   // 1110011
    InstInfo(None, "000", "EBREAK", "SYSTEM"),

    // RV64I Base Instruction Set (in addition to RV32I)
    InstInfo(None, "110", "LWU",  "LOAD"),   // 0000011
    InstInfo(None, "011", "LD",   "LOAD"),
    InstInfo(None, "011", "SD",   "STORE"),  // 0100011
    InstInfo(None, "001", "SLLI", "OP-IMM"), // 0010011
    InstInfo(None, "101", "SRLI", "OP-IMM"),
    InstInfo(None, "101", "SRAI", "OP-IMM"),
    // 32-bits
    InstInfo(None,      "000", "ADDIW", "OP-IMM-32"), // 0011011
    InstInfo(None,      "001", "SLLIW", "OP-IMM-32"),
    InstInfo(None,      "101", "SRLIW", "OP-IMM-32"),
    InstInfo(None,      "101", "SRAIW", "OP-IMM-32"),
    InstInfo("0000000", "000", "ADDW",  "OP-32"),     // 0111011
    InstInfo("0100000", "000", "SUBW",  "OP-32"),
    InstInfo("0000000", "001", "SLLW",  "OP-32"),
    InstInfo("0000000", "101", "SRLW",  "OP-32"),
    InstInfo("0100000", "101", "SRAW",  "OP-32")
  )
}

// scalafmt: { maxColumn = 200 }

trait IBase extends BaseCore with CommonDecode {
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

  /** RV32I Base Integer Instruction Set
    *
    *   - riscv-spec-20191213
    *   - Chapter 2: RV32I Base Integer Instruction Set, Version 2.1
    */
  def deRV32I: Unit = {
    switch(inst(6, 0)) {
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
  }

  /** RV64I Base Integer Instruction Set
    *
    *   - riscv-spec-20191213
    *   - Chapter 5: RV64I Base Integer Instruction Set, Version 2.1
    */
  def deRV64I: Unit = {
    deRV32I
    // RV64I will override same inst in RV32I
    switch(inst(6, 0)) {
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
  }
}

// scalafmt: { maxColumn = 120 } (back to defaults)
