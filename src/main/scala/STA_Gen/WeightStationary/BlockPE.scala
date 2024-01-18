package STA_Gen.WeightStationary

import chisel3._
import STA_Gen.ShowHardwareWiring


class BlockPE( blockRow: Int, blockCol : Int, vectorSize: Int, inputCFlag : Boolean) extends Module {

  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  val numberOfInputA: Int = vectorSize * blockRow
  val numberOfInputB: Int = vectorSize * blockCol
  val numberOfOutput: Int = blockCol

  val io = IO(new Bundle {

    val inputA: Vec[SInt] = Input(Vec(numberOfInputA, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(numberOfInputB, SInt(8.W)))
    val inputC = if( inputCFlag ) Some( Input(Vec(numberOfOutput, SInt(32.W))) ) else None

    val propagateWeight : Bool =  Input(Bool())

    val outputA: Vec[SInt] = Output(Vec(numberOfInputA, SInt(8.W)))
    val outputB: Vec[SInt] = Output(Vec(numberOfInputB, SInt(8.W)))
    val outputC: Vec[SInt] = Output(Vec(numberOfOutput, SInt(32.W)))

  })

  override val desiredName = s"WSSystolicBlockWithAdderTree${blockRow}x${blockCol}x$vectorSize"

//  val PE2DVector: Vector[Vector[WeightStationaryPE]] = Vector.fill(blockRow,blockCol)(Module(new WeightStationaryPE(vectorSize,true)))
  val PE2DVector: Vector[Vector[PE]] = if(inputCFlag) {

    Vector.fill(blockRow,blockCol)(Module(new PE(vectorSize,true)))

  } else {

    Vector.tabulate(blockRow, blockCol)((x,_) => if ( x == 0 ) {
      Module(new PE(vectorSize, false))
    }
    else {
      Module(new PE(vectorSize, true))
    })

  }

  //Wiring passing signal. if signal is true then pass output which come from upper processing element
  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      PE2DVector(i)(j).io.propagateWeight := io.propagateWeight

  //Wiring Input A
  if (ShowHardwareWiring.switch) {
    println("Output stationary systolic block")
    println("wiring A")
  }

  for (i <- 0 until blockRow)
    for (j <- 0 until blockCol)
      for (k <- 0 until vectorSize) {
        PE2DVector(i)(j).io.inputA(k) := io.inputA(i * vectorSize + k)
        if (ShowHardwareWiring.switch)
          println(s"PE2DVector($i)($j).io.inputA($k) := io.inputA(${i * vectorSize + k})")
      }

  //Wiring Input B
  if (ShowHardwareWiring.switch) {
    println()
    println("wiring B")
  }

  for (j <- 0 until blockCol)
    for( k <- 0 until vectorSize){
      PE2DVector(0)(j).io.inputB(k) := io.inputB(j*vectorSize + k)
      if (ShowHardwareWiring.switch)
        println(s"PE2DVector(0)($j).io.inputB($k) := io.inputB(${j*vectorSize + k})")
    }

  for (i <- 1 until blockRow)
    for (j <- 0 until blockCol)
      for(k <- 0 until vectorSize){
        PE2DVector(i)(j).io.inputB(k) := PE2DVector(i - 1)(j).io.outputB(k)
        if (ShowHardwareWiring.switch)
          println(s"PE2DVector($i)($j).io.inputB($k) := PE2DVector(${i - 1})($j).io.outputB($k)")
    }

  for (j <- 0 until blockCol)
    for( k <- 0 until vectorSize) {
      io.outputB(j*vectorSize + k) := PE2DVector(blockRow - 1)(j).io.outputB(k)
      if (ShowHardwareWiring.switch)
        println(s"io.outputB(${j*vectorSize + k}) := PE2DVector(${blockRow - 1})($j).io.outputB($k)")
    }

  //Wiring Output
  if (ShowHardwareWiring.switch) {
    println()
    println("wiring output")
  }

  for (j <- 0 until blockCol) {
    if(inputCFlag) {
      PE2DVector(0)(j).io.inputC.get := io.inputC.get(j)
      if (ShowHardwareWiring.switch)
        println(s"PE2DVector(0)($j).io.inputC := io.inputC($j)")
    }
  }

  for (i <- 1 until blockRow)
    for (j <- 0 until blockCol) {
      PE2DVector(i)(j).io.inputC.get := PE2DVector(i - 1)(j).io.outputC
      if (ShowHardwareWiring.switch)
        println(s"PE2DVector($i)($j).io.inputC := PE2DVector(${i - 1})($j).io.outputC")
    }

  for (j <- 0 until blockCol) {
    io.outputC(j) := RegNext(PE2DVector(blockRow - 1)(j).io.outputC, 0.S )
    if (ShowHardwareWiring.switch)
      println(s"io.outputC($j) := RegNext(PE2DVector(${blockRow - 1})($j).io.outputC, 0.S )")
  }

  io.outputA := RegNext(io.inputA, VecInit.fill(numberOfInputA)(0.S))

  if (ShowHardwareWiring.switch)
    println()

}
