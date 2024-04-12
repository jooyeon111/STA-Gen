package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.MuxCase
import scala.math.{ceil, log10}
import STA_Gen.Submodule.Nto1MuxControlSynchronizer

//TODO
//fix codes

class Railway(arrayRow: Int, arrayCol: Int, blockRow: Int, blockCol: Int) extends Module {

  require(arrayRow >= 1, "[error] Railway signal module needs at least 1 array row")
  require(arrayCol >= 1, "[error] Railway signal module needs at least 1 array column")
  require(arrayRow >= 1, "[error] Railway signal module needs at least 1 block row")
  require(arrayCol >= 1, "[error] Railway signal module needs at least 1 block column")

  val numberOfInputs: Int = (arrayRow + arrayCol - 1) * blockRow * blockCol
  val muxSignalBits: Int = ceil(log10(arrayRow.toDouble) / log10(2.0)).toInt
  val numberOfOutputs: Int = arrayRow * blockRow * blockCol


  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfInputs, SInt(32.W)))
    val start = Input(Bool())
    val output: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))
  })

  val nto1MuxControlSynchronizer = Module(new Nto1MuxControlSynchronizer(arrayCol))
  nto1MuxControlSynchronizer.io.start := io.start

  val semaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for (i <- 0 until numberOfOutputs) yield {
    //TODO find the difference between arrayRow and array col for below codes
    for (j <- 0 until arrayCol)
      yield {
        (nto1MuxControlSynchronizer.io.muxSignal === j.U) -> io.input(i+ (blockRow*blockCol * j))
      }
  }

  for (i <- 0 until numberOfOutputs) {
    io.output(i) := RegNext(MuxCase(0.S(32.W), semaphore(i)), 0.S(32.W))
  }

}

//val semaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for (i <- 0 until arrayCol) yield {
//  for (j <- 0 until arrayCol)
//    yield {
//      (io.muxSignal === j.U) -> io.input(i + j)
//    }
//}
//
//for (i <- 0 until arrayRow) {
//  io.output(i) := RegNext(MuxCase(0.S(32.W), semaphore(i)), 0.S(32.W))
//}

//    val testArray0: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(0),
//      (io.muxSignal === 1.U) -> io.input(4),
//      (io.muxSignal === 2.U) -> io.input(8),
//    )
//
//    val testArray1: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(1),
//      (io.muxSignal === 1.U) -> io.input(5),
//      (io.muxSignal === 2.U) -> io.input(9),
//    )
//
//    val testArray2: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(2),
//      (io.muxSignal === 1.U) -> io.input(6),
//      (io.muxSignal === 2.U) -> io.input(10),
//    )
//
//    val testArray3: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(3),
//      (io.muxSignal === 1.U) -> io.input(7),
//      (io.muxSignal === 2.U) -> io.input(11),
//    )
//    val testArray4: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(4),
//      (io.muxSignal === 1.U) -> io.input(8),
//      (io.muxSignal === 2.U) -> io.input(12),
//    )
//    val testArray5: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(5),
//      (io.muxSignal === 1.U) -> io.input(9),
//      (io.muxSignal === 2.U) -> io.input(13),
//    )
//    val testArray6: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(6),
//      (io.muxSignal === 1.U) -> io.input(10),
//      (io.muxSignal === 2.U) -> io.input(14),
//    )
//    val testArray7: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(7),
//      (io.muxSignal === 1.U) -> io.input(11),
//      (io.muxSignal === 2.U) -> io.input(15),
//    )
//    val testArray8: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(8),
//      (io.muxSignal === 1.U) -> io.input(12),
//      (io.muxSignal === 2.U) -> io.input(16),
//    )
//    val testArray9: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(9),
//      (io.muxSignal === 1.U) -> io.input(13),
//      (io.muxSignal === 2.U) -> io.input(17),
//    )
//    val testArray10: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(10),
//      (io.muxSignal === 1.U) -> io.input(14),
//      (io.muxSignal === 2.U) -> io.input(18),
//    )
//    val testArray11: Array[(Bool, SInt)] = Array(
//      (io.muxSignal === 0.U) -> io.input(11),
//      (io.muxSignal === 1.U) -> io.input(15),
//      (io.muxSignal === 2.U) -> io.input(19),
//    )
//
//    val testVector: Vector[Array[(Bool,SInt)]] = Vector(
//      testArray0,
//      testArray1,
//      testArray2,
//      testArray3,
//      testArray4,
//      testArray5,
//      testArray6,
//      testArray7,
//      testArray8,
//      testArray9,
//      testArray10,
//      testArray11
//    )
