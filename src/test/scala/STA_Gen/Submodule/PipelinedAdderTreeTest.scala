package STA_Gen.Submodule

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.math.{ceil, log10}
import scala.util.Random

//class PipelinedAdderTreeTest extends AnyFreeSpec with Matchers {
//
//  "satisfy sequentially add" in {
//    val adderTreeInputSize : Int = Random.between(1,10)
//    val adderTreeHeight = ceil(log10(adderTreeInputSize.toDouble)/log10(2.0)).toInt
//    val inputVector = Vector.fill(adderTreeInputSize)(Random.nextInt(10))
//    val vectorSum = inputVector.sum
//
//    println(s"Pipe lined adder tree input size : $adderTreeInputSize")
//    println(s"Pipe lined adder tree height : $adderTreeHeight")
//    for( inputValue <- inputVector ){
//      print(inputValue)
//      print(" ")
//    }
//    println()
//    println(s"Vector sum : $vectorSum")
//
//    simulate(new PipelinedAdderTree(adderTreeInputSize)) { c =>
//
//      for( i <- inputVector.indices)
//        c.io.parallelInputs(i).poke(inputVector(i).S)
//      c.clock.step(adderTreeHeight)
//      c.io.accumulationOutput.expect(vectorSum.S)
//
//    }
//  }
//}
