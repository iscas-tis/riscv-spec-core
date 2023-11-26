package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._
import rvspeccore.core.RVConfig

trait CSRSupport extends BaseCore with ExceptionSupport {
  // def ModeU     = 0x0.U // 00 User/Application
  // def ModeS     = 0x1.U // 01 Supervisor
  // def ModeR     = 0x2.U // 10 Reserved
  // def ModeM     = 0x3.U // 11 Machine
  val lr = RegInit(Bool(), false.B)
  val VAddrBits = if(XLEN == 32) 32 else 39
  val retTarget = Wire(UInt(VAddrBits.W))
  retTarget := DontCare
  def csrRead(addr: UInt): UInt = {
    // Read the value of special registers
    // CSR addr require 12bit
    require(addr.getWidth == 12)
    val has:    Bool = MuxLookup(addr, false.B)(now.csr.table.map { x => x.info.addr -> true.B })
    val nowCSR: UInt = MuxLookup(addr, 0.U)(now.csr.table.map { x => x.info.addr -> x.signal })
    val rmask:  UInt = MuxLookup(addr, 0.U)(now.csr.table.map { x => x.info.addr -> x.info.rmask })
    printf("[Debug]CSR_READ:(Have:%d, nowCSR:%x, Addr: %x %x)\n",has,nowCSR,addr,next.reg(1))
    val rData = WireInit(0.U(XLEN.W))

    def doCSRRead(MXLEN: Int): Unit = {
      // common read
      when(has) {
        rData := (nowCSR(MXLEN - 1, 0) & rmask)
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
    val has: Bool = MuxLookup(addr, false.B)(now.csr.table.map { x => x.info.addr -> true.B })
    when(has) {
      // require(mask.getWidth == XLEN)
      // common wirte
      val csrPairs = now.csr.table.zip(next.csr.table)
      csrPairs.foreach { case (CSRInfoSignal(info, nowCSR), CSRInfoSignal(_, nextCSR)) =>
        when(addr === info.addr) {
          // 地址是当前寄存器的地址
          printf("[Debug]Find ADDR, %x %x\n", (info.wfn != null).B, (info.wmask != UnwritableMask).B)
          if (info.wfn != null && info.wmask != UnwritableMask) {
            // 且该寄存器可写 使用mask
            nextCSR := info.wfn((nowCSR & ~info.wmask) | (data & info.wmask))
            printf("[Debug]CSR_Write:(Addr: %x, nowCSR: %x, nextCSR: %x)\n", addr, nowCSR, nextCSR)
          } else {
            // TODO: might cause some exception?

          }
        }
      }
    }.otherwise {
      // all unimplemented CSR registers return 0
      printf("[Error]CSR_Write:Not have this reg...\n")
      raiseException(MExceptionCode.illegalInstruction)
    }


    // special wirte
    // ...
  }

  def Mret()(implicit config: RVConfig): Unit = {
    when(priviledgeMode === ModeM) {
      val mstatusOld = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
      val mstatusNew = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
      mstatusNew.mie   := mstatusOld.mpie
      priviledgeMode   := mstatusOld.mpp
      mstatusNew.mpie  := true.B
      printf("MRET Mstatus: %x, Mode: %x\n", mstatusOld.asUInt, priviledgeMode)
      if(config.CSRMisaExtList.exists(s => s == 'U')) {
        mstatusNew.mpp := ModeU
      } else {
        mstatusNew.mpp := ModeM
      }
      next.csr.mstatus := mstatusNew.asUInt
      lr := false.B
      retTarget := next.csr.mepc(VAddrBits-1, 0)
      printf("nextpc1:%x\n",now.csr.mepc)
      global_data.setpc := true.B
      next.pc := now.csr.mepc
      printf("nextpc2:%x\n",next.pc)
    }.otherwise{
      raiseException(MExceptionCode.illegalInstruction)
    }
  }
  def Sret(): Unit = {
    val mstatusOld = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    val mstatusNew = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    // 3.1.6.5 Virtualization Support in mstatus Register
    // The TSR (Trap SRET) bit is a WARL field that supports intercepting the supervisor exception
    // return instruction, SRET. When TSR=1, attempts to execute SRET while executing in S-mode
    // will raise an illegal instruction exception. When TSR=0, this operation is permitted in S-mode.
    // TSR is read-only 0 when S-mode is not supported.
    val illegalSret = priviledgeMode < ModeS
    val illegalSModeSret = priviledgeMode === ModeS && mstatusOld.tsr.asBool
    when(illegalSret || illegalSModeSret){
      raiseException(MExceptionCode.illegalInstruction)
    }.otherwise{
      // FIXME: is mstatus not sstatus ?
      mstatusNew.sie   := mstatusOld.spie
      priviledgeMode   := Cat(0.U(1.W), mstatusOld.spp)
      mstatusNew.spie  := true.B // 正确的
      mstatusNew.spp   := ModeU
      mstatusNew.mprv  := 0x0.U // Volume II P21 " If xPP != M, xRET also sets MPRV = 0 "
      next.csr.mstatus := mstatusNew.asUInt
      lr := false.B
      retTarget := next.csr.sepc(VAddrBits-1, 0)
      printf("nextpc1:%x\n",now.csr.sepc)
      global_data.setpc := true.B
      next.pc := now.csr.sepc
      printf("nextpc2:%x\n",next.pc)
      printf("next mstatus:%x\n", next.csr.mstatus)
    }
  }
}
