package STA_Gen.Submodule

import chisel3._

class FifoSramOutputVectorRTL(arrayRow: Int, blockRow: Int) extends Module{

  val numberOfOutputPorts = arrayRow * blockRow

  val io = IO(new Bundle {

    val writeEnable = Input(Bool())
    val writeData = Input(UInt(32.W))

    val readEnable = Input(Bool())
    val readData = Output(UInt(32.W))

  })

  val sramVector = Vector.fill(numberOfOutputPorts) {
    Module(new FifoSramRTL(32))
  }


  for (sram <- sramVector) {
    sram.io.readEnable := io.readEnable
    sram.io.writeEnable := io.writeEnable
  }

  for (i <- 0 until numberOfOutputPorts) {
    io.readData(i) := sramVector(i).io.readData
    io.writeData(i) := sramVector(i).io.writeData
  }


}
