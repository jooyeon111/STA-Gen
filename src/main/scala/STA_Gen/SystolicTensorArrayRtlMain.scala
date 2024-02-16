package STA_Gen

import STA_Gen.OutputStationary.SystolicPodRTL
import circt.stage.ChiselStage
//import STA_Gen.WeightStationary.SystolicPodRTL

object SystolicTensorArrayRtlMain extends App{

  val configurationParser = new ConfigurationParser
  val gemmDimensionParser = new GemmDimensionParser
  val taskQueueEntries = 10

  ChiselStage.emitSystemVerilog(
    gen = new STA_Gen.OutputStationary.SystolicPodRTL(configurationParser.systolicTensorArrayConfig, taskQueueEntries),
    firtoolOpts = Array("--verilog", "-o=Output_Stationary_Systolic_Tensor_Array.v", "-disable-all-randomization", "-strip-debug-info", "--lowering-options=disallowPackedArrays")
  )

}