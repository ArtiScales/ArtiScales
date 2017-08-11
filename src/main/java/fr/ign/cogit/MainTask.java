package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.simplu3d.experiments.iauidf.tool.ParcelAttributeTransfert;

public class MainTask {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		//String[] zipCode = { "25086", "25030", "25084", "25112", "25136", "25137", "25147", "25212", "25245", "25267", "25287", "25297", "25381", "25395", "25397", "25410", "25429", "25448", "25454", "25467", "25473", "25477", "25495", "25532", "25542", "25557", "25561", "25593", "25611" };
		String[] zipCode = {"25245"};
		File rootFile = new File("donnee/couplage");
		//MUP-City output selection
		SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile);
		List<File> listMupOutput = sortieMupCity.run();
		
		//Parcel selection
		boolean notBuilt = true;
		ArrayList<File> listSelection = new ArrayList<File>();
		for (File outMupFile : listMupOutput) {
			for (String zip : zipCode) {
				File output = new File(outMupFile, zip);
				output.mkdirs();
				SelectParcels select = new SelectParcels(rootFile, outMupFile, zip, notBuilt);
				listSelection.addAll(select.run());
			}
		}

		// SimPLU simulation
		List<File> listBatis = new ArrayList<File>();
		for (File parcelSelection : listSelection) {
			// cette ligne est vraiment pas belle
			String zip= new File (parcelSelection.getParent()).getParent().substring((new File (parcelSelection.getParent()).getParent().length()-5), new File (parcelSelection.getParent()).getParent().length());
			SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelSelection, zip);
			listBatis = SPLUS.run();
		}
		//converstion buildings/households
	
		BuildingsToHousehold bht = new BuildingsToHousehold(listBatis);
bht.run();
		
	
	
	}

}
