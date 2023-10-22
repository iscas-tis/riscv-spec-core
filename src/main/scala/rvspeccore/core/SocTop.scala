package rvspeccore.core
import chisel3._
import chisel3.util._
import rvspeccore.bus.axi4._
import rvspeccore.device.AXI4RAM

class SoCTop(genCore: => RiscvCore)(implicit config: RVConfig) extends Module {
    val io = IO(new Bundle {
        val imem = new AXI4
        val dmem = new AXI4
    })
    val core = Module(genCore)
    val s_i_idle :: s_i_reading :: s_i_readend :: Nil = Enum(3)
    val read_state = RegInit(s_i_idle)
    val pc   = core.io.now.pc - "h8000_0000".U
    // printf("[SoCTop] pc = %d Bool = %d State = %d\n", pc, !reset.asBool, read_state)
    // val inst = Wire(UInt(32.W))
    core.io.valid := false.B
    core.io.mem.read.data := 0.U
    core.io.inst := 0.U
    // core.io.inst := "h00b00193".U
    io.imem.ar.bits.addr  := 0.U
    io.imem.ar.valid      := 0.U
    io.imem.ar.bits.size  := 0.U
    io.imem.ar.bits.len   := 0.U
    io.imem.r.ready       := 0.U
    io.imem.aw.valid      := 0.U
    io.imem.aw.bits.addr  := "h0000_0000".U
    io.imem.aw.bits.size  := 0.U
    io.imem.aw.bits.len   := 0.U
    io.imem.w.bits.data   := 0.U
    io.imem.w.bits.strb   := 0.U
    io.imem.w.bits.last   := 0.U
    io.imem.w.valid       := 0.U
    io.imem.aw.bits.user  := 0.U
    io.imem.aw.bits.burst := 0.U
    io.imem.aw.bits.cache := 0.U
    io.imem.aw.bits.prot  := 0.U
    io.imem.aw.bits.lock  := 0.U
    io.imem.aw.bits.qos   := 0.U
    io.imem.aw.bits.id    := 0.U
    io.imem.ar.bits.prot  := 0.U
    io.imem.ar.bits.burst := 0.U
    io.imem.ar.bits.cache := 0.U
    io.imem.ar.bits.user  := 0.U
    io.imem.ar.bits.qos   := 0.U
    io.imem.ar.bits.lock  := 0.U
    io.imem.ar.bits.id    := 0.U
    io.imem.b.ready       := 0.U

    // dmem
    io.dmem.ar.bits.addr  := 0.U
    io.dmem.ar.valid      := 0.U
    io.dmem.ar.bits.size  := 0.U
    io.dmem.ar.bits.len   := 0.U
    io.dmem.r.ready       := 0.U
    io.dmem.aw.valid      := 0.U
    io.dmem.aw.bits.addr  := "h0000_0000".U
    io.dmem.aw.bits.size  := 0.U
    io.dmem.aw.bits.len   := 0.U
    io.dmem.w.bits.data   := 0.U
    io.dmem.w.bits.strb   := 0.U
    io.dmem.w.bits.last   := 0.U
    io.dmem.w.valid       := 0.U
    io.dmem.aw.bits.user  := 0.U
    io.dmem.aw.bits.burst := 0.U
    io.dmem.aw.bits.cache := 0.U
    io.dmem.aw.bits.prot  := 0.U
    io.dmem.aw.bits.lock  := 0.U
    io.dmem.aw.bits.qos   := 0.U
    io.dmem.aw.bits.id    := 0.U
    io.dmem.ar.bits.prot  := 0.U
    io.dmem.ar.bits.burst := 0.U
    io.dmem.ar.bits.cache := 0.U
    io.dmem.ar.bits.user  := 0.U
    io.dmem.ar.bits.qos   := 0.U
    io.dmem.ar.bits.lock  := 0.U
    io.dmem.ar.bits.id    := 0.U
    io.dmem.b.ready       := 0.U

    // 读-状态机
    switch(read_state) {
        is(s_i_idle) {
            when(!reset.asBool){
                read_state := s_i_reading
            }
        }
        is(s_i_reading) {
            io.imem.ar.bits.id    := 0.U // For inst mem
            io.imem.ar.bits.addr  := pc
            io.imem.ar.bits.size  := 4.U
            io.imem.ar.bits.burst := 1.U
            io.imem.ar.valid      := 1.U
            when(io.imem.ar.ready){
                read_state := s_i_readend
            }
        }
        is(s_i_readend) {
            io.imem.r.ready := 1.U
            when(io.imem.r.fire){
                core.io.valid := true.B
                core.io.inst := io.imem.r.bits.data
                read_state := s_i_idle
            }
        }
    }
}