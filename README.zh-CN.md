# RISC-V Spec Core

本项目是对 RISC-V 处理器 Chisel 设计的指令集一致性进行形式化验证/测试的工具。
其中包括一个可配置的 `RiscvCore` 作为参考模型来表达 RISC-V 指令集规范文档的语义，和一些 `Helper`、`Checker` 来连接待验证处理器与参考模型和设置验证条件。

其中参考模型支持 RV32/64IMC、Zicsr，MSU 特权级，基于 Sv39 页表分页方案的虚拟内存。

[English README](README.md)

## 目录 <!-- omit in toc -->

- [安装](#安装)
  - [使用本地发布版本（推荐）](#使用本地发布版本推荐)
  - [使用托管版本](#使用托管版本)
- [用法](#用法)
  - [添加 Checker 并设置基本信号](#添加-checker-并设置基本信号)
    - [参考模型配置选项](#参考模型配置选项)
  - [通过 ConnectHelper 获取更多信号](#通过-connecthelper-获取更多信号)
    - [获取通用寄存器值](#获取通用寄存器值)
    - [获取异常处理与 CSR 寄存器值](#获取异常处理与-csr-寄存器值)
    - [获取 TLB 访存相关信号值](#获取-tlb-访存相关信号值)
    - [获取访存信号值](#获取访存信号值)
  - [设置验证条件](#设置验证条件)
  - [通过 ChiselTest 调用形式化验证](#通过-chiseltest-调用形式化验证)
- [验证实例](#验证实例)

## 安装

本项目可以作为项目依赖添加在 Chisel 处理器设计中。
可以[使用本地发布版本（推荐）](#使用本地发布版本推荐)或者[使用托管版本](#使用托管版本)。

### 使用本地发布版本（推荐）

由于本项目开发可能造成接口变化，使用本地发布的版本可以自行进行版本控制，避免因为依赖更新造成代码突然无法运行。

下载项目代码并发布到本地：

```shell
git clone https://github.com/iscas-tis/riscv-spec-core.git
cd riscv-spec-core
sbt +publishLocal # 将所有交叉编译的版本发布到本地
# 和 CHA 一起使用请替换为下述命令，以在项目中依赖 CHA 版本的 Chisel：
# sbt -DCHA=true +publishLocal
```

在 `build.sbt` 中添加依赖：

```scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.1-SNAPSHOT"
// CHA 版本请使用：
// libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.1-cha-SNAPSHOT"
```

### 使用托管版本

本项目 main 分支代码会自动发布到 Maven 仓库，也可以直接在项目中添加依赖。
托管版本暂不提供 CHA 版本；SNAPSHOT 版较正式版有新的变动，建议使用 SNAPSHOT 版。
希望锁定依赖版本请[使用本地发布版本（推荐）](#使用本地发布版本推荐)。

在 `build.sbt` 中添加代码：

```scala
resolvers += Resolver.sonatypeRepo("snapshots") // 添加 SNAPSHOT 版本仓库
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "1.1-SNAPSHOT"
```

## 用法

安装成功后按照下述流程，在处理器中添加代码接入验证。
或者可以直接参考我们给出的[例子](#验证实例)。

### 添加 Checker 并设置基本信号

`Checker` 是一个硬件电路模块，其中包含一个 RISC-V 参考模型和预设的指令集一致性性质。
通过工具提供的一些接口和方法，可以将处理器指令执行信息传入 `Checker`，组成一个可验证的系统。

`Checker` 需要获取一条指令的完整执行信息，包括指令本身、PC、更新后的寄存器等，可以将 `Checker` 实例化在指令提交级（如写回级）。

```scala

// 1. 设置 Checker 中参考模型 `RiscvCore` 支持的功能
// 此处配置为：
// RV64I 基础指令集，支持 M、C、Zicsr 指令扩展，支持 M/U 两个特权级在
// 在 misa 寄存器中显示支持 A 扩展，但参考模型实际不支持
// pc 的初始值为 "h0000_8000".U
// 支持特权级和sv39的TLB
import rvspeccore.core.RVConfig
val rvConfig = RVConfig(
  XLEN = 64,
  extensions = "MCZicsrU",
  fakeExtensions = "A",
  initValue = Map("pc" -> "h0000_8000")
  functions = Seq(
    "Privileged",
    "TLB"
  )
)

// 2. 实例化 Checker
// 此处实例化一个检查完整寄存器值的 Checker，启用内存检查，使用之前设置的参考模型设置
import rvspeccore.checker._
val checker = Module(new CheckerWithResult(checkMem = true)(rvConfig))

// 3. 设置指令提交信号
// 当一条指令完全执行结束，所有所需的数据应该准备好，`instCommit.valid` 应该为 true.B
// RiscvCore 将在收到指令后的一个时钟内执行这条指令，得到执行结果
// checker 会在几个时钟内触发性质进行检查
checker.io.instCommit.valid := XXX
checker.io.instCommit.inst  := XXX
checker.io.instCommit.pc    := XXX

// 4. 为信号连接工具 ConnectHelper 设置上文创建的 checker
// 在其他模块中获取的信号将通过 ConnectHelper 传递给 checker
// 此处为 CheckerWithResult 类型 Checker 专用的连接工具 ConnectCheckerResult
import rvspeccore.checker._
ConnectCheckerResult.setChecker(checker)(XLEN, rvConfig)
```

目前 `Checker` 中只有 `CheckerWithResult` 经过了验证，推荐使用。

#### 参考模型配置选项

参考模型具体支持的配置选项如下：

- 位宽 `XLEN: Int`
  - 32、64
- 扩展支持 `extension: String`
  - 默认支持基础指令集 I
  - 扩展指令集
    - "M"：乘除法扩展指令集 M
    - "C"：压缩扩展指令集 C
    - "Zicsr"：TODO
  - 特权级
    - 默认支持包含机器级 M
    - "S"：系统级 S（必须和 "U" 同时开启）
    - "U"：用户级 U
- 额外扩展 `fakeExtensions: String`
  - 仅设置 `misa` 寄存器中显示支持该扩展，参考模型实际不支持，可选 "A"-"Z" 任意字母。
- 初始值 `initValue: Map[String, String]`
  - 设置部分寄存器的初始值，如 `pc`、`mstatus`、`mtvec`。  
    详细支持列表见 [acceptKeys](src/main/scala/rvspeccore/core/RVConfig.scala)
- 功能模块支持 `functions: Seq[String]`
  - "Privileged"：特权级/特权指令功能
  - "TLB"：基于 Sv39 的 TLB
- 形式化验证功能支持 `formal: Seq[String]`
  - "ArbitraryRegFile"：不设置通用寄存器的初始值，使其初始值为任意值（除 x0 寄存器，其值始终为 0）。  
    待验证处理器中可以通过 `ArbitraryRegFile.gen` 获得相同的任意初始值。

### 通过 ConnectHelper 获取更多信号

`ConnectHelper` 封装了一些飞线（`BoringUtils`）方法，可以跨模块获取信号值。
需要注意，获取的所有信号值要和指令提交的时钟同步，可能需要通过 `Reg` 调整。

目前 `ConnectHelper` 只提供了与 `CheckerWithResult` 对应的 `ConnectCheckerResult`，在 `rvspeccore.checker.ConnectCheckerResult`。

#### 获取通用寄存器值

对于 `Vec(32, UInt(XLEN.W))` 类型的 `regFile`，可以直接设置寄存器值：

```scala
ConnectCheckerResult.setRegSource(rf)
```

如果不是，需要自行通过 `Vec` 转换格式：

```scala
val resultRegWire = Wire(Vec(32, UInt(XLEN.W)))
resultRegWire := rf
resultRegWire(0) := 0.U // 该例子中 rf 的 x0 值不总是保持 0，此处手工适配
ConnectCheckerResult.setRegSource(resultRegWire)
```

#### 获取异常处理与 CSR 寄存器值

```scala
// CSR寄存器
val resultCSRWire = ConnectCheckerResult.makeCSRSource()(64, rvConfig)
resultCSRWire.misa      := RegNext(misa)
resultCSRWire.mvendorid := XXX
resultCSRWire.marchid   := XXX
// ······
// 异常处理
val resultEventWire = ConnectCheckerResult.makeEventSource()(64, rvConfig)
resultEventWire.valid := XXX
resultEventWire.intrNO := XXX
resultEventWire.cause := XXX
resultEventWire.exceptionPC := XXX
resultEventWire.exceptionInst := XXX
// ······
```

#### 获取 TLB 访存相关信号值

通常在待验证处理器的 TLB 模块中获得信号值，需要分析待验证处理器的 TLB 访存状态机。

```scala
val resultTLBWire = ConnectCheckerResult.makeTLBSource(if(tlbname == "itlb") false else true)(64)
// 获得访存值
resultTLBWire.read.valid := true.B
resultTLBWire.read.addr  := io.mem.req.bits.addr
resultTLBWire.read.data  := io.mem.resp.bits.rdata
resultTLBWire.read.level := (level-1.U)
// ······
```

#### 获取访存信号值

在待验证处理器对外进行直接访存的时，从中获得相应的值，需要分析待验证处理器的访存状态机。

```scala
val mem = ConnectCheckerResult.makeMemSource()(64)
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

### 设置验证条件

对于形式化验证，可以通过 `assume` 设置验证的前置条件，仅验证满足 `assume` 条件下的情况。
本项目提供了工具来判断指令的种类，当指令属于该分类时返回 `true.B`，示例如下：

```scala
import rvspeccore.checker._
val inst = XXX // 完整 32 位指令

// 要求 inst 是 RVI 指令集中的一条指令，位宽为隐式参数 `implicit XLEN: Int`
assume(RVI(inst))
// 要求 inst 是 RVI 指令集中的一条指令，显式指定位宽为 64
assume(RVI(inst)(64)) 
// 要求 inst 是 RVI 指令集中寄存器和立即数的运算指令
assume(RVI.regImm(inst))
// 要求 inst 是 ADDI 或 ADD 指令
assume(RVI.ADDI(inst) || RVI.ADD(inst))
```

更多指令分类见代码。

### 通过 ChiselTest 调用形式化验证

待验证处理器和参考模型连接之后，可以使用测试方法，也可以使用形式化验证在约束的范围内进行检查。

下面通过 ChiselTest 对连接好参考模型的 `DUT` 进行形式化验证，调用了
[BtorMC](https://github.com/Boolector/btor2tools)
模型检测工具，使用 BMC 算法检查了 12 个时钟周期内的指令集一致性：

```scala
import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec

import dut._

class DUTFormalSpec extends AnyFlatSpec with Formal with ChiselScalatestTester {
  behavior of "DUT"
  it should "pass BMC check" in {
    verify(new DUT(), Seq(BoundedCheck(12), BtormcEngineAnnotation))
  }
}
```

## 验证实例

[nutshell-fv](https://github.com/iscas-tis/nutshell-fv)  
该项目是在 [NutShell](https://github.com/OSCPU/NutShell) 上进行验证的例子。
我们修改了 NutShell 代码以获取验证所需的处理器信息，并与参考模型进行了同步。
最终通过 [ChiselTest](https://github.com/ucb-bar/chiseltest) 提供的接口调用了 BMC 算法进行验证。
