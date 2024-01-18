package STA_Gen.WeightStationary

import chisel3._
import STA_Gen.Submodule.{Task, TaskQueue}

class ControlLogic(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int, taskQueueEntries: Int)  extends Module {

  val io = IO (new Bundle {
    //Signals from task queue
    val queueTask = Input(new Task)
    val queueValid = Input(Bool())
    val queueReady = Output(Bool())

    //Control logic output
    val sramReadEnableA = Output(Bool())
    val sramReadEnableB = Output(Bool())
    val skewBufferEnableA = Output(Bool())

    //Systolic tensor array
    val propagateSignal: Vec[Bool] = Output(Vec(arrayRow, Bool()))

    //Deskew buffer
    val deskewEnable: Bool = Output(Bool())
  })

  val taskQueue = Module(new TaskQueue(taskQueueEntries))
  val fsm = Module(new FiniteStateMachine(arrayRow, arrayCol, blockRow, blockCol, vectorSize))

  //task queue and fsm wiring
  io.queueReady := taskQueue.io.inputReady
  taskQueue.io.inputTask := io.queueTask
  taskQueue.io.inputValid := io.queueValid

  fsm.io.valid := taskQueue.io.outputValid
  fsm.io.task := taskQueue.io.outputTask
  taskQueue.io.outputReady := fsm.io.ready

  //Control logic output
  io.sramReadEnableA := fsm.io.sramReadEnableA
  io.sramReadEnableB := fsm.io.sramReadEnableB

  io.skewBufferEnableA := fsm.io.skewBufferEnableA
  io.propagateSignal := fsm.io.propagateSignal
  io.deskewEnable := fsm.io.deskewEnable

}
