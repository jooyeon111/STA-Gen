package STA_Gen.OutputStationary

import STA_Gen.Submodule.{FifoSramInputVectorRTL, SystolicTensorArrayConfig, Task}

import chisel3._

class SystolicPodRTL(
  val arrayRow: Int,
  val arrayCol: Int,
  val blockRow: Int,
  val blockCol: Int,
  val vectorSize: Int,
  taskQueueEntries: Int
) extends Module {

  def this(arrayConfig: SystolicTensorArrayConfig, taskQueueEntries: Int) =
    this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize, taskQueueEntries)

  override val desiredName = s"Os_Systolic_Tensor_Array_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")


  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfOutputs: Int = arrayRow * blockRow * blockCol

  val io = IO(new Bundle {

    //Task Queue
    val queueTask = Input(new Task)
    val queueValid = Input(Bool())
    val queueReady = Output(Bool())

    //Input and Output
    val inputA: Vec[SInt] = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(numberOfInputB, SInt(8.W)))
    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

  //Module assign
  val controlLogic = Module(new ControlLogic(arrayRow, arrayCol, blockRow, blockCol, vectorSize, taskQueueEntries))
  val systolicTensorArray = Module(new SystolicTensorArray(arrayRow, arrayCol, blockRow, blockCol, vectorSize))
  val fifoSramVectorA = Module(new FifoSramInputVectorRTL(arrayRow, blockRow, vectorSize))
  val fifoSramVectorB = Module(new FifoSramInputVectorRTL(arrayRow, blockRow, vectorSize))
  val skewBufferA = Module(new SkewBuffer(arrayRow, blockRow, vectorSize))
  val skewBufferB = Module(new SkewBuffer(arrayCol, blockCol, vectorSize))
  val PostProcessModule = Module(new PostProcessingUnit( arrayRow, arrayCol, blockRow, blockCol))

  //Input and output wiring
  controlLogic.io.queueTask := io.queueTask
  controlLogic.io.queueValid := io.queueValid
  io.queueReady := controlLogic.io.queueReady

  fifoSramVectorA.io.writeData := io.inputA
  fifoSramVectorB.io.writeData := io.inputB

  fifoSramVectorA.io.writeEnable := true.B
  fifoSramVectorA.io.readEnable := true.B
  fifoSramVectorB.io.writeEnable := true.B
  fifoSramVectorB.io.readEnable := true.B

  skewBufferA.io.input := fifoSramVectorA.io.readData
  skewBufferB.io.input := fifoSramVectorB.io.readData

  systolicTensorArray.io.inputA := skewBufferA.io.output
  systolicTensorArray.io.inputB := skewBufferB.io.output

  PostProcessModule.io.input := systolicTensorArray.io.outputC
  io.output := PostProcessModule.io.output


  //Control signal wiring
  //SRAM and skew buffer wiring
  fifoSramVectorA.io.readEnable := controlLogic.io.inputASramReadEnable
  fifoSramVectorB.io.readEnable := controlLogic.io.inputBSramReadEnable

  //Systolic array control wiring
  systolicTensorArray.io.partialSumReset := controlLogic.io.partialSumReset
  systolicTensorArray.io.propagateSignal := controlLogic.io.propagateSignal

  //Dimension align module control wiring
  PostProcessModule.io.outputSelectionSignal := controlLogic.io.outputSelectionSignal
  PostProcessModule.io.railwayMuxStartSignal := controlLogic.io.railwayMuxStartSignal

}
