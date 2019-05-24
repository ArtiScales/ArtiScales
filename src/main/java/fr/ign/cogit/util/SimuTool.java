package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class SimuTool {
	public static void main(String[] args) throws Exception {
		// digForRepportOnACity(new File("/home/ubuntu/boulot/these/result2903/tmp/outCompMai9"), "25245", new File("/tmp/ville"));
		// digForPackWithoutSimu(new File("/media/ubuntu/saintmande/Packager/CDense/base/"),
		// new File("/home/ubuntu/boulot/these/result2903/tmp/SimPLUDepot/CDense/base/"), new File("/tmp/missingFromCDenseBase.csv"));

		getStatDenial(new File("/home/ubuntu/boulot/these/result2903/SimPLUDepot/CDense/variantMvData1"), new File("/tmp/salut"));

	}
	// getSimuInfo(new File("/home/ubuntu/boulot/these/result2903/SimPLUDepot/CDense/"), "25056000NTdiv590");
	// digForACity(new File("/media/ubuntu/saintmande/Packager/CDense/"), "25245");

	// Vectors.exportSFC(giveEvalToBuilding(new File("/home/ubuntu/boulot/these/result2903/tmp/SimPLUDepot/CDense/base/TotBatSimuFill.shp"), new
	// File("/home/ubuntu/boulot/these/result2903/MupCityDepot/CDense/base/CDense--N6_St_Moy_ahpE_seed_42-evalAnal-20.0.shp")),new
	// File("/home/ubuntu/boulot/these/result2903/tmp/SimPLUDepot/CDense/base/TotBatSimuFillEval.shp"));

	// }

	public static String makeCamelWordOutOfPhrases(String in) {
		String result = "";
		String[] tab = in.split(" ");
		for (String s : tab) {
			result = result + s.substring(0, 1).toUpperCase() + s.substring(1);
		}
		return result;

	}

	/**
	 * return a collection of buildings sharing the same zipcode (using the field "CODE")
	 * 
	 * @param buildingIn
	 * @param zip
	 * @return
	 * @throws IOException
	 */
	public static SimpleFeatureCollection getBuildingByZip(SimpleFeatureCollection buildingIn, String zip) throws IOException {
		SimpleFeatureIterator it = buildingIn.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if (((String) feat.getAttribute("CODE")).substring(0, 5).equals(zip)) {
					result.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return result.collection();
	}

	public static SimpluParametersJSON getParamFile(List<SimpluParametersJSON> lP, String scenar) throws FileNotFoundException {
		for (SimpluParametersJSON p : lP) {
			if (p.getString("name").equals(scenar)) {
				return p;
			}
		}
		throw new FileNotFoundException("no corresponding param file");
	}

	public static SimpleFeatureCollection giveEvalToBuilding(File buildingFile, File mupOutputFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore buildSDS = new ShapefileDataStore(buildingFile.toURI().toURL());
		SimpleFeatureCollection building = buildSDS.getFeatureSource().getFeatures();

		ShapefileDataStore mupSDS = new ShapefileDataStore(mupOutputFile.toURI().toURL());
		SimpleFeatureCollection mupOutput = mupSDS.getFeatureSource().getFeatures();

		SimpleFeatureIterator buildIt = building.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("SDPShon", Double.class);
		sfTypeBuilder.add("SurfacePar", Double.class);
		sfTypeBuilder.add("SurfaceSol", Double.class);
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("LIBELLE", String.class);
		sfTypeBuilder.add("TYPEZONE", String.class);
		sfTypeBuilder.add("BUILDTYPE", String.class);
		sfTypeBuilder.add("EVAL", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		try {
			while (buildIt.hasNext()) {
				SimpleFeature feat = buildIt.next();
				builder.set("the_geom", feat.getDefaultGeometry());
				builder.set("SDPShon", feat.getAttribute("SDPShon"));
				builder.set("SurfacePar", feat.getAttribute("SurfacePar"));
				builder.set("SurfaceSol", feat.getAttribute("SurfaceSol"));
				builder.set("CODE", feat.getAttribute("CODE"));
				builder.set("LIBELLE", feat.getAttribute("LIBELLE"));
				builder.set("TYPEZONE", feat.getAttribute("TYPEZONE"));
				builder.set("BUILDTYPE", feat.getAttribute("BUILDTYPE"));
				builder.set("EVAL", ParcelFonction.getCloseEvalInParcel(feat, mupOutput));
				result.add(builder.buildFeature(null));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			buildIt.close();
		}
		mupSDS.dispose();
		buildSDS.dispose();
		return result.collection();
	}

	/**
	 * get the objective of housing density for a particular city in its "DEPCOM" attribute
	 * 
	 * TODO get too much time for a simple op. extract the attribute table and play from there
	 *
	 * @param geoFile
	 *            closer
	 * @param zipCode
	 * @return
	 * @throws IOException
	 */
	public static int getDensityGoal(File geoFile, String zipCode) throws IOException {
		File objFile = new File(geoFile, "communities.csv");
		if (!objFile.exists()) {
			extractCSVFromSHP(FromGeom.getCommunities(geoFile), geoFile);
		}

		int result = 0;
		// ShapefileDataStore sds = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
		// SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		// try {
		// while (it.hasNext()) {
		// SimpleFeature feat = it.next();
		// if (feat.getAttribute("DEPCOM").equals(zipCode)) {
		// result = (int) feat.getAttribute("objDens");
		// break;
		// }
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// it.close();
		// }
		// sds.dispose();
		int objP = 0, inseeP = 0;
		CSVReader csv = new CSVReader(new FileReader(objFile));
		String[] firstLine = csv.readNext();
		for (int i = 0; i < firstLine.length; i++) {
			if (firstLine[i].equals("objDens")) {
				objP = i;
			}
			if (firstLine[i].equals("DEPCOM")) {
				inseeP = i;
			}
		}
		for (String[] line : csv.readAll()) {
			if (line[inseeP].equals(zipCode)) {
				result = Integer.valueOf(line[objP]);
			}
		}

		csv.close();
		return result;
	}
	//
	// public static void main(String[] args) throws IOException {
	// extractCSVFromSHP(new File("/home/ubuntu/boulot/these/result0308/dataGeo/communities.shp"), new File("/tm)p/"));
	// }

	public static File extractCSVFromSHP(File shapeFile, File outFolder) throws IOException {

		ShapefileDataStore sds = new ShapefileDataStore(shapeFile.toURI().toURL());
		SimpleFeatureCollection coll = sds.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = coll.features();
		CSVWriter csv = new CSVWriter(new FileWriter(new File(outFolder, shapeFile.getName().replace(".shp", "") + ".csv")), ',', '\0');
		int count = coll.getSchema().getAttributeCount() - 1;
		String[] firstLine = new String[count];
		for (int i = 1; i <= count; i++) {
			firstLine[i - 1] = coll.getSchema().getAttributeDescriptors().get(i).getName().toString();
		}
		csv.writeNext(firstLine);
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				String[] temp = new String[feat.getAttributeCount() - 1];
				for (int i = 1; i < feat.getAttributeCount(); i++) {
					String val = String.valueOf(feat.getAttribute(i));
					if (val.toLowerCase().equals("") || val.toLowerCase().equals("null") || val.toLowerCase().equals("nan")) {
						val = "0";
					}
					temp[i - 1] = val;
				}
				csv.writeNext(temp);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		csv.close();
		sds.dispose();
		return outFolder;
	}

	/**
	 * get the objective of housing unit creation for a particular city in its "DEPCOM" attribute
	 * 
	 * TODO get too much time for a simple op. extract the attribute table and play from there
	 * 
	 * @param geoFile
	 * @param zipCode
	 * @return
	 * @throws IOException
	 */
	public static int getHousingUnitsGoal(File geoFile, String zipCode) throws IOException {

		File objFile = new File(geoFile, "communities.csv");
		if (!objFile.exists()) {
			extractCSVFromSHP(FromGeom.getCommunities(geoFile), geoFile);
		}

		int result = 0;
		// ShapefileDataStore sds = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
		// SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		// try {
		// while (it.hasNext()) {
		// SimpleFeature feat = it.next();
		// if (feat.getAttribute("DEPCOM").equals(zipCode)) {
		// result = (int) feat.getAttribute("objDens");
		// break;
		// }
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// it.close();
		// }
		// sds.dispose();
		int objP = 0, inseeP = 0;
		CSVReader csv = new CSVReader(new FileReader(objFile));
		String[] firstLine = csv.readNext();
		for (int i = 0; i < firstLine.length; i++) {
			if (firstLine[i].equals("objLgt")) {
				objP = i;
			}
			if (firstLine[i].equals("DEPCOM")) {
				inseeP = i;
			}
		}
		for (String[] line : csv.readAll()) {
			if (line[inseeP].equals(zipCode)) {
				result = Integer.valueOf(line[objP]);
			}
		}

		csv.close();
		return result;
	}

	/**
	 * remove scenario specification and .json attribute from a sector file contained in the ressource.
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
	 * get one or multiple communities parcels from infos contained in a parameter file
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
						if ((((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"))).equals(zipIntoSector)) {
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
				if (feat.getAttribute("INSEE") != null && feat.getAttribute("INSEE").equals(insee) && feat.getAttribute("TYPEPLAN") != null
						&& feat.getAttribute("TYPEPLAN").equals("RNU")) {
					answer = true;
					break;
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
		for (File param : locationBuildingType.listFiles()) {
			String nameParam = param.getName();
			if (nameParam.equals("default.json")) {
				continue;
			}
			// if the param repartition concerns a special scenario and it's not ours
			if (nameParam.split(":").length > 1) {
				if (nameParam.split(":")[0].equals(p.getString("scenarioPMSP3D"))) {
					specialScenarZone.add(nameParam);
				} else {
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
			for (File variantFolder : scenarFile.listFiles()) {
				List<File> buildingSimulatedPerVar = new ArrayList<File>();
				for (File superPackFolder : variantFolder.listFiles()) {
					for (File packFolder : superPackFolder.listFiles()) {
						for (File file : packFolder.listFiles()) {
							if (file.getName().endsWith(".shp") && file.getName().startsWith("out-")) {
								buildingSimulatedPerVar.add(file);
							}
						}
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

		for (File scenarFolder : new File(rootFile, "ParcelSelectionDepot").listFiles()) {
			List<File> parcelGenPerScenar = new ArrayList<File>();
			for (File variantFolder : scenarFolder.listFiles()) {
				for (File file : variantFolder.listFiles()) {
					if (file.getName().equals("parcelGenExport.shp")) {
						parcelGenPerScenar.add(file);
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

	/**
	 * When there's a zoning problem (when running the packager distributed on the grid), put the right features of the zoning shapefile into the packages
	 * 
	 * @param fIn
	 *            : folder from where to recursively seek for the <i>geoSnap</i> folder
	 * @param zoningFile
	 *            : the original zoning file from where to copy the different zoning features
	 * @throws Exception
	 */
	public static void replaceZoning(File fIn, File zoningFile) throws Exception {
		ShapefileDataStore zoningSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection zoningOG = zoningSDS.getFeatureSource().getFeatures();
		for (File f : fIn.listFiles()) {
			if (f.getName().equals("geoSnap")) {
				ShapefileDataStore parcelSDS = new ShapefileDataStore(new File(f.getParentFile(), "parcelle.shp").toURI().toURL());
				SimpleFeatureCollection parcelOG = parcelSDS.getFeatureSource().getFeatures();
				Geometry union = Vectors.unionSFC(parcelOG);
				Vectors.exportSFC(Vectors.snapDatas(zoningOG, union), new File(f, "zone_urba.shp"));
				parcelSDS.dispose();
			} else if (f.isDirectory()) {
				replaceZoning(f, zoningFile);
			}
		}
		zoningSDS.dispose();
	}

	/**
	 * put the aggregate denial of cuboid configuration propositions into a .csv
	 * 
	 * @param fIn
	 *            : folder from where the recursive serarch for <i>importantFile</i> log will start
	 * @param fOut
	 *            : file to write the outputed .csv
	 * @return
	 * @throws IOException
	 */
	public static File getStatDenial(File fIn, File fOut) throws IOException {
		HashMap<String, Long> result = new HashMap<String, Long>();
		String str = getStatDenial(fIn, result).toString();
		System.out.println(str);
		str = str.replace("{", "").replace("}", "").replace(" ", "");
		CSVWriter csv = new CSVWriter(new FileWriter(fOut));

		for (String st : str.split(",")) {
			String[] s = st.split("=");
			csv.writeNext(s);
		}
		csv.close();
		return fOut;
	}

	/**
	 * make the statistics about simPLU denials with a recursive search of file within folders
	 * 
	 * @param fIn
	 *            where to start the recursive search
	 * @param result
	 *            list of it for the recursive algorithm
	 * @return
	 * @throws IOException
	 */
	public static HashMap<String, Long> getStatDenial(File fIn, HashMap<String, Long> result) throws IOException {
		for (File f : fIn.listFiles()) {
			if (f.isDirectory()) {
				result = getStatDenial(f, result);
			} else if (f.getName().equals("importantInfo")) {
				CSVReader read = new CSVReader(new FileReader(f), ';');
				for (String[] l : read.readAll()) {
					if (l[0].startsWith("denial reasons : {")) {
						String reasons = l[0].replace("denial reasons : {", "").replace("}", "");
						if (reasons.contains("=")) {
							for (String reason : reasons.split(",")) {
								reason = reason.replace(" ", "");
								String topic = reason.split("=")[0];								
								long val = Integer.valueOf(reason.split("=")[1]);
								Long tmp = result.putIfAbsent(topic, val);
								if (tmp != null) {
									result.replace(topic, tmp, tmp + val);
								}
							}
						}
					}
				}
				read.close();
			}
		}
		return result;
	}

	public static void getSimuInfo(File fIn, String code) throws IOException {
		for (File f : fIn.listFiles()) {

			if (f.isDirectory()) {
				getSimuInfo(f, code);
			}
			if (f.getName().endsWith(code + ".shp")) {
				CSVReader read = new CSVReader(new FileReader(new File(f.getParentFile(), "importantInfo")), ';');
				boolean display = false;
				for (String[] l : read.readAll()) {
					if (l[0].equals(code)) {
						display = true;
						System.out.println("for the parcel " + l[0]);
					} else if (display) {
						if (l[0].startsWith("25")) {
							display = false;
						}
						if (display) {
							System.out.println(l[0]);
						}
					}
				}
				read.close();
			}
		}
	}

	public static void digForACode(File fIn, String nameSHP, String code) throws IOException {
		for (File f : fIn.listFiles()) {

			if (f.isDirectory()) {
				digForACode(f, nameSHP, code);
			}
			if (f.getName().startsWith(nameSHP) && f.getName().endsWith(".shp")) {
				ShapefileDataStore communitiesSDS = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
				SimpleFeatureIterator it = communitiesOG.features();
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					String insee = (String) feat.getAttribute("CODE");
					if (insee != null && insee.equals(code)) {
						System.out.println(f);
						break;
					}
				}
				it.close();
				communitiesSDS.dispose();
			}
		}
	}

	public static void digForACity(File fIn, String thisCity) throws IOException {
		for (File f : fIn.listFiles()) {
			if (f.isDirectory()) {
				digForACity(f, thisCity);
			}
			if (f.getName().equals("parcelle.shp")) {
				ShapefileDataStore communitiesSDS = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
				SimpleFeatureIterator it = communitiesOG.features();
				int toto = 0;
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					String insee = (String) feat.getAttribute("INSEE");
					if (insee != null && insee.equals(thisCity)) {
						if (feat.getAttribute("DoWeSimul").equals("true")) {
							toto++;
						}

					}
				}
				it.close();
				communitiesSDS.dispose();
				if (toto > 3) {
					System.out.println(f);
				}
			}
		}
	}

	public static String digForRepportOnACity(File fIn, String insee, File outFile) throws IOException {
		String result = "";
		FileWriter w = new FileWriter(outFile);
		CSVReader read = new CSVReader(new FileReader(fIn), '£');
		boolean record = false;
		for (String[] s : read.readAll()) {
			String line = s[0];
			if (line.equals("for city " + insee)) {
				record = true;
				System.out.println("ja");
				continue;
			}
			if (record) {
				if (line.startsWith("for city ")) {
					break;
				}
				System.out.println(line);
				result = "\n" + result + line;
				w.write(line + "\n");
			}

		}
		// System.out.println(result);
		read.close();
		w.close();
		return result;
	}

/**
 * dig for SimPLUPackager folders that are uninteresting to be simulated
 * Put them in a csv file ?!
 * Do we copy/delete them? 
 * 
 * @param fPack
 * @param fSimu
 * @param fOut
 * @throws IOException
 */
	public static void digForUselessPacks(File fPack, File fSimu, File fOut) throws IOException {
		List<String[]> lL = new ArrayList<String[]>();
		String[] fl = { "SuperPack", "Pack" };
		lL.add(fl);		
		for (File superPack : fPack.listFiles()) {
			String superP = superPack.getName();
			for (File pack : superPack.listFiles()) {
				String p = pack.getName();
				for (File f : pack.listFiles()) {
					//if there is a parcel shapefile
					if (f.getName().equals("parcelle.shp")) {
						ShapefileDataStore parcelleSDS = new ShapefileDataStore(f.toURI().toURL());
						SimpleFeatureCollection parcelleOG = parcelleSDS.getFeatureSource().getFeatures();
						SimpleFeatureIterator it = parcelleOG.features();
						boolean add = false;
						while (it.hasNext()) {
							SimpleFeature feat = it.next();
							if (feat.getAttribute("DoWeSimul").equals("true"))
								add = true;
								break;
						}
						if (add) {
							String[] l = { superP, p };
							lL.add(l);
						}
						it.close();
						parcelleSDS.dispose();
					}
				}
			}
		}
		CSVWriter csv = new CSVWriter(new FileWriter(fOut));

		csv.writeAll(lL);

		csv.close();
	}

	
	
	/**
	 * create a .csv file from a Folder that contains superPacks that contains Packs from SimPLUPackager 
	 * @param fPack
	 * @param fSimu
	 * @param fOut
	 * @throws IOException
	 */
	public static void digForPackWithoutSimu(File fPack, File fSimu, File fOut) throws IOException {
		List<String[]> lL = new ArrayList<String[]>();
		String[] fl = { "SuperPack", "Pack" };
		lL.add(fl);
		for (File superPack : fPack.listFiles()) {
			String superP = superPack.getName();
			for (File pack : superPack.listFiles()) {
				String p = pack.getName();
				for (File f : pack.listFiles()) {
					if (f.getName().equals("parcelle.shp")) {
						ShapefileDataStore parcelleSDS = new ShapefileDataStore(f.toURI().toURL());
						SimpleFeatureCollection parcelleOG = parcelleSDS.getFeatureSource().getFeatures();
						SimpleFeatureIterator it = parcelleOG.features();
						boolean add = false;
						while (it.hasNext()) {
							SimpleFeature feat = it.next();
							if (feat.getAttribute("DoWeSimul").equals("true")) {
								add = true;
								break;
							}
						}
						it.close();
						if (add) {
							File simuFile = new File(fSimu, superP + "/" + p + "/");
							if (!simuFile.exists()) {
								System.out.println("prb " + simuFile);
								add = true;
							} else {
								boolean exist = false;
								for (File fs : simuFile.listFiles()) {
									if (fs.getName().startsWith("out") && fs.getName().endsWith(".shp")) {
										exist = true;
										break;
									}

								}
								if (exist) {
									add = false;
								}
							}
						}
						if (add) {
							String[] l = { superP, p };
							lL.add(l);
						}
						parcelleSDS.dispose();
					}
				}
			}
		}
		CSVWriter csv = new CSVWriter(new FileWriter(fOut));

		csv.writeAll(lL);

		csv.close();
	}

	public static void digForBesac(File fIn) throws IOException {
		for (File f : fIn.listFiles()) {
			if (f.isDirectory()) {
				digForBesac(f);
			}
			if (f.getName().equals("zone_urba.shp")) {
				ShapefileDataStore communitiesSDS = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
				SimpleFeatureIterator it = communitiesOG.features();

				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					String code = (String) feat.getAttribute("INSEE");
					String libelle = (String) feat.getAttribute("LIBELLE");

					if (code.equals("25056") && libelle.equals("1AU-D")) {
						// File parcelF = new File(f.getParentFile().getParentFile(), "parcelle.shp");
						// ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelF.toURI().toURL());
						// SimpleFeatureCollection parcelOG = parcelSDS.getFeatureSource().getFeatures();
						// SimpleFeatureIterator itParcel = parcelOG.features();
						// int nb = 0;
						// while (itParcel.hasNext()) {
						// SimpleFeature featP = itParcel.next();
						// String auth = (String) featP.getAttribute("DoWeSimul");
						// if (auth.equals("true")) {
						// nb++;
						// }
						// }
						// itParcel.close();
						// if (nb > 10) {
						System.out.println(f);
						// }
					}
				}
				it.close();
				communitiesSDS.dispose();
			}
		}
	}

	/**
	 * fix the field TYPEZONE of buildings simulated with a wrong order of the crossed type of zoning (the most intersected type must be the first one written so it's taken for the
	 * statistics)
	 * 
	 * @param buildingFile
	 * @param zoningFile
	 * @return
	 * @throws IOException
	 */
	public static SimpleFeatureCollection fixBuildingForZone(File buildingFile, File zoningFile, boolean replace) throws IOException {
		ShapefileDataStore bSDS = new ShapefileDataStore(buildingFile.toURI().toURL());
		ShapefileDataStore zSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection buildings = bSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection zoning = zSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = buildings.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try {
			buildingLoop: while (it.hasNext()) {
				SimpleFeature building = it.next();
				Geometry buildingGeom = (Geometry) building.getDefaultGeometry();
				SimpleFeatureCollection zones = Vectors.snapDatas(zoning, buildingGeom);
				SimpleFeatureIterator zoneIt = zones.features();
				try {
					while (zoneIt.hasNext()) {
						SimpleFeature zone = zoneIt.next();
						Geometry zoneGeom = (Geometry) zone.getDefaultGeometry();
						String typeZone = "";

						if (zoneGeom.contains(buildingGeom.buffer(-0.5))) {
							building.setAttribute("TYPEZONE", zone.getAttribute("TYPEZONE"));
							result.add(building);
							continue buildingLoop;
						} else if (zoneGeom.intersects(buildingGeom)) {
							// TODO develop that (but I have no cases there)
							// SimpleFeatureCollection salut = ;
							// for ()
						} else {
							System.out.println("not normal case");
						}

						building.setAttribute("TYPEZONE", typeZone);

					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					zoneIt.close();
				}
				System.out.println(building);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		bSDS.dispose();
		zSDS.dispose();
		if (replace) {
			Vectors.exportSFC(result.collection(), buildingFile);
		}
		return result.collection();
	}

	public static String getLibInfo(BasicPropertyUnit bPU, String field) {
		String stringResult = "";
		List<String> typeZones = new LinkedList<>();
		// if multiple parts of a parcel has been simulated, put a long name containing them all
		try {
			HashMap<String, Double> repart = new HashMap<String, Double>();
			for (SubParcel subParcel : bPU.getCadastralParcels().get(0).getSubParcels()) {
				String temporaryType = "";
				if (field.equals("TYPEZONE")) {
					temporaryType = subParcel.getUrbaZone().getTypeZone();
				} else if (field.equals("LIBELLE")) {
					temporaryType = subParcel.getUrbaZone().getLibelle();
				} else {
					throw new Error("invalid name");
				}

				if (!repart.containsKey(temporaryType)) {
					repart.put(temporaryType, subParcel.getArea());
				} else {
					double tmp = repart.remove(temporaryType);
					repart.put(temporaryType, subParcel.getArea() + tmp);
				}

			}
			List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(repart.entrySet());

			Collections.sort(entryList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
					return obj2.getValue().compareTo(obj1.getValue());
				}
			});

			for (Entry<String, Double> s : entryList) {
				typeZones.add(s.getKey());
			}

			for (String typeZoneTemp : typeZones) {
				stringResult = stringResult + typeZoneTemp + "+";
			}
			stringResult = stringResult.substring(0, stringResult.length() - 1);

		} catch (NullPointerException np) {
			stringResult = "NC";
		}
		return stringResult;
	}

}
