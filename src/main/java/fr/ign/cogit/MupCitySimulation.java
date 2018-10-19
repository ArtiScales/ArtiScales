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
import fr.ign.parameters.Parameters;
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

	// root where everything's happenning
	File rootFile;
	Parameters p;
	String[] variant;
	File variantFile;
	File geoFile;

	public MupCitySimulation(Parameters p, String[] variant, File variantFile, File rootFile, File geoFile) {
		this.rootFile = rootFile;
		this.p = p;
		this.variant = variant;
		this.variantFile = variantFile;
		this.geoFile = geoFile;
	}

	private static void deleteDirectoryStream(Path path) throws IOException {
		//use that?!
		Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	public static void main(String[] args) throws Exception {

	}

	public static File run(Parameters p, String[] variant, File variantFile, File rootFile, File geoFile) throws Exception {

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

		File outputTiff = mupCityDistribTask(p, variant, variantFile, geoFile);
		double sizeCell = Double.parseDouble(outputTiff.getName().split("-")[2].replace(".tif", ""));
		OutputTools.vectorizeMupOutput(Rasters.importRaster(outputTiff), new File(variantFile, outputTiff.getName()), sizeCell);
		return variantFile;
	}

	public static File mupCityDistribTask(Parameters p, String[] variant, File variantFile, File geoFile) throws Exception {

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
				System.out.println("returned : " + f);
				return f;
			}
		}
		throw new NullPointerException("nothing to return");
	}

}
