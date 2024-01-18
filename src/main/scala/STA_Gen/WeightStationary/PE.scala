package STA_Gen.WeightStationary

import STA_Gen.Submodule.MAC
import chisel3._


class PE(vectorSize : Int, inputCFlag : Boolean)extends Module {

  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  override val desiredName = s"WeightStationaryPE$vectorSize"

  val io = IO(new Bundle {

    val inputA: Vec[SInt] = Input(Vec(vectorSize, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(vectorSize, SInt(8.W)))
    val inputC = if(inputCFlag) Some(Input(SInt(32.W))) else None

    val propagateWeight: Bool = Input(Bool())
    //val partialSumReset= Input(Bool())

    val outputB: Vec[SInt] = Output(Vec(vectorSize, SInt(8.W)))
    val outputC: SInt = Output(SInt(32.W))

  })

  val MAC0: MAC = Module(new MAC(vectorSize))


  io.outputB := RegNext(Mux(io.propagateWeight, io.inputB, io.outputB), VecInit.fill(vectorSize)(0.S))

  MAC0.io.inputA := io.inputA
  MAC0.io.inputB := io.outputB

//  io.outputC := MAC0.io.MACOutput + io.inputC.get

  if(inputCFlag)
    io.outputC := RegNext(MAC0.io.MACOutput + io.inputC.get, 0.S(32.W))
  else
    io.outputC := RegNext(MAC0.io.MACOutput, 0.S(32.W))

}
