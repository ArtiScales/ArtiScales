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
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.thema.mupcity.task.DecompTask;
import org.thema.mupcity.task.ProjectCreationTask;
import org.thema.mupcity.task.SimulTask;

import com.google.common.io.Files;
import com.vividsolutions.jts.io.ParseException;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.Indicators.BuildingToHousehold;
import fr.ign.cogit.simplu3d.exec.BasicSimulator;
import fr.ign.parameters.Parameters;

public class MainTask {

	public static void main(String[] args) throws Exception {
		runScenar();
	}

	public void exploSim() throws Exception {
		File rootFile = new File("donnee/couplage");

		// String[] zipCode = { "25086", "25030", "25084", "25112", "25136",
		// "25137", "25147", "25212", "25245", "25267", "25287", "25297",
		// "25381", "25395", "25397", "25410", "25429", "25448", "25454",
		// "25467", "25473", "25477", "25495", "25532", "25542", "25557",
		// "25561", "25593", "25611" };
		String[] zipCode = { "25495" };
		// "25245" "25495"

		// EXPLO
		// MUP-City output selection

		File MupOutputFolder = new File(rootFile, "depotConfigSpat");
		List<File> listMupOutput = null;
		for (File rasterOutputFolder : MupOutputFolder.listFiles()) {
			if (rasterOutputFolder.getName().endsWith(".tif") && rasterOutputFolder.getName().contains("eval_anal")) {
				SelecMUPOutput sortieMupCity = new SelecMUPOutput(rootFile, rasterOutputFolder);
				listMupOutput = sortieMupCity.run();
			}
		}
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
		for (File parcelSelection : listSelection) {
			// cette ligne est vraiment pas belle
			System.out.println(parcelSelection);
			String zip = new File(parcelSelection.getParent()).getParent().substring((new File(parcelSelection.getParent()).getParent().length() - 5),
					new File(parcelSelection.getParent()).getParent().length());
			SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelSelection.getParentFile(), zip, null);
			listBatis.addAll(SPLUS.run());
		}
		// reprise lorsque je bossais sur les codes d'indicateurs

		// List<File> listBatis = new ArrayList<File>();
		// for (File f : new File(rootFile, "output").listFiles()){
		// if (f.getName().startsWith("N")){
		// for (File ff : f.listFiles()){
		// if (!ff.getName().startsWith("N")){
		// for (File fff : ff.listFiles()){
		// for (File ffff : fff.listFiles()){
		// if (ffff.getName().startsWith("si")){
		// listBatis.add(ffff);
		// }
		// }
		// }
		// }
		// }
		// }
		// }

		// listBatis.add(new
		// File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0/25495/notBuilt-notSplit/simu0"));
		// listBatis.add(new
		// File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25495/built-Split/simu1"));
		listBatis.add(new File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N5_St_Moy_ahpx_seed42-eval_anal-20.0/25495/built-notSplit/simu2"));

		setGenStat(rootFile);
		int missingHousingUnits = 0;
		for (File f : listBatis) {
			BuildingToHousehold bht = new BuildingToHousehold(f, 100);
			int constructedHU = bht.run();
			System.out.println(" counstructed housing units : " + constructedHU);
			missingHousingUnits = getHousingUnitsGoals(new File("donnee/couplage"), bht.getZipCode(f)) - constructedHU;
			System.out.println("missingHousingUnits :" + missingHousingUnits);

			// TODO get the maxsurfaceparcelsplit parameter from the param file
			// and put it as a argument

			SelectParcels select2 = new SelectParcels(rootFile, new File(rootFile, "output/" + bht.getMupSimu(f)), bht.getZipCode(f), false, true);

			SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(select2.runGreenfieldSelected().toURI().toURL())).getFeatureSource().getFeatures();

		}
	}

	private static void runScenar() throws Exception {
		File rootFile = new File("donnee/couplage");
		String[] zipCode = { "25495" };

		//
		File paramFiles = new File(MainTask.class.getClassLoader().getResource("paramSet/").getPath());
		// for (File paramFile : paramFiles.listFiles()) {
		// if (paramFile.getName().endsWith(".xml")) {
		File paramFile = new File(MainTask.class.getClassLoader().getResource("paramSet/").getPath() + "param0.xml");

		Parameters p = Parameters.unmarshall(paramFile);

//		// simu MUP-City
//		String name = p.getString("nom");
//		System.out.println(name);
//		File folderIn = new File(rootFile, "/donneeGeographiques");
//		File folderOut = new File(rootFile, "depotConfigSpat/simu");
//		double width = 28303;
//		double height = 21019;
//		double xmin = 914760;
//		double ymin = 6680157;
//		double shiftX = 0;
//		double shiftY = 0;
//		double minSize = 20;
//		double maxSize = 5000;
//		double seuilDensBuild = 0;
//
//		double ahp8 = 0.083;
//		double ahp7 = 0.083;
//		double ahp6 = 0.083;
//		double ahp5 = 0.04;
//		double ahp4 = 0.218;
//		double ahp3 = 0.218;
//		double ahp2 = 0.218;
//		double ahp1 = 0.03;
//		double ahp0 = 0.027;
//
//		boolean useNU = false;
//		long seed = 42;
//
//		System.out.println("----------Project creation----------");
//		File projectFile = ProjectCreationTask.run(name, folderIn, folderOut, xmin, ymin, width, height, shiftX, shiftY, useNU);
//		System.out.println("----------Decomp task----------");
//		DecompTask.run(projectFile, name, minSize, maxSize, seuilDensBuild);
//		System.out.println("----------Simulation task----------");
//		File result = SimulTask.run(projectFile, name, p.getInteger("N"), p.getBoolean("strict"), ahp0, ahp1, ahp2, ahp3, ahp4, ahp5, ahp6, ahp7, ahp8, p.getBoolean("mean"), seed,
//				useNU);
//		System.out.println("resuuuult : " + result);
//		System.out.println("----------End task----------");
//
//		// temp
//		// File result = new File("donnee/couplage/depotConfigSpat/simu/compact/N5_St_Moy_ahpx_seed_42");
//
//		Files.copy(new File(result, "/" + p.getString("MupOutPath")), new File(folderOut.getParentFile(), p.getString("MupOutPath")));

		File outMup = (new SelecMUPOutput(rootFile, new File(rootFile, "depotConfigSpat/" + p.getString("MupOutPath")))).run().get(0);
		File selectParcels = (new SelectParcels(rootFile, outMup, zipCode[0], p.getBoolean("notBuilt"), p.getBoolean("splitParcel"))).runBrownfield();

		int missingHousingUnits = getHousingUnitsGoals(new File("donnee/couplage"), zipCode[0]);

		setGenStat(rootFile);
		String zone = "U";
		missingHousingUnits = fillSelectedParcels(selectParcels, missingHousingUnits, rootFile, zipCode[0], p, zone);

		SelectParcels select2 = new SelectParcels(rootFile, new File(rootFile, "output/" + outMup), zipCode[0], p.getBoolean("notBuilt"), true, p);

		File parcelCollectionAU = select2.runGreenfieldSelected();

		// filling the selected AU lands
		zone = "AU";
		missingHousingUnits = fillSelectedParcels(parcelCollectionAU, missingHousingUnits, rootFile, zipCode[0], p, zone);

		// fill the non-selected parcels till the end of time
		File parcelCollectionAUleft = select2.runGreenfield();

		missingHousingUnits = fillSelectedParcels(parcelCollectionAUleft, missingHousingUnits, rootFile, zipCode[0], p, zone);

		// }
		// }
	}

	private static int fillSelectedParcels(File selectParcels, int missingHousingUnits, File rootFile, String zipcode, Parameters p, String zone) throws Exception {
		SimpleFeatureIterator iterator = (new ShapefileDataStore(selectParcels.toURI().toURL())).getFeatureSource().getFeatures().features();
		File simuFile = new File(selectParcels.getParentFile(), "in" + zone);
		simuFile.mkdir();
		try {
			while (missingHousingUnits > 0 && iterator.hasNext()) {
				SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, simuFile, iterator.next(), zipcode, null, p);
				int fill = simPLUsimu.runOneSim();
				missingHousingUnits = missingHousingUnits - fill;
				System.out.println("missing housing units : " + missingHousingUnits);
				if (!iterator.hasNext()) {
					System.out.println(" MISSING ROOM FOR NEW HOUSING in the greyfield : " + missingHousingUnits + " HOUSING UNITS MISSING");
				}
			}
		} finally {
			iterator.close();
		}

		BuildingToHousehold bhtU = new BuildingToHousehold(simuFile, p.getInteger("HousingUnitSize"));
		bhtU.run();
		mergeBatis(simuFile);

		return missingHousingUnits;
	}

	public static int getHousingUnitsGoals(File rootFile, String zipCode) throws IOException {
		File donneGen = new File(rootFile, "donneeGeographiques/donnecommune.csv");
		CSVReader csvReader = new CSVReader(new FileReader(donneGen));
		List<String[]> content = csvReader.readAll();
		int ColLog = 0;
		int ColZip = 0;
		int nbObjLog = 0;
		for (String[] row : content) {
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
				SimpleFeatureCollection parcelCollection = (new ShapefileDataStore(f.toURI().toURL())).getFeatureSource().getFeatures();
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
