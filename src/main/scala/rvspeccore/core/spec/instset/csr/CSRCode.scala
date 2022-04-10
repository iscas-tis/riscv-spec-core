package rvspeccore.core.spec.instset.csr

import chisel3._
import chisel3.util._

import rvspeccore.core.BaseCore
import rvspeccore.core.spec._
import rvspeccore.core.tool.BitTool._

/** Machine cause register (mcause) values after trap
  *
  *   - riscv-privileged-20191213
  *   - Chapter 3: Machine-Level ISA, Version 1.12
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
