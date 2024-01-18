package STA_Gen.Submodule

import chisel3._
import chisel3.util.ShiftRegister

class MyShiftRegister(bandwidth: Int, inputSize: Int, depthSize: Int) extends Module{

  require(inputSize >= 1, s"[error] Shift register input size must be at least 1 current value: $inputSize")
  require(depthSize >= 1, s"[error] Shift register depth size must be at least 1 current value: $depthSize")

  val io = IO(new Bundle {

    val input: Vec[SInt] = Input(Vec(inputSize, SInt(bandwidth.W)))
    val output: Vec[SInt] = Output(Vec(inputSize, SInt(bandwidth.W)))
    val shiftEnable: Bool = Input(Bool())

  })

  io.output := ShiftRegister(io.input, depthSize , io.shiftEnable)

}
