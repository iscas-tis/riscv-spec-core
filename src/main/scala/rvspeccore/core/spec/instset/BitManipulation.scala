package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

/** "B" Extension for Bit Manipulation, Version 1.0.0
  *
  *   - riscv-spec-20240411
  *   - Chapter 28: "B" Extension for Bit Manipulation, Version 1.0.0
  *   - The B standard extension comprises instructions provided by the Zba, Zbb, and Zbs extensions.
  */
trait BitManipulationInsts {
  val add_uw = Inst("b0000100_?????_?????_000_?????_0111011")
  val andn   = Inst("b0100000_?????_?????_111_?????_0110011")
  val bclr   = Inst("b0100100_?????_?????_001_?????_0110011")
  val bclri  = Inst(
    32 -> "b0100100_?????_?????_011_?????_0110011",
    64 -> "b010010_??????_?????_011_?????_0110011"
  )
  val bext   = Inst("b0100100_?????_?????_101_?????_0110011")
  val bexti  = Inst(
    32 -> "b0100100_?????_?????_101_?????_0010011",
    64 -> "b010010_??????_?????_101_?????_0010011"
  )
  val binv   = Inst("b0110100_?????_?????_001_?????_0110011")
  val binvi  = Inst(
    32 -> "b0110100_?????_?????_001_?????_0010011",
    64 -> "b011010_??????_?????_001_?????_0010011"
  )
  val bseti  = Inst(
    32 -> "b0010100_?????_?????_001_?????_0010011",
    64 -> "b001010_??????_?????_001_?????_0010011"
  )
  val bset   = Inst("b0010100_?????_?????_001_?????_0110011")
  val clmul  = Inst("b0000101_?????_?????_001_?????_0110011")
  val clmulh = Inst("b0000101_?????_?????_011_?????_0110011")
  val clmulr = Inst("b0000101_?????_?????_010_?????_0110011")
  val clz    = Inst("b0110000_00000_?????_001_?????_0010011")
  val clzw   = Inst("b0110000_00000_?????_001_?????_0011011")
  val cpop   = Inst("b0110000_00010_?????_001_?????_0010011")
  val cpopw  = Inst("b0110000_00010_?????_001_?????_0011011")
  val ctz    = Inst("b0110000_00001_?????_001_?????_0010011")
  val ctzw   = Inst("b0110000_00001_?????_001_?????_0011011")
  val max    = Inst("b0000101_?????_?????_110_?????_0110011")
  val maxu   = Inst("b0000101_?????_?????_111_?????_0110011")
  val min    = Inst("b0000101_?????_?????_100_?????_0110011")
  val minu   = Inst("b0000101_?????_?????_101_?????_0110011")
  val orc_b  = Inst("b001010000111_?????_101_?????_0010011")
  val orn    = Inst("b0100000_?????_?????_110_?????_0110011")
  val pack   = Inst("b0000100_?????_?????_100_?????_0110011")
  val packh  = Inst("b0000100_?????_?????_111_?????_0110011")
  val packw  = Inst("b0000100_?????_?????_100_?????_0111011")
  val rev8   = Inst(
    32 -> "b011010011000_?????_101_?????_0010011",
    64 -> "b011010111000_?????_101_?????_0010011"
  )
  val rev_b  = Inst("b011010000111_?????_101_?????_0010011")
  val rol    = Inst("b0110000_?????_?????_001_?????_0110011")
  val rolw   = Inst("b0110000_?????_?????_001_?????_0111011")
  val ror    = Inst("b0110000_?????_?????_101_?????_0110011")
  val rori   = Inst(
    32 -> "b0110000_?????_?????_101_?????_0010011",
    64 -> "b011000_??????_?????_101_?????_0010011"
  )
  val roriw  = Inst("b0110000_?????_?????_101_?????_0011011")
  val rorw   = Inst("b0110000_?????_?????_101_?????_0111011")
  val sext_b = Inst("b0110000_00100_?????_001_?????_0010011")
  val sext_h = Inst("b0110000_00101_?????_001_?????_0010011")
  val sh1add = Inst("b0010000_?????_?????_010_?????_0110011")
  val sh1add_uw = Inst("b0010000_?????_?????_010_?????_0111011")
  val sh2add = Inst("b0010000_?????_?????_100_?????_0110011")
  val sh2add_uw = Inst("b0010000_?????_?????_100_?????_0111011")
  val sh3add = Inst("b0010000_?????_?????_110_?????_0110011")
  val sh3add_uw = Inst("b0010000_?????_?????_110_?????_0111011")
  val slli_uw = Inst("b000010_??????_?????_001_?????_0011011")
  val unzip  = Inst("b0000100_11111_?????_101_?????_0010011")
  val xnor   = Inst("b0100000_?????_?????_100_?????_0110011")
  val xperm_b= Inst("b0010100_?????_?????_100_?????_0110011")
  val xperm_n= Inst("b0010100_?????_?????_010_?????_0110011")
  val zext_h = Inst(
    32 -> "b0000100_00000_?????_100_?????_0110011",
    64 -> "b0000100_00000_?????_100_?????_0111011"
  )
  val zip    = Inst("b0000100_11110_?????_001_?????_0010011")
}

/** "B" Extension for Bit Manipulation, Version 1.0.0
  *
  *   - riscv-spec-20240411
  *   - Chapter 28: "B" Extension for Bit Manipulation, Version 1.0.0
  */
trait BitManipulation extends BaseCore with CommonDecode with BitManipulationInsts {
  def doRV32Zba: Unit = {
    /** Address generation
     * The Zba instructions can be used to accelerate the generation of addresses that index into arrays of
     * basic types (halfword, word, doubleword) using both unsigned word-sized and XLEN-sized indices: a
     * shifted index is added to a base address.
     * - riscv-spec-20240411 P212
     * - Chapter 28.4.1
    */
    when(sh1add(inst)) {decodeR; next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 1)}
    when(sh2add(inst)) { decodeR; next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 2) }
    when(sh3add(inst)) { decodeR; next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 3) }
  }
  def doRV32Zbb: Unit = {
    /** Basic bit-manipulation
     * - riscv-spec-20240411 P212
     * - Chapter 28.4.2
     */
    // Logical with negate
    when(andn(inst)) { decodeR; next.reg(rd) := now.reg(rs1) & (~now.reg(rs2)).asUInt }
    when(orn(inst)) { decodeR; next.reg(rd) := now.reg(rs1) | (~now.reg(rs2)).asUInt }
    when(xnor(inst)) { decodeR; next.reg(rd) := (~(now.reg(rs1) ^ now.reg(rs2))).asUInt }
    // Count leading/trailing zero bits
    when(clz(inst)) { decodeI; next.reg(rd) := Mux(now.reg(rs1) === 0.U, XLEN.U, PriorityEncoder(now.reg(rs1).asBools.reverse))}
    when(ctz(inst)) { decodeI; next.reg(rd) := Mux(now.reg(rs1) === 0.U, XLEN.U, PriorityEncoder(now.reg(rs1).asBools))}
    // Count population
    when(cpop(inst)) { decodeI; next.reg(rd) := PopCount(now.reg(rs1)) }
    // Integer minimum/maximum
    when(max(inst))  { decodeR; next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, now.reg(rs2), now.reg(rs1)) }
    when(maxu(inst)) { decodeR; next.reg(rd) := Mux(now.reg(rs1).asUInt < now.reg(rs2).asUInt, now.reg(rs2), now.reg(rs1)) }
    when(min(inst))  { decodeR; next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, now.reg(rs1), now.reg(rs2)) }
    when(minu(inst)) { decodeR; next.reg(rd) := Mux(now.reg(rs1).asUInt < now.reg(rs2).asUInt, now.reg(rs1), now.reg(rs2)) }
    // Sign- and zero-extension
    when(sext_b(inst)) { decodeI; next.reg(rd) := signExt(now.reg(rs1)(7, 0), XLEN) }
    when(sext_h(inst)) { decodeI; next.reg(rd) := signExt(now.reg(rs1)(15, 0), XLEN) }
    when(zext_h(inst)) { decodeI; next.reg(rd) := zeroExt(now.reg(rs1)(15, 0), XLEN) }
    // Bitwise rotation
    // Function to select the appropriate bit width based on XLEN
    def getRotationShamt(value: UInt, xlen: Int): UInt = {
      value(if (xlen == 32) 4 else 5, 0)
    }
    when(rol(inst)) { decodeR; next.reg(rd) := (now.reg(rs1) << getRotationShamt(now.reg(rs2), XLEN)).asUInt | (now.reg(rs1) >> (XLEN.U - getRotationShamt(now.reg(rs2), XLEN))).asUInt }
    when(ror(inst)) { decodeR; next.reg(rd) := (now.reg(rs1) >> getRotationShamt(now.reg(rs2), XLEN)).asUInt | (now.reg(rs1) << (XLEN.U - getRotationShamt(now.reg(rs2), XLEN))).asUInt }
    when(rori(inst)){ decodeI; next.reg(rd) := (now.reg(rs1) >> getRotationShamt(imm, XLEN)).asUInt | (now.reg(rs1) << (XLEN.U - getRotationShamt(imm, XLEN))).asUInt }
    // OR Combine
    when(orc_b(inst)) {
      decodeR;
      val byteResults = VecInit(Seq.fill(XLEN / 8)(0.U(8.W)))
      for (i <- 0 until XLEN by 8) {
        val byte = now.reg(rs1)(i + 7, i)
        byteResults(i / 8) := Mux(byte.orR, 0xFF.U(8.W), 0x00.U(8.W))
      }
      next.reg(rd) := byteResults.asUInt
    }
    // Byte-reverse
    when(rev8(inst)) {
      decodeR;
      var result = 0.U(XLEN.W)
      var j = XLEN - 8
      for (i <- 0 until XLEN by 8) {
        result = result | (now.reg(rs1)(j + 7, j) << i).asUInt
        j -= 8
      }
      next.reg(rd) := result
    }

  }
  def doRV32Zbc: Unit = {
    /** Carry-less multiplication
     * - riscv-spec-20240411 P214
     * - Chapter 28.4.3
     * Carry-less multiplication is the multiplication in the polynomial ring over GF(2).
     */
    when(clmul(inst)) {
      decodeR;
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      for (i <- 0 until XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i) := now.reg(rs1) << i
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
    }
    when(clmulh(inst)) {
      decodeR;
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      for (i <- 1 to  XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i - 1) := now.reg(rs1) >> (XLEN - i)
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
    }
    when(clmulr(inst)) {
      decodeR;
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      for (i <- 0 until XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i) := now.reg(rs1) >> (XLEN - i - 1)
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
    }

  }
  def doRV32Zbs: Unit = {
    /** Single-bit instructions
     * - riscv-spec-20240411 P215
     * - Chapter 28.4.4
     * The single-bit instructions provide a mechanism to set, clear, invert, or extract a single bit in a register.
     * The bit is specified by its index.
     */
    when(bclr(inst)) {}
    when(bclri(inst)) {}
    when(bext(inst)) {}
    when(bexti(inst)) {}
    when(binv(inst)) {}
    when(binvi(inst)) {}
    when(bset(inst)) {}
    when(bseti(inst)) {}
  }

  def doRV64Zba(): Unit = {
    doRV32Zba
    when(add_uw(inst)) {
      decodeR; next.reg(rd) := now.reg(rs2) + zeroExt(now.reg(rs1)(31, 0), XLEN)
    }
    when(sh1add_uw(inst)) {
      decodeR; next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN)  << 1)
    }
    when(sh2add_uw(inst)) {
      decodeR; next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN)  << 2)
    }
    when(sh3add_uw(inst)) {
      decodeR; next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN)  << 3)
    }
    when(slli_uw(inst)) {
      decodeI; next.reg(rd) := zeroExt(now.reg(rs1)(31, 0), XLEN) << imm(5, 0)
    }
    // pseudoinstructions: zext.w rd, rs(Add unsigned word)
  }
  def doRV64Zbb(): Unit = {
    doRV32Zbb
    // Count leading/trailing zero bits
    when(clzw(inst)) { decodeI; next.reg(rd) := Mux(now.reg(rs1) === 0.U, 32.U, PriorityEncoder(now.reg(rs1)(31, 0).asBools.reverse)) }
    when(ctzw(inst)) { decodeI; next.reg(rd) := Mux(now.reg(rs1) === 0.U, 32.U, PriorityEncoder(now.reg(rs1)(31, 0).asBools)) }
    // Count population
    when(cpopw(inst)) { decodeI; next.reg(rd) := PopCount(now.reg(rs1)(31, 0)) }
    // Sign- and zero-extension
    when(zext_h(inst)) { decodeI; next.reg(rd) := zeroExt(now.reg(rs1)(15, 0), XLEN) }
    // Bitwise rotation
    // rori(64Bit) has been implemented in RV32Zbb
    when(rolw(inst)) {
      decodeR;
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result = ((rs1_data << now.reg(rs2)(4, 0)).asUInt | (rs1_data >> (32.U - now.reg(rs2)(4, 0))).asUInt)
      next.reg(rd) := signExt(result(31, 0), XLEN)
    }
    when(roriw(inst)) {
      decodeI;
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result = (rs1_data >> imm(4, 0)).asUInt | (rs1_data << (32.U - imm(4, 0))).asUInt
      next.reg(rd) := signExt(result(31, 0), XLEN)
    }
    when(rorw(inst)) {
      decodeR;
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result = (rs1_data >> now.reg(rs2)(4, 0)).asUInt | (rs1_data << (32.U - now.reg(rs2)(4, 0))).asUInt
      next.reg(rd) := signExt(result(31, 0), XLEN)
    }

    // Byte-reverse
    when(rev8(inst)) {
      decodeR;
      var result = 0.U(XLEN.W)
      var j = XLEN - 8
      for (i <- 0 until XLEN by 8) {
        result = result | (now.reg(rs1)(j + 7, j) << i).asUInt
        j -= 8
      }
      next.reg(rd) := result
    }
  }
  def doRV64Zbc(): Unit = {
    doRV32Zbc
  }
  def doRV64Zbs(): Unit = {
    doRV32Zbs
    when(bclri(inst)) {}
    when(bexti(inst)) {}
    when(binvi(inst)) {}
    when(bseti(inst)) {}
  }

  def doRV32Zbkb: Unit = {
    /** Bit-manipulation for Cryptography
     * - riscv-spec-20240411 P215
     * - Chapter 28.4.5
     * This extension contains instructions essential for implementing common operations in cryptographic
     * workloads.
     */
    when(pack(inst)) {}
    when(packh(inst)) {}
    when(rev_b(inst)) {}

  }
  def doRV32Zbkc: Unit = {
    /** Carry-less multiplication for Cryptography
     * - riscv-spec-20240411 P216
     * - Chapter 28.4.6
     * Carry-less multiplication is the multiplication in the polynomial ring over GF(2). This is a critical
     * operation in some cryptographic workloads, particularly the AES-GCM authenticated encryption
     * scheme. This extension provides only the instructions needed to efficiently implement the GHASH
     * operation, which is part of this workload.
     */
  }
  def doRV32Zbkx: Unit = {
    /** Crossbar permutations
     * - riscv-spec-20240411 P216
     * - Chapter 28.4.7
     * These instructions implement a "lookup table" for 4 and 8 bit elements inside the general purpose
     * registers. rs1 is used as a vector of N-bit words, and rs2 as a vector of N-bit indices into rs1. Elements in
     * rs1 are replaced by the indexed element in rs2, or zero if the index into rs2 is out of bounds.
     * These instructions are useful for expressing N-bit to N-bit boolean operations, and implementing
     * cryptographic code with secret dependent memory accesses (particularly SBoxes) such that the
     * execution latency does not depend on the (secret) data being operated on.
     */
    when(xperm_b(inst)) {}
    when(xperm_n(inst)) {}
  }

  def doRV64Zbkb(): Unit = {
    doRV32Zbkb
    when(packw(inst)) {}
    when(zip(inst)) {}
    when(unzip(inst)) {}
  }
  def doRV64Zbkc(): Unit = {
    doRV32Zbkc
  }
  def doRV64Zbkx(): Unit = {
    doRV32Zbkx
  }

  def doRV32BitManipulation: Unit = {
    if(config.extensions.Zba){
      doRV32Zba
    }
    if(config.extensions.Zbb){
      doRV32Zbb
    }
    if(config.extensions.Zbc){
      doRV32Zbc
    }
    if(config.extensions.Zbs){
      doRV32Zbs
    }
  }
  def doRV64BitManipulation: Unit = {
    doRV32BitManipulation
    if(config.extensions.Zba){
      doRV64Zba
    }
    if(config.extensions.Zbb){
      doRV64Zbb
    }
    if(config.extensions.Zbc){
      doRV64Zbc
    }
    if(config.extensions.Zbs){
      doRV64Zbs
    }
  }
  def doRVBitManipulation(): Unit = {
    config.XLEN match {
      case 32 => doRV32BitManipulation
      case 64 => doRV64BitManipulation
    }
  }
}