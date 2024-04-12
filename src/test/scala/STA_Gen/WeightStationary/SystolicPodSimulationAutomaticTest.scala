//package STA_Gen.WeightStationary
//
//import chisel3._
//import chiseltest._
//import org.scalatest.flatspec.AnyFlatSpec
//import STA_Gen.Submodule.SystolicTensorArrayConfig
//import STA_Gen.{ConfigurationParser, GemmDimension, GemmDimensionParser, Mapper, SramHexFileWriter}
//
//class SystolicPodSimulationAutomaticTest extends AnyFlatSpec with ChiselScalatestTester   {
//
//  val configurationParser = new ConfigurationParser
//  val gemmDimensionParser = new GemmDimensionParser
//
//  if(configurationParser.getDataflow == "Ws"){
//
//    behavior of "Systolic pod"
//    it should "calculate GEMMs {M,N,K} with config {array row, array col, block row, block col, vector size} systolic tensor array" in {

      //    val targetGemm = GemmDimension(16,16,16)
      //    val targetConfiguration = SystolicTensorArrayConfig(4,3,4,3,4)
      //
      //    val mapper = new Mapper(targetGemm, targetConfiguration)
      //    val taskVector = mapper.taskVector
      //    val sramHexFileWriter = new SramHexFileWriter(mapper)
      //
      //    sramHexFileWriter.createHexFileOutputStationarySimulation()
      //
      //    test(new SystolicPodSimulation(targetConfiguration, taskVector.length,12, sramHexFileWriter.targetDirectory)).withAnnotations(Seq(WriteVcdAnnotation)) { pod =>
      //
      //      for( (task, index) <- taskVector.zipWithIndex ){
      //
      //        pod.io.queueValid.poke(true.B)
      //        pod.io.queueTask.M.poke(task.M.U)
      //        pod.io.queueTask.N.poke(task.N.U)
      //        pod.io.queueTask.K.poke(task.K.U)
      //        pod.clock.step()
      //
      //        if( index == taskVector.length - 1)
      //          pod.io.queueValid.poke(false.B)
      //
      //      }
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step()
      //      pod.clock.step(100)
      //
      //    }
      //    sramHexFileWriter.deleteHexFileOutputStationarySimulation()
//    }
//  }
//}
