package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

trait CSRSupport extends BaseCore {
  def csrRead(addr: UInt): UInt = {
    require(addr.getWidth == 12)

    val has:    Bool = MuxLookup(addr, false.B, now.csr.table.map { case (info, csr) => info.addr -> true.B })
    val nowCSR: UInt = MuxLookup(addr, 0.U, now.csr.table.map { case (info, csr) => info.addr -> csr })

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

    csrPairs.foreach { case ((info, nowCSR), (_, nextCSR)) =>
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
