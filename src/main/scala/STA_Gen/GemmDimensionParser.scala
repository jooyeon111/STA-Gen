package STA_Gen

import STA_Gen.Submodule.SystolicTensorArrayConfig

import scala.io.Source

class GemmDimensionParser {

  val filePath = "GEMM.txt"
  val fileSource = Source.fromFile(filePath)

  private var M: Int = -1
  private var N: Int = -1
  private var K: Int = -1

  try {
    for(line <- fileSource.getLines() if line.trim.nonEmpty){
      val columns = line.split(":")

      val parameter = columns(0).trim
      val parameterValue = columns(1).trim.toInt

      parameter match {
        case "M" => M = parameterValue
        case "N" => N = parameterValue
        case "K" => K = parameterValue

        case _ =>
          Console.err.println("[error] Invalid parameter found")
          sys.exit(1)
      }
    }
  } finally{
    fileSource.close()
  }

  assert(M != -1, "[error] invalid value is found in M ")
  assert(N != -1, "[error] invalid value is found in N ")
  assert(K != -1, "[error] invalid value is found in A ")

  val gemmDimension = GemmDimension(M, N, K)


  def getM: Int = M

  def getN: Int = N

  def getK: Int = K


}
