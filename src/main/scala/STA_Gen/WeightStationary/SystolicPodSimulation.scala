package STA_Gen.WeightStationary

import STA_Gen.OutputStationary.SkewBuffer
import STA_Gen.Submodule.{FifoSramReadVectorSimulation, SystolicTensorArrayConfig, Task}
import chisel3._

class SystolicPodSimulation (
  val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int, taskQueueEntries: Int, sramHexDirectoryName: String)  extends Module {

  def this(arrayConfig: SystolicTensorArrayConfig, taskQueueEntries: Int, sramHexDirectoryName: String) =
    this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize, taskQueueEntries: Int, sramHexDirectoryName: String)

  override val desiredName = s"OsSystolicPod${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")


  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfOutputs: Int = arrayCol * blockCol

  val io = IO(new Bundle {

    val queueTask = Input(new Task)
    val queueValid = Input(Bool())
    val queueReady = Output(Bool())

    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

  /*
  *                                               Module assign
  * */
  val controlLogic = Module(new ControlLogic(arrayRow, arrayCol, blockRow, blockCol, vectorSize, taskQueueEntries))
  val fifoSramVectorA = Module(new FifoSramReadVectorSimulation(arrayRow, blockRow, vectorSize, 8, "src/main/resources/" + sramHexDirectoryName + "/InputAHex"))
  val fifoSramVectorB = Module(new FifoSramReadVectorSimulation(arrayCol, blockCol, vectorSize, 8, "src/main/resources/" + sramHexDirectoryName + "/InputBHex"))
  val skewBufferA = Module(new SkewBuffer(arrayRow, blockRow, vectorSize))
  val systolicTensorArray = Module(new SystolicTensorArray(arrayRow, arrayCol, blockRow, blockCol, vectorSize))
  val outputDeskewBuffer: DeskewBuffer = Module(new DeskewBuffer(arrayCol, blockCol))

  //input and output wiring

  controlLogic.io.queueTask := io.queueTask
  controlLogic.io.queueValid := io.queueValid
  io.queueReady := controlLogic.io.queueReady

  skewBufferA.io.input := fifoSramVectorA.io.readData

  systolicTensorArray.io.inputA := skewBufferA.io.output
  systolicTensorArray.io.inputB := RegNext(fifoSramVectorB.io.readData, RegInit(VecInit(Seq.fill(numberOfInputB)(0.S(8.W)))))

  outputDeskewBuffer.io.input := systolicTensorArray.io.outputC
  io.output := outputDeskewBuffer.io.output



  //control logic wiring
  fifoSramVectorA.io.readEnable := controlLogic.io.sramReadEnableA
  fifoSramVectorB.io.readEnable := controlLogic.io.sramReadEnableB
//
//  for( i <- 0 until arrayRow)
//    skewBufferA.io.shiftEnable(i) := controlLogic.io.skewBufferEnableA

  systolicTensorArray.io.propagateWeight := controlLogic.io.propagateSignal

//  for (i <- 0 until arrayRow)
//    outputDeskewBuffer.io.shiftEnable(i) := controlLogic.io.deskewEnable

}

