# RISC-V Spec Core

本项目是对 RISC-V 处理器 Chisel 设计的指令集一致性进行形式化验证/测试的工具。
其中包括一个可配置的 `RiscvCore` 作为参考模型来表达 RISC-V 指令集规范文档的语义，
和一些 `Helper`、`Checker` 来连接待验证处理器与参考模型和设置验证条件。

其中参考模型支持 RV32/64IMAC、Zicsr、Zifencei，MSU 特权级，虚拟内存机制和 Sv39 页表分页方案。

[English README](README.md)

## 目录 <!-- omit in toc -->

- [用法](#用法)
  - [引入依赖](#引入依赖)
  - [添加 Checker 并设置基本信号](#添加-checker-并设置基本信号)
  - [获取通用寄存器值](#获取通用寄存器值)
  - [获取异常处理与 CSR 寄存器值](#获取异常处理与-csr-寄存器值)
  - [获取 TLB 访存相关信号值](#获取-tlb-访存相关信号值)
  - [获取访存信号值](#获取访存信号值)
- [验证实例](#验证实例)

## 用法

### 引入依赖

作为项目依赖使用 riscv-spec-core，在 `build.sbt` 中添加代码：

```scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.0.0"
```

之后按照下述流程，在处理器中添加代码接入验证。
或者可以直接参考我们给出的[例子](#验证实例)。

### 添加 Checker 并设置基本信号

在待验证处理器的指令提交处，通常是写回级，声明一个 `checker` 变量，并设定参考模型支持的指令子集。

```scala
val FormalConfig = RV64Config("MCS")
val checker = Module(new CheckerWithResult(checkMem = true)(FormalConfig))

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

### 获取异常处理与 CSR 寄存器值

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

### 获取 TLB 访存相关信号值

通常在待验证处理器的TLB模块中获得信号值，需要分析待验证处理器的 TLB 访存状态机。

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

## 验证实例

[nutshell-fv](https://github.com/iscas-tis/nutshell-fv)  
该项目是在 [NutShell](https://github.com/OSCPU/NutShell) 上进行验证的例子。
我们修改了 NutShell 代码以获取验证所需的处理器信息，并与参考模型进行了同步。
最终通过 [ChiselTest](https://github.com/ucb-bar/chiseltest) 提供的接口调用了 BMC 算法进行验证。
