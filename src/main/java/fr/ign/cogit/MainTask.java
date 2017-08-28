package fr.ign.cogit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.Indicators.BuildingToHousehold;

public class MainTask {

	public static void main(String[] args) throws Exception {

		// String[] zipCode = { "25086", "25030", "25084", "25112", "25136",
		// "25137", "25147", "25212", "25245", "25267", "25287", "25297",
		// "25381", "25395", "25397", "25410", "25429", "25448", "25454",
		// "25467", "25473", "25477", "25495", "25532", "25542", "25557",
		// "25561", "25593", "25611" };
		String[] zipCode = { "25495", "25245" };
		File rootFile = new File("donnee/couplage");

		// MUP-City output selection
		SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile);
		List<File> listMupOutput = sortieMupCity.run();

		// Parcel selection

		ArrayList<File> listSelection = new ArrayList<File>();
		for (File outMupFile : listMupOutput) {
			for (String zip : zipCode) {
				File output = new File(outMupFile, zip);
				output.mkdirs();
				boolean splitParcel = false;
				boolean notBuilt = true;
				SelectParcels select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
				listSelection.add(select.runBrownfield());
				notBuilt = false;
				select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
				listSelection.add(select.runBrownfield());
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

		setGenStat(rootFile);
		int missingHousingUnits = 0;
		for (File f : listBatis) {
			BuildingToHousehold bht = new BuildingToHousehold(f, 100);
			missingHousingUnits = getHousingUnitsGoals(new File("donnee/couplage"), "25495") - bht.run();
			System.out.println("missingHousingUnits :" + missingHousingUnits);

			SelectParcels select = new SelectParcels(rootFile, f, bht.getZipCode(f), false, true);

			SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(select.runGreenfield().toURI().toURL()))
					.getFeatureSource().getFeatures();
			// filling the AU lands
			int i = 0;
			SimpleFeatureIterator iterator = parcelCollection.features();
			try {

				while (missingHousingUnits > 0 && iterator.hasNext()) {

					// logger.warning("not enough parcels on the
					// "+bht.getZipCode(f)+" city to fill the housing unit
					// demand");
					// System.exit(0);
					SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, iterator.next(), bht.getZipCode(f));
					missingHousingUnits = missingHousingUnits - SPLUS.runOneSim(f);
				}
			} finally {
				iterator.close();
			}

		}

	}

	public static int getHousingUnitsGoals(File rootFile, String zipCode) throws IOException {
		File donneGen = new File(rootFile, "donneeGeographiques/donnecommune.csv");
		CSVReader csvReader = new CSVReader(new FileReader(donneGen));
		List content = csvReader.readAll();
		int ColLog = 0;
		int ColZip = 0;
		int nbObjLog = 0;
		for (Object object : content) {
			String[] row = (String[]) object;
			int i = 0;
			for (String s : row) {
				if (s.contains("nbLogObjectif")) {
					System.out.println("found log obj at column  : " + i);
					ColLog = i;
				}
				if (s.contains("zipcode")) {

					ColZip = i;
					System.out.println("found zipcode at column " + i);
				}
				i = i + 1;
			}
			if (row[ColZip].equals(zipCode)) {
				System.out.println("gotit");
				return (nbObjLog = Integer.parseInt(row[ColLog]));
			}
		}
		throw new FileNotFoundException("Housing units objectives not found");
	}

	public static void setGenStat(File rootFile) throws IOException {
		File fileName = new File(rootFile, "output/results.csv");
		FileWriter writer = new FileWriter(fileName, true);
		writer.append(
				"MUP-City Simulation, City zipCode , Selection type , SimPLU Simulation, number of simulated households in brownfield land, number of simulated households in brownfield land, numer of house, number of flat, ");
		writer.append("\n");
		writer.close();
	}
}
