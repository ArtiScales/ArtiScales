package fr.ign.cogit;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.StatStuff;
import fr.ign.parameters.Parameters;
import fr.ign.task.ProjectCreationDecompTask;
import fr.ign.task.SimulTask;

public class MainTask {

	public static void main(String[] args) throws Exception {
		runScenar();
	}

	private static void runScenar() throws Exception {

		// Le fichier de configuration
		File paramFile = new File(MainTask.class.getClassLoader().getResource("paramSet/").getPath() + "param0.xml");
		Parameters p = Parameters.unmarshall(paramFile);

		// Dossier de projet
		File rootFile = new File(p.getString("rootFile"));
		String name = p.getString("nom");
		System.out.println("-------------------====+++Scénario " + name + "+++=====----------------");

		// Code INSSE des communes concernées
		String[] zipCodes = { "25495" };

		File depotDonneeGeo = new File(rootFile, "donneeGeographiques");
		File outputMup = new File(rootFile, "depotConfigSpatMUP/simu");
		List<File> listOutputMupToTest = new ArrayList<File>();


//		 /// Étape 1 : simulation de MupCity
//		 String empriseStr = p.getString("emprise");
//		 Pattern ptVir = Pattern.compile(";");
//		 String[] emprise = ptVir.split(empriseStr);
//		 double xmin = Double.valueOf(emprise[0]);
//		 double ymin = Double.valueOf(emprise[1]);
//		 double width = Double.valueOf(emprise[2]);
//		 double height = Double.valueOf(emprise[3]);
//		
//		 // IDem pour ça ?
//		 Map<String, String> dataHT = new Hashtable<String, String>();
//		 // Data1.1
//		 dataHT.put("name", "Data1");
//		 dataHT.put("build", "batimentPro.shp");
//		 dataHT.put("road", "routePro.shp");
//		 dataHT.put("fac", "servicePro.shp");
//		 dataHT.put("lei", "loisirsPro.shp");
//		 dataHT.put("ptTram", "tramPro.shp");
//		 dataHT.put("ptTrain", "trainPro.shp");
//		 dataHT.put("nU", "nonUrbaPro.shp");
//		
//		 System.out.println("----------Project creation and decomp----------");
//		 File projectFile = ProjectCreationDecompTask.run(name, depotDonneeGeo, outputMup, xmin, ymin, width, height, 0, 0, dataHT, p.getDouble("cm"), 14580,
//		 p.getDouble("seuil"));
//		 System.out.println("----------Simulation task----------");
//		 File result = SimulTask.run(projectFile, name, p.getInteger("N"), p.getBoolean("strict"), p.getDouble("ahp0"), p.getDouble("ahp1"), p.getDouble("ahp2"),
//		 p.getDouble("ahp3"), p.getDouble("ahp4"), p.getDouble("ahp5"), p.getDouble("ahp6"), p.getDouble("ahp7"), p.getDouble("ahp8"), p.getBoolean("mean"),
//		 p.getInteger("seed"), false);
//		 System.out.println("result : " + result);
//		 System.out.println("----------End task----------");
//
//		
//		 // Recherche des sorties de MUP-City que l'on va vouloir simuler (shortcut)
//		 double nivObs = p.getDouble("cm")*p.getDouble("nivCellUtilise");
//		 for (File f : result.listFiles()){
//		 if (f.getName().contains("evalAnal") && f.getName().contains(String.valueOf(nivObs))){
//		 FileOutputStream flow = new FileOutputStream(new File(outputMup.getParentFile(), f.getName()));
//		 Files.copy(f.toPath(), flow);
//		 listOutputMupToTest.add(f);
//		 flow.close();
//		 }
//		 }

		// Si l'on met directement les simu MUP
		double nivObs = p.getDouble("cm") * p.getDouble("nivCellUtilise");
		for (File f : outputMup.getParentFile().listFiles()) {
			if (f.getName().contains("evalAnal") && f.getName().contains(String.valueOf(nivObs)) && f.getName().endsWith(".tif")) {
				listOutputMupToTest.add(f);
			}
		}

		/// Étape 2 : étape de couplage (très imbriqué..)

		// Vectorisation des sorties de MupCity
		List<File> outMupList = (new SelecMUPOutput(rootFile, listOutputMupToTest)).run();
		StatStuff.setGenStat(rootFile);
		// pour toutes les sorties
		for (File outMup : outMupList) {
			System.out.println("----------==+Pour la sortie " + outMup.getName() + "+==----------");
			// pour toutes les communes
			for (String zipCode : zipCodes) {
				System.out.println("------=+Pour la commune " + zipCode + "+=------");
				// Liste de types de sélection à partir du phasage définis dans le fichier de paramètre
				List<String> listeAction = selectionType(p);
				SelectParcels selectParcels = new SelectParcels(rootFile, outMup, zipCode, p.getBoolean("splitParcel"));
				int missingHousingUnits = GetFromGeom.getHousingUnitsGoals(rootFile, zipCode);
				System.out.println("il manque " + missingHousingUnits + " logements pour cette commune");
				for (String action : listeAction) {
					System.out.println("---=+Pour le remplissage " + action + "+=---");
					// tant qu'il reste des logements à construire
					while (missingHousingUnits > 0) {
						//sera le jeux de parcelles à tester
						File ParcelToTest = new File("");
						switch (action) {
						case "Ubuilt":
							ParcelToTest = selectParcels.runBrownfieldConstructed();
							break;
						case "UnotBuilt":
							ParcelToTest = selectParcels.runBrownfield();
							break;
						case "AUnotBuilt":
							ParcelToTest = selectParcels.runGreenfieldSelected();
							break;
						case "ALLnotBuilt":
							ParcelToTest = selectParcels.runAll();
						}
						//on calcule directement le nombre de logements par simulations de SimPLU
						missingHousingUnits = SimPLUSimulator.fillSelectedParcels(ParcelToTest, missingHousingUnits, rootFile, zipCode, p, action);
					}
				}
			}
		}
		// On sélectionne et découpage les parcelles selon l
		// Le nombre d'habitations manquantes

		/// Étape 3 : simulation avvec SimPLU3D

		// // On prépare les statistiques
		//
		// String zone = "U";
		// // On lance les simulation avec SimPLU3D
		// missingHousingUnits = SimPLUSimulator.fillSelectedParcels(selectParcels, missingHousingUnits, rootFile, zipCode[0], p, zone);
		//
		// // Etape 2 : bis on reselectionne en greenfield
		// SelectParcels select2 = new SelectParcels(rootFile, new File(rootFile, "output/" + outMup), zipCode[0], p.getBoolean("notBuilt"), true, p);
		//
		// File parcelCollectionAU = select2.runGreenfieldSelected();
		//
		// // filling the selected AU lands
		// zone = "AU";
		// missingHousingUnits = SimPLUSimulator.fillSelectedParcels(parcelCollectionAU, missingHousingUnits, rootFile, zipCode[0], p, zone);
		//
		// // fill the non-selected parcels till the end of time
		// File parcelCollectionAUleft = select2.runGreenfield();
		//
		// missingHousingUnits = SimPLUSimulator.fillSelectedParcels(parcelCollectionAUleft, missingHousingUnits, rootFile, zipCode[0], p, zone);
		//
		// // }
		// // }

	}

	/**
	 * Know which selection method to use determined by the param file
	 * 
	 * @return a list with all the different selections
	 * 
	 * @return
	 */
	private static List<String> selectionType(Parameters p) {
		List<String> routine = new ArrayList<String>();
		if (p.getBoolean("Anarchie")) {
			// TODO sélection aléatoire de parcelles
		} else if (p.getBoolean("JustEval")) {
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
	// public void exploSim() throws Exception {
	// //Dossier parent du projet
	// File rootFile = new File("donnee/couplage");
	//
	// // String[] zipCode = { "25086", "25030", "25084", "25112", "25136",
	// // "25137", "25147", "25212", "25245", "25267", "25287", "25297",
	// // "25381", "25395", "25397", "25410", "25429", "25448", "25454",
	// // "25467", "25473", "25477", "25495", "25532", "25542", "25557",
	// // "25561", "25593", "25611" };
	//
	// //Code INSEE sur lesquels on lance la simulation
	// String[] zipCode = { "25495" };
	// // "25245" "25495"
	//
	// // EXPLO
	// // On sélectionne les configurations spatiales de MupCITY sur lesquelles on va lancer le ParcelleManager puis SimPLU3D
	//
	// //Résultat de simulation de MupCITY avec évaluation
	// File MupOutputFolder = new File(rootFile, "depotConfigSpat");
	// List<File> listMupOutput = null;
	//
	// //
	// for (File rasterOutputFolder : MupOutputFolder.listFiles()) {
	// if (rasterOutputFolder.getName().endsWith(".tif") && rasterOutputFolder.getName().contains("eval_anal")) {
	// SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile, rasterOutputFolder);
	// listMupOutput = sortieMupCity.run();
	// }
	// }
	// // Parcel selection
	//
	// ArrayList<File> listSelection = new ArrayList<File>();
	// for (File outMupFile : listMupOutput) {
	// for (String zip : zipCode) {
	// File output = new File(outMupFile, zip);
	// output.mkdirs();
	// boolean splitParcel = false;
	// boolean notBuilt = true;
	// SelectParcels select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
	// listSelection.add(select.runBrownfield());
	// notBuilt = false;
	// select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
	// listSelection.add(select.runBrownfield());
	// splitParcel = true;
	// select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
	// listSelection.add(select.runBrownfield());
	// }
	// }
	//
	// // SimPLU simulation
	// List<File> listBatis = new ArrayList<File>();
	// for (File parcelSelection : listSelection) {
	// // cette ligne est vraiment pas belle
	// System.out.println(parcelSelection);
	// String zip = new File(parcelSelection.getParent()).getParent().substring((new File(parcelSelection.getParent()).getParent().length() - 5),
	// new File(parcelSelection.getParent()).getParent().length());
	// SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelSelection.getParentFile(), zip, null);
	// listBatis.addAll(SPLUS.run());
	// }
	// // reprise lorsque je bossais sur les codes d'indicateurs
	//
	// // List<File> listBatis = new ArrayList<File>();
	// // for (File f : new File(rootFile, "output").listFiles()){
	// // if (f.getName().startsWith("N")){
	// // for (File ff : f.listFiles()){
	// // if (!ff.getName().startsWith("N")){
	// // for (File fff : ff.listFiles()){
	// // for (File ffff : fff.listFiles()){
	// // if (ffff.getName().startsWith("si")){
	// // listBatis.add(ffff);
	// // }
	// // }
	// // }
	// // }
	// // }
	// // }
	// // }
	//
	// // listBatis.add(new
	// // File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0/25495/notBuilt-notSplit/simu0"));
	// // listBatis.add(new
	// // File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25495/built-Split/simu1"));
	// listBatis.add(new File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N5_St_Moy_ahpx_seed42-eval_anal-20.0/25495/built-notSplit/simu2"));
	//
	// StatStuff.setGenStat(rootFile);
	// int missingHousingUnits = 0;
	// for (File f : listBatis) {
	// BuildingToHousehold bht = new BuildingToHousehold(f, 100);
	// int constructedHU = bht.run();
	// System.out.println(" counstructed housing units : " + constructedHU);
	// missingHousingUnits = GetFromGeom.getHousingUnitsGoals(new File("donnee/couplage"), bht.getZipCode(f)) - constructedHU;
	// System.out.println("missingHousingUnits :" + missingHousingUnits);
	//
	// // TODO get the maxsurfaceparcelsplit parameter from the param file
	// // and put it as a argument
	//
	// SelectParcels select2 = new SelectParcels(rootFile, new File(rootFile, "output/" + bht.getMupSimu(f)), bht.getZipCode(f), false, true);
	//
	// SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(select2.runGreenfieldSelected().toURI().toURL())).getFeatureSource().getFeatures();
	//
	// }
	// }
}
