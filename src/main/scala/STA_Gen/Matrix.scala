package STA_Gen

import scala.util.Random

object Matrix {
  def apply ( matrixElements: Vector[Vector[Int]]) = new Matrix(matrixElements)
  def randMatrix(rows : Int, cols : Int) = new Matrix (Vector.fill(rows,cols)(Random.between(1,5)))
  def randMatrix(dims : (Int,Int)) = new Matrix (Vector.fill(dims._1,dims._2)(Random.between(1,5)))
}

class Matrix( val matrixElements : Vector[Vector[Int]] ) {

  require(matrixElements.nonEmpty,"Empty matrix is cannot be allowed")

  def rows : Int = matrixElements.length
  def cols : Int = matrixElements(0).length
  def dims : (Int,Int) = (matrixElements.length, matrixElements(0).length)


  def * (that : Matrix) : Matrix = {
    assert(this.cols == that.rows, " Matrix multiplication dimension aligned ")
    new Matrix(for (row <- this.matrixElements)
      yield for (col <- that.matrixElements.transpose)
        yield row zip col map Function.tupled(_ * _) reduceLeft (_ + _))
  }

  def + (that : Matrix) : Matrix = {
    assert(this.dims == that.dims, "Only same dimension is available")
    new Matrix(
      for ((ra, rb) <- this.matrixElements zip that.matrixElements)
        yield for ((elema, elemb) <- ra zip rb)
          yield elema + elemb
    )
  }

  def printMatrix : Unit = {
    matrixElements.foreach{
      line => println(line.map(_.toString).reduce(_ + "\t" + _))
    }
  }

  def printMatrixHex : Unit = {
    matrixElements.foreach {
      line => println(line.map(_.toHexString.toUpperCase).reduce(_ + "\t" + _))
    }
  }
}
