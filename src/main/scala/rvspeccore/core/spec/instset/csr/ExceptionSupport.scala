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

/** Supervisor cause register (scause) values after trap
  *
  *   - riscv-privileged-20191213
  *   - Chapter 4: Supervisor-Level ISA, Version 1.12
  *   - 4.1 Supervisor CSRs
  *   - 4.1.8 Supervisor Cause Register (scause)
  *     - Table 4.2: Supervisor cause register (scause) values after trap
  */
object SExceptionCode {
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
  val illegalInstruction = WireInit(false.B)

  def exceptionSupportInit() {
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
  }

  def raiseException(exceptionCode: Int): Unit = {
    printf("[Error]Exception:%d\n",exceptionCode.U)
    def doRaiseException(MXLEN: Int): Unit = {
      // common part
      next.csr.mcause := Cat(false.B, exceptionCode.U((MXLEN - 1).W))
      next.csr.mepc   := now.pc
      next.csr.mtval  := 0.U // : For other traps, mtval is set to zero
      // TODO: modify the exception case
      // special part
      exceptionCode match {
        case MExceptionCode.illegalInstruction => {
          // : illegal-instruction exception occurs, then mtval will contain the shortest of:
          // : * the actual faulting instruction
          // : * the first ILEN bits of the faulting instruction
          // : * the first MXLEN bits of the faulting instruction
          // simply implement it for now
          // FIXME: 实际上 非法指令存的是指令本身 其他的错误并非存储指令到mtval中 其他的也需要改
          when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := io.inst(15, 0) }
            .otherwise { next.csr.mtval := io.inst(31, 0) }
        }
        // 暂时将csr读不存在的寄存器设置为instructionAccessFault TODO: 需要进一步明确
        case MExceptionCode.instructionAccessFault => {
          when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := io.inst(15, 0) }
            .otherwise { next.csr.mtval := io.inst(31, 0) }
        }
        case MExceptionCode.breakpoint => {
          when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := io.inst(15, 0) }
            .otherwise { next.csr.mtval := io.inst(31, 0) }
        }
        case MExceptionCode.environmentCallFromMmode => {
          when(io.inst(1, 0) =/= "b11".U(2.W)) { next.csr.mtval := io.inst(15, 0) }
            .otherwise { next.csr.mtval := io.inst(31, 0) }
        }
        // FIXME:三种非对齐访存 把非必要的Case进行合并
        case MExceptionCode.storeOrAMOAddressMisaligned => {
          next.csr.mtval := io.mem.write.addr
          printf("[Debug]:storeOrAMOAddressMisaligned %x %x\n",io.mem.write.addr,next.csr.mtval)
        }
        case MExceptionCode.loadAddressMisaligned => {
          next.csr.mtval := io.mem.read.addr
          printf("[Debug]:loadAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.mtval)

        }
        case MExceptionCode.instructionAddressMisaligned => {
          // next.csr.mtval := io.mem.read.addr
          printf("[Debug]:instructionAddressMisaligned %x %x\n",io.mem.read.addr,next.csr.mtval)
        }
      }
      printf("Mtvec mode:%x addr:%x\n",now.csr.mtvec(1,0), now.csr.mtvec(MXLEN - 1, 2) << 2)
      // jump
      switch(now.csr.mtvec(1, 0)) {
        is(0.U(2.W)) { 
          // setPc := true.B
          global_data.setpc := true.B
          next.pc := (now.csr.mtvec(MXLEN - 1, 2)) << 2
          printf("NextPC:%x\n", next.pc)
        }
        is(1.U(2.W)) { 
          global_data.setpc := true.B
          next.pc := now.csr.mtvec(MXLEN - 1, 2) + (4 * exceptionCode).U 
          printf("NextPC:%x\n", next.pc)
        }
        // >= 2 reserved
      }
    }

    switch(now.csr.MXLEN) {
      is(32.U(8.W)) { doRaiseException(32) }
      is(64.U(8.W)) { if (XLEN >= 64) { doRaiseException(64) } }
    }
  }

  // TODO: def raise an Interrupt
  // may have Modifier
}
