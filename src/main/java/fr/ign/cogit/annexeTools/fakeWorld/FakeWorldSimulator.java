package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.parameters.Parameters;

public class FakeWorldSimulator {

	public static void main(String[] args) throws Exception {

		// TODO try before push
		// Parent folder with all subfolder
		String absoluteRootFolder = "/home/mcolomb/informatique/ArtiScales/fakeWorld/";

		File rootFolderFile = new File(absoluteRootFolder);

		for (File pathSubFolder : rootFolderFile.listFiles()) {

			if (!pathSubFolder.getName().contains("art71")) {
				continue;
			}
			if (pathSubFolder.isDirectory()) {

				List<File> lF = new ArrayList<>();
				// Line to change to select the right scenario

				String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorldMax/")
						.getPath();

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

//			String simulOut = pathSubFolder + "/out/";
//			(new File(simulOut)).mkdirs();
//			p.set("simu", simulOut);
//			SimPLUSimulator.ID_PARCELLE_TO_SIMULATE.add("30000");
				// Selected parcels shapefile
				SimPLUSimulator simplu = new SimPLUSimulator(new File(p.getString("rootFile")), p);

				simplu.run();
			}
		}

	}

}
