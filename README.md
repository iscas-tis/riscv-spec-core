# χRVFormal

This project is a framework for formal verification/testing the consistency
between a Chisel-designed RISC-V processor and the instruction set
specification.
Including a configurable `RiscvCore` as a reference model to represent the
semantics of the RISC-V ISA document, as well as several `Helper` and `Checker`
to connect the user's processor design with the reference model and set
verification conditions.

Reference model support RV32/64IMC, Zicsr, MSU privilege level,
virtual-momory system with Sv39.

Read more detailed Chinese README: [中文说明](README.zh-CN.md).

## Table of Contents <!-- omit in toc -->

- [Usage](#usage)
  - [Import dependency](#import-dependency)
  - [Add Checker and Set Basic Instruction Information](#add-checker-and-set-basic-instruction-information)
  - [General Register](#general-register)
  - [CSR and Exception](#csr-and-exception)
  - [TLB](#tlb)
  - [Memory Access](#memory-access)
- [Verification Example](#verification-example)
- [Publications](#publications)

## Usage

### Import dependency

To use riscv-spec-core as a managed dependency, add this in your `build.sbt`:

```scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.3-SNAPSHOT"
```

Then add verification code in your DUT as the following description.
Or see the [example](#verification-example).

### Add Checker and Set Basic Instruction Information

Instantiation a `checker`, and set the supportted instruction set of reference
module at the instruction commit level.

```scala
val rvConfig = RVConfig(64, "MCS", "A")
val checker = Module(new CheckerWithResult(checkMem = true)(rvConfig))

checker.io.instCommit.valid := XXX
checker.io.instCommit.inst  := XXX
checker.io.instCommit.pc    := XXX

ConnectCheckerResult.setChecker(checker)(XLEN, rvConfig)
```

### General Register

```scala
val resultRegWire = Wire(Vec(32, UInt(XLEN.W)))
resultRegWire := rf
resultRegWire(0) := 0.U
ConnectCheckerResult.setRegSource(resultRegWire)
```

### CSR and Exception

```scala
// CSR
val resultCSRWire = rvspeccore.checker.ConnectCheckerResult.makeCSRSource()(64, rvConfig)
resultCSRWire.misa      := RegNext(misa)
resultCSRWire.mvendorid := XXX
resultCSRWire.marchid   := XXX
// ······
// exception
val resultEventWire = rvspeccore.checker.ConnectCheckerResult.makeEventSource()(64, rvConfig)
resultEventWire.valid := XXX
resultEventWire.intrNO := XXX
resultEventWire.cause := XXX
resultEventWire.exceptionPC := XXX
resultEventWire.exceptionInst := XXX
// ······
```

### TLB

Get signals in TLB of DUT.

```scala
val resultTLBWire = rvspeccore.checker.ConnectCheckerResult.makeTLBSource(if(tlbname == "itlb") false else true)(64)
// memory access in TLB
resultTLBWire.read.valid := true.B
resultTLBWire.read.addr  := io.mem.req.bits.addr
resultTLBWire.read.data  := io.mem.resp.bits.rdata
resultTLBWire.read.level := (level-1.U)
// ······
```

### Memory Access

Get the signal when DUT access memory.

```scala
val mem = rvspeccore.checker.ConnectCheckerResult.makeMemSource()(64)
when(backend.io.dmem.resp.fire) {
    // load or store complete
    when(isRead) {
        isRead       := false.B
        mem.read.valid := true.B
        mem.read.addr  := SignExt(addr, 64)
        mem.read.data  := backend.io.dmem.resp.bits.rdata
        mem.read.memWidth := width
    }.elsewhen(isWrite) {
        isWrite       := false.B
        mem.write.valid := true.B
        mem.write.addr  := SignExt(addr, 64)
        mem.write.data  := wdata
        mem.write.memWidth := width
        // pass addr wdata wmask
    }.otherwise {
        // assert(false.B)
        // may receive some acceptable error resp, but microstructure can handle
    }
}
```

## Verification Example

[nutshell-fv](https://github.com/iscas-tis/nutshell-fv)  
In this example of processor design, we modified the code to get a verifiable
system with reference model.
And then perform formal verification using BMC through ChiselTest.

## Publications

If our work has been helpful to you, please cite:

**SETTA 2024: Formal Verification of RISC-V Processor Chisel Designs** [Link](https://link.springer.com/chapter/10.1007/978-981-96-0602-3_8) | [BibTex](https://citation-needed.springer.com/v2/references/10.1007/978-981-96-0602-3_8?format=bibtex&flavour=citation)
