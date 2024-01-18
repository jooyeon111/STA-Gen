package STA_Gen.DualStationary

import chisel3._
import STA_Gen.Submodule.MyShiftRegister

class SkewBufferVector(bandwidth: Int, arrayDimension: Int, blockDimension: Int, vectorSize: Int) extends Module {

  require(arrayDimension >= 1, "[error] Array row must be at least 1")
  require(blockDimension >= 1, "[error] Array col must be at least 1")
  require(vectorSize >= 1, "[error] Block row must be at least 1")

  val numberOfPorts: Int = arrayDimension * blockDimension * vectorSize
  val io = IO(new Bundle {
    //Input
    val input: Vec[SInt] = Input(Vec(numberOfPorts, SInt(8.W)))
    //Control signal
    val shiftEnable: Vec[Bool] = Input(Vec(arrayDimension, Bool()))
    //Output
    val output: Vec[SInt] = Output(Vec(numberOfPorts, SInt(8.W)))
  })

  /*
  *                                               Module assign
  * */
  val skewVector: Vector[MyShiftRegister] =
    (0 until arrayDimension map ( depth => Module(new MyShiftRegister(bandwidth, blockDimension * vectorSize, depth )))).toVector

  /*
  *                                               Input wiring
  * */
  for (i <- 0 until arrayDimension)
    for (j <- 0 until blockDimension * vectorSize)
      skewVector(i).io.input(j) := io.input(j + i * blockDimension * vectorSize)


  /*
  *                                            Control signal wiring
  * */
  for (i <- 0 until arrayDimension)
    skewVector(i).io.shiftEnable := io.shiftEnable(i)

  /*
  *                                               Output wiring
  * */
  for (i <- 0 until arrayDimension)
    for (j <- 0 until blockDimension * vectorSize )
      io.output(j + i * blockDimension * vectorSize) := skewVector(i).io.output(j)

}
