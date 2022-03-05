package rvspeccore.core.spec

import chisel3._
import chisel3.util._

import instset._

case class InstInfo(funct7: Option[UInt], funct3: Option[UInt], instName: String, opcodeName: String)

object InstInfo {
  def apply[A, B](funct7: A, funct3: B, instName: String, opcodeName: String): InstInfo = {
    new InstInfo(
      funct7 match {
        case bits: String => { Some(("b" + bits).U(7.W)) }
        case _            => { None }
      },
      funct3 match {
        case bits: String => { Some(("b" + bits).U(3.W)) }
        case _            => { None }
      },
      instName,
      opcodeName
    )
  }
}

abstract class Insts {
  val table: Seq[InstInfo]

  val exFunct7: Map[String, String] = Map()

  lazy val funct3Map: Map[String, UInt] = table.map(inst => inst.funct3.map(inst.instName -> _)).flatten.toMap
  lazy val funct7Map: Map[String, UInt] = table.map(inst => inst.funct7.map(inst.instName -> _)).flatten.toMap ++
    exFunct7.map { case (name, code) => name -> ("b" + code).U(7.W) }
  lazy val opcodeNameMap: Map[String, String] = table.map(inst => inst.instName -> inst.opcodeName).toMap
}

object RVInsts extends Insts {
  val instSets: Seq[Insts] = List(IBaseInsts, MExtensionInsts)

  val table = instSets.map(_.table).flatten

  override val exFunct7: Map[String, String] = instSets.map(_.exFunct7).reduce(_ ++ _)
}
