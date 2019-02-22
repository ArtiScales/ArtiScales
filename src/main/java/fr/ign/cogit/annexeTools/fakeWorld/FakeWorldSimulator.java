package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.modules.SimPLUSimulator;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class FakeWorldSimulator {

	public static void main(String[] args) throws Exception {

		// Parent folder with all subfolder
		String absoluteRootFolder = "./fakeWorld/";
		// String absoluteRootFolder = "/home/ubuntu/boulot/these/fakeWorld/";

		File rootFolderFile = new File(absoluteRootFolder);
		testBuildingTypes(rootFolderFile, new File(absoluteRootFolder+"/out"));
	}

	public static void tryRules(File rootFolderFile) throws Exception {
		for (File pathSubFolder : rootFolderFile.listFiles()) {

			if (!pathSubFolder.getName().contains("art71")) {
				continue;
			}
			if (pathSubFolder.isDirectory()) {

				List<File> lF = new ArrayList<>();
				// Line to change to select the right scenario

				String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorldMax/")
						.getPath();

				lF.add(new File(rootParam, "parameterTechnic.xml"));
				lF.add(new File(rootParam, "parameterScenario.xml"));

				SimpluParametersJSON p = new SimpluParametersJSON(lF);

				// Rappel de la construction du code :

				// 1/ Basically the parcels are filtered on the code with the
				// following
				// attributes
				// codeDep + codeCom + comAbs + section + numero

				// 2/ Alternatively we can decided to active an attribute (Here id)
				AttribNames.setATT_CODE_PARC("CODE");

				System.out.println(pathSubFolder);

				p.set("rootFile", pathSubFolder);
				p.set("selectedParcelFile", pathSubFolder + "parcelle.shp");
				p.set("geoFile", pathSubFolder);
				p.set("pluFile", pathSubFolder);
				p.set("pluPredicate", pathSubFolder + "predicate.csv");

				if (pathSubFolder.getName().contains("art8")) {
					p.set("intersection", false);
				}

				// String simulOut = pathSubFolder + "/out/";
				// (new File(simulOut)).mkdirs();
				// p.set("simu", simulOut);
				// SimPLUSimulator.ID_PARCELLE_TO_SIMULATE.add("30000");
				// Selected parcels shapefile

				SimPLUSimulator simplu = new SimPLUSimulator(new File("./src/main/resources/"),
						new File(p.getString("rootFile")), p, new File("/tmp/yop"));

				simplu.run();
			}
		}

	}

	public static void testBuildingTypes(File rootFolderFile, File outputFolder) throws Exception {

		List<File> lF = new ArrayList<>();
		// Line to change to select the right scenario

		File paramFile = new File(rootFolderFile, "/paramFolder");
		lF.add(new File(paramFile + "/paramSet/DDense/parameterScenario.json"));
		lF.add(new File(paramFile + "/paramSet/DDense/parameterTechnic.json"));

		List<File> lFTemp = new ArrayList<>();
		
		
		File folderProfileBuildingType =  new File(paramFile, "/profileBuildingType/");
		
		for (File buildingTypeFile : folderProfileBuildingType.listFiles()) {
			if (buildingTypeFile.getName().endsWith(".json")) {
				lFTemp.add(buildingTypeFile);
			}
		}
		
		lFTemp.parallelStream().forEach(x -> {
			try {
				launchSimulation(rootFolderFile, outputFolder, x, lF, paramFile );
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	
	private static void launchSimulation(File rootFolderFile, File outputFolder, File buildingTypeFile, List<File> lF , File paramFile) throws Exception {
	
			// if (!type.equals("smallBlockFlat")) {
			// continue;
			// }
			SimpluParametersJSON p = new SimpluParametersJSON(lF);
			AttribNames.setATT_CODE_PARC("CODE");
			outputFolder = new File(outputFolder, buildingTypeFile.getName());
			outputFolder.mkdirs();
			SimPLUSimulator plu = new SimPLUSimulator(paramFile, rootFolderFile, p, outputFolder);
			plu.run(BuildingType.valueOf(buildingTypeFile.getName().replace(".json", "").toUpperCase()), p);
	
	}
}
