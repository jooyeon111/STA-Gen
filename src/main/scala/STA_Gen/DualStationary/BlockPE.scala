package STA_Gen.DualStationary

import chisel3._
import circt.stage.ChiselStage

class BlockPE(blockRow : Int, blockCol : Int, vectorSize : Int, isUpperRightBorder: Boolean ,isFirstRow : Boolean)  extends Module{

  require(blockRow >= 1, "[ERROR] Block row must be at least 1")
  require(blockCol >= 1, "[ERROR] Block col must be at least 1")
  require(vectorSize >= 1, "[ERROR] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"dual_stationary_block_PE_${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA: Int = vectorSize * blockRow
  val numberOfInputB: Int = vectorSize * blockCol
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOsOutput: Int = numberOfPEs
  val numberOfWsOutput: Int = blockCol

  val io = IO(new Bundle {

    val inputA = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB = Input(Vec(numberOfInputB, SInt(8.W)))
    val wsInputC = if (isFirstRow) None else Some(Input(Input(Vec(numberOfInputB, SInt(32.W)))))
    val osInputC = if (isUpperRightBorder) None else Some(Input(Input(Vec(numberOfPEs, SInt(32.W)))))

    val isOsOrWs = Input(Bool())
    val propagateWeight = Input(Bool())
    val propagateOutput = if (isUpperRightBorder) None else Some(Input(Bool()))
    val partialSumReset = Input(Bool())

    val outputA = Output(Vec(numberOfInputA, SInt(8.W)))
    val outputB = Output(Vec(numberOfInputB, SInt(8.W)))
    val osOutputC = Output(Vec(numberOfOsOutput, SInt(32.W)))
    val wsOutputC = Output(Vec(numberOfWsOutput, SInt(32.W)))

  })

  val PE2DVector: Vector[Vector[PE]] = Vector.fill(blockRow,blockCol)(Module(new PE(vectorSize, isFirstRow)))


  //Control signals
  //Wiring propagate weight
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.propagateWeight := io.propagateWeight


  //Wiring propagate weight
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.partialSumReset := io.partialSumReset

  //Wiring isOsOrWs
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.isOsOrWs := io.isOsOrWs


  //Input and Outputs
  //Wiring input A and output A
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      for (k <- 0 until vectorSize)
        PE2DVector(i)(j).io.inputA(k) := io.inputA(i * vectorSize + k)

  io.outputA := RegNext(io.inputA, VecInit.fill(numberOfInputA)(0.S))


  //Wiring input B and output B
  for (j <- 0 until blockCol)
    for (k <- 0 until vectorSize)
      PE2DVector(0)(j).io.inputB(k) := io.inputB(j * vectorSize + k)

  for (i <- 1 until blockRow)
    for (j <- 0 until blockCol)
      for (k <- 0 until vectorSize) {
        //        PE2DVector(i)(j).io.inputB(k) := Mux(io.isOsOrWs, io.inputB(j * vectorSize + k), PE2DVector(i - 1)(j).io.outputB)
        PE2DVector(i)(j).io.inputB(k) := Mux(io.isOsOrWs, io.inputB(j * vectorSize + k), PE2DVector(i - 1)(j).io.outputB(k))
      }

  for (j <- 0 until blockCol)
    for (k <- 0 until vectorSize) {
      io.outputB(j * vectorSize + k) := PE2DVector(blockRow - 1)(j).io.outputB(k)
    }


  //Wiring osInputC and osOutputC
  for (i <- 0 until blockRow; j <- 0 until blockCol) {
    if (isUpperRightBorder) {
      io.osOutputC(i * blockCol + j) := RegNext(PE2DVector(i)(j).io.osOutputC, 0.S(32.W))
    } else {
      io.osOutputC(i * blockCol + j) := RegNext(Mux(io.propagateOutput.get, io.osInputC.get(i * blockCol + j), PE2DVector(i)(j).io.osOutputC), 0.S(32.W))
    }
  }

  //Wiring wsInputC and wsOutputC
  for (j <- 0 until blockCol)
    if (!isFirstRow)
      PE2DVector(0)(j).io.wsInputC.get := io.wsInputC.get(j)

  for (i <- 1 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.wsInputC.get := PE2DVector(i - 1)(j).io.wsOutputC

  for (j <- 0 until blockCol)
    io.wsOutputC(j) := RegNext(PE2DVector(blockRow - 1)(j).io.wsOutputC, 0.S)

}
object BlockPE extends App{
  ChiselStage.emitSystemVerilog(
    gen = new BlockPE(4, 4,4, false, false),
    firtoolOpts = Array("--verilog", "-o=dual_stationary_block_PE.v" , "-disable-all-randomization" ) //, "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
}