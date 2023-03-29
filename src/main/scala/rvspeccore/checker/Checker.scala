package rvspeccore.checker

import chisel3._
import chisel3.util._

import rvspeccore.core._
import rvspeccore.core.spec.instset.csr.EventSig
abstract class Checker()(implicit config: RVConfig) extends Module {
  implicit val XLEN: Int = config.XLEN
}

class InstCommit()(implicit XLEN: Int) extends Bundle {
  val valid = Bool()
  val inst  = UInt(32.W)
  val pc    = UInt(XLEN.W)
}
object InstCommit {
  def apply()(implicit XLEN: Int) = new InstCommit
}
class StoreOrLoadInfo(implicit XLEN: Int) extends Bundle {
  val addr = UInt(XLEN.W)
  val data = UInt(XLEN.W)
  val memWidth = UInt(log2Ceil(XLEN + 1).W)
}
class QueueModule(implicit XLEN: Int) extends Module{
  val io = IO (new Bundle{
    val in  = Flipped(Decoupled(new StoreOrLoadInfo()))
    val out = Decoupled(new StoreOrLoadInfo())
  })

  val queue = Queue(io.in, 2)
  io.out <> queue
}
/** Checker with result port.
  *
  * Check pc of commited instruction and next value of all register. Although
  * `pc` in the result port, but it won't be checked.
  */
class CheckerWithResult(checkMem: Boolean = true)(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val result     = Input(State())
    val mem        = if (checkMem) Some(Input(new MemIO)) else None
    val event      = Input(new EventSig())
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst
  if (checkMem) {
    // printf("[specCore] Valid:%x PC: %x Inst: %x\n", specCore.io.valid, specCore.io.now.pc, specCore.io.inst)
    // specCore.io.mem.read.data := { if (checkMem) io.mem.get.read.data else DontCare }

    val LoadQueue  = Module(new QueueModule)
    val StoreQueue  = Module(new QueueModule)
    LoadQueue.io.out.ready := false.B
    StoreQueue.io.out.ready := false.B
    // Load Queue
    val load_push  = Wire(new StoreOrLoadInfo)
    val store_push = Wire(new StoreOrLoadInfo)
    // LOAD
    when(io.mem.get.read.valid){
      LoadQueue.io.in.valid := true.B
      load_push.addr := io.mem.get.read.addr
      load_push.data := io.mem.get.read.data
      load_push.memWidth := io.mem.get.read.memWidth
      LoadQueue.io.in.bits := load_push
      // printf("Load into Queue.... valid: %x %x %x %x\n", LoadQueue.io.in.valid, load_push.addr, load_push.data, load_push.memWidth)
    }.otherwise{
      LoadQueue.io.in.valid := false.B
      load_push.addr := 0.U
      load_push.data := 0.U
      load_push.memWidth := 0.U
      LoadQueue.io.in.bits := load_push
    }
    when(RegNext(specCore.io.mem.read.valid, false.B)){
      // 暴露接口值
      assert(RegNext(LoadQueue.io.out.bits.addr, 0.U)      === RegNext(specCore.io.mem.read.addr, 0.U))
    }
    when(specCore.io.mem.read.valid){
      LoadQueue.io.out.ready := true.B
      // printf("Load out Queue....  valid: %x %x %x %x\n", LoadQueue.io.out.valid, LoadQueue.io.out.bits.addr, LoadQueue.io.out.bits.data, LoadQueue.io.out.bits.memWidth)
      specCore.io.mem.read.data := { if (checkMem) LoadQueue.io.out.bits.data else DontCare }
      // assert(LoadQueue.io.out.bits.addr      === specCore.io.mem.read.addr)
      // assert(LoadQueue.io.out.bits.memWidth  === specCore.io.mem.read.memWidth)
    }.otherwise{
      specCore.io.mem.read.data := 0.U
    }

    // Store 
    when(io.mem.get.write.valid){
      StoreQueue.io.in.valid := true.B
      store_push.addr := io.mem.get.write.addr
      store_push.data := io.mem.get.write.data
      store_push.memWidth := io.mem.get.write.memWidth
      StoreQueue.io.in.bits := store_push
      // printf("Store into Queue.... valid: %x %x %x %x\n", StoreQueue.io.in.valid, store_push.addr, store_push.data, store_push.memWidth)
    }.otherwise{
      StoreQueue.io.in.valid := false.B
      store_push.addr := 0.U
      store_push.data := 0.U
      store_push.memWidth := 0.U
      StoreQueue.io.in.bits := store_push
    }
    when(specCore.io.mem.write.valid){
      StoreQueue.io.out.ready := true.B
      // printf("Store out Queue....  valid: %x %x %x %x\n", StoreQueue.io.out.valid, StoreQueue.io.out.bits.addr, StoreQueue.io.out.bits.data, StoreQueue.io.out.bits.memWidth)
      assert(StoreQueue.io.out.bits.addr      === specCore.io.mem.write.addr)
      assert(StoreQueue.io.out.bits.data      === specCore.io.mem.write.data)
      assert(StoreQueue.io.out.bits.memWidth  === specCore.io.mem.write.memWidth)
    }
  }else{
    specCore.io.mem.read.data := DontCare
  }
  // Initial false default
  when(RegNext(io.instCommit.valid, false.B)) {
    for (i <- 0 until 32) {
      assert(RegNext(io.result.reg(i.U), 0.U) === RegNext(specCore.io.next.reg(i.U), 0.U))
    }  
  }
  // assert in current clock
  when(io.instCommit.valid) {
    // Event Check
    // FIXME: Now or Next ?
    when(io.event.valid) {
      assert(io.event.intrNO === specCore.io.event.intrNO)
      assert(io.event.cause === specCore.io.event.cause)
      assert(io.event.exceptionPC === specCore.io.event.exceptionPC)
      assert(io.event.exceptionInst === specCore.io.event.exceptionInst)
    }
    
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    // for (i <- 0 until 32) {
    //   assert(io.result.reg(i.U) === specCore.io.next.reg(i.U))
    // }
    // next pc: hard to get next pc in a pipeline
    // check it at next instruction

    // next csr
    io.result.csr.table.zip(specCore.io.next.csr.table).map {
      case (result, next) => {
        assert(result.signal === next.signal)
      }
    }

    // io.result.csr.vTable.zip(specCore.io.next.csr.vTable).map {
    //   case (resultSig, nextSig) => {
    //     assert(resultSig === nextSig)
    //   }
    // }

  }
}

class WriteBack()(implicit XLEN: Int) extends Bundle {
  val valid = Bool()
  val dest  = UInt(5.W)
  val data  = UInt(XLEN.W)
}
object WriteBack {
  def apply()(implicit XLEN: Int) = new WriteBack
}

/** Checker with write back port.
  *
  * Check pc of commited instruction and the register been write back.
  */
class CheckerWithWB(checkMem: Boolean = true)(implicit config: RVConfig) extends Checker {
  val io = IO(new Bundle {
    val instCommit = Input(InstCommit())
    val wb         = Input(WriteBack())
    val mem        = if (checkMem) Some(Input(new MemIO)) else None
  })

  // link to spec core
  val specCore = Module(new RiscvCore)
  specCore.io.valid := io.instCommit.valid
  specCore.io.inst  := io.instCommit.inst

  specCore.io.mem.read.data := { if (checkMem) io.mem.get.read.data else DontCare }

  val specCoreWBValid = WireInit(false.B)
  val specCoreWBDest  = WireInit(0.U(5.W))
  for (i <- 0 until 32) {
    when(specCore.io.now.reg(i) =/= specCore.io.next.reg(i)) {
      specCoreWBValid := true.B
      specCoreWBDest  := i.U
    }
  }

  // assert in current clock
  when(io.instCommit.valid) {
    // now pc
    assert(io.instCommit.pc === specCore.io.now.pc)
    // next reg
    when(specCoreWBValid) {
      // prevent DUT not rise a write back
      assert(io.wb.dest === specCoreWBDest)
      assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
    }.otherwise {
      // DUT may try to write back to x0, but it should not take effect
      // if DUT dose write in x0, it will be check out at next instruction
      when(io.wb.valid && io.wb.dest =/= 0.U) {
        assert(io.wb.data === specCore.io.next.reg(io.wb.dest))
      }
    }
    // next pc: no next pc signal in this case
    if (checkMem) {
      assert(io.mem.get.read.valid === specCore.io.mem.read.valid)
      assert(io.mem.get.read.addr === specCore.io.mem.read.addr)
      assert(io.mem.get.read.memWidth === specCore.io.mem.read.memWidth)

      assert(io.mem.get.write.valid === specCore.io.mem.write.valid)
      assert(io.mem.get.write.addr === specCore.io.mem.write.addr)
      assert(io.mem.get.write.memWidth === specCore.io.mem.write.memWidth)
      assert(io.mem.get.write.data === specCore.io.mem.write.data)
    }
  }
}
