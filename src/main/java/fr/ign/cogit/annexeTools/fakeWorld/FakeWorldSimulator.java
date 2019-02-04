package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.parameters.Parameters;

public class FakeWorldSimulator {

	public static void main(String[] args) throws Exception {

		// Parent folder with all subfolder
		String absoluteRootFolder = "/home/mcolomb/informatique/fakeWorld/";

		File rootFolderFile = new File(absoluteRootFolder);
		testBuildingTypes(rootFolderFile);
	}

	public static void tryRules(File rootFolderFile) throws Exception {
		for (File pathSubFolder : rootFolderFile.listFiles()) {

			if (!pathSubFolder.getName().contains("art71")) {
				continue;
			}
			if (pathSubFolder.isDirectory()) {

				List<File> lF = new ArrayList<>();
				// Line to change to select the right scenario

				String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorldMax/").getPath();

				lF.add(new File(rootParam + "parameterTechnic.xml"));
				lF.add(new File(rootParam + "parameterScenario.xml"));

				Parameters p = Parameters.unmarshall(lF);

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
				SimPLUSimulator simplu = new SimPLUSimulator(new File(p.getString("rootFile")), p);

				simplu.run();
			}
		}

	}

	public static void testBuildingTypes(File rootFolderFile) throws Exception {

		List<File> lF = new ArrayList<>();
		// Line to change to select the right scenario

		String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorldMax/").getPath();
		lF.add(new File(rootParam + "parameterScenario.xml"));
		lF.add(new File(rootParam + "parameterTechnic.xml"));

		for (String type : iterateOnBuildingType()) {
			try {
				Parameters p = Parameters.unmarshall(lF);
				File f = new File(rootFolderFile, type);

				AttribNames.setATT_CODE_PARC("CODE");

				p.set("rootFile", f);
				SimPLUSimulator plu = new SimPLUSimulator(f, p);
				plu.run(BuildingType.valueOf(type.toUpperCase()), p);
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("get lost");
			}
		}
	}

	public static String[] iterateOnBuildingType() {
		String[] result = { "detachedHouse", "midBlockFlat", "multifamilyHouse", "smallBlockFlat", "smallHouse" };
		return result;
	}

}
