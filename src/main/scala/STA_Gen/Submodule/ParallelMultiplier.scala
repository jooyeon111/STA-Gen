package STA_Gen.Submodule

import chisel3._


class ParallelMultiplier(vectorSize : Int) extends Module {

    require(vectorSize >= 1, "Number of inputs inside of parallel multiplier unit must be at least 1")

    val io = IO (new Bundle {
        val inputA: Vec[SInt] = Input(Vec(vectorSize,SInt(8.W)))
        val inputB: Vec[SInt] = Input(Vec(vectorSize,SInt(8.W)))
        val PMulOutput: Vec[SInt] = Output(Vec(vectorSize,SInt(16.W)))
    })

    //io.C := io.A.zip(io.B).map( x => x._1 * x._2)

    for(i <- 0 until vectorSize){
      io.PMulOutput(i) := RegNext(io.inputA(i) * io.inputB(i), 0.S)
    }

    
}

