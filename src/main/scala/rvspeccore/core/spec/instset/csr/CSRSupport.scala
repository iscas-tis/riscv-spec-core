package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

trait CSRSupport extends BaseCore {
  def csrRead(addr: UInt): UInt = {
    require(addr.getWidth == 12)

    val has:    Bool = MuxLookup(addr, false.B, now.csr.table.map { x => x.info.addr -> true.B })
    val nowCSR: UInt = MuxLookup(addr, 0.U, now.csr.table.map { x => x.info.addr -> x.signal })

    val rData = WireInit(0.U(XLEN.W))

    def doCSRRead(MXLEN: Int): Unit = {
      // common read
      when(has) {
        rData := nowCSR(MXLEN - 1, 0)
      }.otherwise {
        rData := 0.U(MXLEN.W)
      }

      // special read
      switch(addr) {
        is(CSRInfos.mepc.addr) {
          // - 3.1.14 Machine Exception Program Counter (mepc)
          // : If an implementation allows IALIGN to be either 16 or 32 (by
          // : changing CSR misa, for example), then, whenever IALIGN=32, bit
          // : mepc[1] is masked on reads so that it appears to be 0.
          when(now.csr.IALIGN === 32.U(8.W)) {
            rData := Cat(Fill(MXLEN - 2, 1.U(1.W)), 0.U(2.W)) & now.csr.mepc(MXLEN - 1, 0)
          }
        }
      }
    }

    switch(now.csr.MXLEN) {
      is(32.U(8.W)) { doCSRRead(32) }
      is(64.U(8.W)) { if (XLEN >= 64) { doCSRRead(64) } }
    }

    rData
  }
  def csrWrite(addr: UInt, data: UInt, mask: UInt = Fill(XLEN, 1.U(1.W))): Unit = {
    require(addr.getWidth == 12)
    require(mask.getWidth == XLEN)

    // common wirte
    val csrPairs = now.csr.table.zip(next.csr.table)

    csrPairs.foreach { case (CSRInfoSignal(info, nowCSR), CSRInfoSignal(_, nextCSR)) =>
      when(addr === info.addr) {
        if (info.softwareWritable) {
          nextCSR := (nowCSR & ~mask) | (data & mask)
        } else {
          // TODO: might cause some exception?
        }
      }
    }

    // special wirte
    // ...
  }
}

/** unpack mstatus with this Bundle
  *
  * <https://www.chisel-lang.org/chisel3/docs/cookbooks/cookbook#how-do-i-unpack-a-value-reverse-concatenation-like-in-verilog>
  *
  *   - riscv-privileged-20211203
  *   - Chapter 3: Machine-Level ISA, Version 1.12
  *   - 3.1 Machine-Level CSRs
  *   - 3.1.6 Machine Status Registers (mstatus and mstatush)
  *     - Figure 3.6: Machine-mode status register (mstatus) for RV32.
  *     - Figure 3.7: Machine-mode status register (mstatus) for RV64.
  */
class MstatusStruct()(implicit XLEN: Int) extends Bundle {
  // common
  // 31 or 63
  val SD = UInt(1.W)

  // RV32
  // 30 - 23
  val WPRI_30_23 = if (XLEN == 32) UInt(8.W) else null

  // RV64
  // 62 - 23
  val WPRI_62_38 = if (XLEN == 64) UInt(25.W) else null
  val MBE        = if (XLEN == 64) UInt(2.W) else null
  val SBE        = if (XLEN == 64) UInt(2.W) else null
  val SXL        = if (XLEN == 64) UInt(2.W) else null
  val UXL        = if (XLEN == 64) UInt(2.W) else null
  val WPRI_31_23 = if (XLEN == 64) UInt(9.W) else null

  // common
  // 22 - 17
  val TSR  = UInt(1.W)
  val TW   = UInt(1.W)
  val TVM  = UInt(1.W)
  val MXR  = UInt(1.W)
  val SUM  = UInt(1.W)
  val MPRV = UInt(1.W)
  // 16 - 9
  val XS  = UInt(2.W)
  val FS  = UInt(2.W)
  val MPP = UInt(2.W)
  val VS  = UInt(2.W)
  // 8 - 0
  val SPP    = UInt(1.W)
  val MPIE   = UInt(1.W)
  val UBE    = UInt(1.W)
  val SPIE   = UInt(1.W)
  val WPRI_4 = UInt(1.W)
  val MIE    = UInt(1.W)
  val WPRI_2 = UInt(1.W)
  val SIE    = UInt(1.W)
  val WPRI_0 = UInt(1.W)
}
