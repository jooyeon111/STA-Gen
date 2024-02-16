package STA_Gen.Submodule

import chisel3._

class FifoSramInputVectorRTL(arrayDimension: Int, blockDimension: Int, vectorSize: Int) extends Module{

  val numberOfInputPorts = arrayDimension * blockDimension * vectorSize

  val io = IO(new Bundle {

    val writeEnable = Input(Bool())
    val writeData = Input(Vec(numberOfInputPorts, SInt(8.W)))

    val readEnable = Input(Bool())
    val readData = Output(Vec(numberOfInputPorts, SInt(8.W)))

  })

  val sramVector = Vector.fill(numberOfInputPorts){Module(new FifoSramRTL(8)) }


  for (sram <- sramVector) {
    sram.io.readEnable := io.readEnable
    sram.io.writeEnable := io.writeEnable
  }

  for(i <- 0 until numberOfInputPorts){
    io.readData(i) := sramVector(i).io.readData
    sramVector(i).io.writeData := io.writeData(i)
  }


}
