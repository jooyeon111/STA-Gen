package STA_Gen.Submodule

import chisel3._
import chisel3.util.log2Ceil

class FifoSramRTL(wordSize: Int = 8) extends Module {

  val io = IO(new Bundle {

    val writeEnable = Input(Bool())
    val writeData = Input(UInt(wordSize.W))

    val readEnable = Input(Bool())
    val readData = Output(UInt(wordSize.W))

  })

  //TODO logics that can handle various number of words size
  val numberOfWords: Int = 256
  val mem = SyncReadMem(numberOfWords, UInt(wordSize.W))

  val readPtr = RegInit(0.U(log2Ceil(numberOfWords)))
  val writePtr = RegInit(0.U(log2Ceil(numberOfWords)))
  val count = RegInit(0.U(log2Ceil(numberOfWords) + 1))

  val full = Wire(Bool())
  val empty = Wire(Bool())

  when(io.writeEnable && !full) {
    mem(writePtr) := io.writeData
    writePtr := writePtr + 1.U
    count := count + 1.U
  }

  when(io.readEnable && !empty) {
    readPtr := readPtr + 1.U
    io.readData := mem.read(readPtr)
    count := count - 1.U
  }

  when(count === 0.U){
    readPtr := 0.U
  }

  when(count === numberOfWords.U){
    writePtr := 0.U
  }

  full := count === numberOfWords.U
  empty := count === 0.U
}
