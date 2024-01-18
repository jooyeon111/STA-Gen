package STA_Gen.DualStationary

import chisel3._
import STA_Gen.WeightStationary
import STA_Gen.OutputStationary

class PostProcessingUnit(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int) extends Module{


  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"dual_stationary_post_processing_unit_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOsInputC: Int = (arrayCol + arrayRow - 1) * numberOfPEs
  val numberOfOsOutputC: Int = arrayRow * blockRow * blockCol
  val numberOfWsInputC: Int = arrayCol * blockCol
  val numberOfSignals: Int = arrayCol

  val io = IO(new Bundle {

    val osInputC = Input(Vec(numberOfOsInputC, SInt(32.W)))
    val wsInputC = Input(Vec(numberOfWsInputC, SInt(32.W)))

    //Os
    val osOutputSelection: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))
    val osDeskewShiftEnable: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))
    val railwayMuxStart: UInt = Input(Bool())

    //Ws
    val wsOutputSelection: Vec[Bool] = Input(Vec(numberOfSignals, Bool()))
    val wsDeskewEnable : Vec[Bool] = Input(Vec(numberOfSignals, Bool()))

    val osOutputC = Output(Vec(numberOfOsOutputC, SInt(32.W)))
    val wsOutputC = Output(Vec(numberOfWsInputC, SInt(32.W)))

  })

  val osPostProcessingUnit = Module(new OutputStationary.PostProcessingUnit(arrayRow, arrayCol, blockRow, blockCol))
  val wsPostProcessingUnit = Module(new WeightStationary.PostProcessingUnit(arrayRow, arrayCol, blockRow, blockCol, vectorSize))


  osPostProcessingUnit.io.input := io.osOutputC
  osPostProcessingUnit.io.deskewShiftEnable := io.osDeskewShiftEnable
  osPostProcessingUnit.io.outputSelectionSignal := io.osOutputSelection
  osPostProcessingUnit.io.railwayMuxStartSignal := io.railwayMuxStart
  io.osOutputC := osPostProcessingUnit.io.output

  wsPostProcessingUnit.io.input := io.wsOutputC
  wsPostProcessingUnit.io.selectionSignal := io.wsOutputSelection
  wsPostProcessingUnit.io.deskewEnable := io.wsDeskewEnable
  io.wsOutputC := wsPostProcessingUnit.io.output

}
