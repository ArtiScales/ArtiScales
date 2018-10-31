package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.util.SimuTool;
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
		List<Parameters> listScenarios = getParamFile("MCIgn", new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet"));

		// List<Parameters> listScenarios = getParamFile("scenar0MKDom", new File("/home/mbrasebin/Documents/Code/ArtiScales/ArtiScales/src/main/resources/paramSet/"));

		rootFile = new File(listScenarios.get(0).getString("rootFile"));
		geoFile = new File(rootFile, "dataGeo");
		regulFile = new File(rootFile, "dataRegul");
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
				String name = p.getString("nom");
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
		} else {
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
		System.out.println(mupCityOutput);
		// File parcelPackages = parcelManagerSelectionAndPack();
		SelectParcels selecPar = new SelectParcels(rootFile, mupCityOutput, listScenarios);
		List<List<File>> parcelPackages = selecPar.run();

		////////////////
		// SimPLU3D part
		////////////////
		List<List<List<File>>> buildingSimulatedPerSimu = new ArrayList<List<List<File>>>();
		for (List<File> listVariantes : parcelPackages) {
			List<List<File>> buildingSimulatedPerScenar = new ArrayList<List<File>>();
			String scenarName = listVariantes.get(0).getParentFile().getName();
			for (File varianteFile : listVariantes) {
				List<File> buildingSimulatedPerVariant = new ArrayList<File>();
				System.out.println(scenarName);
				Parameters p = SimuTool.getParamFile(listScenarios, scenarName);
				for (File packFile : varianteFile.listFiles()) {
					SimPLUSimulator simPluSim = new SimPLUSimulator(rootFile, packFile, p);
					buildingSimulatedPerVariant = simPluSim.run();
				}
				buildingSimulatedPerScenar.add(buildingSimulatedPerVariant);
			}
			buildingSimulatedPerSimu.add(buildingSimulatedPerScenar);
		}

		if (buildingSimulatedPerSimu.isEmpty()) {
			buildingSimulatedPerSimu = SimuTool.generateResultConfig(rootFile);
		}

		// Some indicators
		for (List<List<File>> listVariantes : buildingSimulatedPerSimu) {
			String scenarName = listVariantes.get(0).get(0).getParentFile().getParent();
			for (List<File> buildingSimulatedPerVariant : listVariantes) {

				Parameters p = SimuTool.getParamFile(listScenarios, scenarName);
				File bTHFile = new File(rootFile, "indic/bTH/" + scenarName + "/" + buildingSimulatedPerVariant.get(0).getParent());
				bTHFile.mkdirs();
				BuildingToHousingUnit bTH = new BuildingToHousingUnit(buildingSimulatedPerVariant, bTHFile, p);
				bTH.runParticularSimpleEstimation();
				bTH.simpleCityEstimate();
			}
		}
	}

	public static Hashtable<String, String[]> prepareVariant(Parameters p) {
		Hashtable<String, String[]> variants = new Hashtable<String, String[]>();
		try {
			String[] originalScenar = { p.getString("emprise"), p.getString("cm"), p.getString("seuil"), p.getString("data"), p.getString("nivCellUtilise"), p.getString("seed") };
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
	 * Scan all the file from a folder and return a list of parameters, representing different scenarios TODO voir comment on gère les variantes?
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
					if (paramFile.getName().contains("parametre")) {
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
					if (paramFile.getName().contains("parametre")) {
						templistFile.add(paramFile);
					}
				}
				listParameters.add(Parameters.unmarshall(templistFile));
			}
		}
		return listParameters;
	}

	private static List<File> getScenarVariante(File file) {
		return null;
	}

	/**
	 * old nested method to rum global ArtiScales simulations
	 * 
	 * @throws Exception
	 */
	// private static void runExplo() throws Exception {
	//
	// // List of parameters files
	// List<File> listParameters = new ArrayList<>();
	//
	// // Folder root with parameters
	// String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenar0/").getPath();
	// System.out.println(rootParam);
	//
	// // Alll parameters are store both in a list and in a parameters objets
	// listParameters.add(new File(rootParam + "parametreTechnique.xml"));
	// listParameters.add(new File(rootParam + "parametreScenario.xml"));
	// Parameters p = Parameters.unmarshall(listParameters);
	// // Dossiers de projet
	// rootFile = new File(p.getString("rootFile"));
	// geoFile = new File(p.getString("geoFile"));
	// pluFile = new File(p.getString("pluFile"));
	//
	// // kind verification
	// if (!rootFile.exists() || !geoFile.exists() || !pluFile.exists()) {
	// System.out.println("please check the file setting in the parameter file ");
	// System.exit(99);
	// }
	//
	// // Begin of running scenario
	// String name = p.getString("nom");
	// System.out.println("-------------------====+++Scénario " + name + "+++=====----------------");
	//
	// // Code INSSE des communes concernées
	// String[] zipCodes = StatStuff.makeZipTab(p.getString("listZipCode"));
	//
	// File outputMup = new File(rootFile, "depotConfigSpatMUP/simu");
	// List<File> listOutputMupToTest = new ArrayList<File>();
	//
	// // Do we need to generate MupCity simulations ?
	// if (p.getBoolean("createMUPSimu")) {
	// /// Étape 1 : simulation de MupCity
	// String empriseStr = p.getString("emprise");
	// Pattern ptVir = Pattern.compile(";");
	// String[] emprise = ptVir.split(empriseStr);
	// double xmin = Double.valueOf(emprise[0]);
	// double ymin = Double.valueOf(emprise[1]);
	// double width = Double.valueOf(emprise[2]);
	// double height = Double.valueOf(emprise[3]);
	//
	// // Mettre ça dans le fichier paramètre?
	// Map<String, String> dataHT = new Hashtable<String, String>();
	// // Data1.1
	// dataHT.put("name", "DataSys");
	// dataHT.put("build", "batimentSys.shp");
	// dataHT.put("road", "routeSys.shp");
	// dataHT.put("fac", "serviceSys.shp");
	// dataHT.put("lei", "loisirSys.shp");
	// dataHT.put("ptTram", "tramSys.shp");
	// dataHT.put("ptTrain", "trainSys.shp");
	// dataHT.put("nU", "nonUrbaSys.shp");
	//
	// System.out.println("----------Project creation and decomp----------");
	// File projectFile = ProjectCreationDecompTask.run(name, geoFile, outputMup, xmin, ymin, width, height, 0, 0, dataHT, p.getDouble("cm"), 14580, p.getDouble("seuil"));
	// System.out.println("----------Simulation task----------");
	// File result = SimulTask.run(projectFile, name, p.getInteger("N"), p.getBoolean("strict"), p.getDouble("ahp0"), p.getDouble("ahp1"), p.getDouble("ahp2"),
	// p.getDouble("ahp3"), p.getDouble("ahp4"), p.getDouble("ahp5"), p.getDouble("ahp6"), p.getDouble("ahp7"), p.getDouble("ahp8"), p.getBoolean("mean"),
	// p.getInteger("seed"), false);
	// System.out.println("result : " + result);
	// System.out.println("----------End task----------");
	//
	// // Recherche des sorties de MUP-City que l'on va vouloir simuler
	//
	// double nivObs = p.getDouble("cm") * p.getDouble("nivCellUtilise");
	// for (File f : result.listFiles()) {
	// if (f.getName().contains("evalAnal") && f.getName().contains(String.valueOf(nivObs))) {
	// FileOutputStream flow = new FileOutputStream(new File(outputMup.getParentFile(), f.getName()));
	// Files.copy(f.toPath(), flow);
	// listOutputMupToTest.add(f);
	// flow.close();
	// }
	// }
	// }
	//
	// // Si l'on met directement les simu MUP
	//
	// else {
	// double nivObs = p.getDouble("cm") * p.getDouble("nivCellUtilise");
	// System.out.println();
	// for (File f : outputMup.getParentFile().listFiles()) {
	// if (f.getName().contains("evalAnal") && f.getName().contains(String.valueOf(nivObs)) && f.getName().endsWith(".tif")) {
	// listOutputMupToTest.add(f);
	// System.out.println("MUP-City's output in the machine : " + f);
	// }
	// }
	// }
	//
	// /// Étape 2 : étape de couplage (très imbriqué..)
	//
	// // Vectorisation des sorties de MupCity
	// List<File> outMupList = (new SelecMUPOutput(rootFile, listOutputMupToTest)).run();
	//
	// // pour toutes les sorties
	// for (File outMup : outMupList) {
	// System.out.println("----------==+Pour la sortie " + outMup.getName() + "+==----------");
	// // pour toutes les communes
	// for (String zipCode : zipCodes) {
	// System.out.println("------=+Pour la commune " + zipCode + "+=------");
	// // Liste de types de sélection à partir du phasage définis dans le fichier de
	// // paramètre
	// List<String> listeAction = selectionType(p);
	//// SelectParcels selectParcels = new SelectParcels(rootFile, geoFile, pluFile, outMup, zipCode, p.getBoolean("splitParcel"), p);
	//
	// // mode normal -- on construit tout ce que l'on peut. On peut peut-être ajouter
	// // un seuil d'évaluation?
	// if (!p.getBoolean("fill")) {
	// // on ne va simuler que sur des emplacements permis par le zonage
	// if (p.getBoolean("respectZoning")) {
	//
	// // parcel selection
	// // File parcelSelected = selectParcels.runZoningAllowed();
	//
	// // SimPLU simulation
	// SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, geoFile, pluFile, parcelSelected, zipCode, p, listParameters);
	// List<File> batisSimulatedFile = simPLUsimu.run();
	//
	// // merge for workability reasons
	// VectorFct.mergeBatis(batisSimulatedFile);
	//
	// BuildingToHousingUnit bTH = new BuildingToHousingUnit(batisSimulatedFile, p);
	// bTH.runParticularSimpleEstimation();
	// bTH.simpleCityEstimate();
	//
	// } else {
	//
	// }
	// }
	// // mode fill : on va chercher à remplir les communes avec l'objectif de logement
	// // fixé dans le scot
	// else {
	// int missingHousingUnits = GetFromGeom.getHousingUnitsGoals(geoFile, zipCode);
	// System.out.println("il manque " + missingHousingUnits + " logements pour cette commune");
	// for (String action : listeAction) {
	// System.out.println("---=+Pour le remplissage " + action + "+=---");
	// // tant qu'il reste des logements à construire
	// while (missingHousingUnits > 0) {
	// // sera le jeux de parcelles à tester
	// File ParcelToTest = new File("");
	// switch (action) {
	// case "Ubuilt":
	// ParcelToTest = selectParcels.runBrownfieldConstructed();
	// break;
	// case "UnotBuilt":
	// ParcelToTest = selectParcels.runBrownfieldUnconstructed();
	// break;
	// case "AUnotBuilt":
	// ParcelToTest = selectParcels.runGreenfieldSelected();
	// break;
	// case "ALLnotBuilt":
	// ParcelToTest = selectParcels.runNaturalLand();
	// case "justEval":
	// ParcelToTest = selectParcels.runAll();
	// }
	// // on calcule directement le nombre de logements par simulations de SimPLU
	// missingHousingUnits = SimPLUSimulator.fillSelectedParcels(rootFile, geoFile, pluFile, ParcelToTest, missingHousingUnits, zipCode, p, listParameters);
	// }
	// if (missingHousingUnits == 0) {
	// break;
	// }
	// }
	// }
	// }
	// }
	// }

	/**
	 * Know which selection method to use determined by the param file
	 * 
	 * @return a list with all the different selections
	 * 
	 * @return
	 */
	private static List<String> varianteType(Parameters p) {
		List<String> routine = new ArrayList<String>();
		if (p.getBoolean("Anarchie")) {
			// TODO sélection aléatoire de parcelles
		} else if (p.getBoolean("JustEval")) {
			routine.add("justEval");
			// TODO sélection de parcelles uniquement selon l'évaluation
		} else {
			if (p.getBoolean("Ubuilt")) {
				routine.add("Ubuilt");
			}
			if (p.getBoolean("UnotBuilt")) {
				routine.add("UnotBuilt");
			}
			if (p.getBoolean("AUnotBuilt")) {
				routine.add("AUnotBuilt");
			}
			if (p.getBoolean("ALLnotBuilt")) {
				routine.add("ALLnotBuilt");
			}
		}
		return routine;
	}

}
