# RISC-V Spec Core

RISC-V 的最小规范实现

## 特点

1. 所有计算在一个时钟内完成，不涉及时序性质
2. 不使用子模块，减少中间状态，分析指令后直接执行
3. 在注释中注明指令执行对应的 [RISC-V Spec](https://riscv.org/technical/specifications/) 原文

## TODO
Add AXI interface
