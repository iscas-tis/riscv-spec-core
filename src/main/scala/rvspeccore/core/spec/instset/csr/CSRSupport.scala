package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.RVConfig

trait CSRSupport extends BaseCore {
  def ModeU     = 0x0.U // 00 User/Application
  def ModeS     = 0x1.U // 01 Supervisor
  def ModeR     = 0x2.U // 10 Reserved
  def ModeM     = 0x3.U // 11 Machine

  val priviledgeMode = RegInit(UInt(2.W), 0x3.U)
  val lr = RegInit(Bool(), false.B)
  val VAddrBits = if(XLEN == 32) 32 else 39
  val retTarget = Wire(UInt(VAddrBits.W))
  retTarget := DontCare
  def csrRead(addr: UInt): UInt = {
    // Read the value of special registers
    // CSR addr require 12bit
    require(addr.getWidth == 12)
    val has:    Bool = MuxLookup(addr, false.B, now.csr.table.map { x => x.info.addr -> true.B })
    val nowCSR: UInt = MuxLookup(addr, 0.U, now.csr.table.map { x => x.info.addr -> x.signal })
    printf("[Debug]CSR_READ:(Have:%d, nowCSR:%x, Addr: %x)\n",has,nowCSR,addr)
    val rData = WireInit(0.U(XLEN.W))

    def doCSRRead(MXLEN: Int): Unit = {
      // common read
      when(has) {
        rData := nowCSR(MXLEN - 1, 0)
      }.otherwise {
        // all unimplemented CSR registers return 0
        rData := 0.U(MXLEN.W)
      }

      // special read
      switch(addr) {
        is(now.csr.CSRInfos.mepc.addr) {
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
  def csrWrite(addr: UInt, data: UInt): Unit = {
    def UnwritableMask = 0.U(XLEN.W)
    require(addr.getWidth == 12)
    // require(mask.getWidth == XLEN)
    // common wirte
    val csrPairs = now.csr.table.zip(next.csr.table)
    csrPairs.foreach { case (CSRInfoSignal(info, nowCSR), CSRInfoSignal(_, nextCSR)) =>
      when(addr === info.addr) {
        // 地址是当前寄存器的地址
        if (info.wfn != null && info.wmask != UnwritableMask) {
          // 且该寄存器可写 使用mask
          nextCSR := info.wfn((nowCSR & ~info.wmask) | (data & info.wmask))
          printf("[Debug]CSR_Write:(Addr: %x, nowCSR: %x, nextCSR: %x)\n", addr, nowCSR, nextCSR)
        } else {
          // TODO: might cause some exception?
        }
      }
    }

    // special wirte
    // ...
  }

  def Mret()(implicit config: RVConfig): Unit = {
    val mstatusOld = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    val mstatusNew = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    // // mstatusNew.mpp.m := ModeU //TODO: add mode U
    mstatusNew.mie   := mstatusOld.mpie
    priviledgeMode   := mstatusOld.mpp
    mstatusNew.mpie  := true.B
    if(config.CSRMisaExtList.exists(s => s == 'U')) {
      mstatusNew.mpp := ModeU
    } else {
      mstatusNew.mpp := ModeM
    }
    next.csr.mstatus := mstatusNew.asUInt
    lr := false.B
    retTarget := next.csr.mepc(VAddrBits-1, 0)
  }
  def Sret(): Unit = {
    // FIXME: is mstatus not sstatus ?
    val mstatusOld = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    val mstatusNew = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    // mstatusNew.mpp.m := ModeU //TODO: add mode U
    mstatusNew.sie   := mstatusOld.spie
    priviledgeMode   := Cat(0.U(1.W), mstatusOld.spp)
    mstatusNew.spie  := true.B
    mstatusNew.spp   := ModeM // FIXME: is which mode ?
    mstatusNew.mprv  := 0x0.U // Volume II P21 " If xPP != M, xRET also sets MPRV = 0 "
    next.csr.mstatus := mstatusNew.asUInt
    lr := false.B
    retTarget := next.csr.sepc(VAddrBits-1, 0)
  }
}
