package STA_Gen

import STA_Gen.Submodule.SystolicTensorArrayConfig

import scala.collection.mutable.ListBuffer

//Mapping one GEMM to one systolic tensor array mapping methodology
//Basically generate output row first like C(0)(0) -> C(0)(1) -> C(0)(2) ....
class Mapper(val gemm: GemmDimension, config: SystolicTensorArrayConfig) {

  val A: Matrix = Matrix.randMatrix(gemm.M, gemm.K)
  val B: Matrix = Matrix.randMatrix(gemm.K, gemm.N)
  val C: Matrix = A * B

  private val rowMultiplication = config.arrayRow * config.blockRow
  private val colMultiplication = config.arrayCol * config.blockCol

  val numberOfInputPortA = config.arrayRow * config.blockRow * config.vectorSize
  val numberOfInputPortB: Int = config.arrayCol * config.blockCol * config.vectorSize

  private val dataflow = "OS"
  val mappingConfiguration = dataflow + generateFolderName()
  val taskVector = taskDimensionMapping()
  val iterationA: Int = gemm.M / colMultiplication
  val iterationB: Int = gemm.N / rowMultiplication

  val stringBasedReshapedA: Seq[String] =
    reshapeInputMatrixA(A, rowMultiplication, iterationA).matrixElements.map(row => row.mkString(System.lineSeparator()))

  val stringBasedReshapedB: Seq[String] =
    reshapeInputMatrixB(new Matrix(B.matrixElements.transpose), colMultiplication, iterationB)
      .matrixElements.map(row => row.mkString(System.lineSeparator()))

  printTargetMatrix()

  private def printTargetMatrix(): Unit = {
    println(" Matrix A ")
    A.printMatrix
    println(" Matrix B ")
    B.printMatrix
    println(" Matrix C = A * B")
    C.printMatrix
  }

  def taskDimensionMapping(): Vector[GemmDimension] = {

    val rowCoverageQuotient: Int = gemm.M / rowMultiplication
    val colCoverageQuotient: Int = gemm.N / colMultiplication

    val rowCoverageRemainder: Int = gemm.M % rowMultiplication
    val colCoverageRemainder: Int = gemm.N % colMultiplication

    val fullCase: Int = rowCoverageQuotient * colCoverageQuotient
    val rightCase: Int = if (colCoverageRemainder != 0) rowCoverageQuotient else 0
    val bottomCase: Int = if (rowCoverageRemainder != 0) colCoverageQuotient else 0
    val bottomRightCase: Int = if (rowCoverageRemainder != 0 && colCoverageRemainder != 0) 1 else 0

    val temporalTaskVector = ListBuffer.empty[GemmDimension]

    //TODO clean this codes up
    for( i <- 0 until fullCase)
      temporalTaskVector += GemmDimension(rowMultiplication, colMultiplication, gemm.K)

    for( i <- 0 until rightCase)
      temporalTaskVector += GemmDimension(rowMultiplication,colCoverageRemainder , gemm.K)

    for (i <- 0 until bottomCase)
      temporalTaskVector += GemmDimension(rowCoverageRemainder, colMultiplication, gemm.K)

    for(i <- 0 until bottomRightCase)
      temporalTaskVector += GemmDimension(rowCoverageRemainder, colCoverageRemainder, gemm.K)

    temporalTaskVector.toVector

  }

  private def reshapeInputMatrixA(input: Matrix, multiplication: Int, iterationNumber: Int): Matrix = {

    val zeroPadRows =
    divideMatrix(input, multiplication, input.cols)
      .map(x => zeroPad2DVectorBasedOnConfiguration(x))
      .map(x => divideMatrix(x, x.rows, config.vectorSize))
      .map { x0 =>
        new Matrix(
          x0.map {
            x1 => x1.matrixElements.flatten
          }
        )
      }.map(x => new Matrix(x.matrixElements.transpose))
      .map( x => concatMatrixColumnByColumn(x, iterationNumber))
      .head.rows


    val reshapedMatrix: Matrix = divideMatrix(input, multiplication, input.cols)
      .map( x => zeroPad2DVectorBasedOnConfiguration(x))
      .map( x=> divideMatrix(x, x.rows, config.vectorSize))
      .map{ x0 =>
        new Matrix(
          x0.map{
            x1 => x1.matrixElements.flatten
          }
        )
      }.map( x => new Matrix(x.matrixElements.transpose))
      .map( x => concatMatrixColumnByColumn(x, iterationNumber))
      .map( x => zeroPad2DVector(x, zeroPadRows, x.cols))
      .reduceLeft(concatenateVectorsHorizontally)

    reshapedMatrix
  }

  private def reshapeInputMatrixB(input: Matrix, multiplication: Int, iterationNumber: Int): Matrix = {

    val zeroPadRows = {
    divideMatrix(input, multiplication, input.cols)
      .map(x => zeroPad2DVectorBasedOnConfiguration(x))
      .map(x => divideMatrix(x, x.rows, config.vectorSize))
      .map { x0 =>
        new Matrix(
          x0.map {
            x1 => x1.matrixElements.flatten
          }
        )
      }.map(x => new Matrix(x.matrixElements.transpose))
      .head.rows

    }
    println(zeroPadRows)

    val reshapedMatrix: Matrix = divideMatrix(input, multiplication, input.cols)
      .map(x => zeroPad2DVectorBasedOnConfiguration(x))
      .map(x => divideMatrix(x, x.rows, config.vectorSize))
      .map { x0 =>
        new Matrix(
          x0.map {
            x1 => x1.matrixElements.flatten
          }
        )
      }.map(x => new Matrix(x.matrixElements.transpose))
      .map( x => zeroPad2DVector(x, zeroPadRows, x.cols))
      .reduceLeft(concatenateVectorsHorizontally)


    concatMatrixColumnByColumn(reshapedMatrix, iterationNumber)
  }

  private def divideMatrix(matrix: Matrix, subMatrixRows: Int, subMatrixCols: Int): Vector[Matrix] = {
    require(subMatrixRows > 0 && subMatrixCols > 0, "[error] Submatrix dimensions must be greater than 0")

    val numRows = matrix.rows
    val numCols = if (numRows > 0) matrix.cols else 0

    require(numCols > 0, "[error] Matrix must have at least one column")

    val subMatrices = for {
      rowStart <- 0 until numRows by subMatrixRows
      colStart <- 0 until numCols by subMatrixCols
    } yield {
      val subMatrix = for {
        i <- rowStart until Math.min(rowStart + subMatrixRows, numRows)
      } yield {
        for {
          j <- colStart until Math.min(colStart + subMatrixCols, numCols)
        } yield matrix.matrixElements(i)(j)
      }.toVector
      new Matrix(subMatrix.toVector)
    }

    subMatrices.toVector

  }

  def zeroPad2DVector(matrix: Matrix, targetRows: Int, targetCols: Int): Matrix = {
    require(targetRows >= matrix.rows, "[error] Target rows must be greater than or equal to the original number of rows")
    require(targetCols >= matrix.cols, "[error] Target columns must be greater than or equal to the original number of columns")

    new Matrix(
      matrix.matrixElements.map { row =>
        row ++ Vector.fill(targetCols - row.length)(0)
      } ++ Vector.fill(targetRows - matrix.rows)(Vector.fill(targetCols)(0))
    )
  }

  def zeroPad2DVectorBasedOnConfiguration(matrix: Matrix) : Matrix = {

    val mok = matrix.cols.toDouble / config.vectorSize.toDouble
    val paddingSize = scala.math.ceil(mok).toInt * config.vectorSize

    zeroPad2DVector(matrix, matrix.rows ,paddingSize)

  }


  //TODO fix codes
  def concatMatrixColumnByColumn(matrix: Matrix, iterations: Int): Matrix = {
    val numRows = matrix.rows
    val numCols = matrix.cols

    val result = (0 until iterations).foldLeft(Vector.empty[Vector[Int]]) { (acc, _) =>
      (0 until numCols).foldLeft(acc) { (innerAcc, colIdx) =>
        innerAcc ++ matrix.matrixElements.map { row =>
          row(colIdx)
        }.grouped(numRows).toVector
      }
    }

    new Matrix(result.transpose)
  }

  def concatenateVectorsHorizontally(matrix1: Matrix, matrix2: Matrix): Matrix = {
    require(matrix1.rows == matrix2.rows, "Matrices must have the same number of rows")

    new Matrix(
      matrix1.matrixElements.zip(matrix2.matrixElements).map { case (row1, row2) =>
        row1 ++ row2
      }
    )
  }

  private def generateFolderName(): String = {
    ("{" + gemm.M.toString + "," + gemm.N.toString + "," + gemm.K.toString + "}"
      + "{" + config.arrayRow.toString + "," + config.arrayCol.toString + ","
      + config.blockRow.toString + "," + config.blockCol.toString + "," + config.vectorSize + "}")
  }


}

