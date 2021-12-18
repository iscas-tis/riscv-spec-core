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

  /** Working like `Cat`, but only with literal value. Specifically for the
    * situation need literal, like is condition in switch-is.
    */
  def catLit[T <: Bits](a: T, r: T*): UInt = catLit(a :: r.toList)
  def catLit[T <: Bits](r: Seq[T]): UInt = {
    require(r.length >= 1)
    val a = r
      .map(x => (x.litValue, x.getWidth))
      .foldLeft((BigInt(0), 0))((x, y) => ((x._1 << y._2) + y._1, x._2 + y._2))
    a._1.U(a._2.W)
  }
}
