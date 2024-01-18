
package STA_Gen

import java.nio.file.Files.write
import java.nio.file._
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator


//TODO: Exception file handling
class SramHexFileWriter (mapper: Mapper){


  private val resourcesFolderPathString = "src/main/resources/"
  val targetDirectory: String = resourcesFolderPathString + mapper.mappingConfiguration

  private val resourceDirectoryPath: Path = Paths.get(targetDirectory)

  private val inputATextFileVector: Vector[Path] = Vector.tabulate(mapper.numberOfInputPortA)(
    x => Paths.get(targetDirectory + "/InputAHex" + x + ".txt"))
  private val inputBTextFileVector: Vector[Path] = Vector.tabulate(mapper.numberOfInputPortB)(
    x => Paths.get(targetDirectory + "/InputBHex" + x + ".txt"))

//  private val mapper = new Mapper(gemm, config)

  def createHexFileOutputStationarySimulation() : Unit = {

    //Create upper directory
    try {
      Files.createDirectory(resourceDirectoryPath)
    } catch {
      case e: FileAlreadyExistsException => println(s"[warn] Directory ${resourceDirectoryPath.toString} is already created")
    }

    //Create text files
    for(fileA <- inputATextFileVector)
        Files.createFile(fileA)

    for (fileB <- inputBTextFileVector)
        Files.createFile(fileB)

    //Write SRAM file
    for (i <- mapper.stringBasedReshapedA.indices)
      write(inputATextFileVector(i), mapper.stringBasedReshapedA(i).getBytes())

    for (i <- mapper.stringBasedReshapedB.indices)
      write(inputBTextFileVector(i), mapper.stringBasedReshapedB(i).getBytes())

  }

  def deleteHexFileOutputStationarySimulation() : Unit = {

//    for (fileA <- inputATextFileVector)
//      Files.delete(fileA)
//
//    for (fileB <- inputBTextFileVector)
//      Files.delete(fileB)
//
//    Files.delete(resourceDirectoryPath)
    Files.walk(resourceDirectoryPath).sorted(Comparator.reverseOrder).forEach(path => {
//        System.out.println("Deleting: " + path)
        Files.delete(path)
    })

  }


}
