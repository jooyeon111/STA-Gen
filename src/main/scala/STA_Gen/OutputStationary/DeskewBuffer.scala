package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.ShiftRegister

class DeskewBuffer(arrayRow: Int, arrayCol: Int, blockRow: Int, blockCol: Int) extends Module {

  require(arrayRow >= 1, "[error] Deskew buffer vector needs at least 1 array row")
  require(arrayCol >= 1, "[error] Deskew buffer vector needs at least 1 array column")
  require(blockRow >= 1, "[error] Deskew buffer vector needs at least 1 block row")
  require(blockCol >= 1, "[error] Deskew buffer vector needs at least 1 block column")

  val numberOfPorts: Int = (arrayRow + arrayCol - 1) * blockRow * blockCol

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfPorts, SInt(32.W)))
    val output: Vec[SInt] = Output(Vec(numberOfPorts, SInt(32.W)))
  })

  for( i <- 0 until arrayRow + arrayCol - 1)
    for(j <- 0 until blockRow * blockCol){

      val index = i * blockRow*blockCol + j
      val depth = if( i < arrayRow - 1 ){arrayRow - i - 1} else 0

      io.output(index) := ShiftRegister(io.input(index), depth, 0.S, true.B)

    }

}
