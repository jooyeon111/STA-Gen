package STA_Gen.Submodule

import chisel3._
import chisel3.util.ShiftRegister

class ControlSynchronizerMultiple(period: Int, maintain: Int = 1) extends Module{

//  override val desiredName = s"Control_synchronizer_$maintain"

  val io = IO(new Bundle {
    val start = Input(Bool())
    val enable = Output(Bool())
  })

  val subModuleVector: Vector[ControlSynchronizerMultipleSubModule] =
    Vector.fill(maintain){Module(new ControlSynchronizerMultipleSubModule)}

  for( i <- 0 until maintain)
    subModuleVector(i).io.parallelInput := ShiftRegister(io.start, period - 1, false.B, true.B)

  for( i <- 0 until maintain - 1)
    subModuleVector(i+1).io.serialInput := subModuleVector(i).io.serialOutput


  subModuleVector(0).io.serialInput := RegNext(false.B, false.B)
  io.enable := subModuleVector(maintain - 1).io.serialOutput

}
