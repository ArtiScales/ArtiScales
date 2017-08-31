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
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.Indicators.BuildingToHousehold;

public class MainTask {

	public static void main(String[] args) throws Exception {

		File rootFile = new File("donnee/couplage");

		// String[] zipCode = { "25086", "25030", "25084", "25112", "25136",
		// "25137", "25147", "25212", "25245", "25267", "25287", "25297",
		// "25381", "25395", "25397", "25410", "25429", "25448", "25454",
		// "25467", "25473", "25477", "25495", "25532", "25542", "25557",
		// "25561", "25593", "25611" };
		String[] zipCode = { "25245" };
		// "25245"

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
				splitParcel = true;
				select = new SelectParcels(rootFile, outMupFile, zip, notBuilt, splitParcel);
				listSelection.add(select.runBrownfield());
			}
		}

		// SimPLU simulation
		List<File> listBatis = new ArrayList<File>();
		boolean singleBuild = true;
		for (File parcelSelection : listSelection) {
			// cette ligne est vraiment pas belle
			System.out.println(parcelSelection);
			String zip = new File(parcelSelection.getParent()).getParent().substring(
					(new File(parcelSelection.getParent()).getParent().length() - 5),
					new File(parcelSelection.getParent()).getParent().length());
			SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelSelection.getParentFile(), zip, null,
					singleBuild);
			listBatis.addAll(SPLUS.run());
		}
		// converstion buildings/households

		// List<File> listBatis = new ArrayList<File>();
		// listBatis.add(new
		// File("donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25245/notBuilt-notSplit/simu0/"));
		setGenStat(rootFile);
		int missingHousingUnits = 0;
		for (File f : listBatis) {
			BuildingToHousehold bht = new BuildingToHousehold(f, 100);
			int constructedHU = bht.run();
			System.out.println(" counstructed housing units : " + constructedHU);
			missingHousingUnits = getHousingUnitsGoals(new File("donnee/couplage"), bht.getZipCode(f)) - constructedHU;

			System.out.println("missingHousingUnits :" + missingHousingUnits);
			SelectParcels select2 = new SelectParcels(rootFile, new File(rootFile, "output/" + bht.getMupSimu(f)),
					bht.getZipCode(f), false, true);

			SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(
					select2.runGreenfieldSelected().toURI().toURL())).getFeatureSource().getFeatures();

			// filling the selected AU lands
			File fillFile = new File(f, "fillingBuildings");
			fillFile.mkdir();

			int i = 0;
			missingHousingUnits = fillAUSelectedParcels(parcelCollection, missingHousingUnits, bht, rootFile, fillFile,
					singleBuild, i);
			// fill the non-selected parcels till the end of time

			SimpleFeatureCollection parcelCollection2 = (new ShapefileDataStore(
					select2.runGreenfield().toURI().toURL())).getFeatureSource().getFeatures();

			missingHousingUnits = fillAUSelectedParcels(parcelCollection2, missingHousingUnits, bht, rootFile, fillFile,
					singleBuild, i);
			BuildingToHousehold bhtFill = new BuildingToHousehold(fillFile, 100);
			bhtFill.run();
			mergeBatis(fillFile);
		}
	}

	private static int fillAUSelectedParcels(SimpleFeatureCollection parcelCollection, int missingHousingUnits,
			BuildingToHousehold bht, File rootFile, File f, boolean singleBuild, int i) throws Exception {

		SimpleFeatureIterator iterator = parcelCollection.features();
		try {
			while (missingHousingUnits > 0 && iterator.hasNext() ) {
				SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, f, iterator.next(),
						bht.getZipCode(f.getParentFile()),
						new File(rootFile,
								"output/" + bht.getMupSimu(f.getParentFile()) + "/" + bht.getZipCode(f.getParentFile())
										+ "/" + bht.getSelection(f.getParentFile()) + "/snap"),
						singleBuild);
				System.out.println(f);
				int fill = SPLUS.runOneSim(f.getParentFile(), i);
				System.out.println("FIIIIIIIIIIL " + fill);
				missingHousingUnits = missingHousingUnits - fill;
				System.out.println("done " + i);
				i = i + 1;
				if (!iterator.hasNext()) {
					System.out.println(
							"STILLLL MISSING ROOM FOR NEW HOUSING : " + missingHousingUnits + " HOUSING UNITS MISSING");
				}

			}
		} finally {
			iterator.close();
		}
		return missingHousingUnits;
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
					ColLog = i;
				}
				if (s.contains("zipcode")) {
					ColZip = i;
				}
				i = i + 1;
			}
			if (row[ColZip].equals(zipCode)) {
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

	public static File mergeBatis(File file2MergeIn) throws Exception {
		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();
		for (File f : file2MergeIn.listFiles()) {
			if (f.toString().contains(".shp")) {
				SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(f.toURI().toURL()))
						.getFeatureSource().getFeatures();
				for (Object obj : parcelCollection.toArray()) {
					SimpleFeature feat = (SimpleFeature) obj;
					newParcel.add(feat);
				}
			}
		}
		File out = new File(file2MergeIn, "TotBatSimuFill.shp");
		if (!newParcel.isEmpty()) {
			SelectParcels.exportSFC(newParcel.collection(), out);
		}
		return out;
	}
}
