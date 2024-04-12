package STA_Gen.OutputStationary

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import STA_Gen.{ConfigurationParser, GemmDimensionParser, Mapper, SramHexFileWriter}

class SystolicPodSimulationAutomaticTest extends AnyFlatSpec with ChiselScalatestTester   {

  val cParser = new ConfigurationParser
  val gParser = new GemmDimensionParser

  if(cParser.getDataflow == "Os") {

    behavior of "Output Stationary Systolic Tensor Array"
    it should s"calculate GEMMs {${gParser.getM} x ${gParser.getN} x ${gParser.getK} " +
      s"with config { ${cParser.getM} x ${cParser.getN} } x { ${cParser.getA} x ${cParser.getB} x ${cParser.getC} } systolic tensor array" in {

      val targetConfiguration = cParser.systolicTensorArrayConfig
      val targetGemm = gParser.gemmDimension

      val mapper = new Mapper(targetGemm, targetConfiguration)
      val taskVector = mapper.taskVector
      val sramHexFileWriter = new SramHexFileWriter(mapper)

      mapper.printTargetMatrix()
      sramHexFileWriter.createHexFileOutputStationarySimulation()

      test(new SystolicPodSimulation(targetConfiguration, taskVector.length, 12, sramHexFileWriter.targetDirectory)).withAnnotations(Seq(WriteVcdAnnotation)) { pod =>

        for ((task, index) <- taskVector.zipWithIndex) {

          pod.io.queueValid.poke(true.B)
          pod.io.queueTask.M.poke(task.M.U)
          pod.io.queueTask.N.poke(task.N.U)
          pod.io.queueTask.K.poke(task.K.U)
          pod.clock.step()

          if (index == taskVector.length - 1)
            pod.io.queueValid.poke(false.B)

        }
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step()
        pod.clock.step(200)

      }
      sramHexFileWriter.deleteHexFileOutputStationarySimulation()
    }

  }
}


