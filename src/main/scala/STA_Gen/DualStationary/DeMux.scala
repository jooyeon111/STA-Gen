package STA_Gen.DualStationary

import chisel3._

class DeMux[T <: Data](portType: T) extends Module {

  val io = IO(new Bundle {

    val input = Input(portType)
    val sel: Bool = Input(Bool())
    val outputFalse = Output(portType)
    val outputTrue = Output(portType)

  })

  io.outputFalse := DontCare
  io.outputTrue := DontCare

  when(io.sel){
    io.outputTrue := io.input
  }.otherwise{
    io.outputFalse := io.input
  }

}
