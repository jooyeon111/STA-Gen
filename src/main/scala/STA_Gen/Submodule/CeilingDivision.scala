package STA_Gen.Submodule

import chisel3._

class CeilingDivision extends Module {

  val io = IO(new Bundle {
    val numerator = Input(UInt(8.W))
    val denominator = Input(UInt(8.W))
    val result = Output(UInt(8.W))
  })
  val quotient = Wire(UInt(8.W))
  val remainder = Wire(UInt(8.W))

  quotient := io.numerator / io.denominator
  remainder := io.numerator % io.denominator

  when(remainder =/= 0.U){
    io.result := quotient + 1.U
  }.otherwise{
    io.result := quotient
  }

}
