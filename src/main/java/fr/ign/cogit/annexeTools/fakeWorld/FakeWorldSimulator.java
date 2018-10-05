package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.outputs.XmlGen;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.parameters.Parameters;

public class FakeWorldSimulator {
	
	public static void main(String[] args) throws Exception {

		List<File> lF = new ArrayList<>();
		// Line to change to select the right scenario

		String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorld/").getPath();

		System.out.println(rootParam);

		lF.add(new File(rootParam + "parametreTechnique.xml"));
		lF.add(new File(rootParam + "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		System.out.println(p.getString("nom"));
		// Rappel de la construction du code :

		// 1/ Basically the parcels are filtered on the code with the following
		// attributes
		// codeDep + codeCom + comAbs + section + numero

		// 2/ Alternatively we can decided to active an attribute (Here id)
		AttribNames.setATT_CODE_PARC("CODE");
		


		// RootFolder
		File rootFolder = new File(p.getString("rootFile"));
		// Selected parcels shapefile
		File selectedParcels = new File(p.getString("selectedParcelFile"));
		// GeographicData folder
		File geoFile = new File(p.getString("geoFile"));
		// PLU Folder
		File pluFile = new File(p.getString("pluFile"));

		// writed stuff
		XmlGen resultxml = new XmlGen(new File(rootFolder, "mainSimPLUSIMresult.xml"), "result");
		XmlGen logxml = new XmlGen(new File(rootFolder, "mainSimPLUSIMlog.xml"), "log");

		SimPLUSimulator simplu = new SimPLUSimulator(rootFolder, geoFile, pluFile, selectedParcels, p.getString("listZipCode"), p, lF, resultxml, logxml);

		simplu.run();
		// SimPLUSimulator.fillSelectedParcels(new File(rootFolder), geoFile,
		// pluFile, selectedParcels, 50, "25495", p);

	}

}
