package fr.ign.cogit;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
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
	//size of the analyzed cells
	double sizeCell;
	//list of input to analyse
	List<File> rasterMupOutputList;

	public SelecMUPOutput(File rootfile, List<File> rastermupoutputList) {
		rootFile = rootfile;
		rasterMupOutputList = rastermupoutputList;
	}

	public static void main(String[] args) throws Exception {
		List<File> listMupOut = new ArrayList<File>();
		listMupOut.add(new File("/home/mcolomb/donnee/couplage/depotConfigSpat/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0.tif"));
		run(new File("/home/mcolomb/donnee/couplage"), listMupOut);
	}

	public static List<File> run(File rootfile, List<File> rasteroutputfolder) throws Exception {
		// automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile, rasteroutputfolder);
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

		ArrayList<File> listMupOutput = new ArrayList<File>();
		// get the cells size

		for (File rasterMupOutput : rasterMupOutputList) {
			String rasterOutputString = rasterMupOutput.getName().replace(".tif", "");
			Pattern ech = Pattern.compile("-");

			String[] list = ech.split(rasterOutputString);
			sizeCell = Double.parseDouble(list[2].replace("CM", "").replace(".0",""));

			File outputMup = new File(output, rasterOutputString);
			outputMup.mkdirs();

			listMupOutput.add(outputMup);
			File outputMupRaster = new File(outputMup, rasterMupOutput.getName());

			Files.copy(rasterMupOutput.toPath(), new FileOutputStream(outputMupRaster));
			File vectFile = new File(outputMup, outputMup.getName() + "-vectorized.shp");
			// avoid alreadymade operation
			if (!vectFile.exists()) {
				OutputTools.VectorizeMupOutput(Rasters.importRaster(outputMupRaster), vectFile, sizeCell);
			}
		}

		return listMupOutput;
	}
}
