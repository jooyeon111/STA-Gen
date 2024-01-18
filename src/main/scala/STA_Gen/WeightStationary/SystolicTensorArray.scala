package STA_Gen.WeightStationary

import STA_Gen.Submodule.SystolicTensorArrayConfig
import chisel3._
import STA_Gen.ShowHardwareWiring

class SystolicTensorArray(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int) extends Module {
  def this(arrayConfig: SystolicTensorArrayConfig) = this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize)

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"WSSTA${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA: Int = arrayRow * blockRow * vectorSize
  val numberOfInputB: Int = arrayCol * blockCol * vectorSize
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOutputs : Int = arrayCol * blockCol

  val io = IO(new Bundle {

    val inputA: Vec[SInt] = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(numberOfInputB, SInt(8.W)))

//    val propagateSignal: Vec[Vec[Bool]] = Input(Vec(arrayRow, Vec(arrayCol, Bool())))
//    val propagateSignal : Bool = Input(Bool())
    val propagateWeight : Vec[Bool] = Input(Vec(arrayRow, Bool()))

    val outputC: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))

  })

//  val bufferInputA: Vec[SInt] = RegNext(io.inputA)
//  val bufferInputB: Vec[SInt] = RegNext(io.inputB)

  val systolicBlock2DVector: Vector[Vector[BlockPE]] = Vector.tabulate(arrayRow, arrayCol)((x,_) => if ( x == 0 ) {
    Module(new BlockPE(blockRow, blockCol, vectorSize, false))

  } else{
    Module(new BlockPE(blockRow, blockCol, vectorSize, true))
  })

//  for (i <- 0 until arrayRow)
//    for (j <- 0 until arrayCol)
//      systolicBlock2DVector(i)(j).io.propagateSignal := io.propagateSignal(i)(j)


  for (i <- 0 until arrayRow)
    for (j <- 0 until arrayCol)
      systolicBlock2DVector(i)(j).io.propagateWeight := io.propagateWeight(i)

  if (ShowHardwareWiring.switch) {
    println("Output stationary systolic tensor array")
    println("wiring A")
  }

  for (i <- 0 until arrayRow)
    for (k <- 0 until blockRow * vectorSize) {
      systolicBlock2DVector(i)(0).io.inputA(k) := io.inputA(k + (i * blockRow * vectorSize))
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector($i)(0).io.inputA($k) := io.inputA(${k + (i * blockRow * vectorSize)})")
    }

  for (i <- 0 until arrayRow)
    for (j <- 1 until arrayCol)
      for (k <- 0 until blockRow * vectorSize) {
        systolicBlock2DVector(i)(j).io.inputA(k) := systolicBlock2DVector(i)(j - 1).io.outputA(k)
        if (ShowHardwareWiring.switch)
          println(s"systolicBlock2DVector($i)($j).io.inputA($k) := systolicBlock2DVector($i)(${j - 1}).io.outputA($k)")
      }


  if (ShowHardwareWiring.switch) {
    println()
    println("wiring B")
  }

  for (i <- 0 until arrayCol)
    for (k <- 0 until blockCol * vectorSize) {
      systolicBlock2DVector(0)(i).io.inputB(k) := io.inputB(k + (i * blockCol * vectorSize))
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector(0)($i).io.inputB($k) := io.inputB(${k + (i * blockCol * vectorSize)})")
    }

  for (i <- 1 until arrayRow)
    for (j <- 0 until arrayCol)
      for (k <- 0 until blockCol * vectorSize) {
        systolicBlock2DVector(i)(j).io.inputB(k) := systolicBlock2DVector(i - 1)(j).io.outputB(k)
        if (ShowHardwareWiring.switch)
          println(s"systolicBlock2DVector($i)($j).io.inputB($k) := systolicBlock2DVector(${i - 1})($j).io.outputB($k)")
      }

  if (ShowHardwareWiring.switch) {
    println()
    println("wiring output")
  }

  for (i <- 1 until arrayRow)
    for (j <- 0 until arrayCol)
      for (k <- 0 until blockCol) {
        systolicBlock2DVector(i)(j).io.inputC.get(k) := systolicBlock2DVector(i - 1)(j).io.outputC(k)
        if (ShowHardwareWiring.switch)
          println(s"systolicBlock2DVector($i)($j).io.inputC($k) := systolicBlock2DVector(${i - 1})($j).io.outputC($k)")
      }

  for (i <- 0 until arrayCol)
    for (k <- 0 until blockCol) {
      io.outputC(k + (i * blockCol)) := systolicBlock2DVector(arrayRow - 1)(i).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"io.outputC(${k + (i * blockCol)}) := systolicBlock2DVector(${arrayRow - 1})($i).io.outputC($k)")
    }


  if (ShowHardwareWiring.switch)
    println()
}
