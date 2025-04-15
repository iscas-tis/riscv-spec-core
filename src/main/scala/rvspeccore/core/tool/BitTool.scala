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

  /** Reorder bits in `source` by the index in dest.
    *
    * @example
    *
    * {{{
    *   reorder(5, (4, 2), (7, 6))(inst, 12, (6, 2)) // it like:
    *   Cat(inst(3, 2), inst(12), inst(6, 4), 0.U(2.W))
    * }}}
    *
    * @param a
    *   index of dest
    * @param source
    * @param b
    *   index of source
    * @return
    *   Catted bits
    */
  def reorder[T <: Bits](a: Any*)(source: T, b: Any*): UInt = {
    def unfold(x: Any): List[Int] = {
      x match {
        case (l: Int, r: Int) => (l to r by -1).toList
        case y: Int           => List(y)
        case _ => {
          require(false, "The type of index need be Int or Tuple2[Int, Int]")
          List(0)
        }
      }
    }

    val aList = a.toList.map(unfold(_)).flatten
    val bList = b.toList.map(unfold(_)).flatten
    require(aList.size == bList.size)
    require(aList.distinct.size == aList.size)
    require(bList.max < source.getWidth)

    val order = aList.zip(bList).sortBy(_._1)(Ordering.Int.reverse)
    require(order.head._1 - order.last._1 + 1 == order.size)

    if (order.last._1 == 0) {
      Cat(order.map(x => source(x._2)))
    } else {
      Cat(Cat(order.map(x => source(x._2))), 0.U(order.last._1.W))
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
