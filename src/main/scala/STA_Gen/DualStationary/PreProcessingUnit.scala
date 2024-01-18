package STA_Gen.DualStationary

import chisel3._


class PreProcessingUnit(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int)  extends Module{

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"pre_processing_unit_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOsOutput: Int = (arrayCol + arrayRow - 1) * numberOfPEs
  val numberOfWsOutput: Int = arrayCol * blockCol

  val io = IO(new Bundle {

    val inputA = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB = Input(Vec(numberOfInputB, SInt(8.W)))

    val isOsOrWs = Input(Bool())

    val outputA = Output(Vec(numberOfInputA, SInt(8.W)))
    val outputB = Output(Vec(numberOfInputB, SInt(8.W)))

  })

  val skewBufferA = Module(new SkewBufferVector(8, arrayRow, blockRow, vectorSize))
  skewBufferA.io.input := RegNext(io.inputA, VecInit.fill(numberOfInputA)(0.S))
  skewBufferA.io.shiftEnable := true.B
  io.outputA := skewBufferA.io.output

  val skewBufferB = Module(new SkewBufferVector(8, arrayCol, blockCol, vectorSize))
  val deMuxB = Module(new DeMux( Vec(numberOfInputB, SInt(8.W)) ))

  deMuxB.io.input := RegNext(io.inputB, VecInit.fill(numberOfInputA)(0.S))
  skewBufferB.io.input := deMuxB.io.outputFalse
  skewBufferB.io.shiftEnable := true.B
  io.outputB := Mux(io.isOsOrWs, deMuxB.io.outputFalse, skewBufferB.io.output )

}
