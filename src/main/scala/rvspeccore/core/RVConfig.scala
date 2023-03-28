package rvspeccore.core

import chisel3._
import chisel3.util._

sealed abstract class RVConfig(extensions: String) {

  /**   - riscv-spec-20191213
    *   - We use the term XLEN to refer to the width of an integer register in
    *     bits.
    */
  // From Test to define a implicit value RVConfig and Use String to define "M", "C" or not
  val XLEN: Int
  val M: Boolean = extensions.indexOf("M") != -1
  val C: Boolean = extensions.indexOf("C") != -1
  val S: Boolean = extensions.indexOf("S") != -1
  val U: Boolean = extensions.indexOf("U") != -1
  // CSRs Config

  // Misa
  // var CSRMisaExtList = List('A', 'S', 'I', 'U')
  var CSRMisaExtList = List('A', 'I')
  if(M){ CSRMisaExtList = CSRMisaExtList :+ 'M'}
  if(C){ CSRMisaExtList = CSRMisaExtList :+ 'C'}
  if(S){ 
    CSRMisaExtList = CSRMisaExtList :+ 'S'
    if(!U){ CSRMisaExtList = CSRMisaExtList :+ 'U'} 
  }
  if(U){ CSRMisaExtList = CSRMisaExtList :+ 'U'} 

}

case class RV32Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 32
}
case class RV64Config(extensions: String = "") extends RVConfig(extensions) {
  val XLEN = 64
}
