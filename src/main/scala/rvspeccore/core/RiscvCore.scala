package rvspeccore.core

import chisel3._
import chisel3.util._

import spec._
import spec.instset.csr.CSR
import spec.instset.csr.EventSig
import spec.instset.csr.SatpStruct

abstract class BaseCore()(implicit config: RVConfig) extends Module {
  // Define Basic parts
  implicit val XLEN: Int = config.XLEN
  val io = IO(new Bundle {
    val inst  = Input(UInt(32.W))
    val valid = Input(Bool())
    val mem = new MemIO
    val tlb = new TLBIO
    val now  = Output(State())
    val next = Output(State())
    val event = Output(new EventSig)
    val iFetchpc = Output(UInt(XLEN.W))
  })
  // Initial State
  val now  = RegInit(State.wireInit())
  val next = Wire(State())
  val mem = Wire(new MemIO)
  val tlb = Wire(new TLBIO)
  // Global Data
  val global_data = Wire(new GlobalData)
  val priviledgeMode = RegInit(UInt(2.W), 0x3.U)
  val event = Wire(new EventSig)
  val iFetchpc = Wire(UInt(XLEN.W))
}
class GlobalData extends Bundle {
  val setpc    = Bool()
}
class ReadMemIO()(implicit XLEN: Int) extends Bundle {
  val valid    = Output(Bool())
  val addr     = Output(UInt(XLEN.W))
  val memWidth = Output(UInt(log2Ceil(XLEN + 1).W))
  val data     = Input(UInt(XLEN.W))
}

class WriteMemIO()(implicit XLEN: Int) extends Bundle {
  val valid    = Output(Bool())
  val addr     = Output(UInt(XLEN.W))
  val memWidth = Output(UInt(log2Ceil(XLEN + 1).W))
  val data     = Output(UInt(XLEN.W))
}

class MemIO()(implicit XLEN: Int) extends Bundle {
  val read  = new ReadMemIO
  val write = new WriteMemIO
}

class TLBIO()(implicit XLEN: Int) extends Bundle {
  val Anotherread  = Vec(3 + 3, new ReadMemIO())
  val Anotherwrite = Vec(3, new WriteMemIO())
}

class State()(implicit XLEN: Int, config: RVConfig) extends Bundle {
  val reg = Vec(32, UInt(XLEN.W))
  val pc  = UInt(XLEN.W)
  val csr = CSR()
}

object State {
  def apply()(implicit XLEN: Int, config: RVConfig): State = new State
  def wireInit(pcStr: String = "h8000_0000")(implicit XLEN: Int, config: RVConfig): State = {
    val state = Wire(new State)
    state.reg := Seq.fill(32)(0.U(XLEN.W))
    state.pc  := pcStr.U(XLEN.W)
    state.csr := CSR.wireInit()
    state
  }
}

class RiscvCore()(implicit config: RVConfig) extends BaseCore with RVInstSet {
  // should keep the value in the next clock
  // if there no changes below
  // Initial the value of next
  global_data.setpc := false.B
  event := 0.U.asTypeOf(new EventSig)
  iFetchpc  := now.pc
  next := now
  // printf("io.iFetchpc: %x %x\n", io.iFetchpc, iFetchpc)
  // dont read or write mem
  // if there no LOAD/STORE below
  mem := 0.U.asTypeOf(new MemIO)
  tlb := 0.U.asTypeOf(new TLBIO)
  // ID & EXE
  when(io.valid) {
    // printf("PC: %x Inst:%x io.PC:%x \n", now.pc, inst, io.now.pc)
    // printf("io.mem.read.valid:%x addr:%x data:%x\n", io.mem.read.valid, io.mem.read.addr, io.mem.read.data)
    // when(now.pc(1,0) =/= "b00".U & !now.csr.misa(CSR.getMisaExtInt('C'))){
    //   raiseException(0)
    //   next.csr.mtval := now.pc
    // }.otherwise{
    next.csr.cycle := now.csr.cycle + 1.U
    exceptionSupportInit()
    val (resultStatus, resultPC) = if(XLEN == 32) (true.B, now.pc) else iFetchTrans(now.pc)    
    when(resultStatus){
      inst := io.inst
    }.otherwise{
      // printf("[Debug]iFetch Fail and Give NOP:")
      inst := 0.U(XLEN.W) // With a NOP instruction
    }
    iFetchpc := resultPC
    // Decode and Excute
    // Attention: Config(_) "_" means Config(__have_some_value__)
    // Debug
    // printf("Current Inst:%x\n",inst)
    config match {
      case RV32Config(_) => {
        doRV32I
        if (config.M) { doRV32M }
        if (config.C) { doRV32C }
      }
      case RV64Config(_) => {
        doRV64I
        if (config.M) { doRV64M }
        if (config.C) { doRV64C }
      }
    }
    // do without config for now
    doRVPriviledged
    doRVZicsr
    doRVZifencei

    next.reg(0) := 0.U

    when(!global_data.setpc) {
      if (config.C) {
        // + 4.U for 32 bits width inst
        // + 2.U for 16 bits width inst in C extension
        next.pc := now.pc + Mux(inst(1, 0) === "b11".U, 4.U, 2.U)
      } else {
        next.pc := now.pc + 4.U
      }
    }

    tryRaiseException()
    // }
  }

  // mem port
  io.mem <> mem
  io.tlb <> tlb

  // update
  now := next

  // output
  io.now  := now
  io.next := next
  io.event := event
  io.iFetchpc := iFetchpc
}
