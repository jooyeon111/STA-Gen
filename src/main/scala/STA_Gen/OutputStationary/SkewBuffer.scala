package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.ShiftRegister

class SkewBuffer(arrayDimension: Int, blockDimension: Int, vectorSize: Int) extends Module {

  require(arrayDimension >= 1, "[error] Array row must be at least 1")
  require(blockDimension >= 1, "[error] Array col must be at least 1")
  require(vectorSize >= 1, "[error] Block row must be at least 1")

  val numberOfPorts: Int = arrayDimension * blockDimension * vectorSize

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfPorts, SInt(8.W)))
    val output: Vec[SInt] = Output(Vec(numberOfPorts, SInt(8.W)))
  })

  for (i <- 0 until arrayDimension)
    for (j <- 0 until blockDimension * vectorSize) {
      val index = i * blockDimension * vectorSize + j
      val depth = i + 1
      io.output(index) := ShiftRegister(io.input(index), depth, 0.S, true.B)
    }

}
