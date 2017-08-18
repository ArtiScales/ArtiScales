package fr.ign.cogit;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class MainTask {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		// String[] zipCode = { "25086", "25030", "25084", "25112", "25136",
		// "25137", "25147", "25212", "25245", "25267", "25287", "25297",
		// "25381", "25395", "25397", "25410", "25429", "25448", "25454",
		// "25467", "25473", "25477", "25495", "25532", "25542", "25557",
		// "25561", "25593", "25611" };
		String[] zipCode = {"25495","25245"};
		File rootFile = new File("donnee/couplage");
		// MUP-City output selection
		SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile);
		List<File> listMupOutput = sortieMupCity.run();

		// Parcel selection
		boolean notBuilt = true;
		boolean oneParcelPerCell = false;
		ArrayList<File> listSelection = new ArrayList<File>();
		for (File outMupFile : listMupOutput) {
			for (String zip : zipCode) {
				File output = new File(outMupFile, zip);
				output.mkdirs();
//				SelectParcels select = new SelectParcels(rootFile, outMupFile, zip, notBuilt,oneParcelPerCell);
//				listSelection.addAll(select.run());
				notBuilt = false;
				SelectParcels select = new SelectParcels(rootFile, outMupFile, zip, notBuilt,oneParcelPerCell);
				listSelection.addAll(select.run());
			}
		}

		// SimPLU simulation
		List<File> listBatis = new ArrayList<File>();
		for (File parcelSelection : listSelection) {
			// cette ligne est vraiment pas belle
			System.out.println(parcelSelection);
			String zip = new File(parcelSelection.getParent()).getParent().substring(
					(new File(parcelSelection.getParent()).getParent().length() - 5),
					new File(parcelSelection.getParent()).getParent().length());
			SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelSelection.getParentFile(), zip);
			listBatis.addAll(SPLUS.run());
		}
		// converstion buildings/households

		File fileName = new File(rootFile, "output/results.csv");
		FileWriter writer = new FileWriter(fileName, true);
		writer.append(
				"MUP-City Simulation, City zipCode , Selection type , SimPLU Simulation, number of simulated households");
		writer.append("\n");
		writer.close();

		BuildingsToHousehold bht = new BuildingsToHousehold(listBatis, 100);
		bht.run();

	}

}
