package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.{is, log2Ceil, switch}
import STA_Gen.Submodule.{CeilingDivision, ControlSynchronizerMultiple, ControlSynchronizerSingle, Task}

class FiniteStateMachine (
  val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int)  extends Module {

  val io = IO(new Bundle {

    //Task handshaking protocol
    val valid = Input(Bool())
    val task = Input(new Task)
    val ready = Output(Bool())

    //Control signals
    //Sram ream signal and skew buffer enable
//    val sramReadEnable = Output(Bool())
    val inputASramReadEnable = Output(Bool())
    val inputBSramReadEnable = Output(Bool())
//    val skewBufferEnableA = Output(Bool())
//    val skewBufferEnableB = Output(Bool())

    //Systolic tensor array
    val partialSumReset: Vec[Vec[Bool]] = Output(Vec(arrayRow, Vec(arrayCol, Bool())))
    val propagateSignal: Vec[Vec[Bool]] = Output(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))

    //Dimension organizer
    val outputSelectionSignal: Vec[Bool] = Output(Vec(arrayRow + arrayCol - 1, Bool()))
//    val deskewShiftEnable: Vec[Bool] = Output(Vec(arrayRow + arrayCol - 1, Bool()))
    val railwayMuxStartSignal: Bool = Output(Bool())

  })

  //Control signals control initialization
  //SRAM read enable
//  io.sramReadEnable := false.B
  io.inputASramReadEnable := false.B
  io.inputBSramReadEnable := false.B

//  val inputASramReadEnableCounter: ControlSynchronizerMultiple = Module (new ControlSynchronizerMultiple(0, ))

  //skew buffer enable
//  io.skewBufferEnableA := true.B
//  io.skewBufferEnableB := true.B

  //Partial sum reset counter
  val partialSumResetCounterVector: Vector[Vector[ControlSynchronizerSingle]] =
    Vector.tabulate(arrayRow, arrayCol)( (x,y) => {Module(new ControlSynchronizerSingle( log2Ceil(vectorSize) + 4 + x + y ))})

  for(i <- 0 until arrayRow ; j <- 0 until arrayCol) {
    partialSumResetCounterVector(i)(j).io.start := false.B
    io.partialSumReset(i)(j) := partialSumResetCounterVector(i)(j).io.enable
  }

  //propagate signal counter2
  val propagateSignalCounterVector: Vector[Vector[ControlSynchronizerMultiple]] =
    Vector.tabulate(arrayRow - 1, arrayCol - 1) ( (x,y) => Module( new ControlSynchronizerMultiple(log2Ceil(vectorSize) + 6  + x + y, Math.min(x, arrayCol - 1 - y) + 1 )))


  for (i <- 0 until arrayRow - 1; j <- 0 until arrayCol - 1) {
    propagateSignalCounterVector(i)(j).io.start := false.B
    io.propagateSignal(i)(j) := propagateSignalCounterVector(i)(j).io.enable
  }


  //output select signal counter
  val outputSelectionSignalCounterVector: Vector[ControlSynchronizerMultiple] =
    Vector.tabulate(arrayRow + arrayCol - 1)( x => if (x < arrayRow )
      Module (new ControlSynchronizerMultiple( log2Ceil(vectorSize) + 5 + x, x + 1))
    else Module (new ControlSynchronizerMultiple(log2Ceil(vectorSize) + 5 + x , arrayRow + arrayCol - 1 - x )))


  for( i <- 0 until arrayRow + arrayCol - 1){
    outputSelectionSignalCounterVector(i).io.start := false.B
    io.outputSelectionSignal(i) := outputSelectionSignalCounterVector(i).io.enable
  }


  //Deskew buffer enable
//  for (i <- 0 until arrayRow + arrayCol - 1)
//    io.deskewShiftEnable(i) := true.B

  //Railway signal
  val railwayMuxSignalCounter = Module(new ControlSynchronizerSingle(log2Ceil(vectorSize) + 4 + arrayRow))
  railwayMuxSignalCounter.io.start := false.B
  io.railwayMuxStartSignal := railwayMuxSignalCounter.io.enable

  //dual shape modifier
//  val dualShapeModifierCounter = Module(new ControlSynchronizerSingle(log2Ceil(vectorSize) + 5 + arrayRow))
//  dualShapeModifierCounter.io.start := false.B
//  io.dualShapeModifierStart := dualShapeModifierCounter.io.enable

//  val shapeModifier4Counter = Module(new ControlSynchronizerMultiple(log2Ceil(vectorSize) + 5 + arrayRow, arrayCol + 1))
//  shapeModifier4Counter.io.start := false.B
//  io.shapeModifier4InputValid := shapeModifier4Counter.io.enable

  object State extends ChiselEnum {
    val waiting, loading, Continuous, error = Value
  }

  val state = RegInit(State.waiting)

  //ready signal generation
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

        for (i <- 0 until arrayRow + arrayCol - 1)
          outputSelectionSignalCounterVector(i).io.start := true.B

        railwayMuxSignalCounter.io.start := true.B
//        shapeModifier4Counter.io.start := true.B
        state := State.Continuous


      }.elsewhen(io.ready && !io.valid){


        for (i <- 0 until arrayRow; j <- 0 until arrayCol)
          partialSumResetCounterVector(i)(j).io.start := true.B

        for (i <- 0 until arrayRow - 1; j <- 0 until arrayCol - 1)
          propagateSignalCounterVector(i)(j).io.start := true.B

        for (i <- 0 until arrayRow + arrayCol - 1)
          outputSelectionSignalCounterVector(i).io.start := true.B

        railwayMuxSignalCounter.io.start := true.B
//        shapeModifier4Counter.io.start := true.B
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
