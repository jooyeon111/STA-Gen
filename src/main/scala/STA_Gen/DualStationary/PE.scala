package STA_Gen.DualStationary

import chisel3._
import circt.stage.ChiselStage
import STA_Gen.Submodule.{MAC, PipelinedAdderTree}

class PE(vectorSize : Int, isFirstRow : Boolean) extends Module {

  override val desiredName = s"dual_stationary_PE_$vectorSize"


  val io = IO(new Bundle {

    val inputA = Input(Vec(vectorSize, SInt(8.W)))
    val inputB = Input(Vec(vectorSize, SInt(8.W)))
    val wsInputC = if (isFirstRow) None else Some(Input(SInt(32.W)))

    val isOsOrWs: Bool = Input(Bool())
    val propagateWeight: Bool = Input(Bool())
    val partialSumReset: Bool = Input(Bool())

    val outputB = Output(Vec(vectorSize, SInt(8.W)))
    val osOutputC = Output(SInt(32.W))
    val wsOutputC = Output(SInt(32.W))

  })

  //Wire
  val loadingWeight = Wire(Vec(vectorSize, SInt(8.W)))
  val readyInputB = Wire(Vec(vectorSize, SInt(16.W)))

  //Module
  val inputDeMux = Module(new DeMux(Vec(vectorSize, SInt(8.W))))
  val mac = Module(new MAC(vectorSize))
  val partialSumDeMux = Module(new DeMux(SInt(32.W)))

  inputDeMux.io.input := io.inputB
  inputDeMux.io.sel := io.isOsOrWs

  loadingWeight := RegNext(Mux(io.propagateWeight, io.inputB, io.outputB), VecInit.fill(vectorSize)(0.S))

  readyInputB := Mux(io.isOsOrWs, loadingWeight, inputDeMux.io.outputTrue)
  mac.io.inputB := readyInputB
  mac.io.inputA := io.inputA

  partialSumDeMux.io.sel := io.isOsOrWs
  partialSumDeMux.io.input := mac.io.MACOutput

  io.wsOutputC := RegNext(partialSumDeMux.io.outputFalse + io.wsInputC.get, 0.S(32.W) )
  io.osOutputC := RegNext(partialSumDeMux.io.outputTrue + Mux(io.partialSumReset, 0.S, io.osOutputC), 0.S(32.W) )
  io.outputB := loadingWeight

}

object PE extends App{
  ChiselStage.emitSystemVerilog(
    gen = new PE(4, false),
    firtoolOpts = Array("--verilog", "-o=dual_stationary_PE.v" , "-disable-all-randomization" ) //, "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )
}