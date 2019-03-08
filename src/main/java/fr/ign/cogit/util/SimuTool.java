package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class SimuTool {

	public static SimpluParametersJSON getParamFile(List<SimpluParametersJSON> lP, String scenar)
			throws FileNotFoundException {
		for (SimpluParametersJSON p : lP) {
			if (p.getString("name").equals(scenar)) {
				return p;
			}
		}
		throw new FileNotFoundException("no corresponding param file");
	}

	/**
	 * remove scenario specification and .json attribute from a sector file
	 * contained in the ressource.
	 * 
	 * @param stringParam
	 * @return
	 */

	public static String cleanSectorName(String stringParam) {
		// delete name of specials parameters
		if (stringParam.split(":").length == 2) {
			stringParam = stringParam.split(":")[1];
		}
		// del the .xml ref
		stringParam = stringParam.replace(".json", "");
		return stringParam;
	}

	public static void deleteDirectoryStream(Path path) throws IOException {
		Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

	/**
	 * get one or multiple communities parcels from infos contained in a parameter
	 * file
	 * 
	 * @param p
	 * @param geoFile
	 * @param regulFile
	 * @param tmpFile
	 * @return
	 * @throws Exception
	 */
	public static List<String> getIntrestingCommunities(SimpluParameters p, File geoFile, File regulFile, File variantFile) throws Exception {
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
			ShapefileDataStore comSDS = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
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

		if (!p.getString("decompIntoSector").equals("") && result.contains(p.getString("decompIntoSector"))) {
			String zipIntoSector = p.getString("decompIntoSector");
			try {
				System.out.println("this " + zipIntoSector + " is split into Sections");
				result.remove(zipIntoSector);
				List<String> diffSection = new ArrayList<>();
				ShapefileDataStore parcSDS = new ShapefileDataStore(FromGeom.getParcels(geoFile).toURI().toURL());
				SimpleFeatureIterator it = parcSDS.getFeatureSource().getFeatures().features();
				try {
					while (it.hasNext()) {
						SimpleFeature feat = it.next();
						if ((((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")))
								.equals(zipIntoSector)) {
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

		// // check the integrity of parcel
		// //if the parcels doesn't exists
		// System.out.println(result.size());
		// List<String> zip = new ArrayList<String>();
		// ShapefileDataStore parcSDS = new
		// ShapefileDataStore(FromGeom.getParcels(geoFile).toURI().toURL());
		// SimpleFeatureIterator it =
		// parcSDS.getFeatureSource().getFeatures().features();
		// try {
		// while (it.hasNext()) {
		// SimpleFeature feat = it.next();
		// String insee = (String) feat.getAttribute("CODE_DEP") + (String)
		// feat.getAttribute("CODE_COM");
		// if (zip) {
		// zip.remove(insee);
		// } else if (!out && !zip.contains(insee)) {
		// zip.add(insee);
		// }
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// it.close();
		// }
		// parcSDS.dispose();
		//
		// System.out.println(result.size());
		//
		// check if this zip is already done
		File parcelPart = new File(variantFile, "parcelGenExport.shp");

		if (parcelPart.exists()) {

			List<String> zip = new ArrayList<String>();
			ShapefileDataStore parcSDS = new ShapefileDataStore(parcelPart.toURI().toURL());
			SimpleFeatureIterator it = parcSDS.getFeatureSource().getFeatures().features();
			try {
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					String insee = (String) feat.getAttribute("INSEE");
					if (!zip.contains(insee)) {
						zip.add(insee);
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				it.close();
			}
			parcSDS.dispose();
			for (String z : zip) {
				if (result.contains(z)) {
					result.remove(z);
				}
			}
		}
		return result;
	}

	public static void writteError(String zipError, String error, File rootFile) throws IOException {
		FileWriter writer = new FileWriter(new File(rootFile, "mistakenCommunities"), true);
		writer.append(zipError + "\n");
		writer.append(error);
		writer.append("\n");
		writer.close();
	}

	public static boolean isCommunityRNU(File zoningFile, String insee) throws IOException {
		boolean answer = false;
		ShapefileDataStore zoningSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureIterator it = zoningSDS.getFeatureSource().getFeatures().features();
		try {
			while (it.hasNext() && !answer) {
				SimpleFeature feat = it.next();
				if (feat.getAttribute("INSEE") != null && feat.getAttribute("INSEE").equals(insee)
						&& feat.getAttribute("TYPEPLAN") != null && feat.getAttribute("TYPEPLAN").equals("RNU")) {
					answer = true;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		zoningSDS.dispose();
		return answer;
	}

	public static List<String> getLocationParamNames(File locationBuildingType, SimpluParameters p) {
		List<String> listZones = new ArrayList<String>();
		List<String> specialScenarZone = new ArrayList<String>();
		System.out.println(locationBuildingType);
		for (File param : locationBuildingType.listFiles()) {
			String nameParam = param.getName();
			if (nameParam.equals("default.json")) {
				continue;
			}
			// if the param repartition concerns a special scenario and it's not ours
			if (nameParam.split(":").length > 1) {
				if (nameParam.split(":")[0].equals(p.getString("scenarioPMSP3D"))) {
					specialScenarZone.add(nameParam);
				}
				else {
					continue;
				}
			}
			listZones.add(nameParam);
		}
		// if theres a zone special for the scenario and a regular one, the regular one
		// must be erased
		if (!specialScenarZone.isEmpty()) {
			for (String s : specialScenarZone) {
				listZones.remove(s.split(":")[1]);
			}
		}
		return listZones;
	}

	public static File createScenarVariantFolders(File packFile, File rootFile, String name) {

		String varFile = packFile.getParentFile().getName();
		String scenarFile = packFile.getParentFile().getParentFile().getName();
		File newFile = new File(rootFile, name + "/" + scenarFile + "/" + varFile);
		newFile.mkdirs();
		return newFile;
	}

	public static Hashtable<String, List<String[]>> getCitiesFromparticularHousingUnit(File housingUnit)
			throws IOException {
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
