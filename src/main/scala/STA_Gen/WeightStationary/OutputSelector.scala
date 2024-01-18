package STA_Gen.WeightStationary

import chisel3._

class OutputSelector(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int) extends Module{

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")

  val numberOfSignals: Int = arrayCol
  val numberOfOutputs: Int = numberOfSignals * blockCol

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfOutputs, SInt(32.W)))
    val selectionSignal: Vec[Bool] = Input(Vec(numberOfSignals, Bool()))
    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))
  })

  for (i <- 0 until numberOfSignals)
    for (j <- 0 until blockCol)
      io.output(j + i * blockCol) := RegNext(Mux(io.selectionSignal(i), io.input(j + i * blockCol), 0.S), 0.S)


}
