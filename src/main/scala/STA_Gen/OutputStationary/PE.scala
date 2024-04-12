package STA_Gen.OutputStationary

import chisel3._
import STA_Gen.Submodule.MAC

class PE(val vectorSize : Int) extends Module {
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")
    
  override val desiredName = s"Os_PE_$vectorSize"

  val io = IO (new Bundle {

    //Input
    val inputA: Vec[SInt] = Input(Vec(vectorSize,SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(vectorSize,SInt(8.W)))

    //Control
    val partialSumReset: Bool = Input(Bool())

    //Output
    val outputC: SInt = Output(SInt(32.W))

  })

  val MAC0: MAC = Module (new MAC(vectorSize))

  MAC0.io.inputA := io.inputA
  MAC0.io.inputB := io.inputB

  val outputRegister = RegInit(0.S)

  outputRegister := MAC0.io.MACOutput + Mux(io.partialSumReset, 0.S, outputRegister)

  io.outputC := outputRegister

}
