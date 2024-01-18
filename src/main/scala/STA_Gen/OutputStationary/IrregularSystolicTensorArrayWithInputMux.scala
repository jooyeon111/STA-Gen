package STA_Gen.OutputStationary

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.MuxCase
import circt.stage.ChiselStage

class IrregularSystolicTensorArrayWithInputMux (
  val arrayRow: Int,
  val arrayCol : Int,
  val blockRow : Int,
  val blockCol : Int,
  val vectorSize : Int,
//  val unifiedInputPortNumber: Int
) extends Module {

//  require(arrayRow*blockRow == arrayCol*blockCol, "[error] Only square systolic tensor array is allowed")
//  require(arrayRow*blockRow*vectorSize <= unifiedInputPortNumber, "[error] unified number of must be at least number of systolic tensor array")
//  require(unifiedInputPortNumber % arrayRow * blockRow == 0, "[error]")

  val unifiedInputPortNumberA = 16
  val unifiedInputPortNumberB = 32


  override val desiredName = s"Os_Sta_${arrayRow}x${arrayCol}x${blockRow}x${blockCol}x${vectorSize}_syn"

  val numberOfPEs: Int = blockRow * blockCol
  val numberOfOutputs: Int = (arrayCol + arrayRow - 1) * numberOfPEs
  val numberOfSystolicTensorArrayInputA = arrayRow * blockRow * vectorSize
  val numberOfSystolicTensorArrayInputB = arrayCol * blockCol * vectorSize
  val muxSignalBitsA: Int = log2Ceil(unifiedInputPortNumberA /numberOfSystolicTensorArrayInputA)
  val muxSignalBitsB: Int = log2Ceil(unifiedInputPortNumberB /numberOfSystolicTensorArrayInputB)

  val io = IO(new Bundle {
    val inputA: Vec[SInt] = Input(Vec(unifiedInputPortNumberA, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(unifiedInputPortNumberB, SInt(8.W)))

    val inputAMuxSignal: UInt = Input(UInt(muxSignalBitsA.W))
    val inputBMuxSignal: UInt = Input(UInt(muxSignalBitsB.W))

    val propagateOutput: Vec[Vec[Bool]] = Input(Vec(arrayRow - 1, Vec(arrayCol - 1, Bool())))
    val partialSumReset: Vec[Vec[Bool]] = Input(Vec(arrayRow, Vec(arrayCol, Bool())))

    val outputC: Vec[SInt] = Output(Vec(numberOfOutputs, SInt(32.W)))
  })

  val delayedInputA = RegNext(io.inputA, VecInit.fill(unifiedInputPortNumberA)(0.S(8.W)))
  val delayedInputB = RegNext(io.inputB, VecInit.fill(unifiedInputPortNumberB)(0.S(8.W)))
  val systolicTensorArray = Module(new SystolicTensorArray(arrayRow, arrayCol, blockRow, blockCol, vectorSize))

  val inputASemaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for ( i <- 0 until numberOfSystolicTensorArrayInputA) yield {
    for ( j <- 0 until unifiedInputPortNumberA/numberOfSystolicTensorArrayInputA)
      yield {
        (io.inputAMuxSignal === j.U) -> delayedInputA(i * unifiedInputPortNumberA/numberOfSystolicTensorArrayInputA + j)
      }
  }

  val inputBSemaphore: IndexedSeq[IndexedSeq[(Bool, SInt)]] = for (i <- 0 until numberOfSystolicTensorArrayInputB) yield {
    for (j <- 0 until unifiedInputPortNumberB / numberOfSystolicTensorArrayInputB)
      yield {
        (io.inputBMuxSignal === j.U) -> delayedInputB(i * unifiedInputPortNumberB/numberOfSystolicTensorArrayInputB + j)
      }
  }
  for (i <- 0 until numberOfSystolicTensorArrayInputA) {
    systolicTensorArray.io.inputA(i) := MuxCase(0.S(8.W), inputASemaphore(i))
  }

  for (i <- 0 until numberOfSystolicTensorArrayInputB) {
    systolicTensorArray.io.inputB(i) := MuxCase(0.S(8.W), inputBSemaphore(i))
  }

  for (i <- 0 until arrayRow - 1)
    for (j <- 0 until arrayCol - 1)
      systolicTensorArray.io.propagateSignal(i)(j) := RegNext(io.propagateOutput(i)(j), false.B)

  //Wiring partial sum reset signal
  for (i <- 0 until arrayRow)
    for (j <- 0 until arrayCol)
      systolicTensorArray.io.partialSumReset(i)(j) := RegNext(io.partialSumReset(i)(j), false.B)

  io.outputC := systolicTensorArray.io.outputC
}

object IrregularSystolicTensorArrayWithInputMux extends App {
  ChiselStage.emitSystemVerilog(
    gen = new IrregularSystolicTensorArrayWithInputMux(8, 8, 1, 2, 2),
    firtoolOpts = Array("--verilog", "-o=Os_Sta_8x8x1x2x2_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
  ChiselStage.emitSystemVerilog(
    gen = new IrregularSystolicTensorArrayWithInputMux(8, 8, 1, 2, 2),
    firtoolOpts = Array("--verilog", "-o=Os_Sta_8x8x2x1x2_syn.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
}
