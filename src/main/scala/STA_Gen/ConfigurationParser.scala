package STA_Gen

import STA_Gen.Submodule.SystolicTensorArrayConfig

import scala.io.Source

//TODO make dataflow as enumeration
class ConfigurationParser {

  val filePath = "config.txt"
  val fileSource = Source.fromFile(filePath)

  private var M: Int = -1
  private var N: Int = -1
  private var A: Int = -1
  private var B: Int = -1
  private var C : Int = -1
  private var dataflow: String = "Unknown"

  try {
    for(line <- fileSource.getLines() if line.trim.nonEmpty){
      val columns = line.split(":")

      val parameter = columns(0).trim
      val parameterValue = columns(1).trim

      parameter match {
        case "M" => M = parameterValue.toInt
        case "N" => N = parameterValue.toInt
        case "A" => A = parameterValue.toInt
        case "B" => B = parameterValue.toInt
        case "C" => C = parameterValue.toInt
        case "Dataflow" => dataflow = parameterValue
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
  assert(A != -1, "[error] invalid value is found in A ")
  assert(B != -1, "[error] invalid value is found in B ")
  assert(C != -1, "[error] invalid value is found in C ")
  assert(dataflow == "Os" || dataflow  =="Ws", "[error] unknown dataflow is found")

  val systolicTensorArrayConfig = SystolicTensorArrayConfig(M, N, A, B, C)

  def getDataflow: String = dataflow

}
