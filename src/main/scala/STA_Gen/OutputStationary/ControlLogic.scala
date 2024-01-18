package STA_Gen.OutputStationary

import chisel3._
import STA_Gen.Submodule.Task
import STA_Gen.Submodule.TaskQueue

import scala.math.{ceil, log10}

class ControlLogic(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int, taskQueueEntries: Int)  extends Module{

  require(arrayRow >= 1, "[error] Control logic needs at least 1 array row")
  require(arrayCol >= 1, "[error] Control logic needs at least 1 array column")
  require(blockRow >= 1, "[error] Control logic needs at least 1 block row")
  require(blockCol >= 1, "[error] Control logic needs at least 1 block column")
  require(taskQueueEntries >= 1, "[error] Control logic needs at least 1 task entries")

  val muxSignalBits: Int = ceil(log10(arrayRow.toDouble) / log10(2.0)).toInt

  val io = IO(new Bundle {

    //Signals from task queue
    val queueTask = Input(new Task)
    val queueValid = Input(Bool())
    val queueReady = Output(Bool())

    //Control logic output
    //Sram read signals and skew buffer enable
//    val sramReadEnable = Output(Bool())
    val inputASramReadEnable = Output(Bool())
    val inputBSramReadEnable = Output(Bool())
    val skewBufferEnableA = Output(Bool())
    val skewBufferEnableB = Output(Bool())

    //Systolic arrays
    val propagateSignal: Vec[Vec[Bool]] = Output(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))
    val partialSumReset: Vec[Vec[Bool]] = Output(Vec(arrayRow, Vec(arrayCol, Bool())))

    //Dimension align module
    val outputSelectionSignal: Vec[Bool] = Output(Vec(arrayRow + arrayCol - 1, Bool()))
    val deskewShiftEnable: Vec[Bool] = Output(Vec(arrayRow + arrayCol - 1, Bool()))
    val railwayMuxStartSignal: Bool = Output(Bool())
//    val dualShapeModifierStart: Bool = Output(Bool())
//  val shapeModifier4InputValid = Output(Bool())


  })

  val taskQueue = Module(new TaskQueue(taskQueueEntries))
  val fsm =  Module(new FiniteStateMachine(arrayRow, arrayCol, blockRow, blockCol, vectorSize))

  //task queue and fsm wiring
  io.queueReady := taskQueue.io.inputReady
  taskQueue.io.inputTask := io.queueTask
  taskQueue.io.inputValid := io.queueValid

  fsm.io.valid := taskQueue.io.outputValid
  fsm.io.task := taskQueue.io.outputTask
  taskQueue.io.outputReady := fsm.io.ready

  //Control logic output
//  io.sramReadEnable := fsm.io.sramReadEnable
  io.inputASramReadEnable := fsm.io.inputASramReadEnable
  io.inputBSramReadEnable := fsm.io.inputBSramReadEnable
  io.skewBufferEnableA := fsm.io.skewBufferEnableA
  io.skewBufferEnableB := fsm.io.skewBufferEnableB

  io.propagateSignal := fsm.io.propagateSignal
  io.partialSumReset := fsm.io.partialSumReset

  io.outputSelectionSignal := fsm.io.outputSelectionSignal
  io.deskewShiftEnable := fsm.io.deskewShiftEnable
  io.railwayMuxStartSignal := fsm.io.railwayMuxStartSignal
//  io.shapeModifier4InputValid := fsm.io.shapeModifier4InputValid
}
