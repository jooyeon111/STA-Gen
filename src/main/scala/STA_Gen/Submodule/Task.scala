package STA_Gen.Submodule

import chisel3._
class Task extends Bundle {
  val M = UInt(8.W)
  val N = UInt(8.W)
  val K = UInt(8.W)
}
