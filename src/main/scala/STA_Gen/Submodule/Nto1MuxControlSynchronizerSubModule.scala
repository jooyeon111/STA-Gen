package STA_Gen.Submodule

import chisel3._

import scala.math.{ceil, log10}

class Nto1MuxControlSynchronizerSubModule(numberOfTargetInput: Int , targetMuxControlNumber: Int) extends Module{

  val muxSignalBits: Int = ceil(log10(numberOfTargetInput.toDouble) / log10(2.0)).toInt

  val io = IO(new Bundle {
    val muxControl = Input(Bool())
    val serialInput = Input(UInt(muxSignalBits.W))
    val serialOutput = Output(UInt(muxSignalBits.W))
  })

  io.serialOutput := RegNext(Mux(io.muxControl, targetMuxControlNumber.U, io.serialInput), 0.U)

}
