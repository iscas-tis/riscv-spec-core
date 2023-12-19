package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import csr._

/** “Zicsr” Control and Status Register (CSR) Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 24: RV32/64G Instruction Set Listings
  *     - Table 24.2: Instruction listing for RISC-V
  */
trait ZicsrExtensionInsts {
  // - RV32/RV64 Zicsr Standard Extension
  val CSRRW  = Inst("b????????????_?????_001_?????_1110011")
  val CSRRS  = Inst("b????????????_?????_010_?????_1110011")
  val CSRRC  = Inst("b????????????_?????_011_?????_1110011")
  val CSRRWI = Inst("b????????????_?????_101_?????_1110011")
  val CSRRSI = Inst("b????????????_?????_110_?????_1110011")
  val CSRRCI = Inst("b????????????_?????_111_?????_1110011")
  // Figure 2.3: RISC-V base instruction formats showing immediate variants
  //   31                                  20 | 19     15| 14    12 | 11                   7 | 6      0
  // /----------------------------------------|----------|----------|------------------------|----------\
  // | imm_11_0                            12 | rs1 5    | funct3 3 | rd                   5 | opcode 7 | I-type
  // | source/dest                         12 | source 5 | CSRRW  3 | dest                 5 | SYSTEM 7 | CSRRW  Read / Write
  // | source/dest                         12 | source 5 | CSRRS  3 | dest                 5 | SYSTEM 7 | CSRRS  Read & Set Bit
  // | source/dest                         12 | source 5 | CSRRC  3 | dest                 5 | SYSTEM 7 | CSRRC  Read & Clear Bit
  // | source/dest                         12 | uimm   5 | CSRRWI 3 | dest                 5 | SYSTEM 7 | CSRRWI Read / Write Imm
  // | source/dest                         12 | uimm   5 | CSRRSI 3 | dest                 5 | SYSTEM 7 | CSRRSI Read & Set Bit Imm
  // | source/dest                         12 | uimm   5 | CSRRCI 3 | dest                 5 | SYSTEM 7 | CSRRCI Read & Clear Bit Imm
  // \----------------------------------------|----------------------------------------------|----------/
  //   31                                  20 | 19     15| 14    12 | 11                   7 | 6      0
  // e.g. 0x30102573  csrrs a0,csr,zero
  // 001100000001_00000_010_01010_1110011
  // misa 0x301 =Bin(0011_0000_0001)
  // csrr	a0,misa is equal to csrrs a0,csr,zero
}

/** “Zicsr” Control and Status Register (CSR) Instructions
  *
  *   - riscv-spec-20191213
  *   - Chapter 9 “Zicsr”, Control and Status Register (CSR) Instructions,
  *     Version 2.0
  */
trait ZicsrExtension extends BaseCore with CommonDecode with ZicsrExtensionInsts with CSRSupport with ExceptionSupport {
  def wen(addr:UInt, justRead:Bool = false.B) : Bool = {
    // val justRead = isSet && src1 === 0.U  // csrrs and csrrsi are exceptions when their src1 is zero
    val isIllegalWrite = addr(11,10) === "b11".U && (!justRead)
    val isIllegalMode  = priviledgeMode < addr(9, 8)
    // val isIllegalWrite = wen && (addr(11, 10) === "b11".U) && !justRead  // Write a read-only CSR register
    val isIllegalAccess = isIllegalMode || isIllegalWrite
    val has:    Bool = MuxLookup(addr, false.B, now.csr.table.map { x => x.info.addr -> true.B })
    when(isIllegalAccess || !has) {
      raiseException(MExceptionCode.illegalInstruction)
    }
    isIllegalWrite
  }
  def doRVZicsr: Unit = {
    // printf("PC: %x Inst:%x\n",now.pc,inst)
    when(CSRRW(inst)) {
      // t = CSRs[csr]; CSRs[csr] = x[rs1]; x[rd] = t
      // printf("Is CSRRW:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0))){
        when(rd =/= 0.U) {
          next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        }
        csrWrite(imm(11, 0), now.reg(rs1))
      }

    }
    when(CSRRS(inst)) {
      // t = CSRs[csr]; CSRs[csr] = t | x[rs1]; x[rd] = t
      // printf("Is CSRRS:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0), now.reg(rs1) === 0.U)){
        // imm_11_0, rs1 , funct3, rd             , opcode ), inst); 
        // imm := signExt(    imm_11_0                                      , XLEN) }
        // printf("imm:%x rs1:%x rd:%x\n",imm,rs1,rd)
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        // printf("After Write:%x\n",next.reg(rd))
        when(rs1 =/= 0.U) {
          csrWrite(imm(11, 0), zeroExt(csrRead(imm(11, 0)), XLEN) | now.reg(rs1))
        }
      }
    }
    when(CSRRC(inst)) {
      // t = CSRs[csr]; CSRs[csr] = t &~x[rs1]; x[rd] = t
      // printf("Is CSRRC:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0))){
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        when(rs1 =/= 0.U) {
          // FIXME: 新写法wmask下导致的失灵 [待验证]
          csrWrite(imm(11, 0), zeroExt(csrRead(imm(11, 0)), XLEN) & ~now.reg(rs1))
        }
      }
    }
    when(CSRRWI(inst)) {
      // x[rd] = CSRs[csr]; CSRs[csr] = zimm
      // printf("Is CSRRWI:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0))){
        when(rd =/= 0.U) {
          next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        }
        csrWrite(imm(11, 0), zeroExt(rs1, XLEN))
      }
    }
    when(CSRRSI(inst)) {
      // t = CSRs[csr]; CSRs[csr] = t | zimm; x[rd] = t
      // printf("Is CSRRSI:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0), now.reg(rs1) === 0.U)){
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        // TODO: might have some exceptions when csrrs and csrrsi rs1 is zero?
        when(rs1 =/= 0.U) {
          csrWrite(imm(11, 0), zeroExt(csrRead(imm(11, 0)), XLEN) | zeroExt(rs1, XLEN))
        }
      }
    }
    when(CSRRCI(inst)) {
      // t = CSRs[csr]; CSRs[csr] = t &~zimm; x[rd] = t
      // printf("Is CSRRCI:%x\n",inst)
      decodeI
      when(!wen(imm(11, 0))){
        next.reg(rd) := zeroExt(csrRead(imm(11, 0)), XLEN)
        when(rs1 =/= 0.U) {
          // FIXME: 新写法wmask下导致的失灵？ [待验证]
          csrWrite(imm(11, 0), zeroExt(csrRead(imm(11, 0)), XLEN) & ~zeroExt(rs1, XLEN))
        }
      }
    }
  }
}
