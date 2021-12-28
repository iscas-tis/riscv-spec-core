package rvspeccore.core

import chisel3._
import chisel3.util._

sealed abstract class RVConfig {
  val width: Int
}

case class RV32Config() extends RVConfig {
  val width = 32
}
case class RV64Config() extends RVConfig {
  val width = 64
}
