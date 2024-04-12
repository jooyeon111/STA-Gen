package STA_Gen.OutputStationary

import chisel3._
import STA_Gen.ShowHardwareWiring

class BlockPE (blockRow : Int, blockCol : Int, vectorSize : Int, inputCFlag : Boolean)  extends Module{

  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"Os_Block_PE_${blockRow}x${blockCol}x${vectorSize}"

  val numberOfInputA: Int = vectorSize * blockRow
  val numberOfInputB: Int = vectorSize * blockCol
  val numberOfPEs: Int = blockRow * blockCol

  val io = IO(new Bundle {

    //Input
    val inputA: Vec[SInt] = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(numberOfInputB, SInt(8.W)))
    val inputC = if(inputCFlag) Some(Input(Input(Vec(numberOfPEs, SInt(32.W))))) else None

    //Control
    val propagateSignal = if(inputCFlag) Some( Input(Bool() ) ) else None
    val partialSumReset: Bool = Input(Bool())

    //Output
    val outputA: Vec[SInt] = Output(Vec(numberOfInputA, SInt(8.W)))
    val outputB: Vec[SInt] = Output(Vec(numberOfInputB, SInt(8.W)))
    val outputC: Vec[SInt] = Output(Vec(numberOfPEs, SInt(32.W)))

  })

  //Generating 2D array processing elements
  val PE2DVector: Vector[Vector[PE]] = Vector.fill(blockRow,blockCol)(Module( new PE(vectorSize)))

  //Wiring register reset signal
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.partialSumReset := io.partialSumReset

  //Wiring Input A
  if (ShowHardwareWiring.switch){
    println("Output stationary systolic block")
    println("wiring A")
  }
  
  for( i <- 0 until blockRow)
    for(j <- 0 until blockCol)
      for(k <-0 until vectorSize){
        PE2DVector(i)(j).io.inputA(k) := io.inputA(i*vectorSize+k)
        if (ShowHardwareWiring.switch)
          println(s"PE2DVector($i)($j).io.inputA($k) = io.inputA(${i*vectorSize+k})")
  }
      

  //Wiring Input B
  if (ShowHardwareWiring.switch){
    println()
    println("wiring B")
  }

  for( i <- 0 until blockRow)
    for(j <- 0 until blockCol)
      for(k <-0 until vectorSize) {
        PE2DVector(i)(j).io.inputB(k) := io.inputB(j*vectorSize+k)
        if (ShowHardwareWiring.switch)
          println(s"PE2DVector($i)($j).io.inputB($k) = io.inputB(${j*vectorSize+k})")
  }

  //Wiring Output
  if (ShowHardwareWiring.switch){
    println()
    println("wiring output")
  }    

  val outputCRegisterVec = RegInit(VecInit(Seq.fill(numberOfPEs)(0.S(32.W))))

  for (i <- 0 until blockRow; j <- 0 until blockCol){

    val index = i * blockCol + j

    if(inputCFlag){
      outputCRegisterVec(index) := Mux(io.propagateSignal.get, io.inputC.get(index), PE2DVector(i)(j).io.outputC)
    }else{
      outputCRegisterVec(index) := PE2DVector(i)(j).io.outputC
    }

    if (ShowHardwareWiring.switch)
      println(s"outputRegisterVector($index) := Mux(io.propagateSignal,io.inputC.get($index), PE2DVector($i)($j).io.outputC)")

  }

  val outputARegisterVec = RegInit(VecInit(Seq.fill(numberOfInputA)(0.S(32.W))))
  val outputBRegisterVec = RegInit(VecInit(Seq.fill(numberOfInputB)(0.S(32.W))))

  outputARegisterVec := io.inputA
  outputBRegisterVec := io.inputB

  io.outputA := outputARegisterVec
  io.outputB := outputBRegisterVec
  io.outputC := outputCRegisterVec

  if (ShowHardwareWiring.switch)
    println()

}


