package STA_Gen.Submodule

import chisel3._
import chisel3.util.Queue

class TaskQueue(entries: Int) extends Module {

  require(entries >= 1, "[error] Task queue needs at least 1 entry")

  val io = IO(new Bundle {

//    val input = Flipped(Decoupled(new Task))
//    val output = Decoupled(new Task)

    val inputTask = Input(new Task)
    val inputValid = Input(Bool())
    val inputReady = Output(Bool())

    val outputTask = Output(new Task)
    val outputValid = Output(Bool())
    val outputReady = Input(Bool())

  })

  val queue = Module(new Queue(new Task, entries))

  queue.io.enq.bits := io.inputTask
  queue.io.enq.valid := io.inputValid
  io.inputReady := queue.io.enq.ready

  io.outputTask := queue.io.deq.bits
  io.outputValid := queue.io.deq.valid
  queue.io.deq.ready := io.outputReady

}
