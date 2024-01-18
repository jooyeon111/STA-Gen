package STA_Gen.WeightStationary

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.MuxCase
import circt.stage.ChiselStage

/*
  This module is for synthesizing systolic tensor array
 */
class SystolicTensorArrayWithInputMux(
  val arrayRow: Int,
  val arrayCol : Int,
  val blockRow : Int,
  val blockCol : Int,
  val vectorSize : Int,
  val unifiedInputPortNumber: Int
) extends Module {

  require(arrayRow*blockRow == arrayCol*blockCol, "[error] Only square systolic tensor array is allowed")
  require(arrayRow*blockRow*vectorSize <= unifiedInputPortNumber, "[error] unified number of must be at least number of systolic tensor array")
  require(unifiedInputPortNumber % arrayRow * blockRow == 0, "[error]")

  override val desiredName = s"Ws_Sta_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x${vectorSize}_syn"

  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOutputs: Int = arrayCol * blockCol
  val numberOfSystolicTensorArrayInput = arrayRow * blockRow * vectorSize
  val muxSignalBits: Int = log2Ceil(unifiedInputPortNumber /numberOfSystolicTensorArrayInput)

  val io = IO(new Bundle {
    val inputA: Vec[SInt] = Input(Vec(unifiedInputPortNumber, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(unifiedInputPortNumber, SInt(8.W)))

    val inputAMuxSignal: UInt = Input(UInt(muxSignalBits.W))
    val inputBMuxSignal: UInt = Input(UInt(muxSignalBits.W))

    val propagateWeight: Vec[Bool] = Input(Vec(arrayRow, Bool()))

    val outputC: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))
  })

  val delayedInputA = RegNext(io.inputA, VecInit.fill(unifiedInputPortNumber)(0.S(8.W)))
  val delayedInputB = RegNext(io.inputB, VecInit.fill(unifiedInputPortNumber)(0.S(8.W)))
  val systolicTensorArray = Module(new SystolicTensorArray(arrayRow, arrayCol, blockRow, blockCol, vectorSize))

  val inputASemaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for ( i <- 0 until numberOfSystolicTensorArrayInput) yield {
    for ( j <- 0 until unifiedInputPortNumber/numberOfSystolicTensorArrayInput)
      yield {
        (io.inputAMuxSignal === j.U) -> delayedInputA(i * unifiedInputPortNumber/numberOfSystolicTensorArrayInput + j)
      }
  }

  val inputBSemaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for (i <- 0 until numberOfSystolicTensorArrayInput) yield {
    for (j <- 0 until unifiedInputPortNumber / numberOfSystolicTensorArrayInput)
      yield {
        (io.inputBMuxSignal === j.U) -> delayedInputB(i * unifiedInputPortNumber/numberOfSystolicTensorArrayInput + j)
      }
  }
  for (i <- 0 until numberOfSystolicTensorArrayInput) {
    systolicTensorArray.io.inputA(i) := MuxCase(0.S(8.W), inputASemaphore(i))
  }

  for (i <- 0 until numberOfSystolicTensorArrayInput) {
    systolicTensorArray.io.inputB(i) := MuxCase(0.S(8.W), inputBSemaphore(i))
  }

  for (i <- 0 until arrayRow)
      systolicTensorArray.io.propagateWeight(i) := RegNext(io.propagateWeight(i), false.B)

  io.outputC := systolicTensorArray.io.outputC

}

object SystolicTensorArrayWithInputMux extends App {
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(32,32,1,1,1, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_32x32x1x1x1_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(16, 16, 2, 2, 1, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_16x16x2x2x1_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(8, 8, 4, 4, 1, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_8x8x4x4x1_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(16, 16, 1, 1, 4, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_16x16x1x1x4_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(8, 8, 2, 2, 4, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_8x8x2x2x4_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(4, 4, 4, 4, 4, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_4x4x4x4x4_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(8, 8, 1, 1, 16, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_8x8x1x1x16_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(4, 4, 2, 2, 16, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_4x4x2x2x16_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new SystolicTensorArrayWithInputMux(2, 2, 4, 4, 16, 128),
    firtoolOpts = Array("--verilog", "-o=Ws_Sta_2x2x4x4x16_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
}
