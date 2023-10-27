# RISC-V Spec Core

This project is a framework for formal verification/testing the consistency
between a Chisel-designed RISC-V processor and the instruction set
specification.
Including a configurable `RiscvCore` as a reference model to represent the
semantics of the RISC-V ISA document, as well as several `Helper` and `Checker`
to connect the user's processor design with the reference model and set
verification conditions.

## Usage

To use riscv-spec-core as a managed dependency, add this in your `build.sbt`:

```scala
libraryDependencies += "cn.ac.ios.tis" %% "riscvspeccore" % "0.1.0"
```

Specific usage see example [nutshell-fv](https://github.com/iscas-tis/nutshell-fv).

## Verification Example

[nutshell-fv](https://github.com/iscas-tis/nutshell-fv)  
In this example of processor design, we modified the code to get a verifiable
system with reference model.
And then perform formal verification using BMC through ChiselTest.
