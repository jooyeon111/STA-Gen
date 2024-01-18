package STA_Gen.Submodule

case class SystolicTensorArrayConfig(

  arrayRow : Int,
  arrayCol : Int,
  blockRow : Int,
  blockCol : Int,
  vectorSize : Int

) {

  require(arrayRow >= 1, "[error] Array row must be at least 1")
  require(arrayCol >= 1, "[error] Array col must be at least 1")
  require(blockRow >= 1, "[error] Block row must be at least 1")
  require(blockCol >= 1, "[error] Block col must be at least 1")
  require(vectorSize >= 1, "[error] Number of multiplier inside of processing elements must be at least 1")

  def printConfig() : Unit = {

    println(s"Array row : $arrayRow")
    println(s"Array col : $arrayCol")
    println(s"Block row : $blockRow")
    println(s"Block col : $blockCol")
    println(s"Vector size : $vectorSize")
    println()
  }

  def convertString : String = {
    arrayRow.toString + "x" + arrayCol.toString + "x" + blockRow.toString + "x" + blockCol.toString + "x" + vectorSize.toString
  }
}
