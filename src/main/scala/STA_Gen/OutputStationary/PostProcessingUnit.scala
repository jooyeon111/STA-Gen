package STA_Gen.OutputStationary

import chisel3._


class PostProcessingUnit(arrayRow: Int, arrayCol: Int, blockRow: Int, blockCol: Int) extends Module {

  require(arrayRow >= 1, "[error] Output stationary dimension align module needs at least 1 array row")
  require(arrayCol >= 1, "[error] Output stationary dimension align module needs at least 1 array column")
  require(blockRow >= 1, "[error] Output stationary dimension align module needs at least 1 block row")
  require(blockCol >= 1, "[error] Output stationary dimension align module needs at least 1 block column")

  val numberOfInputs: Int = (arrayRow + arrayCol - 1) * blockRow * blockCol
  val numberOfOutputs: Int = arrayRow * blockRow * blockCol

  val io = IO(new Bundle {

    //Input
    val input: Vec[SInt] = Input(Vec(numberOfInputs, SInt(32.W)))

    //Control
    val outputSelectionSignal: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))
    val railwayMuxStartSignal: UInt = Input(Bool())

    //Output
    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

  /*
  *                                               Module assign
  * */
  val outputSelector = Module(new OutputSelector(arrayRow, arrayCol, blockRow, blockCol))
  val DeskewBuffer = Module(new DeskewBuffer(arrayRow, arrayCol, blockRow, blockCol))
  val railway = Module(new Railway(arrayRow, arrayCol, blockRow, blockCol))

  /*
  *                                            Control signal wiring
  * */

  outputSelector.io.selectionSignal := io.outputSelectionSignal
  railway.io.start := io.railwayMuxStartSignal

  /*
  *                                          Input and output wiring
  * */

  outputSelector.io.input := io.input
  DeskewBuffer.io.input := outputSelector.io.output
  railway.io.input := DeskewBuffer.io.output
  io.output := railway.io.output

}
