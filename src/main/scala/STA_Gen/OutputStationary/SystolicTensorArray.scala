package STA_Gen.OutputStationary

import STA_Gen.Submodule.SystolicTensorArrayConfig
import chisel3._
import STA_Gen.{GemmDimension, ShowHardwareWiring}


class SystolicTensorArray(val arrayRow: Int, val arrayCol : Int, val blockRow : Int, val blockCol : Int, val vectorSize : Int)  extends Module {

  def this(arrayConfig : SystolicTensorArrayConfig) = this(arrayConfig.arrayRow, arrayConfig.arrayCol, arrayConfig.blockRow, arrayConfig.blockCol, arrayConfig.vectorSize)

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1,  "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"Os_Sta_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x$vectorSize"

  val numberOfInputA : Int = arrayRow * blockRow * vectorSize
  val numberOfInputB : Int = arrayCol * blockCol * vectorSize
  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOutputs : Int = (arrayCol + arrayRow - 1) * numberOfPEs

  val io = IO(new Bundle {

    val inputA: Vec[SInt] = Input(Vec(numberOfInputA,SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(numberOfInputB,SInt(8.W)))

    val propagateSignal: Vec[Vec[Bool]] =  Input(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))
    val partialSumReset: Vec[Vec[Bool]] =  Input(Vec(arrayRow, Vec(arrayCol, Bool())))

    val outputC: Vec[SInt] = Output(Vec(numberOfOutputs,SInt(32.W)))

  })

  //Systolic block 2d vector
  val systolicBlock2DVector : Vector[Vector[BlockPE]] =
    Vector.tabulate(arrayRow, arrayCol)( (x,y) => if(x == 0 || y == arrayCol - 1)
    { Module(new BlockPE(blockRow, blockCol, vectorSize, false)) }
    else { Module(new BlockPE(blockRow, blockCol, vectorSize, true)) })


  //Wiring propagate signals
  if (ShowHardwareWiring.switch) {
    println("Output stationary systolic tensor array")
    println("wiring propagate signal")
  }

  for(i <- 0 until arrayRow - 1)
    for(j<- 0 until arrayCol - 1){
      systolicBlock2DVector(i + 1)(j).io.propagateSignal.get := io.propagateSignal(i)(j)
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector(${i + 1})($j).io.propagateSignal.get := io.propagateSignal($i)($j)")
  }


  //Wiring partial sum reset signal
  if (ShowHardwareWiring.switch) {
    println("wiring partial sum reset signal")
  }
  for (i <- 0 until arrayRow)
    for(j <- 0 until arrayCol) {
      systolicBlock2DVector(i)(j).io.partialSumReset := io.partialSumReset(i)(j)
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector($i)($j).io.partialSumReset := io.partialSumReset($i)($j)")
  }

  //Wiring A
  if (ShowHardwareWiring.switch){
    println("wiring A")
  }

  for (i <- 0 until arrayRow)
    for(k <- 0 until blockRow * vectorSize){
      systolicBlock2DVector(i)(0).io.inputA(k) := io.inputA(k + (i*blockRow*vectorSize))
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector($i)(0).io.inputA($k) := io.inputA(${k + (i*blockRow*vectorSize)})")
  }

  for (i <- 0 until arrayRow)
    for(j <- 1 until arrayCol )
      for(k <- 0 until blockRow * vectorSize){
        systolicBlock2DVector(i)(j).io.inputA(k) := systolicBlock2DVector(i)(j-1).io.outputA(k)
        if (ShowHardwareWiring.switch)
          println(s"systolicBlock2DVector($i)($j).io.inputA($k) := systolicBlock2DVector($i)(${j-1}).io.outputA($k)")
  }


  //Wiring B

  if (ShowHardwareWiring.switch){
    println()
    println("wiring B")
  }

  for (i <- 0 until arrayCol)
    for(k <- 0 until blockCol * vectorSize){
      systolicBlock2DVector(0)(i).io.inputB(k) := io.inputB(k + (i*blockCol*vectorSize))
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector(0)($i).io.inputB($k) := io.inputB(${k + (i*blockCol*vectorSize)})")
  }

  for (i <- 1 until arrayRow)
    for(j <- 0 until arrayCol)
      for(k <- 0 until blockCol * vectorSize){
        systolicBlock2DVector(i)(j).io.inputB(k) := systolicBlock2DVector(i-1)(j).io.outputB(k)
        if (ShowHardwareWiring.switch)
          println(s"systolicBlock2DVector($i)($j).io.inputB($k) := systolicBlock2DVector(${i-1})($j).io.outputB($k)")
  }

  //Wiring output

  if (ShowHardwareWiring.switch){
    println()
    println("wiring output")
  }

//  for(i <- 0 until arrayRow; j <- 0 until arrayCol; k <- 0 until numberOfPEs) {
//
//    if(i == 0 && j == 0){
//      io.outputC(k) := systolicBlock2DVector(i)(j).io.outputC(k)
//      if (ShowHardwareWiring.switch)
//        println(s"io.outputC($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
//    }
//
//    if( (0 < i && i < arrayRow && j == 0) || ( i == arrayRow - 1 && 0 < j &&  j < arrayCol - 1) ){
//      io.outputC(i*numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.outputC(k)
//      if (ShowHardwareWiring.switch)
//        println(s"io.outputC(${i*numberOfPEs + j *numberOfPEs + k}) := systolicBlock2DVector($i)($j).io.outputC($k)")
//
//    }
//
//    if( i == arrayRow - 1 && j == arrayCol - 1){
//      io.outputC(i*numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.outputC(k)
//      if (ShowHardwareWiring.switch)
//        println(s"io.outputC(${i*numberOfPEs + j *numberOfPEs + k}) := systolicBlock2DVector($i)($j).io.outputC($k)")
//    }
//
//    if( (0 <= i && i < arrayRow - 1 && j == arrayCol - 1) || (i == 0 && 0 < j && j < arrayCol - 1)){
//      systolicBlock2DVector(i + 1)(j - 1).io.inputC.get(k) := systolicBlock2DVector(i)(j).io.outputC(k)
//      if (ShowHardwareWiring.switch)
//        println(s"systolicBlock2DVector(${i + 1})(${j - 1}).io.outputC($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
//    }
//
//    if (0 < i  &&  i< arrayRow - 1 && 0< j && j < arrayCol - 1) {
//      systolicBlock2DVector(i + 1)(j - 1).io.inputC.get(k) := systolicBlock2DVector(i)(j).io.outputC(k)
//      if (ShowHardwareWiring.switch)
//        println(s"systolicBlock2DVector(${i + 1})(${j - 1}).io.inputC.get($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
//    }
//  }

  for(i <- 0 until arrayRow; j <- 0 until arrayCol; k <- 0 until numberOfPEs) {

    //Case0
    if(i == 0 && j == 0){
      io.outputC(k) := systolicBlock2DVector(i)(j).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"io.outputC($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
    }

    //Case1
    if( (0 < i && i < arrayRow && j == 0 && i != 0) || ( i == arrayRow - 1 && 0 < j &&  j < arrayCol - 1 && i != 0) ){
      io.outputC(i*numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"io.outputC(${i*numberOfPEs + j *numberOfPEs + k}) := systolicBlock2DVector($i)($j).io.outputC($k)")

    }

    //Case2
    if( i == arrayRow - 1 && j == arrayCol - 1){
      io.outputC(i*numberOfPEs + j * numberOfPEs + k) := systolicBlock2DVector(i)(j).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"io.outputC(${i*numberOfPEs + j *numberOfPEs + k}) := systolicBlock2DVector($i)($j).io.outputC($k)")
    }

    //Case3
    if( (0 <= i && i < arrayRow - 1 && j == arrayCol - 1 && j != 0) || (i == 0 && 0 < j && j < arrayCol - 1 && j != 0 )){

      systolicBlock2DVector(i + 1)(j - 1).io.inputC.get(k) := systolicBlock2DVector(i)(j).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector(${i + 1})(${j - 1}).io.outputC($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
    }

    //Case4
    if (0 < i  &&  i< arrayRow - 1 && 0< j && j < arrayCol - 1) {
      systolicBlock2DVector(i + 1)(j - 1).io.inputC.get(k) := systolicBlock2DVector(i)(j).io.outputC(k)
      if (ShowHardwareWiring.switch)
        println(s"systolicBlock2DVector(${i + 1})(${j - 1}).io.inputC.get($k) := systolicBlock2DVector($i)($j).io.outputC($k)")
    }
  }

  if (ShowHardwareWiring.switch)
    println()

}
