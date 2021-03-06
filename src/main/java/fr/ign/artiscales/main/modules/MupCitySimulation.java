package fr.ign.cogit.modules;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.MutablePair;

import fr.ign.cogit.GTFunctions.Rasters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.task.Initialize;
import fr.ign.task.ProjectCreationDecompTask;
import fr.ign.task.SimulTask;
import fr.ign.tools.DataSetSelec;
import fr.ign.tools.OutputTools;

public class MupCitySimulation {

	/**
	 * Object to create MUP-City vectorized output from a scenario with variants
	 * 
	 * @author mcolomb
	 *
	 */

	// root where everything's happening
	File rootFile;
	SimpluParametersJSON p;
	String[] variant;
	File variantFile;
	File geoFile;

	public MupCitySimulation(SimpluParametersJSON p, String[] variant, File variantFile, File rootFile, File geoFile) {
		this.rootFile = rootFile;
		this.p = p;
		this.variant = variant;
		this.variantFile = variantFile;
		this.geoFile = geoFile;
	}

	public static void main(String[] args) throws Exception {
		convertTifToShp(new File("/home/mcolomb/informatique/ArtiScales/svg"));
	}

	public static File run(SimpluParametersJSON p, String[] variant, File variantFile, File rootFile, File geoFile) throws Exception {

		// calculation of mup city's simulation and vectorizaton
		MupCitySimulation mupSim = new MupCitySimulation(p, variant, variantFile, rootFile, geoFile);

		return mupSim.run();
	}

	/**
	 * Vectorise les sorties de MUP-City et les copient dans le répertoire "output".
	 * 
	 * @return renvoie une liste de toutes les simulations MUP-City à tester.
	 */
	public File run() throws Exception {
		File outputTiff = mupCityTask(p, variant, variantFile, geoFile);
		double sizeCell = Double.valueOf(variant[1]) * Double.valueOf(variant[4]);
		//FIXME
//		OutputTools.vectorizeMupOutput(Rasters.importRaster(outputTiff), new File(variantFile, outputTiff.getName().replace(".tif", "")), sizeCell);
		return variantFile;
	}

	public static File mupCityTask(SimpluParametersJSON p, String[] variant, File variantFile, File geoFile) throws Exception {

		Initialize.init();
		String name = p.getString("name");

		String empriseStr = variant[0];
		Pattern ptVir = Pattern.compile(";");
		String[] emprise = ptVir.split(empriseStr);
		double xmin = Double.valueOf(emprise[0]);
		double ymin = Double.valueOf(emprise[1]);
		double width = Double.valueOf(emprise[2]);
		double height = Double.valueOf(emprise[3]);

		DataSetSelec.predefSet();
		Map<String, String> dataHT = DataSetSelec.get(variant[3]);

		dataHT.put("name", "DataManu");
		dataHT.put("build", "building.shp");
		dataHT.put("road", "road.shp");
		dataHT.put("fac", "service.shp");
		dataHT.put("lei", "leisure.shp");
		dataHT.put("ptTram", "tram.shp");
		dataHT.put("ptTrain", "train.shp");
		dataHT.put("nU", "nonUrba.shp");

		System.out.println("----------Project creation and decomp----------");
		MutablePair<String, File> projectFile = ProjectCreationDecompTask.run(name, geoFile, variantFile, xmin, ymin, width, height, 0, 0, dataHT,
				14580, Double.valueOf(variant[1]), Double.valueOf(variant[2]), true);
		System.out.println("----------Simulation task----------");
		File result = SimulTask.run(projectFile.getRight(), projectFile.getLeft(), p.getInteger("N"), p.getBoolean("strict"), p.getDouble("ahp0"),
				p.getDouble("ahp1"), p.getDouble("ahp2"), p.getDouble("ahp3"), p.getDouble("ahp4"), p.getDouble("ahp5"), p.getDouble("ahp6"),
				p.getDouble("ahp7"), p.getDouble("ahp8"), p.getBoolean("mean"), Integer.valueOf(variant[5]), true);
		System.out.println("result : " + result);
		System.out.println("----------End task----------");

		// Recherche des sorties de MUP-City que l'on va vouloir simuler

		double nivObs = Double.valueOf(variant[1]) * Double.valueOf(variant[4]);
		for (File f : result.listFiles()) {
			if (f.getName().endsWith("evalAnal-" + String.valueOf(nivObs) + ".tif")) {
				System.out.println("returned : " + f);
				return f;
			}
		}
		throw new NullPointerException("nothing to return");
	}

	public static void convertTifToShp(File rootFile) throws Exception {
		for (File scenarFile : (new File(rootFile, "MupCityDepot")).listFiles()) {
			if (scenarFile.isDirectory()) {
				System.out.println(scenarFile);
				for (File variantFile : scenarFile.listFiles()) {
					for (File simuFile : variantFile.listFiles()) {
						String simuName = simuFile.getName();
						if (simuName.endsWith(".tif")) {
							double ech = Double.valueOf(simuName.split("evalAnal-")[1].replace(".0.tif", ""));
							System.out.println(ech);
					    //FIXME
//							OutputTools.vectorizeMupOutput(Rasters.importRaster(simuFile),
//									new File(simuFile.getParentFile(), simuName.replace(".tif", ".shp")), ech);
						}
					}
				}
			}
		}
	}

}
