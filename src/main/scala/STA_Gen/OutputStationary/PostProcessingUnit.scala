package STA_Gen.OutputStationary

import chisel3._


class PostProcessingUnit(arrayRow: Int, arrayCol: Int, blockRow: Int, blockCol: Int) extends Module {

  require(arrayRow >= 1, "[error] Output stationary dimension align module needs at least 1 array row")
  require(arrayCol >= 1, "[error] Output stationary dimension align module needs at least 1 array column")
  require(blockRow >= 1, "[error] Output stationary dimension align module needs at least 1 block row")
  require(blockCol >= 1, "[error] Output stationary dimension align module needs at least 1 block column")

  val numberOfInputs: Int = (arrayRow + arrayCol - 1) * blockRow * blockCol
  val numberOfOutputs: Int = arrayRow * blockRow * blockCol
//  val muxSignalBits: Int = ceil(log10(arrayRow.toDouble) / log10(2.0)).toInt

  val io = IO(new Bundle {

    val input: Vec[SInt] = Input(Vec(numberOfInputs, SInt(32.W)))

    //output selector
    val outputSelectionSignal: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))

    //Deskew
//    val deskewShiftEnable: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))

    //Railway
    val railwayMuxStartSignal: UInt = Input(Bool())

    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

  /*
  *                                               Module assign
  * */
  val outputSelector = Module(new OutputSelector(arrayRow, arrayCol, blockRow, blockCol))
  val DeskewBuffer = Module(new DeskewBuffer(arrayRow, arrayCol, blockRow, blockCol))
  val railway = Module(new Railway(arrayRow, arrayCol, blockRow, blockCol))
//  val ShapeModifier = Module(new ShapeModifier(arrayRow, arrayCol, blockRow, blockCol))
//  val shapeModiifer4 = Module(new ShapeModifier4(arrayRow, arrayCol, blockRow, blockCol, queueEntries))

  /*
  *                                            Control signal wiring
  * */

  outputSelector.io.selectionSignal := io.outputSelectionSignal
//  DeskewBuffer.io.shiftEnable := io.deskewShiftEnable
  railway.io.start := io.railwayMuxStartSignal
//  shapeModiifer4.io.inputValid := io.shapeModifier4InputValid
//  io.shapeModifier4Ready := shapeModiifer4.io.moduleReady

  /*
  *                                          Input and output wiring
  * */

  outputSelector.io.input := io.input
  DeskewBuffer.io.input := outputSelector.io.output
  railway.io.input := DeskewBuffer.io.output
//  shapeModiifer4.io.input := railway.io.output
//  io.output := shapeModiifer4.io.output
  io.output := railway.io.output

}
