package ArtiScalePlugin

import java.io.File
import fr.ign.cogit._

trait MupCitySimulation {
  def apply(): File = {
    fr.ign.cogit.MupCitySimulation.run(name, inputFolder, outputFolder, xmin, ymin, width, height, shiftX, shiftY, maxSize, minSize, seuilDensBuild)
  }
}

object ProjectCreationDecompTask extends ProjectCreationDecompTask
