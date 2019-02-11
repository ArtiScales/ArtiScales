package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.parameters.Parameters;

public class SimuTool {

	public static Parameters getParamFile(List<Parameters> lP, String scenar) throws FileNotFoundException {

		for (Parameters p : lP) {
			if (p.getString("name").equals(scenar)) {
				return p;
			}
		}
		throw new FileNotFoundException("no corresponding param file");
	}

	public static void deleteDirectoryStream(Path path) throws IOException {
		Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	/**
	 * get one or multiple communities parcels from infos contained in a parameter file
	 * 
	 * @param p
	 * @param geoFile
	 * @param regulFile
	 * @param tmpFile
	 * @return
	 * @throws Exception
	 */
	public static List<String> getIntrestingCommunities(Parameters p, File geoFile, File regulFile, File tmpFile) throws Exception {
		List<String> result = new ArrayList<String>();
		if (p.getString("singleCity").equals("true")) {
			String zips = p.getString("zip");
			// if multiple zips
			if (zips.contains(",")) {
				for (String z : zips.split(",")) {
					result.add(z);
				}
			}
			// if single zip
			else {
				result.add(zips);
			}
		} else {
			ShapefileDataStore comSDS = new ShapefileDataStore(GetFromGeom.getCommunities(geoFile).toURI().toURL());
			SimpleFeatureIterator it = comSDS.getFeatureSource().getFeatures().features();
			try {
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					if (!result.contains(feat.getAttribute("DEPCOM"))) {
						result.add((String) feat.getAttribute("DEPCOM"));
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				it.close();
			}
			comSDS.dispose();
		}

		if (!p.getString("decompIntoSector").equals("")) {
			String zipIntoSector = p.getString("decompIntoSector");
			try {
				result.remove(zipIntoSector);
				List<String> diffSection = new ArrayList<>();
				ShapefileDataStore parcSDS = new ShapefileDataStore(GetFromGeom.getParcels(geoFile).toURI().toURL());
				SimpleFeatureIterator it = parcSDS.getFeatureSource().getFeatures().features();
				try {
					while (it.hasNext()) {
						SimpleFeature feat = it.next();
						if ((((String) feat.getAttribute("CODE_DEP"))+((String) feat.getAttribute("CODE_COM"))).equals(zipIntoSector)) {
							if (!diffSection.contains(feat.getAttribute("SECTION"))) {
								diffSection.add((String) feat.getAttribute("SECTION"));
							}
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					it.close();
				}
				for (String s : diffSection) {
					result.add(zipIntoSector + s);
				}
				parcSDS.dispose();
			} catch (Exception e) {
				System.out.println("this city " + zipIntoSector + " is not in the set");
			}
		}
		return result;

	}

	public static File createScenarVariantFolders(File packFile, File rootFile, String name) {

		String varFile = packFile.getParentFile().getName();
		String scenarFile = packFile.getParentFile().getParentFile().getName();
		File newFile = new File(rootFile, name + "/" + scenarFile + "/" + varFile);
		newFile.mkdirs();
		return newFile;
	}

	public static Hashtable<String, List<String[]>> getCitiesFromparticularHousingUnit(File housingUnit) throws IOException {
		Hashtable<String, List<String[]>> result = new Hashtable<String, List<String[]>>();

		CSVReader csv = new CSVReader(new FileReader(housingUnit));
		String[] fString = csv.readNext();

		int zipNum = 0;

		for (int i = 0; i < fString.length; i++) {
			if (fString[i].equals("numero_parcelle")) {
				zipNum = i;
			}
		}

		for (String[] line : csv.readAll()) {
			String zipCode = line[zipNum].substring(0, 5);
			if (result.containsKey(zipCode)) {
				List<String[]> tempList = result.remove(zipCode);
				tempList.add(line);
				result.put(zipCode, tempList);
			} else {
				List<String[]> put = new ArrayList<String[]>();
				put.add(line);
				result.put(zipCode, put);
			}
		}
		csv.close();
		return result;
	}

	public static Hashtable<String, List<File>> getCitiesFromSimPLUSimulFolder(File simPLUDepot) throws IOException {
		Hashtable<String, List<File>> result = new Hashtable<String, List<File>>();
		for (File buildSimuFile : simPLUDepot.listFiles()) {
			if (buildSimuFile.getName().endsWith(".shp")) {
				ShapefileDataStore bSDS = new ShapefileDataStore(buildSimuFile.toURI().toURL());
				SimpleFeatureCollection bSFC = bSDS.getFeatureSource().getFeatures();
				String zipCode = ((String) bSFC.features().next().getAttribute("CODE")).substring(0, 5);

				if (result.containsKey(zipCode)) {
					List<File> tempList = result.remove(zipCode);
					tempList.add(buildSimuFile);
					result.put(zipCode, tempList);
				} else {
					List<File> put = new ArrayList<>();
					put.add(buildSimuFile);
					result.put(zipCode, put);
				}
				bSDS.dispose();
			}
		}
		return result;
	}

	public static List<List<List<File>>> generateResultConfigSimPLU(File rootFile) {
		List<List<List<File>>> buildingSimulatedPerSimu = new ArrayList<List<List<File>>>();

		for (File scenarFile : new File(rootFile, "SimPLUDepot").listFiles()) {
			List<List<File>> buildingSimulatedPerScenar = new ArrayList<List<File>>();
			for (File variantFile : scenarFile.listFiles()) {
				List<File> buildingSimulatedPerVar = new ArrayList<File>();
				for (File fileFile : variantFile.listFiles()) {
					if (fileFile.getName().endsWith(".shp") && fileFile.getName().startsWith("out-")) {
						buildingSimulatedPerVar.add(fileFile);
					}
				}
				buildingSimulatedPerScenar.add(buildingSimulatedPerVar);
			}
			buildingSimulatedPerSimu.add(buildingSimulatedPerScenar);
		}
		return buildingSimulatedPerSimu;
	}

	public static List<List<File>> generateResultParcels(File rootFile) {
		List<List<File>> buildingSimulatedPerSimu = new ArrayList<List<File>>();

		for (File scenarFile : new File(rootFile, "ParcelSelectionFile").listFiles()) {
			List<File> parcelGenPerScenar = new ArrayList<File>();
			for (File variantFile : scenarFile.listFiles()) {
				for (File fileFile : variantFile.listFiles()) {
					if (fileFile.getName().endsWith(".shp") && fileFile.getName().startsWith("parcel")) {
						parcelGenPerScenar.add(fileFile);
					}
				}
			}
			buildingSimulatedPerSimu.add(parcelGenPerScenar);
		}
		return buildingSimulatedPerSimu;
	}

	public static HashMap<String, Integer> increm(HashMap<String, Integer> in, String subject) {
		int initValue = in.getOrDefault(subject, 0);
		in.put(subject, initValue + 1);
		return in;
	}

}
