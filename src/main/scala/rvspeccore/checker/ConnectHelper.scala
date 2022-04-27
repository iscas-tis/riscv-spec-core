package rvspeccore.checker

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import rvspeccore.core.spec.instset.csr.CSR

abstract class ConnectHelper {}

/** Connect RegFile to io.result.reg by BoringUtils
  */
object ConnectCheckerResult extends ConnectHelper {
  val uniqueIdReg: String = "ConnectCheckerResult-UniqueIdReg"
  val uniqueIdCSR: String = "ConnectCheckerResult-uniqueIdCSR"

  def setRegSource(regVec: Vec[UInt]) = {
    BoringUtils.addSource(regVec, uniqueIdReg)
  }
  def makeCSRSource()(implicit XLEN: Int): CSR = {
    val csr = Wire(CSR())
    csr := DontCare
    BoringUtils.addSource(csr, uniqueIdCSR)
    csr
  }
  def setChecker(checker: CheckerWithResult)(implicit XLEN: Int) = {
    // reg
    val regVec = Wire(Vec(32, UInt(XLEN.W)))
    regVec := DontCare
    BoringUtils.addSink(regVec, uniqueIdReg)

    checker.io.result.reg := regVec
    checker.io.result.pc  := DontCare

    // csr
    val csr = Wire(CSR())
    csr := DontCare
    BoringUtils.addSink(csr, uniqueIdCSR)
    checker.io.result.csr := csr
  }
}
