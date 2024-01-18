package STA_Gen.WeightStationary

import chisel3._
import chisel3.util.{is, switch}
import STA_Gen.Submodule.{CeilingDivision, ControlSynchronizerMultiple, Task}

class FiniteStateMachine ( val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int)  extends Module{

  val io = IO(new Bundle {

    //Task handshaking protocol
    val valid = Input(Bool())
    val task = Input(new Task)
    val ready = Output(Bool())

    //Control signals
    //Sram ream signal and skew buffer enable
    val sramReadEnableA = Output(Bool())
    val sramReadEnableB = Output(Bool())
    val skewBufferEnableA = Output(Bool())

    //Systolic tensor array
    val propagateSignal: Vec[Bool]= Output(Vec(arrayRow,Bool()))

    //Deskew buffer
    val deskewEnable: Bool = Output(Bool())

  })


  val propagateSignalCounterVector: Vector[ControlSynchronizerMultiple] =
    Vector.tabulate(arrayRow)( x => Module(new ControlSynchronizerMultiple(arrayRow - x)))

  for( i <- 0 until arrayRow){
    propagateSignalCounterVector(i).io.start := false.B
    io.propagateSignal(i) := propagateSignalCounterVector(i).io.enable
  }

  //state assignment
  object State extends ChiselEnum {
    val waiting, weightLoading, inputLoading, error = Value
  }

  val state = RegInit(State.waiting)

  //ready signal generation
  val weightReady = WireDefault(true.B)
  val weightLoadingCounter = RegInit(0.U(32.W))
  val weightLoadingExpired = RegInit(0.U(32.W))
  val weightCeilingDivision = Module(new CeilingDivision)

  weightCeilingDivision.io.numerator := io.task.K
  weightCeilingDivision.io.denominator := vectorSize.U

  switch(state) {
    is(State.waiting) {
      when(weightReady && io.valid) {
        state := State.weightLoading
        weightLoadingExpired := weightCeilingDivision.io.result
        io.sramReadEnableB := true.B
        state := State.weightLoading
      }.elsewhen(weightReady && !io.valid){
        state := State.waiting
      }.otherwise{
        state := State.error
      }

    }
    is(State.weightLoading) {
      io.sramReadEnableB := true.B
      weightReady := false.B

      for( i <- 0 until arrayRow){
        propagateSignalCounterVector(i).io.start := true.B
      }

      when(weightLoadingCounter < weightLoadingExpired) {
        weightLoadingCounter := weightLoadingCounter + 1.U
      }.otherwise {
        weightLoadingCounter := 0.U
        weightReady := true.B
      }

      when(weightReady){
        state := State.inputLoading
      }

    }
    is(State.inputLoading) {

    }
    is(State.error) {
      //TODO: recovery logic
    }
  }

  io.ready := weightReady
  io.sramReadEnableA := false.B
  io.sramReadEnableB := false.B
  io.skewBufferEnableA := true.B
  io.deskewEnable := true.B

}
