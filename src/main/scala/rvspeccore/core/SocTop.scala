package rvspeccore.core
import chisel3._
import chisel3.util._
import rvspeccore.bus.axi4._
import rvspeccore.device.AXI4RAM

class SoCTop(genCore: => RiscvCore)(implicit config: RVConfig) extends Module {
    val io = IO(new Bundle {
        val mem = new AXI4
    })
    val core = Module(genCore)
    val pc   = core.io.now.pc - "h8000_0000".U
    printf("[SoCTop] pc = %d\n", pc)
    // val inst = Wire(UInt(32.W))
    
    core.io.valid := !reset.asBool
    core.io.mem.read.data := 0.U
    core.io.inst := "h00b00193".U
    io.mem.ar.bits.addr  := 0.U
    io.mem.ar.valid      := 0.U
    io.mem.ar.bits.size  := 0.U
    io.mem.ar.bits.len   := 0.U
    io.mem.r.ready       := 0.U
    io.mem.aw.valid      := 0.U
    io.mem.aw.bits.addr  := "h0000_0000".U
    io.mem.aw.bits.size  := 4.U
    io.mem.aw.bits.len   := 1.U
    io.mem.w.bits.data   := 0.U
    io.mem.w.bits.strb   := 0.U
    io.mem.w.bits.last   := 0.U
    io.mem.w.valid       := 0.U
    io.mem.aw.bits.user  := 0.U
    io.mem.aw.bits.burst := 0.U
    io.mem.aw.bits.cache := 0.U
    io.mem.aw.bits.prot  := 0.U
    io.mem.aw.bits.lock  := 0.U
    io.mem.aw.bits.qos   := 0.U
    io.mem.aw.bits.id    := 0.U
    io.mem.ar.bits.prot  := 0.U
    io.mem.ar.bits.burst := 0.U
    io.mem.ar.bits.cache := 0.U
    io.mem.ar.bits.user  := 0.U
    io.mem.ar.bits.qos   := 0.U
    io.mem.ar.bits.lock  := 0.U
    io.mem.ar.bits.id    := 0.U
    io.mem.b.ready       := 0.U
}