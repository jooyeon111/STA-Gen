package STA_Gen.Submodule

import chisel3._

class PipelinedAdderTree(vectorSize : Int) extends Module {

  require(vectorSize >= 1, "[error] Vector size must be over 1")
  override val desiredName = s"PipeLinedAdderTree$vectorSize"

  val io = IO (new Bundle {
    val parallelInputs: Vec[SInt] = Input(Vec(vectorSize ,SInt(16.W)))
    val accumulationOutput: SInt = Output(SInt(32.W))
  })

  if(vectorSize == 1){
    io.accumulationOutput := io.parallelInputs(0)
  }else {
    io.accumulationOutput := io.parallelInputs.reduceTree(
      (a, b) => RegNext(a +& b, 0.S),
      a => RegNext(a, 0.S)
    )
  }

}

