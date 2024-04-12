package STA_Gen.OutputStationary

import STA_Gen.Submodule.MyShiftRegister
import chisel3._

class DeskewBufferVector(arrayRow: Int, arrayCol: Int, blockRow: Int, blockCol: Int) extends Module {


  require(arrayRow >= 1, "[error] Deskew buffer vector needs at least 1 array row")
  require(arrayCol >= 1, "[error] Deskew buffer vector needs at least 1 array column")
  require(blockRow >= 1, "[error] Deskew buffer vector needs at least 1 block row")
  require(blockCol >= 1, "[error] Deskew buffer vector needs at least 1 block column")

  val numberOfPorts: Int = (arrayRow + arrayCol - 1) * blockRow * blockCol
  //val muxSignalBits: Int = ceil(log10(arrayRow.toDouble) / log10(2.0)).toInt

  val io = IO(new Bundle {
    val input: Vec[SInt] = Input(Vec(numberOfPorts, SInt(32.W)))
    val shiftEnable: Vec[Bool] = Input(Vec(arrayRow + arrayCol - 1, Bool()))
    val output: Vec[SInt] = Output(Vec(numberOfPorts, SInt(32.W)))
  })

  /*
  *                                               Module assign
  * */
  val skewVector: Vector[MyShiftRegister] =
    Vector.tabulate(arrayRow + arrayCol - 1)(x => if (x < arrayRow - 1) {
      Module(new MyShiftRegister(32, blockRow * blockCol, arrayRow - x - 1))
    }
    else {
      Module(new MyShiftRegister(32, blockRow * blockCol, 0))
    })

  /*
  *                                               Input wiring
  * */
  for (i <- 0 until arrayRow + arrayCol - 1)
    for (j <- 0 until blockRow * blockCol)
      skewVector(i).io.input(j) := io.input(i * blockRow * blockCol + j)


  /*
  *                                            Control signal wiring
  * */
  for (i <- 0 until arrayRow + arrayCol - 1)
    skewVector(i).io.shiftEnable := io.shiftEnable(i)

  /*
  *                                               Output wiring
  * */
  for (i <- 0 until arrayRow + arrayCol - 1)
    for (j <- 0 until blockRow * blockCol)
      io.output(i * blockRow * blockCol + j) := skewVector(i).io.output(j)


}
