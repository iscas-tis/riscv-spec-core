package rvspeccore.core.spec.instset

import chisel3._
import chisel3.util._
import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.tool.CheckTool

/** "B" Extension for Bit Manipulation, Version 1.0.0
  *
  *   - riscv-spec-20240411
  *   - Chapter 28: "B" Extension for Bit Manipulation, Version 1.0.0
  *   - 28.5. Instructions (in alphabetical order)
  */
trait BExtensionInsts {
  val add_uw = Inst("b0000100_?????_?????_000_?????_0111011")
  val andn   = Inst("b0100000_?????_?????_111_?????_0110011")
  val bclr   = Inst("b0100100_?????_?????_001_?????_0110011")
  val bclri = Inst(
    32 -> "b0100100_?????_?????_001_?????_0010011",
    64 -> "b010010_??????_?????_001_?????_0010011"
  )
  val bext = Inst("b0100100_?????_?????_101_?????_0110011")
  val bexti = Inst(
    32 -> "b0100100_?????_?????_101_?????_0010011",
    64 -> "b010010_??????_?????_101_?????_0010011"
  )
  val binv = Inst("b0110100_?????_?????_001_?????_0110011")
  val binvi = Inst(
    32 -> "b0110100_?????_?????_001_?????_0010011",
    64 -> "b011010_??????_?????_001_?????_0010011"
  )
  val bseti = Inst(
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
  val rev8 = Inst(
    32 -> "b011010011000_?????_101_?????_0010011",
    64 -> "b011010111000_?????_101_?????_0010011"
  )
  val rev_b = Inst("b011010000111_?????_101_?????_0010011")
  val rol   = Inst("b0110000_?????_?????_001_?????_0110011")
  val rolw  = Inst("b0110000_?????_?????_001_?????_0111011")
  val ror   = Inst("b0110000_?????_?????_101_?????_0110011")
  val rori = Inst(
    32 -> "b0110000_?????_?????_101_?????_0010011",
    64 -> "b011000_??????_?????_101_?????_0010011"
  )
  val roriw     = Inst("b0110000_?????_?????_101_?????_0011011")
  val rorw      = Inst("b0110000_?????_?????_101_?????_0111011")
  val sext_b    = Inst("b0110000_00100_?????_001_?????_0010011")
  val sext_h    = Inst("b0110000_00101_?????_001_?????_0010011")
  val sh1add    = Inst("b0010000_?????_?????_010_?????_0110011")
  val sh1add_uw = Inst("b0010000_?????_?????_010_?????_0111011")
  val sh2add    = Inst("b0010000_?????_?????_100_?????_0110011")
  val sh2add_uw = Inst("b0010000_?????_?????_100_?????_0111011")
  val sh3add    = Inst("b0010000_?????_?????_110_?????_0110011")
  val sh3add_uw = Inst("b0010000_?????_?????_110_?????_0111011")
  val slli_uw   = Inst("b000010_??????_?????_001_?????_0011011")
  val unzip     = Inst("b0000100_01111_?????_101_?????_0010011")
  val xnor      = Inst("b0100000_?????_?????_100_?????_0110011")
  val xperm_b   = Inst("b0010100_?????_?????_100_?????_0110011")
  val xperm_n   = Inst("b0010100_?????_?????_010_?????_0110011")
  val zext_h = Inst(
    32 -> "b0000100_00000_?????_100_?????_0110011",
    64 -> "b0000100_00000_?????_100_?????_0111011"
  )
  val zip = Inst("b0000100_01111_?????_001_?????_0010011")
}

/** "B" Extension for Bit Manipulation, Version 1.0.0
  *
  *   - riscv-spec-20240411
  *   - Chapter 28: "B" Extension for Bit Manipulation, Version 1.0.0
  *   - 28.4. Extensions
  */
trait BExtension extends BaseCore with CommonDecode with BExtensionInsts with CheckTool {

  /** Function to select the appropriate bit width based on XLEN */
  def getRotationShamt(value: UInt, xlen: Int): UInt = {
    value(if (xlen == 32) 4 else 5, 0).asUInt
  }

  def xpermb_lookup(idx: UInt, lut: UInt): UInt = {
    val shiftAmt = Cat(idx, 0.U(3.W))
    ((lut >> shiftAmt)(7, 0)).asUInt
  }

  def xpermn_lookup(idx: UInt, lut: UInt): UInt = {
    val shiftAmt = Cat(idx, 0.U(2.W))
    ((lut >> shiftAmt)(3, 0)).asUInt
  }

  /** Address generation
    *
    *   - riscv-spec-20240411 P212
    *   - 28.4.1. Zba: Address generation
    */
  def doRV32Zba: Unit = {
    when(sh1add(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 1); updateNextWrite(rd)
    }
    when(sh2add(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 2); updateNextWrite(rd)
    }
    when(sh3add(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (now.reg(rs1) << 3); updateNextWrite(rd)
    }
  }

  /** Basic bit-manipulation
    *
    *   - riscv-spec-20240411 P212
    *   - 28.4.2. Zbb: Basic bit-manipulation
    */
  def doRV32Zbb: Unit = {
    // scalafmt: { maxColumn = 200 }
    doCommonRV32ZbkbZbb
    // Count leading/trailing zero bits
    when(clz(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := Mux(now.reg(rs1) === 0.U, XLEN.U, PriorityEncoder(now.reg(rs1).asBools.reverse)); updateNextWrite(rd) }
    when(ctz(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := Mux(now.reg(rs1) === 0.U, XLEN.U, PriorityEncoder(now.reg(rs1).asBools)); updateNextWrite(rd) }
    // Count population
    when(cpop(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := PopCount(now.reg(rs1)); updateNextWrite(rd) }
    // Integer minimum/maximum
    when(max(inst))  { decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, now.reg(rs2), now.reg(rs1)); updateNextWrite(rd) }
    when(maxu(inst)) { decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := Mux(now.reg(rs1).asUInt < now.reg(rs2).asUInt, now.reg(rs2), now.reg(rs1)); updateNextWrite(rd) }
    when(min(inst))  { decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := Mux(now.reg(rs1).asSInt < now.reg(rs2).asSInt, now.reg(rs1), now.reg(rs2)); updateNextWrite(rd) }
    when(minu(inst)) { decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := Mux(now.reg(rs1).asUInt < now.reg(rs2).asUInt, now.reg(rs1), now.reg(rs2)); updateNextWrite(rd) }
    // Sign- and zero-extension
    when(sext_b(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := signExt(now.reg(rs1)(7, 0), XLEN); updateNextWrite(rd) }
    when(sext_h(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := signExt(now.reg(rs1)(15, 0), XLEN); updateNextWrite(rd) }
    when(zext_h(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := zeroExt(now.reg(rs1)(15, 0), XLEN); updateNextWrite(rd) }
    // OR Combine
    when(orc_b(inst)) {
      decodeR;
      checkSrcImm(rs1);
      val byteResults = VecInit(Seq.fill(XLEN / 8)(0.U(8.W)))
      for (i <- 0 until XLEN by 8) {
        val byte = now.reg(rs1)(i + 7, i)
        byteResults(i / 8) := Mux(byte.orR, 0xff.U(8.W), 0x00.U(8.W))
      }
      next.reg(rd) := byteResults.asUInt
      updateNextWrite(rd)
    }
    // scalafmt: { maxColumn = 120 } (back to defaults)
  }

  /** Carry-less multiplication
    *
    *   - riscv-spec-20240411 P214
    *   - 28.4.3. Zbc: Carry-less multiplication
    */
  def doRV32Zbc: Unit = {
    doCommonRV32ZbkcZbc
    when(clmulr(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2)
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      for (i <- 0 until XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i) := now.reg(rs1) >> (XLEN - i - 1)
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
      updateNextWrite(rd)
    }

  }

  /** Single-bit instructions
    *
    *   - riscv-spec-20240411 P215
    *   - 28.4.4. Zbs: Single-bit instructions
    */
  def doRV32Zbs: Unit = {
    when(bclr(inst)) {
      decodeR; checkSrcReg(rs1, rs2);
      next.reg(rd) := now.reg(rs1) & ~((1.U << getRotationShamt(now.reg(rs2), XLEN)).asUInt)
      updateNextWrite(rd)
    }
    when(bclri(inst)) {
      decodeI; checkSrcImm(rs1); next.reg(rd) := now.reg(rs1) & ~((1.U << getRotationShamt(imm, XLEN)).asUInt)
      updateNextWrite(rd)
    }
    when(bext(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := (now.reg(rs1) >> getRotationShamt(now.reg(rs2), XLEN)) & 1.U;
      updateNextWrite(rd)
    }
    when(bexti(inst)) {
      decodeI; checkSrcImm(rs1); next.reg(rd) := (now.reg(rs1) >> getRotationShamt(imm, XLEN)) & 1.U;
      updateNextWrite(rd)
    }
    when(binv(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs1) ^ (1.U << getRotationShamt(now.reg(rs2), XLEN))
      updateNextWrite(rd)
    }
    when(binvi(inst)) {
      decodeI; checkSrcImm(rs1); next.reg(rd) := now.reg(rs1) ^ (1.U << getRotationShamt(imm, XLEN))
      updateNextWrite(rd)
    }
    when(bset(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs1) | (1.U << getRotationShamt(now.reg(rs2), XLEN))
      updateNextWrite(rd)
    }
    when(bseti(inst)) {
      decodeI; checkSrcImm(rs1); next.reg(rd) := now.reg(rs1) | (1.U << getRotationShamt(imm, XLEN))
      updateNextWrite(rd)
    }
  }

  def doRV64Zba(): Unit = {
    doRV32Zba
    when(add_uw(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + zeroExt(now.reg(rs1)(31, 0), XLEN)
      updateNextWrite(rd)
    }
    when(sh1add_uw(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN) << 1)
      updateNextWrite(rd)
    }
    when(sh2add_uw(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN) << 2)
      updateNextWrite(rd)
    }
    when(sh3add_uw(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs2) + (zeroExt(now.reg(rs1)(31, 0), XLEN) << 3)
      updateNextWrite(rd)
    }
    when(slli_uw(inst)) {
      decodeI; checkSrcImm(rs1); next.reg(rd) := zeroExt(now.reg(rs1)(31, 0), XLEN) << imm(5, 0)
      updateNextWrite(rd)
    }
    // pseudoinstructions: zext.w rd, rs(Add unsigned word)
  }
  def doRV64Zbb(): Unit = {
    // scalafmt: { maxColumn = 200 }
    doRV32Zbb
    doCommonRV64ZbkbZbb
    // Count leading/trailing zero bits
    when(clzw(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := Mux(now.reg(rs1) === 0.U, 32.U, PriorityEncoder(now.reg(rs1)(31, 0).asBools.reverse)); updateNextWrite(rd) }
    when(ctzw(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := Mux(now.reg(rs1) === 0.U, 32.U, PriorityEncoder(now.reg(rs1)(31, 0).asBools)); updateNextWrite(rd) }
    // Count population
    when(cpopw(inst)) { decodeI; checkSrcImm(rs1); next.reg(rd) := PopCount(now.reg(rs1)(31, 0)); updateNextWrite(rd) }

    // scalafmt: { maxColumn = 120 } (back to defaults)
  }
  def doRV64Zbc(): Unit = {
    doRV32Zbc
  }
  def doRV64Zbs(): Unit = {
    doRV32Zbs
  }

  /** Bit-manipulation for Cryptography
    *
    *   - riscv-spec-20240411 P215
    *   - 28.4.5. Zbkb: Bit-manipulation for Cryptography
    */
  def doRV32Zbkb: Unit = {
    doCommonRV32ZbkbZbb
    when(pack(inst)) {
      decodeR; checkSrcReg(rs1, rs2);
      next.reg(rd) := now.reg(rs2)(((XLEN >> 1) - 1), 0) << (XLEN / 2) | now.reg(rs1)(((XLEN >> 1) - 1), 0)
      updateNextWrite(rd)
    }
    when(packh(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := zeroExt((now.reg(rs2)(7, 0) << 8) | now.reg(rs1)(7, 0), XLEN);
      updateNextWrite(rd)
    }
    when(rev_b(inst)) {
      decodeR;
      checkSrcImm(rs1)
      var result = 0.U(XLEN.W)
      for (i <- 0 until XLEN by 8) {
        val swapped = Reverse(now.reg(rs1)(i + 7, i))
        result = (result | (swapped << i)).asUInt
      }
      next.reg(rd) := result
      updateNextWrite(rd)
    }
    when(zip(inst) && (XLEN.U === 32.U)) {
      decodeR;
      var result = 0.U(XLEN.W)
      checkSrcImm(rs1);
      for (i <- 0 until XLEN / 2) {
        val lower = now.reg(rs1)(i)            // 低 halfSize 位的第 i 位
        val upper = now.reg(rs1)(i + XLEN / 2) // 高 halfSize 位的第 i 位
        result = (result | (upper << ((i << 1) + 1)) | (lower << (i << 1))).asUInt
      }
      next.reg(rd) := result;
      updateNextWrite(rd)
    }
    when(unzip(inst) && (XLEN.U === 32.U)) {
      decodeR;
      var result = 0.U(XLEN.W)
      checkSrcImm(rs1);
      for (i <- 0 until XLEN / 2) {
        val lower = now.reg(rs1)(i << 1)
        val upper = now.reg(rs1)((i << 1) + 1)
        result = (result | (upper << (i + XLEN / 2)) | (lower << i)).asUInt
      }
      next.reg(rd) := result;
      updateNextWrite(rd)
    }

  }

  /** Carry-less multiplication for Cryptography
    *
    *   - riscv-spec-20240411 P216
    *   - 28.4.6. Zbkc: Carry-less multiplication for Cryptography
    */
  def doRV32Zbkc(): Unit = {
    doCommonRV32ZbkcZbc
  }

  /** Crossbar permutations
    *
    *   - riscv-spec-20240411 P216
    *   - 28.4.7. Zbkx: Crossbar permutations
    */
  def doRV32Zbkx: Unit = {
    when(xperm_b(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2)
      var result = 0.U(XLEN.W)
      for (i <- 0 until XLEN by 8) {
        val index    = now.reg(rs2)(i + 7, i)
        val bitValue = xpermb_lookup(index, now.reg(rs1))
        result = (result | (bitValue << i)).asUInt
      }
      next.reg(rd) := result
      updateNextWrite(rd)
    }
    when(xperm_n(inst)) {
      decodeR;
      var result = 0.U(XLEN.W)
      checkSrcReg(rs1, rs2)
      for (i <- 0 until XLEN by 4) {
        val index    = now.reg(rs2)(i + 3, i)
        val bitValue = xpermn_lookup(index, now.reg(rs1))
        result = (result | (bitValue << i)).asUInt
      }
      next.reg(rd) := result
      updateNextWrite(rd)
    }
  }

  def doRV64Zbkb(): Unit = {
    doRV32Zbkb
    doCommonRV64ZbkbZbb
    when(packw(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := signExt((now.reg(rs2)(15, 0) << 16) | now.reg(rs1)(15, 0), XLEN);
      updateNextWrite(rd)
    }
  }

  def doCommonRV32ZbkbZbb(): Unit = {
    // Logical with negate
    when(andn(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs1) & (~now.reg(rs2)).asUInt; updateNextWrite(rd)
    }
    when(orn(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := now.reg(rs1) | (~now.reg(rs2)).asUInt; updateNextWrite(rd)
    }
    when(xnor(inst)) {
      decodeR; checkSrcReg(rs1, rs2); next.reg(rd) := (~(now.reg(rs1) ^ now.reg(rs2))).asUInt; updateNextWrite(rd)
    }
    // Bitwise rotation
    when(rol(inst)) {
      decodeR; checkSrcReg(rs1, rs2);
      next.reg(rd) := (now.reg(rs1) << getRotationShamt(now.reg(rs2), XLEN)) | (now.reg(
        rs1
      ) >> (XLEN.U - getRotationShamt(now.reg(rs2), XLEN))); updateNextWrite(rd)
    }
    when(ror(inst)) {
      decodeR; checkSrcReg(rs1, rs2);
      next.reg(rd) := (now.reg(rs1) >> getRotationShamt(now.reg(rs2), XLEN)) | (now.reg(
        rs1
      ) << (XLEN.U - getRotationShamt(now.reg(rs2), XLEN))); updateNextWrite(rd)
    }
    when(rori(inst)) {
      decodeI; checkSrcImm(rs1);
      next.reg(rd) := (now.reg(rs1) >> getRotationShamt(imm, XLEN)) | (now
        .reg(rs1) << (XLEN.U - getRotationShamt(imm, XLEN))); updateNextWrite(rd)
    }
    // Byte-reverse
    when(rev8(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2);
      var result = 0.U(XLEN.W)
      var j      = XLEN - 8
      for (i <- 0 until XLEN by 8) {
        result = result | (now.reg(rs1)(j + 7, j) << i).asUInt
        j -= 8
      }
      next.reg(rd) := result
      updateNextWrite(rd)
    }
  }

  def doCommonRV64ZbkbZbb(): Unit = {
    // Bitwise rotation
    // rori(64Bit) has been implemented in RV32Zbb
    when(rolw(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2);
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result   = ((rs1_data << now.reg(rs2)(4, 0)).asUInt | (rs1_data >> (32.U - now.reg(rs2)(4, 0))).asUInt)
      next.reg(rd) := signExt(result(31, 0), XLEN)
      updateNextWrite(rd)
    }
    when(roriw(inst)) {
      decodeI;
      checkSrcImm(rs1);
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result   = (rs1_data >> imm(4, 0)).asUInt | (rs1_data << (32.U - imm(4, 0))).asUInt
      next.reg(rd) := signExt(result(31, 0), XLEN)
      updateNextWrite(rd)
    }
    when(rorw(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2);
      val rs1_data = zeroExt(now.reg(rs1)(31, 0), XLEN)
      val result   = (rs1_data >> now.reg(rs2)(4, 0)).asUInt | (rs1_data << (32.U - now.reg(rs2)(4, 0))).asUInt
      next.reg(rd) := signExt(result(31, 0), XLEN)
      updateNextWrite(rd)
    }
  }

  def doCommonRV32ZbkcZbc(): Unit = {
    when(clmul(inst)) {
      decodeR;
      checkSrcReg(rs1, rs2);
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      for (i <- 0 until XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i) := now.reg(rs1) << i
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
      updateNextWrite(rd)
    }
    when(clmulh(inst)) {
      decodeR;
      val partialResults = VecInit(Seq.fill(XLEN)(0.U(XLEN.W)))
      checkSrcReg(rs1, rs2);
      for (i <- 1 to XLEN) {
        when(((now.reg(rs2) >> i.U) & 1.U) > 0.U) {
          partialResults(i - 1) := now.reg(rs1) >> (XLEN - i)
        }
      }
      next.reg(rd) := partialResults.reduce(_ ^ _)
      updateNextWrite(rd)
    }
  }

  def doRV64Zbkc(): Unit = {
    doRV32Zbkc
  }
  def doRV64Zbkx(): Unit = {
    doRV32Zbkx
  }

  def doRV32B: Unit = {
    if (config.extensions.Zba) {
      doRV32Zba
    }
    if (config.extensions.Zbb) {
      doRV32Zbb
    }
    if (config.extensions.Zbc) {
      doRV32Zbc
    }
    if (config.extensions.Zbs) {
      doRV32Zbs
    }
    if (config.extensions.Zbkb) {
      doRV32Zbkb
    }
    if (config.extensions.Zbkc) {
      doRV32Zbkc
    }
    if (config.extensions.Zbkx) {
      doRV32Zbkx
    }
  }

  def doRV64B: Unit = {
    doRV32B
    if (config.extensions.Zba) {
      doRV64Zba
    }
    if (config.extensions.Zbb) {
      doRV64Zbb
    }
    if (config.extensions.Zbc) {
      doRV64Zbc
    }
    if (config.extensions.Zbs) {
      doRV64Zbs
    }
    if (config.extensions.Zbkb) {
      doRV64Zbkb
    }
    if (config.extensions.Zbkc) {
      doRV64Zbkc
    }
    if (config.extensions.Zbkx) {
      doRV64Zbkx
    }
  }
  def doRVB(): Unit = {
    config.XLEN match {
      case 32 => doRV32B
      case 64 => doRV64B
    }
  }
}
