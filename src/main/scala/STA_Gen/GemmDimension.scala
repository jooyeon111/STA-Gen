package STA_Gen

case class GemmDimension(M: Int, N: Int, K : Int) {

  require(M >= 1, "[error] GEMM input row must be at least 1")
  require(N >= 1, "[error] GEMM input col or weight row must be at least 1")
  require(K >= 1, "[error] GEMM weight col must be at least 1")

  def printDimension(): Unit = {

    println(s"M : $M")
    println(s"N : $N")
    println(s"K : $K")
    println()
  }

}

