package STA_Gen.Submodule

import chisel3._
import chisel3.util.ShiftRegister

class ControlSynchronizer(delay: Int, maintain: Int = 1 ) extends Module{

  assert(delay > 0, "[error] There is no no delay logic")
  assert(maintain > 0, "[error] Signal must maintain at least 1 cycle")

  val io = IO(new Bundle {
    val start = Input(Bool())
    val enable = Output(Bool())
  })

  if(maintain == 1){
    io.enable := ShiftRegister(io.start, delay, false.B, true.B)
  } else {

    val subModuleVector: Vector[ControlSynchronizerSubModule] = Vector.fill(maintain){Module(new ControlSynchronizerSubModule)}

    for( i <- 0 until maintain)
      subModuleVector(i).io.parallelInput := ShiftRegister(io.start, delay - 1, false.B, true.B)

    for( i <- 0 until maintain - 1)
      subModuleVector(i+1).io.serialInput := subModuleVector(i).io.serialOutput


    subModuleVector(0).io.serialInput := RegNext(false.B, false.B)
    io.enable := subModuleVector(maintain - 1).io.serialOutput

  }
}
