package rvspeccore.utils

import chisel3._
import chisel3.util._

class RegMem[T <: Data](size: Int, t: T) {
  val regs = RegInit(VecInit(Seq.fill(size)(0.U.asTypeOf(t))))

  def read(idx: UInt) = {
    regs(idx)
  }
  def write(idx: UInt, data: T) = {
    regs(idx) := data
  }
  def write(idx: UInt, data: T, mask: Seq[Bool]) = {
    val accessor = regs(idx).asInstanceOf[Vec[Data]]
    val dataVec  = data.asInstanceOf[Vec[Data]]
    require(accessor.length == dataVec.length)
    require(accessor.length == mask.length)
    for (i <- 0 until mask.length) {
      when(mask(i)) {
        accessor(i) := dataVec(i)
      }
    }
  }

  def apply(idx: UInt) = read(idx)
}

object RegMem {
  def apply[T <: Data](size: Int, t: T) = new RegMem(size, t)
}

class SyncRegMem[T <: Data](size: Int, t: T) extends RegMem(size, t) {
  override def read(idx: UInt) = {
    RegNext(regs(idx))
  }
  def read(idx: UInt, en: Bool) = {
    RegEnable(regs(idx), en)
  }
}

object SyncRegMem {
  def apply[T <: Vec[UInt]](size: Int, t: T) = new SyncRegMem(size, t)
}
