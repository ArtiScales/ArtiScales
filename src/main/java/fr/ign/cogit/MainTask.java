package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.indicators.ParcelStat;
import fr.ign.cogit.util.SimuTool;
import fr.ign.cogit.util.VectorFct;
import fr.ign.parameters.Parameters;

public class MainTask {

	static File rootFile;
	static File geoFile;
	static File regulFile;

	public static void main(String[] args) throws Exception {
		runScenar();
	}

	/**
	 * run different scenarios Is better for grid distributions
	 * 
	 * @throws Exception
	 */
	private static void runScenar() throws Exception {

		// general parameters

		// list of different scenarios to test
	//	List<Parameters> listScenarios = getParamFile("DDense", new File("/home/ubuntu/workspace/ArtiScales/src/main/resources/paramSet"));
		List<Parameters> listScenarios = getParamFile("DDense", new File("./src/main/resources/paramSet"));

		// List<Parameters> listScenarios = getParamFile("scenar0MKDom", new
		// File("/home/mbrasebin/Documents/Code/ArtiScales/ArtiScales/src/main/resources/paramSet/"));

		rootFile = new File(listScenarios.get(0).getString("rootFile"));
		geoFile = new File(rootFile, "dataGeo");
		regulFile = new File(rootFile, "dataRegulation");
		// kind verification
		if (!rootFile.exists() || !geoFile.exists() || !regulFile.exists()) {
			System.out.println("please check the file setting in the parameter file ");
			System.exit(99);
		}

		////////////////
		// MUP-City part
		////////////////

		List<List<File>> mupCityOutput = new ArrayList<List<File>>();

		// if we want to simulate MUPCity part or we directly using outputs
		if (listScenarios.get(0).getBoolean("createMUPSimu")) {
			File mupCityDepot = new File(rootFile, "MupCityDepot");
			for (Parameters p : listScenarios) {
				List<File> listVariant = new ArrayList<File>();
				String name = p.getString("name");
				File scenarFile = new File(mupCityDepot, name);
				int i = 0;
				for (String[] variant : prepareVariant(p).values()) {
					File variantFile = new File(scenarFile, "variant" + i);
					i++;
					MupCitySimulation mupSimu = new MupCitySimulation(p, variant, variantFile, rootFile, geoFile);
					File mupOutShp = mupSimu.run();
					listVariant.add(mupOutShp);
				}
				mupCityOutput.add(listVariant);
			}
		}
		// if MUP-City simulations has already been calculated
		else {
			for (File f : (new File(rootFile, "MupCityDepot")).listFiles()) {
				if (f.isDirectory()) {
					List<File> variante = new ArrayList<File>();
					for (File varitante : f.listFiles()) {
						for (File var : varitante.listFiles()) {
							if (var.getName().contains("evalAnal") && var.getName().endsWith(".shp")) {
								variante.add(var);
								System.out.println("MUP-City's output in the machine : " + var);
							}
						}
					}
					mupCityOutput.add(variante);
				}
			}
		}

		////////////////
		// Selection and parcel management part
		////////////////
		// File parcelPackages = parcelManagerSelectionAndPack();
		SelectParcels selecPar = new SelectParcels(rootFile, mupCityOutput, listScenarios);
		List<List<File>> parcelPackages = selecPar.run();

		// ////////////////
		// // SimPLU3D part
		// ////////////////
		// List<List<List<File>>> buildingSimulatedPerSimu = new ArrayList<List<List<File>>>();
		// for (List<File> listVariantes : parcelPackages) {
		// List<List<File>> buildingSimulatedPerScenar = new ArrayList<List<File>>();
		// String scenarName = listVariantes.get(0).getParentFile().getName();
		// for (File varianteFile : listVariantes) {
		// List<File> buildingSimulatedPerVariant = new ArrayList<File>();
		// Parameters p = SimuTool.getParamFile(listScenarios, scenarName);
		// for (File packFile : varianteFile.listFiles()) {
		// if (packFile.isDirectory()) {
		// SimPLUSimulator simPluSim = new SimPLUSimulator(packFile, p);
		// List<File> listFilesSimul = simPluSim.run();
		// if (!(listFilesSimul == null)) {
		// buildingSimulatedPerVariant.addAll(listFilesSimul);
		// }
		// }
		// }
		// VectorFct.mergeBatis(buildingSimulatedPerVariant);
		// buildingSimulatedPerScenar.add(buildingSimulatedPerVariant);
		// }
		// buildingSimulatedPerSimu.add(buildingSimulatedPerScenar);
		// }
		//
		// ////////////////
		// // Indicators
		// ////////////////
		//
		// // Building to housingUnit indicator
		//
		// // we get the hierarchy of files if the previous steps hasnt been processed
		// if (buildingSimulatedPerSimu.isEmpty()) {
		// buildingSimulatedPerSimu = SimuTool.generateResultConfigSimPLU(rootFile);
		// }
		// for (List<List<File>> listVariantes : buildingSimulatedPerSimu) {
		// String scenarName = listVariantes.get(0).get(0).getParentFile().getParentFile().getName();
		// for (List<File> buildingSimulatedPerVariant : listVariantes) {
		// Parameters p = SimuTool.getParamFile(listScenarios, scenarName);
		// File bTHFile = new File(rootFile, "indic/bTH/" + scenarName + "/" + buildingSimulatedPerVariant.get(0).getParentFile().getName());
		// bTHFile.mkdirs();
		// BuildingToHousingUnit bTH = new BuildingToHousingUnit(buildingSimulatedPerVariant, bTHFile, p);
		// bTH.distributionEstimate();
		// }
		// }
		//
		// // Parcel selected indicator
		// // we get the hierarchy of files if the previous steps hasn't been processed
		//
		// List<List<File>> parcelGen = SimuTool.generateResultParcels(rootFile);
		// // we calculate
		// for (List<File> listVariantes : parcelGen) {
		// String scenarName = listVariantes.get(0).getParentFile().getParentFile().getName();
		// for (File parcelsPerVariant : listVariantes) {
		// Parameters p = SimuTool.getParamFile(listScenarios, scenarName);
		// File parcelOutFile = new File(rootFile, "indic/parcelOut/" + scenarName + "/" + parcelsPerVariant.getParentFile().getName());
		// parcelOutFile.mkdirs();
		// File parcelOut = new File(parcelOutFile, "parcelGenExport.shp");
		// Vectors.copyShp("parcelGen", parcelsPerVariant.getParentFile(), parcelOutFile);
		// ParcelStat pc = new ParcelStat(p, parcelOut);
		// pc.run();
		// }
		// }
	}

	public static Hashtable<String, String[]> prepareVariant(Parameters p) {
		Hashtable<String, String[]> variants = new Hashtable<String, String[]>();
		try {
			String[] originalScenar = { p.getString("emprise"), p.getString("cm"), p.getString("seuil"), p.getString("data"),
					p.getString("nivCellUtilise"), p.getString("seed") };
			variants.put("original", originalScenar);
			for (int i = 1; i <= 1000; i++) {
				if (!p.getString("variante" + i).isEmpty()) {
					String[] variant = unmarshalVariant(p.getString("variante" + i));
					variants.put("variante" + i, variant);
				}
			}
		} catch (NullPointerException npa) {
		}

		return variants;

	}

	/**
	 * return technical parameters from the parameter file
	 * 
	 * @param line
	 *            from the parameter file
	 * @return tab with parameters sorted like that : \n 0 : emprise \n 1 : minimal size of cell \n 2 : threshold of building density \n 3 : dataset to use \n 4 : level of cell
	 *         size to use \n 5 : seed \n
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

	/**
	 * Scan all the file from a folder and return a list of parameters, representing different scenarios
	 * 
	 * @param fIn
	 *            : folder where every scenarios parameters are stored
	 * @return list : list of Parameters object to run
	 * @throws Exception
	 */
	private static List<Parameters> getParamFile(File fIn) throws Exception {
		List<Parameters> listParameters = new ArrayList<Parameters>();
		for (File folder : fIn.listFiles()) {
			if (folder.isDirectory()) {
				for (File paramFile : folder.listFiles()) {
					if (paramFile.getName().contains("parameter")) {
						List<File> templistFile = new ArrayList<File>();
						templistFile.add(paramFile);
						templistFile.add(paramFile);
						listParameters.add(Parameters.unmarshall(templistFile));
					}
				}
			}
		}
		return listParameters;
	}

	private static List<Parameters> getParamFile(String nameComputer, File fIn) throws Exception {
		List<Parameters> listParameters = new ArrayList<Parameters>();
		for (File folder : fIn.listFiles()) {
			if (folder.isDirectory() && folder.getName().contains(nameComputer)) {
				List<File> templistFile = new ArrayList<File>();
				for (File paramFile : folder.listFiles()) {
					if (paramFile.getName().contains("parameter")) {
						templistFile.add(paramFile);
					}
				}
				listParameters.add(Parameters.unmarshall(templistFile));
			}
		}
		return listParameters;
	}

}
