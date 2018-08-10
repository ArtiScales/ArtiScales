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

	static File rootFile;
	static File geoFile;
	static File pluFile;
	
	public static void main(String[] args) throws Exception {
		runScenar();
	}

	private static void runScenar() throws Exception {

		// Le fichier de configuration
		File paramFile = new File(MainTask.class.getClassLoader().getResource("paramSet/").getPath() + "param0.xml");
		Parameters p = Parameters.unmarshall(paramFile);

		// Dossiers de projet
		rootFile = new File(p.getString("rootFile"));
		geoFile =  new File(p.getString("geoFile"));
		pluFile = new File(p.getString("pluFile"));
		
		String name = p.getString("nom");
		System.out.println("-------------------====+++Scénario " + name + "+++=====----------------");

		// Code INSSE des communes concernées
		String[] zipCodes = { "25495" };


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
//		 // Mettre ça dans le fichier paramètre?
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
				SelectParcels selectParcels = new SelectParcels(rootFile,geoFile,pluFile, outMup, zipCode, p.getBoolean("splitParcel"));
				int missingHousingUnits = GetFromGeom.getHousingUnitsGoals(geoFile, zipCode);
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
						missingHousingUnits = SimPLUSimulator.fillSelectedParcels(rootFile,geoFile, pluFile, ParcelToTest, missingHousingUnits, zipCode, p);
					}
				}
			}
		}
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

}
