package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainTask {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		String[] zipCode = { "25086", "25030", "25084", "25112", "25136", "25137", "25147", "25212", "25245", "25267", "25287", "25297", "25381", "25395", "25397", "25410", "25429", "25448", "25454", "25467", "25473", "25477", "25495", "25532", "25542", "25557", "25561", "25593", "25611" };
		File rootFile = new File("/home/mcolomb/donnee/couplage");
		//MUP-City output selection
		SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile);
		List<File> listMupOutput = sortieMupCity.run();
		

		//Parcel selection
		boolean notBuilt = true;
		ArrayList<File> listSelection = new ArrayList<File>();
		for (File outMupFile : listMupOutput) {
			for (String zip : zipCode) {
				SelectParcels select = new SelectParcels(rootFile, new File(outMupFile, zip), zip, notBuilt);
				listSelection.addAll(select.run());
			}
		}
		// SimPLU simulation
//		for (File parcelSelection : listSelection) {
//			SimPLUSimulator.shpIn = parcelSelection;
//			SimPLUSimulator.zipCode = zipCode[0];
//			SimPLUSimulator.folderName = new File(rootFile, "simPLU-out");
//		}
		//converstion buildings/households
	}

}
