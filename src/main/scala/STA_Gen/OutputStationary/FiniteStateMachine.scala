package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.{is, log2Ceil, switch}
import STA_Gen.Submodule.{CeilingDivision, ControlSynchronizer, Task}

class FiniteStateMachine (
  val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int)  extends Module {

  val io = IO(new Bundle {

    //Task Queue
    val valid: Bool = Input(Bool())
    val task: Task = Input(new Task)
    val ready: Bool = Output(Bool())

    //Output
    val inputASramReadEnable: Bool = Output(Bool())
    val inputBSramReadEnable: Bool = Output(Bool())

    val partialSumReset: Vec[Vec[Bool]] = Output(Vec(arrayRow, Vec(arrayCol, Bool())))
    val propagateSignal: Vec[Vec[Bool]] = Output(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))

    val railwayMuxStartSignal: Bool = Output(Bool())

  })

  //Control signals control initialization
  //SRAM read enable

  private val essentialWaitingTime = 4

  io.inputASramReadEnable := false.B
  io.inputBSramReadEnable := false.B

  //Partial sum reset counter
  val partialSumResetCounterVector: Vector[Vector[ControlSynchronizer]] =
    Vector.tabulate(arrayRow, arrayCol)( (x,y) => {Module(new ControlSynchronizer( log2Ceil(vectorSize) + essentialWaitingTime + x + y ))})

  for(i <- 0 until arrayRow ; j <- 0 until arrayCol) {
    partialSumResetCounterVector(i)(j).io.start := false.B
    io.partialSumReset(i)(j) := partialSumResetCounterVector(i)(j).io.enable
  }

  //propagate signal counter
  val propagateSignalCounterVector: Vector[Vector[ControlSynchronizer]] =
    Vector.tabulate(arrayRow - 1, arrayCol - 1) ( (x,y) => Module( new ControlSynchronizer(log2Ceil(vectorSize) + (essentialWaitingTime + 2)  + x + y, Math.min(x, arrayCol - 1 - y) + 1 )))


  for (i <- 0 until arrayRow - 1; j <- 0 until arrayCol - 1) {
    propagateSignalCounterVector(i)(j).io.start := false.B
    io.propagateSignal(i)(j) := propagateSignalCounterVector(i)(j).io.enable
  }

  //railway signal counter
  val railwayMuxSignalCounter = Module(new ControlSynchronizer(log2Ceil(vectorSize) + essentialWaitingTime - 1 + arrayRow))
  railwayMuxSignalCounter.io.start := false.B
  io.railwayMuxStartSignal := railwayMuxSignalCounter.io.enable

  object State extends ChiselEnum {
    val waiting, loading, Continuous, error = Value
  }

  val state = RegInit(State.waiting)

  io.ready := true.B
  val ceilingDivision = Module(new CeilingDivision)
  val loadingCounter = RegInit(0.U(32.W))
  val loadingExpired = RegInit(0.U(32.W))

  ceilingDivision.io.numerator := io.task.K
  ceilingDivision.io.denominator := vectorSize.U

  switch(state) {
    is(State.waiting){
      when(io.ready && io.valid){
        state := State.loading
        loadingExpired := ceilingDivision.io.result - 2.U
        io.inputASramReadEnable := true.B
        io.inputBSramReadEnable := true.B
      }.elsewhen(io.ready && !io.valid){
        state := State.waiting
      }.otherwise {
        state := State.error
      }
    }
    is(State.loading){

      io.ready := false.B
      io.inputASramReadEnable := true.B
      io.inputBSramReadEnable := true.B

      when(loadingCounter < loadingExpired) {
        loadingCounter := loadingCounter + 1.U
      }.otherwise{
        loadingCounter := 0.U
        io.ready := true.B
      }

      when(!io.ready){

        state := State.loading

      }.elsewhen(io.ready && io.valid) {

        for (i <- 0 until arrayRow; j <- 0 until arrayCol)
          partialSumResetCounterVector(i)(j).io.start := true.B

        for (i <- 0 until arrayRow - 1; j <- 0 until arrayCol - 1)
          propagateSignalCounterVector(i)(j).io.start := true.B

        railwayMuxSignalCounter.io.start := true.B
        state := State.Continuous


      }.elsewhen(io.ready && !io.valid){


        for (i <- 0 until arrayRow; j <- 0 until arrayCol)
          partialSumResetCounterVector(i)(j).io.start := true.B

        for (i <- 0 until arrayRow - 1; j <- 0 until arrayCol - 1)
          propagateSignalCounterVector(i)(j).io.start := true.B

        railwayMuxSignalCounter.io.start := true.B
        state := State.waiting

      }.otherwise {
        state := State.error
      }
    }
    is(State.Continuous){

      io.inputASramReadEnable := true.B
      io.inputBSramReadEnable := true.B
      io.ready := false.B
      state := State.loading

    }
    is(State.error){
      //TODO: recovery logic
    }
  }
}
