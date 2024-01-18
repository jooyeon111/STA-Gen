package STA_Gen.Submodule

import chisel3._

class MAC(vectorSize : Int) extends Module {

  require(vectorSize >= 1, "Number of multiplier inside of MAC unit must be at least 1")

  val io = IO (new Bundle {
    val inputA: Vec[SInt] = Input(Vec(vectorSize, SInt(8.W)))
    val inputB: Vec[SInt] = Input(Vec(vectorSize, SInt(8.W)))
    //val previousInput : SInt = Input(SInt(32.W))
    val MACOutput: SInt = Output(SInt(32.W))
  })

  val parallelMultiplier0: ParallelMultiplier = Module(new ParallelMultiplier(vectorSize))
  val reductionUnit: PipelinedAdderTree = Module(new PipelinedAdderTree(vectorSize))

  parallelMultiplier0.io.inputA := io.inputA
  parallelMultiplier0.io.inputB := io.inputB

  reductionUnit.io.parallelInputs := RegNext( parallelMultiplier0.io.PMulOutput , VecInit.fill(vectorSize)(0.S))
  io.MACOutput := reductionUnit.io.accumulationOutput// + io.previousInput

}
