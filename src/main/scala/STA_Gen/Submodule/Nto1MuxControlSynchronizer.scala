package STA_Gen.Submodule

import chisel3._
import scala.math.{ceil, log10}

class Nto1MuxControlSynchronizer(numberOfTargetInput: Int) extends Module{

  val muxSignalBits: Int = ceil(log10(numberOfTargetInput.toDouble) / log10(2.0)).toInt

  val io = IO(new Bundle {
    val start = Input(Bool())
    val muxSignal = Output(UInt(muxSignalBits.W))
  })


  val subModuleVector: Vector[Nto1MuxControlSynchronizerSubModule] =
    Vector.tabulate(numberOfTargetInput)(x => Module(new Nto1MuxControlSynchronizerSubModule(numberOfTargetInput, numberOfTargetInput - x - 1)))

  for (i <- 0 until numberOfTargetInput)
    subModuleVector(i).io.muxControl := io.start

  for (i <- 0 until numberOfTargetInput - 1)
    subModuleVector(i + 1).io.serialInput := subModuleVector(i).io.serialOutput

  subModuleVector(0).io.serialInput := RegNext(0.U, 0.U)
  io.muxSignal := subModuleVector(numberOfTargetInput - 1).io.serialOutput


}
