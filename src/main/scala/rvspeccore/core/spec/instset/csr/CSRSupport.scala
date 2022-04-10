package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

trait CSRSupport extends BaseCore {
  def csrRead(addr: UInt): UInt = {
    require(addr.getWidth == 12)

    val has: Bool = MuxLookup(addr, false.B, now.csr.table.map { case (info, csr) => info.addr -> true.B })
    val nowCSR    = MuxLookup(addr, 0.U, now.csr.table.map { case (info, csr) => info.addr -> csr })

    val rData = Wire(UInt(XLEN.W))

    when(has) {
      rData := nowCSR
    }.otherwise {
      rData := 0.U(XLEN.W)
    }

    rData
  }
  def csrWrite(addr: UInt, data: UInt, mask: UInt = Fill(XLEN, 1.U(1.W))): Unit = {
    require(addr.getWidth == 12)
    require(mask.getWidth == XLEN)

    // common wirte
    val nowCSR  = MuxLookup(addr, 0.U, now.csr.table.map { case (info, csr) => info.addr -> csr })
    val nextCSR = MuxLookup(addr, 0.U, next.csr.table.map { case (info, csr) => info.addr -> csr })

    nextCSR := (nowCSR & ~mask) | (data & mask)
  }
}
