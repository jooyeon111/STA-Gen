package STA_Gen.OutputStationary

import chisel3._
import STA_Gen.Submodule.{FifoSramReadVectorSimulation, SystolicTensorArrayConfig, Task}

class SystolicPodSimulation(
  val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int, taskQueueEntries: Int, shapeModifierEntries: Int  ,sramHexDirectoryName: String)  extends Module {


  def this(arrayConfig: SystolicTensorArrayConfig, taskQueueEntries: Int, shapeModifierEntries: Int,  sramHexDirectoryName: String) =
    this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize, taskQueueEntries, shapeModifierEntries, sramHexDirectoryName = sramHexDirectoryName)


  override val desiredName = s"Os_Systolic_Tensor_Array_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  val numberOfOutputs: Int = arrayRow * blockRow * blockCol

  val io = IO(new Bundle {

    val queueTask = Input(new Task)
    val queueValid = Input(Bool())
    val queueReady = Output(Bool())

    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

  //Module assign
  val controlLogic = Module(new ControlLogic(arrayRow, arrayCol, blockRow, blockCol, vectorSize, taskQueueEntries))
  val systolicTensorArray = Module(new SystolicTensorArray(arrayRow, arrayCol, blockRow, blockCol, vectorSize))
  val fifoSramVectorA = Module(new FifoSramReadVectorSimulation(arrayRow, blockRow, vectorSize, 8, sramHexDirectoryName + "/InputAHex"))
  val fifoSramVectorB = Module(new FifoSramReadVectorSimulation(arrayCol, blockCol, vectorSize, 8, sramHexDirectoryName + "/InputBHex"))
  val skewBufferA = Module(new SkewBuffer(arrayRow, blockRow, vectorSize))
  val skewBufferB = Module(new SkewBuffer(arrayCol, blockCol, vectorSize))
  val PostProcessModule = Module(new PostProcessingUnit(arrayRow, arrayCol, blockRow, blockCol))

  //Input and output wiring
  controlLogic.io.queueTask := io.queueTask
  controlLogic.io.queueValid := io.queueValid
  io.queueReady := controlLogic.io.queueReady

  skewBufferA.io.input := fifoSramVectorA.io.readData
  skewBufferB.io.input := fifoSramVectorB.io.readData

  systolicTensorArray.io.inputA := skewBufferA.io.output
  systolicTensorArray.io.inputB := skewBufferB.io.output

  PostProcessModule.io.input := systolicTensorArray.io.outputC
  io.output := PostProcessModule.io.output

  //Control signal wiring
  fifoSramVectorA.io.readEnable := controlLogic.io.inputASramReadEnable
  fifoSramVectorB.io.readEnable := controlLogic.io.inputBSramReadEnable

  //Systolic array control wiring
  systolicTensorArray.io.partialSumReset := controlLogic.io.partialSumReset
  systolicTensorArray.io.propagateSignal := controlLogic.io.propagateSignal


  PostProcessModule.io.outputSelectionSignal := controlLogic.io.outputSelectionSignal
  PostProcessModule.io.railwayMuxStartSignal := controlLogic.io.railwayMuxStartSignal

}
