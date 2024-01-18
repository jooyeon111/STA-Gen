package STA_Gen.DualStationary

import chisel3._
import circt.stage.ChiselStage
import STA_Gen.Submodule.SystolicTensorArrayConfig

class SystolicTensorArray(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int) extends Module{

  def this(arrayConfig: SystolicTensorArrayConfig) = this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize)

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"dual_stationary_systolic_tensor_array_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOsOutput: Int = (arrayCol + arrayRow - 1) * numberOfPEs
  val numberOfWsOutput: Int = arrayCol * blockCol

  val io = IO(new Bundle {

    val inputA = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB = Input(Vec(numberOfInputB, SInt(8.W)))

    val isOsOrWs = Input(Bool())
    val propagateWeight = Input(Vec(arrayRow, Bool()))
    val propagateOutput = Input(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))
    val partialSumReset = Input(Vec(arrayRow, Vec(arrayCol, Bool())))


    val osOutputC = Output(Vec(numberOfOsOutput, SInt(32.W)))
    val wsOutputC = Output(Vec(numberOfWsOutput, SInt(32.W)))

  })

  val systolicBlock2DVector: Vector[Vector[BlockPE]] =
    Vector.tabulate(arrayRow, arrayCol)((x, y) => if (x == 0 || y == arrayCol - 1)
      Module(new BlockPE(blockRow, blockCol, vectorSize, true, true))
    else if ( x == arrayCol - 1)
      Module(new BlockPE(blockRow, blockCol, vectorSize, false, true))
    else
      Module(new BlockPE(blockRow, blockCol, vectorSize, false, false))
    )


  for (i <- 0 until arrayRow - 1)
    for (j <- 0 until arrayCol - 1)
      systolicBlock2DVector(i + 1)(j).io.propagateOutput.get := RegNext(io.propagateOutput(i)(j), false.B)

  for (i <- 0 until arrayRow)
    for (j <- 0 until arrayCol) {
      systolicBlock2DVector(i)(j).io.isOsOrWs := RegNext(io.isOsOrWs(i)(j), false.B)
      systolicBlock2DVector(i)(j).io.propagateWeight := RegNext(io.propagateWeight(i), false.B)
      systolicBlock2DVector(i)(j).io.partialSumReset := RegNext(io.partialSumReset(i)(j), false.B)
    }


  //Wiring A
  for (i <- 0 until arrayRow)
    for (k <- 0 until blockRow * vectorSize)
      systolicBlock2DVector(i)(0).io.inputA(k) := RegNext(io.inputA(k + (i * blockRow * vectorSize)), VecInit.fill(numberOfInputA)(0.S))

  for (i <- 0 until arrayRow)
    for (j <- 1 until arrayCol)
      for (k <- 0 until blockRow * vectorSize)
        systolicBlock2DVector(i)(j).io.inputA(k) := systolicBlock2DVector(i)(j - 1).io.outputA(k)


  //Wiring B
  for (i <- 0 until arrayCol)
    for (k <- 0 until blockCol * vectorSize)
      systolicBlock2DVector(0)(i).io.inputB(k) := RegNext(io.inputB(k + (i * blockCol * vectorSize)), VecInit.fill(numberOfInputB)(0.S))

  for (i <- 1 until arrayRow)
    for (j <- 0 until arrayCol)
      for (k <- 0 until blockCol * vectorSize)
        systolicBlock2DVector(i)(j).io.inputB(k) := systolicBlock2DVector(i - 1)(j).io.outputB(k)


  //Wiring output stationary output
  for (i <- 0 until arrayRow; j <- 0 until arrayCol; k <- 0 until numberOfPEs) {

    //Case0
    if (i == 0 && j == 0)
      io.osOutputC(k) := systolicBlock2DVector(i)(j).io.osOutputC(k)


    //Case1
    if ((0 < i && i < arrayRow && j == 0 && i != 0) || (i == arrayRow - 1 && 0 < j && j < arrayCol - 1 && i != 0))
      io.osOutputC(i * numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.osOutputC(k)


    //Case2
    if (i == arrayRow - 1 && j == arrayCol - 1)
      io.osOutputC(i * numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.osOutputC(k)

    //Case3
    if ((0 <= i && i < arrayRow - 1 && j == arrayCol - 1 && j != 0) || (i == 0 && 0 < j && j < arrayCol - 1 && j != 0))
      systolicBlock2DVector(i + 1)(j - 1).io.osInputC.get(k) := systolicBlock2DVector(i)(j).io.osOutputC(k)


    //Case4
    if (0 < i && i < arrayRow - 1 && 0 < j && j < arrayCol - 1)
      systolicBlock2DVector(i + 1)(j - 1).io.osInputC.get(k) := systolicBlock2DVector(i)(j).io.osOutputC(k)

  }

  //Wiring weight stationary output
  for (i <- 1 until arrayRow)
    for (j <- 0 until arrayCol)
      for (k <- 0 until blockCol)
        systolicBlock2DVector(i)(j).io.wsInputC.get(k) := systolicBlock2DVector(i - 1)(j).io.wsOutputC(k)

  for (i <- 0 until arrayCol)
    for (k <- 0 until blockCol)
      io.wsOutputC(k + (i * blockCol)) := systolicBlock2DVector(arrayRow - 1)(i).io.wsOutputC(k)


}

object SystolicTensorArray extends App{
  ChiselStage.emitSystemVerilog(
    gen = new BlockPE(4, 4,4, false, false),
    firtoolOpts = Array("--verilog", "-o=dual_stationary_systolic_tensor_array.v" , "-disable-all-randomization" ) //, "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
}
