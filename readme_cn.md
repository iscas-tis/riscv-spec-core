# RISC-V Spec Core

This project is a framework for formal verification/testing the consistency
between a Chisel-designed RISC-V processor and the instruction set
specification.
Including a configurable `RiscvCore` as a reference model to represent the
semantics of the RISC-V ISA document, as well as several `Helper` and `Checker`
to connect the user's processor design with the reference model and set
verification conditions.

## Usage

### Import Library
To use riscv-spec-core as a managed dependency, add this in your `build.sbt`:

```scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "0.1-SNAPSHOT"
```

### 添加Checker
在待验证处理器的指令提交处，通常是写回级，声明一个checker变量，并设定参考模型支持的指令子集。
```scala
val FormalConfig = RV64Config("MCS")
val checker = Module(new CheckerWithResult(checkMem = true)(FormalConfig))
```
### 引入信号
在待验证的处理器中抽出checker需要的信号，并最终调用setChecker函数，示例如下：
```scala
checker.io.instCommit.valid := XXX
checker.io.instCommit.inst  := XXX
checker.io.instCommit.pc    := XXX
ConnectCheckerResult.setChecker(checker)(XLEN, FormalConfig)
```
### 获取通用寄存器值
```scala
val resultRegWire = Wire(Vec(32, UInt(XLEN.W)))
resultRegWire := rf
resultRegWire(0) := 0.U
ConnectCheckerResult.setRegSource(resultRegWire)
```

### 获取异常处理与CSR寄存器值
```scala
// CSR寄存器
val resultCSRWire = rvspeccore.checker.ConnectCheckerResult.makeCSRSource()(64, FormalConfig)
resultCSRWire.misa      := RegNext(misa)
resultCSRWire.mvendorid := XXX
resultCSRWire.marchid   := XXX
// ······
// 异常处理
val resultEventWire = rvspeccore.checker.ConnectCheckerResult.makeEventSource()(64, FormalConfig)
resultEventWire.valid := XXX
resultEventWire.intrNO := XXX
resultEventWire.cause := XXX
resultEventWire.exceptionPC := XXX
resultEventWire.exceptionInst := XXX
// ······
```


### 获取TLB访存相关信号值
通常在待验证处理器的TLB模块中获得信号值，需要分析待验证处理器的TLB访存状态机。
```scala
val resultTLBWire = rvspeccore.checker.ConnectCheckerResult.makeTLBSource(if(tlbname == "itlb") false else true)(64)
// 获得访存值
resultTLBWire.read.valid := true.B
resultTLBWire.read.addr  := io.mem.req.bits.addr
resultTLBWire.read.data  := io.mem.resp.bits.rdata
resultTLBWire.read.level := (level-1.U)
// ······
```

### 获取访存信号值
在待验证处理器对外进行直接访存的时，从中获得相应的值，需要分析待验证处理器的访存状态机。
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
Specific usage see example [nutshell-fv](https://github.com/iscas-tis/nutshell-fv).
## Verification Example

[nutshell-fv](https://github.com/iscas-tis/nutshell-fv)  
In this example of processor design, we modified the code to get a verifiable
system with reference model.
And then perform formal verification using BMC through ChiselTest.
