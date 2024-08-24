package rvspeccore.core.tool

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec.instset.csr._
import java.awt.print.Book
// TODO: Optimize code writing style

trait LoadStore extends BaseCore {
//   def ModeU     = 0x0.U // 00 User/Application
//   def ModeS     = 0x1.U // 01 Supervisor
//   def ModeR     = 0x2.U // 10 Reserved
//   def ModeM     = 0x3.U // 11 Machine
  def iFetch = 0x0.U
  def Load   = 0x1.U
  def Store  = 0x2.U
  def width2Mask(width: UInt): UInt = {
    MuxLookup(width, 0.U(64.W))(
      Seq(
        8.U  -> "hff".U(64.W),
        16.U -> "hffff".U(64.W),
        32.U -> "hffff_ffff".U(64.W),
        64.U -> "hffff_ffff_ffff_ffff".U(64.W)
      )
    )
  }
  def memRead(addr: UInt, memWidth: UInt): UInt = {
      val bytesWidth = log2Ceil(XLEN / 8)
      val rOff       = addr(bytesWidth - 1, 0) << 3 // addr(byteWidth-1,0) * 8
      val rMask      = width2Mask(memWidth)
      mem.read.valid    := true.B
      mem.read.addr     := addr
      mem.read.memWidth := memWidth
      (mem.read.data >> rOff) & rMask
  }
  def memWrite(addr: UInt, memWidth: UInt, data: UInt): Unit = {
      mem.write.valid    := true.B
      mem.write.addr     := addr
      mem.write.memWidth := memWidth
      mem.write.data     := data
  }
}
