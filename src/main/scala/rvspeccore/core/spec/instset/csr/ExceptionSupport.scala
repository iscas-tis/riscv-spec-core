package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

/** Machine cause register (mcause) values after trap
  *
  *   - riscv-privileged-20211203
  *   - Chapter 3: Machine-Level ISA, Version 1.12
  *   - 3.1 Machine-Level CSRs
  *   - 3.1.15 Machine Cause Register (mcause)
  *     - Table 3.6: Machine cause register (mcause) values after trap
  *     - Table 3.7: Synchronous exception priority in decreasing priority order
  */
object MExceptionCode {
  // - Interrupt True
  // reserved 0
  val supervisorSoftwareInterupt = 1
  // reserved 2
  val machineSoftwareInterrupt = 3
  // reserved 4
  val supervisorTimerInterupt = 5
  // reserved 6
  val machineTimerInterrupt = 7
  // reserved 8
  val supervisorExternalInterrupt = 9
  // reserved 10
  val machineExternalInterrupt = 11
  // reserved 12-15
  // designted for platform use >= 16

  // - Interrupt False (Exception)
  val instructionAddressMisaligned = 0
  val instructionAccessFault       = 1
  val illegalInstruction           = 2
  val breakpoint                   = 3
  val loadAddressMisaligned        = 4
  val loadAccessFault              = 5
  val storeOrAMOAddressMisaligned  = 6
  val storeOrAMOAccessFault        = 7
  val environmentCallFromUmode     = 8
  val environmentCallFromSmode     = 9
  // reserved 10
  val environmentCallFromMmode = 11
  val instructionPageFault     = 12
  val loadPageFault            = 13
  // reserved 14
  val storeOrAMOPageFault = 15
  // reserved or designted for platform use >= 16
}
object Priority {
  val excPriority = Seq(
    MExceptionCode.breakpoint, // TODO: different BP has different priority
    MExceptionCode.instructionPageFault,
    MExceptionCode.instructionAccessFault,
    MExceptionCode.illegalInstruction,
    MExceptionCode.instructionAddressMisaligned,
    MExceptionCode.environmentCallFromMmode,
    MExceptionCode.environmentCallFromSmode,
    MExceptionCode.environmentCallFromUmode,
    MExceptionCode.storeOrAMOAddressMisaligned,
    MExceptionCode.loadAddressMisaligned,
    MExceptionCode.storeOrAMOPageFault,
    MExceptionCode.loadPageFault,
    MExceptionCode.storeOrAMOAccessFault,
    MExceptionCode.loadAccessFault
  )
}

/** Supervisor cause register (scause) values after trap
  *
  *   - riscv-privileged-20191213
  *   - Chapter 4: Supervisor-Level ISA, Version 1.12
  *   - 4.1 Supervisor CSRs
  *   - 4.1.8 Supervisor Cause Register (scause)
  *     - Table 4.2: Supervisor cause register (scause) values after trap
  */
object SExceptionCode {
  // FIXME: not use now
  // - Interrupt True
  // reserved 0
  val supervisorSoftwareInterupt = 1
  // reserved 2-4
  val supervisorTimerInterupt = 5
  // reserved 6-8
  val supervisorExternalInterrupt = 9
  // reserved 10-15
  // designted for platform use >= 16

  // - Interrupt False (Exception)
  val instructionAddressMisaligned = 0
  val instructionAccessFault       = 1
  val illegalInstruction           = 2
  val breakpoint                   = 3
  val loadAddressMisaligned        = 4
  val loadAccessFault              = 5
  val storeOrAMOAddressMisaligned  = 6
  val storeOrAMOAccessFault        = 7
  val environmentCallFromUmode     = 8
  val environmentCallFromSmode     = 9
  // reserved 10-11
  val instructionPageFault = 12
  val loadPageFault        = 13
  // reserved 14
  val storeOrAMOPageFault = 15
  // reserved or designted for platform use >= 16
}

trait ExceptionSupport extends BaseCore {
  def ModeU              = 0x0.U // 00 User/Application
  def ModeS              = 0x1.U // 01 Supervisor
  def ModeR              = 0x2.U // 10 Reserved
  def ModeM              = 0x3.U // 11 Machine
  val raiseExceptionIntr = WireInit(false.B)
  val illegalInstruction = WireInit(false.B)
  // 看看产生的是中断还是异常
  // 仲裁之后的统一执行 尾部折叠判断优先级
  val exceptionVec = WireInit(VecInit(Seq.fill(16)(false.B)))
  val exceptionNO  = MuxCase(0.U(log2Ceil(XLEN).W), Priority.excPriority.map(i => exceptionVec(i) -> i.U(5.W)))
  def exceptionSupportInit() = {
    illegalInstruction := true.B
  }
  def legalInstruction(): Unit = {
    illegalInstruction := false.B
  }

  def tryRaiseException(): Unit = {
    // when M mode
    when(illegalInstruction) {
      raiseException(MExceptionCode.illegalInstruction)
    }
    when(raiseExceptionIntr) {
      event.valid := true.B
      dealExceptionCode()
    }.otherwise {
      event.valid := false.B
    }
  }
  def raiseException(exceptionCode: Int): Unit = {
    exceptionVec(exceptionCode) := true.B
    raiseExceptionIntr          := true.B
  }
  def dealExceptionCode(): Unit = {

    // event.intrNO := exceptionNO
    event.cause         := exceptionNO
    event.exceptionPC   := now.pc
    event.exceptionInst := inst(31, 0)
    // FIXME: 目前仅仅考虑了异常
    val deleg  = now.csr.medeleg
    val delegS = (deleg(exceptionNO)) && (now.internal.privilegeMode < ModeM)
    // TODO: def raise an Interrupt
    // FIXME: 需要对中断做出处理 但是当前只针对异常进行处理
    val mstatusOld = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    val mstatusNew = WireInit(now.csr.mstatus.asTypeOf(new MstatusStruct))
    when(delegS) {
      event.cause                 := next.csr.scause
      mstatusNew.spp              := now.internal.privilegeMode
      mstatusNew.spie             := mstatusOld.sie
      mstatusNew.sie              := false.B
      next.internal.privilegeMode := ModeS
      switch(now.csr.MXLEN) {
        is(32.U(8.W)) { doRaiseExceptionS(exceptionNO, 32) }
        is(64.U(8.W)) { if (XLEN >= 64) { doRaiseExceptionS(exceptionNO, 64) } }
      }
    }.otherwise {
      event.cause                 := next.csr.mcause
      mstatusNew.mpp              := now.internal.privilegeMode
      mstatusNew.mpie             := mstatusOld.mie
      mstatusNew.mie              := false.B
      next.internal.privilegeMode := ModeM
      switch(now.csr.MXLEN) {
        is(32.U(8.W)) { doRaiseExceptionM(exceptionNO, 32) }
        is(64.U(8.W)) { if (XLEN >= 64) { doRaiseExceptionM(exceptionNO, 64) } }
      }
    }
    next.csr.mstatus := mstatusNew.asUInt
    def doRaiseExceptionM(exceptionCode: UInt, MXLEN: Int): Unit = {
      // printf("[Debug]Mtval:%x\n", next.csr.mtval)
      // common part
      next.csr.mcause             := Cat(0.U, zeroExt(exceptionCode, MXLEN - 1))
      next.csr.mepc               := now.pc
      mstatusNew.mpp              := now.internal.privilegeMode
      mstatusNew.mpie             := mstatusOld.mie
      mstatusNew.mie              := false.B
      next.internal.privilegeMode := ModeM // 之前写的大bug
      // FIXME: tva此处写法欠妥
      next.csr.mtval   := 0.U // : For other traps, mtval is set to zero
      next.csr.mstatus := mstatusNew.asUInt
      // TODO: modify the exception case
      // special part
      switch(exceptionCode) {
        is(MExceptionCode.illegalInstruction.U) {
          // FIXME: optionally, NutShell do nothing...
          next.csr.mtval := 0.U
          // when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := io.inst(15, 0) }
          //   .otherwise { next.csr.mtval := io.inst(31, 0) }
        }
        is(MExceptionCode.instructionAccessFault.U) {
          when(inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := inst(15, 0) }
            .otherwise { next.csr.mtval := inst(31, 0) }
        }
        is(MExceptionCode.environmentCallFromMmode.U) {
          next.csr.mtval := 0.U
        }
        is(MExceptionCode.storeOrAMOAddressMisaligned.U) {
          next.csr.mtval := mem.write.addr
          // printf("[Debug]:storeOrAMOAddressMisaligned %x %x\n",io.mem.write.addr,next.csr.mtval)
        }
        is(MExceptionCode.loadAddressMisaligned.U) {
          next.csr.mtval := mem.read.addr
          // printf("[Debug]:loadAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.mtval)
        }
        is(MExceptionCode.instructionAddressMisaligned.U) {
          // next.csr.mtval := io.mem.read.addr
          // printf("[Debug]:instructionAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.mtval)
        }
        is(MExceptionCode.storeOrAMOAccessFault.U) {
          // printf("[Debug]:storeOrAMOAccessFault %x %x\n",io.mem.write.addr,next.csr.mtval)
        }
        is(MExceptionCode.loadPageFault.U) {
          // printf("[Debug]:loadPageFault %x %x\n",io.mem.read.addr,next.csr.mtval)
        }
        is(MExceptionCode.instructionPageFault.U) {
          // printf("[Debug]:instructionPageFault %x %x\n",io.mem.read.addr,next.csr.mtval)
        }
      }
      // printf("Mtvec mode:%x addr:%x\n",now.csr.mtvec(1,0), now.csr.mtvec(MXLEN - 1, 2) << 2)
      // jump
      switch(now.csr.mtvec(1, 0)) {
        is(0.U(2.W)) {
          // setPc := true.B
          global_data.setpc := true.B
          next.pc           := (now.csr.mtvec(MXLEN - 1, 2)) << 2
          // printf("NextPC:%x\n", next.pc)
        }
        is(1.U(2.W)) {
          global_data.setpc := true.B
          next.pc           := now.csr.mtvec(MXLEN - 1, 2) + zeroExt(exceptionCode, MXLEN) << 2
          // printf("NextPC:%x\n", next.pc)
        }
        // >= 2 reserved
      }
    }

    def doRaiseExceptionS(exceptionCode: UInt, MXLEN: Int): Unit = {
      // common part
      next.csr.scause := Cat(false.B, zeroExt(exceptionCode, MXLEN - 1))
      // printf("[Debug]:scause %x, normal %x \n", next.csr.scause, Cat(false.B, zeroExt(exceptionCode, MXLEN - 1)))
      next.csr.sepc               := now.pc
      mstatusNew.spp              := now.internal.privilegeMode
      mstatusNew.spie             := mstatusOld.sie
      mstatusNew.sie              := false.B
      next.internal.privilegeMode := ModeS
      next.csr.stval              := 0.U // : For other traps, mtval is set to zero
      next.csr.mstatus            := mstatusNew.asUInt
      // TODO: modify the exception case
      // special part
      switch(exceptionCode) {
        is(MExceptionCode.illegalInstruction.U) {
          // : illegal-instruction exception occurs, then mtval will contain the shortest of:
          // : * the actual faulting instruction
          // : * the first ILEN bits of the faulting instruction
          // : * the first MXLEN bits of the faulting instruction
          // simply implement it for now
          // FIXME: 实际上 非法指令存的是指令本身 其他的错误并非存储指令到mtval中 其他的也需要改
          when(inst(1, 0) =/= "b11".U(2.W)) { next.csr.stval := inst(15, 0) }
            .otherwise { next.csr.stval := inst(31, 0) }
        }
        is(MExceptionCode.instructionAccessFault.U) {
          when(inst(1, 0) =/= "b11".U(2.W)) { next.csr.stval := inst(15, 0) }
            .otherwise { next.csr.stval := inst(31, 0) }
        }
        // 实际上是S Mode
        is(MExceptionCode.breakpoint.U) {
          when(inst(1, 0) =/= "b11".U(2.W)) { next.csr.stval := inst(15, 0) }
            .otherwise { next.csr.stval := inst(31, 0) }
        }
        is(MExceptionCode.environmentCallFromMmode.U) {
          // FIXME: 很奇怪 把这个删了代码就不能跑了 无法理解
          // FIXME: 实际上已经不存在这种情况了
          // when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.stval := io.inst(15, 0) }
          //   .otherwise { next.csr.stval := io.inst(31, 0) }
          next.csr.stval := 0.U
        }
        // FIXME:三种非对齐访存 把非必要的Case进行合并
        is(MExceptionCode.storeOrAMOAddressMisaligned.U) {
          next.csr.stval := mem.write.addr
          // printf("[Debug]:storeOrAMOAddressMisaligned %x %x\n",io.mem.write.addr,next.csr.stval)
        }
        is(MExceptionCode.loadAddressMisaligned.U) {
          next.csr.stval := mem.read.addr
          // printf("[Debug]:loadAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.stval)

        }
        is(MExceptionCode.instructionAddressMisaligned.U) {
          // next.csr.stval := io.mem.read.addr
          // printf("[Debug]:instructionAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.stval)
        }
      }
      // printf("Stvec mode:%x addr:%x\n",now.csr.stvec(1,0), now.csr.stvec(MXLEN - 1, 2) << 2)
      // jump
      // 还需要使用不同的东西进行跳转
      switch(now.csr.stvec(1, 0)) {
        is(0.U(2.W)) {
          // setPc := true.B
          global_data.setpc := true.B
          next.pc           := (now.csr.stvec(MXLEN - 1, 2)) << 2
          // printf("NextPC:%x\n", next.pc)
        }
        is(1.U(2.W)) {
          global_data.setpc := true.B
          next.pc           := now.csr.stvec(MXLEN - 1, 2) + zeroExt(exceptionCode, MXLEN) << 2
          // printf("NextPC:%x\n", next.pc)
        }
        // >= 2 reserved
      }
    }

  }
}
