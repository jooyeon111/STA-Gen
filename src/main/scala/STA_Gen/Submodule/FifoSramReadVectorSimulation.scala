package STA_Gen.Submodule

import chisel3._

//simulation for ...
class FifoSramReadVectorSimulation(arrayDimension: Int, blockDimension: Int, vectorSize: Int, wordSize: Int, memoryHexaFile: String)  extends Module{

  require(arrayDimension >= 1, "[error] Array dimension must be at least 1")
  require(blockDimension >= 1, "[error] Block dimension must be at least 1")
  require(vectorSize >= 1, "[error] Vector size dimension must be at least 1")
  require(wordSize == 8 || wordSize == 32, "[error] Word size dimension must be 8 or 32")

  val numberOfInputPorts = arrayDimension * blockDimension * vectorSize

  val io = IO(new Bundle {
    val readEnable = Input(Bool())
    val readData = Output(Vec(numberOfInputPorts, SInt(wordSize.W)))

  })

  val sramVector = Vector.tabulate(numberOfInputPorts){x => Module(new FifoSramReadSimulation(wordSize, memoryHexaFile + x.toString  +".txt"))}


  for (sram <- sramVector){
    sram.io.readEnable := io.readEnable
  }

  for( i <- 0 until numberOfInputPorts){
    io.readData(i) := sramVector(i).io.readData
  }

}
