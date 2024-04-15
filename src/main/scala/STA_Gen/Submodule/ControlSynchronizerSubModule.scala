package STA_Gen.Submodule

import chisel3._

class ControlSynchronizerSubModule extends Module {

  val io = IO(new Bundle {
    val serialInput = Input(Bool())
    val parallelInput = Input(Bool())
    val serialOutput = Output(Bool())
  })

  io.serialOutput := RegNext(Mux(io.parallelInput, true.B, io.serialInput), false.B)

}
