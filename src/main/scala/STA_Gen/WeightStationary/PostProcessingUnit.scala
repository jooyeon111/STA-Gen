package STA_Gen.WeightStationary

import chisel3._

class PostProcessingUnit(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int) extends Module {

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"post_processing_unit_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfSignals: Int = arrayCol
  val numberOfOutputs: Int = numberOfSignals * blockCol

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfOutputs, SInt(32.W)))
    val selectionSignal: Vec[Bool] = Input(Vec(numberOfSignals, Bool()))
    val deskewEnable : Vec[Bool] = Input(Vec(numberOfSignals, Bool()))
    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))
  })


  val outputSelector = Module( new OutputSelector(arrayRow, arrayCol, blockRow, blockCol) )
  val DeskewBuffer = Module(new DeskewBuffer(arrayCol, blockCol))

  outputSelector.io.selectionSignal := io.selectionSignal
//  DeskewBuffer.io.shiftEnable := io.deskewEnable

  outputSelector.io.input := io.input
  DeskewBuffer.io.input :=  outputSelector.io.output
  io.output := DeskewBuffer.io.output

}
