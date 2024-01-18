package STA_Gen.Submodule

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.experimental.loadMemoryFromFileInline

class FifoSramReadSimulation(wordSize: Int, memoryHexaFile: String) extends Module {

  require(wordSize == 8 || wordSize == 32, "[error] Word size dimension must be 8 or 32")

  val io = IO(new Bundle {

    val readEnable = Input(Bool())
    val readData = Output(SInt(wordSize.W))

  })

  val numberOfWords: Int = 256
  val mem = SyncReadMem(numberOfWords, SInt(wordSize.W))
  val readPtr = RegInit(1.U(log2Ceil(numberOfWords).W))

  io.readData := 0.S

  if(memoryHexaFile.trim().nonEmpty){
    loadMemoryFromFileInline(mem, memoryHexaFile)
  }

  when(io.readEnable) {
    readPtr := readPtr + 1.U
    io.readData := mem.read(readPtr, io.readEnable)
  }

}
