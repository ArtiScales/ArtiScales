package fr.ign.cogit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.ign.cogit.GTFunctions.Rasters;
import fr.ign.cogit.util.SimuTool;
import fr.ign.parameters.Parameters;
import fr.ign.task.ProjectCreationDecompTask;
import fr.ign.task.SimulTask;
import fr.ign.tools.DataSetSelec;
import fr.ign.tools.OutputTools;

/**
 * Object to vectorize MUP-City's output
 * 
 * @author mcolomb
 *
 */
public class SelecMUPOutput {
	// root where everything's happenning
	File rootFile;
	// list of input to analyse
	List<File> rasterMupOutputList;

	public SelecMUPOutput(File rootfile, List<File> rastermupoutputList) {
		rootFile = rootfile;
		rasterMupOutputList = rastermupoutputList;
	}



	public static void main(String[] args) throws Exception {

		String folder = "/tmp/tmp/";

		System.out.println("Sleeping");

		Thread.sleep(1000);

		SimuTool.deleteDirectoryStream((new File(folder)).toPath());

		List<File> listMupOut = new ArrayList<File>();
		long t = System.currentTimeMillis();
		listMupOut.add(new File(
				"/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/depotConfigSpatMUP/Stability-dataAutom-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_1180786471690866433-evalAnal-20.0.tif"));
		run(new File(folder), listMupOut);
		System.out.println((System.currentTimeMillis() - t) / 1000 + " s");
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
		File output = new File(rootFile, "outputMupCity");
		output.mkdirs();

		List<File> listMupOutput = new ArrayList<File>();

		for (File rasterMupOutput : rasterMupOutputList) {
			// get the size of the cell form the file's folder
			double sizeCell = Double.parseDouble(Pattern.compile("-").split(rasterMupOutput.getName().replace(".tif", ""))[2].replace("CM", "").replace(".0", ""));

			File outputMup = new File(output, rasterMupOutput.getName().replace(".tif", ""));
			outputMup.mkdirs();

			listMupOutput.add(outputMup);

			Files.copy(rasterMupOutput.toPath(), new FileOutputStream(new File(outputMup, rasterMupOutput.getName())));
			File vectFile = new File(outputMup, outputMup.getName() + "-vectorized.shp");
			// avoid alreadymade operation
			if (!vectFile.exists()) {
				OutputTools.vectorizeMupOutput(Rasters.importRaster(new File(outputMup, rasterMupOutput.getName())), vectFile, sizeCell);
			}
		}

		return listMupOutput;
	}

	public static List<List<File>> mupCityDistribTask(List<Parameters> scenars, File rootFile, File geoFile) throws Exception {

		// TODO faire qqch pour les variante
		File mupCityDepot = new File(rootFile, "MupCityDepotTemp");
		List<List<File>> mupCityOutput = new ArrayList<List<File>>();

		for (Parameters p : scenars) {
			List<File> listVariant = new ArrayList<File>();
			String name = p.getString("nom");
			File scenarFile = new File(mupCityDepot, name);

			int i = 0;
			for (String[] variant : prepareVariant(p).values()) {
				File variantFile = new File(scenarFile, "variant" + i);

				i++;

				String empriseStr = variant[0];
				Pattern ptVir = Pattern.compile(";");
				String[] emprise = ptVir.split(empriseStr);
				double xmin = Double.valueOf(emprise[0]);
				double ymin = Double.valueOf(emprise[1]);
				double width = Double.valueOf(emprise[2]);
				double height = Double.valueOf(emprise[3]);

				DataSetSelec.predefSet();
				Map<String, String> dataHT = DataSetSelec.get(variant[3]);

				dataHT.put("name", "DataSys");
				dataHT.put("build", "batimentSys.shp");
				dataHT.put("road", "routeSys.shp");
				dataHT.put("fac", "serviceSys.shp");
				dataHT.put("lei", "loisirSys.shp");
				dataHT.put("ptTram", "tramSys.shp");
				dataHT.put("ptTrain", "trainSys.shp");
				dataHT.put("nU", "nonUrbaSys.shp");

				System.out.println("----------Project creation and decomp----------");
				File projectFile = ProjectCreationDecompTask.run(name, geoFile, variantFile, xmin, ymin, width, height, 0, 0, dataHT, Double.valueOf(variant[1]), 14580,
						Double.valueOf(variant[2]));
				System.out.println("----------Simulation task----------");
				File result = SimulTask.run(projectFile, name, p.getInteger("N"), p.getBoolean("strict"), p.getDouble("ahp0"), p.getDouble("ahp1"), p.getDouble("ahp2"),
						p.getDouble("ahp3"), p.getDouble("ahp4"), p.getDouble("ahp5"), p.getDouble("ahp6"), p.getDouble("ahp7"), p.getDouble("ahp8"), p.getBoolean("mean"),
						Integer.valueOf(variant[5]), false);
				System.out.println("result : " + result);
				System.out.println("----------End task----------");

				// Recherche des sorties de MUP-City que l'on va vouloir simuler

				double nivObs = Double.valueOf(variant[1]) * Double.valueOf(variant[4]);
				for (File f : result.listFiles()) {
					if (f.getName().contains("evalAnal") && f.getName().contains(String.valueOf(nivObs))) {
						listVariant.add(f);
						System.out.println("added in list " + f);
					}
				}
			}
			mupCityOutput.add(listVariant);
		}
		return mupCityOutput;
	}

	public static Hashtable<String, String[]> prepareVariant(Parameters p) {
		Hashtable<String, String[]> variants = new Hashtable<String, String[]>();
		try {
			String[] originalScenar = { p.getString("emprise"), p.getString("cm"), p.getString("seuil"), p.getString("data"), p.getString("emprise") };
			variants.put("original", originalScenar);
			for (int i = 1; i <= 1000; i++) {
				if (!p.getString("variant" + i).isEmpty()) {
					String[] variant = unmarshalVariant(p.getString("variant" + i));
					variants.put("variant" + 1, variant);
				}
			}
		} catch (NullPointerException npa) {

		}
		return variants;

	}

	/**
	 * return technical parameters from the parameter file
	 * 
	 * @param tab
	 *            with parameters sorted like that : 0 : emprise 1 : minimal cize of cell 2 : threshold of building density 3 : dataset to use 4 : level of cell size to use 5 :
	 *            seed
	 * @return
	 */
	private static String[] unmarshalVariant(String line) {
		String[] result = new String[6];

		String[] splited = line.split("--");

		result[0] = splited[0].split("=")[1];
		result[1] = splited[1].split("=")[1];
		result[2] = splited[2].split("=")[1];
		result[3] = splited[3].split("=")[1];
		result[4] = splited[4].split("=")[1];
		result[5] = splited[5].split("=")[1];
		return result;
	}

}
