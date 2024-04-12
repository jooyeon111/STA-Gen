package STA_Gen.WeightStationary

import STA_Gen.Submodule.MyShiftRegister
import chisel3._
import chisel3.util.ShiftRegister

class DeskewBuffer(arrayCol: Int, blockCol: Int) extends Module {

  require(arrayCol >= 1, "[error] Deskew buffer vector array col must be at least 1")
  require(blockCol >= 1, "[error] Deskew buffer vector block col must be at least 1")

  val numberOfPorts: Int = arrayCol * blockCol

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfPorts, SInt(8.W)))
    val output: Vec[SInt] = Output(Vec(numberOfPorts, SInt(8.W)))
  })
//
//  val skewVector: Vector[MyShiftRegister] =
//    (arrayCol to 1 by -1 map (depth => Module(new MyShiftRegister(32, blockCol, depth)))).toVector
//
//  for (i <- 0 until arrayCol)
//    for (j <- 0 until blockCol)
//      skewVector(i).io.input(j) := io.input(j + i * blockCol)
//
//  for (i <- 0 until arrayCol)
//    skewVector(i).io.shiftEnable := true.B
//
//  for (i <- 0 until arrayCol)
//    for (j <- 0 until blockCol )
//      io.output(j + i * blockCol) := skewVector(i).io.output(j)

  for ( i <- 0 until arrayCol)
    for (j <- 0 until blockCol){
      val index = i * blockCol + j
      val depth = arrayCol - index

      io.output(index) := ShiftRegister(io.input(index), depth, 0.S, true.B)

    }


}
