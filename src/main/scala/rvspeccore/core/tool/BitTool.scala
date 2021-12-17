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
  def unpack(dest: Seq[UInt], source: UInt): Unit = {
    if (dest.nonEmpty) {
      val left        = dest.head
      val leftWidth   = left.getWidth
      val sourceWidth = source.getWidth

      if (leftWidth < sourceWidth) {
        left := source(sourceWidth - 1, sourceWidth - leftWidth)
        unpack(dest.tail, source(sourceWidth - leftWidth - 1, 0))
      } else {
        if (leftWidth == sourceWidth) {
          left := source
        } else {
          left := Cat(source, 0.U((leftWidth - sourceWidth).W))
        }
        dest.tail.foreach(x => x := 0.U)
      }
    }
  }
}
