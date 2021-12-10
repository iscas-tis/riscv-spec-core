package rvspeccore.core.tool

import chisel3._
import chisel3.util._

object BitTool {
  def signExt(a: UInt, width: Int) = {
    val len     = a.getWidth
    val signBit = a(len - 1)
    if (len >= width) {
      a(width - 1, 0)
    } else {
      Cat(Fill(width - len, signBit), a)
    }
  }
  def zeroExt(a: UInt, width: Int) = {
    val len = a.getWidth
    if (len >= width) {
      a(width - 1, 0)
    } else {
      Cat(0.U((width - len).W), a)
    }
  }
}
