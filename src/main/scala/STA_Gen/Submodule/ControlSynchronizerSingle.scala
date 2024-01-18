package STA_Gen.Submodule

import chisel3._
import chisel3.util.ShiftRegister

class ControlSynchronizerSingle(period: Int) extends Module {

  val io = IO (new Bundle {
    val start = Input(Bool())
    val enable = Output(Bool())
  })

  io.enable := ShiftRegister(io.start, period, false.B, true.B)

}
