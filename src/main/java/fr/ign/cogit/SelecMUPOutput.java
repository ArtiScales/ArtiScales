package fr.ign.cogit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import fr.ign.cogit.GTFunctions.Rasters;
import fr.ign.tools.OutputTools;
/**
 * Object to vectorize MUP-City's output
 * @author mcolomb
 *
 */
public class SelecMUPOutput {
	//root where everything's happenning
	File rootFile;
	//list of input to analyse
	List<File> rasterMupOutputList;

	public SelecMUPOutput(File rootfile, List<File> rastermupoutputList) {
		rootFile = rootfile;
		rasterMupOutputList = rastermupoutputList;
	}

	private static void deleteDirectoryStream(Path path) throws IOException {
		  Files.walk(path)
		    .sorted(Comparator.reverseOrder())
		    .map(Path::toFile)
		    .forEach(File::delete);
		}
	
	public static void main(String[] args) throws Exception {
		
		String folder = "/tmp/tmp/";
		
		System.out.println("Sleeping");
		
		Thread.sleep(1000);
		
		deleteDirectoryStream((new File(folder)).toPath());
		
		List<File> listMupOut = new ArrayList<File>();
		long t = System.currentTimeMillis();
		listMupOut.add(new File("/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/depotConfigSpatMUP/Stability-dataAutom-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_1180786471690866433-evalAnal-20.0.tif"));
		run(new File(folder), listMupOut);
		System.out.println((System.currentTimeMillis() - t) / 1000 + "  s");
	}

	public static List<File> run(File rootfile, List<File> listRaster) throws Exception {
		// automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile, listRaster);
		return smo.run();
	}
	


	/**
	 * Vectorise les sorties de MUP-City et les copient dans le répertoire "output".
	 * 
	 * @return renvoie une liste de toutes les simulations MUP-City à tester.
	 */
	public List<File> run() throws Exception {
		File output = new File(rootFile, "output");
		output.mkdirs();

		List<File> listMupOutput = new ArrayList<File>();
		
		for (File rasterMupOutput : rasterMupOutputList) {
			// get the size of the cell form the file's folder
			double sizeCell = Double.parseDouble(Pattern.compile("-").split(rasterMupOutput.getName().replace(".tif", ""))[2].replace("CM", "").replace(".0",""));
			
			
			File outputMup = new File(output, rasterMupOutput.getName().replace(".tif", ""));
			outputMup.mkdirs();

			listMupOutput.add(outputMup);
		
			Files.copy(rasterMupOutput.toPath(), new FileOutputStream(new File(outputMup, rasterMupOutput.getName())));
			File vectFile = new File(outputMup, outputMup.getName() + "-vectorized.shp");
			// avoid alreadymade operation
			if (!vectFile.exists()) {
				OutputTools.vectorizeMupOutput(Rasters.importRaster(new File(outputMup, rasterMupOutput.getName())), vectFile,sizeCell) ;
			}
		}

		return listMupOutput;
	}
}
