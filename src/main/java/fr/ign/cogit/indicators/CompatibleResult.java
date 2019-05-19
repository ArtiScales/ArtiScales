package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.modules.SelectParcels;
import fr.ign.cogit.modules.SimPLUSimulator;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;
import fr.ign.cogit.util.SimuTool;

public class CompatibleResult extends Indicators {
	File newGeoFile, newParcelDepot, newOutSimPLU, tmpFile;
	static String indicName = "compatibleResult";
	String fLine;

	// general counts
	int nbBuildingDifference, nbHUDifference;
	double aParcelDifference;

	List<String> parcelFree = new ArrayList<String>();
	List<String> parcelTaken = new ArrayList<String>();
	HashMap<String, Integer> notPossibleToConstruct = new HashMap<String, Integer>();

	public CompatibleResult(File rootFile, SimpluParametersJSON par, String scenarName, String variantName) throws Exception {
		super(par, rootFile, scenarName, variantName, indicName);
		fLine = "insee,objLgt,diffAfterSimu,diffAfterCompatibleResult,NumberBuildingsDifference,areaOfParcelDifference,Satisfies the objective of housing unit creation";
		newGeoFile = new File(getIndicFile(), "dataGeo");
		newGeoFile.mkdirs();
		newParcelDepot = new File(getIndicFile(), "newParcelDepot");
		newParcelDepot.mkdir();
		newOutSimPLU = new File(getIndicFile(), "newSimPLUDepot");
		newOutSimPLU.mkdirs();
		tmpFile = new File(getIndicFile(), "tmpFile");
		tmpFile.mkdirs();
		prepareNewGeoFile();
	}

	public static void main(String[] args) throws Exception {

		File root = new File("./result2903/");
		File rootParam = new File(root, "paramFolder");
		String scenario = "CDense";
		String variant = "variantMvData1";
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		CompatibleResult cr = new CompatibleResult(root, p, scenario, variant);
		cr.complete();
		// cr.complete("25473");
		cr.joinStatToCommunities("resume.csv");
	}

	public File joinStatToCommunities(String nameFileToJoin) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesOGSDS = new ShapefileDataStore((new File(getRootFile(), "/dataGeo/old/communities.shp")).toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesOGSDS.getFeatureSource().getFeatures();
		File result = joinStatToSFC(communitiesOG, new File(getIndicFile(), nameFileToJoin), new File(getIndicFile(), "commStat.shp"));
		communitiesOGSDS.dispose();
		return result;
	}

	public File joinStatToSFC(SimpleFeatureCollection collec, File statFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("communities");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("NbBuildDif", Double.class);
		sfTypeBuilder.add("AParcDif", Double.class);
		sfTypeBuilder.add("impCompObj", Boolean.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		try {
			while (it.hasNext()) {
				SimpleFeature featCity = it.next();
				String insee = (String) featCity.getAttribute("DEPCOM");
				CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
				String[] firstLine = stat.readNext();
				int inseeP = 0, NbBuildDifP = 0, AParcDifP = 0, impCompObjP = 0;
				for (int i = 0; i < firstLine.length; i++) {
					switch (firstLine[i]) {
					case "code":
						inseeP = i;
						break;
					case "NumberBuildingsDifference":
						NbBuildDifP = i;
						break;
					case "areaOfParcelDifference":
						AParcDifP = i;
						break;
					case "Satisfies the objective of housing unit creation":
						impCompObjP = i;
						break;
					}
				}
				for (String[] l : stat.readAll()) {
					if (l[inseeP].equals(insee)) {
						builder.set("the_geom", featCity.getDefaultGeometry());
						builder.set("INSEE", l[inseeP]);
						builder.set("NbBuildDif", Double.valueOf(l[NbBuildDifP]));
						builder.set("AParcDif", Double.valueOf(l[AParcDifP]));
						builder.set("impCompObj", Boolean.valueOf(l[impCompObjP]));
						result.add(builder.buildFeature(null));
						break;
					}
				}
				stat.close();
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return Vectors.exportSFC(result, outFile);
	}

	public SimpleFeatureCollection doWeFillWithBuilding(File rootFile, SimpluParametersJSON par, String scenarName, String variantName)
			throws FileNotFoundException, IOException {

		List<String> listInsee = FromGeom.getInsee(FromGeom.getCommunities(new File(rootFile, "dataGeo")), "DEPCOM");
		for (String insee : listInsee) {
			CSVReader csv = new CSVReader(new FileReader(new File(rootFile, "indic/bTH/" + scenarName + "/" + variantName + "/genStat.csv")));
			int diffNbLgt = 0;
			// first line stuff
			int posCode = 0;
			int posNbLgt = 0;
			String[] firstLine = csv.readNext();
			for (int i = 0; i < firstLine.length; i++) {
				String s = firstLine[i];
				if (s.equals("code")) {
					posCode = i;
				} else if (s.equals("diff_objectifPLH_housingUnit")) {
					posNbLgt = i;
				}
			}
			for (String[] line : csv.readAll()) {
				if (line[posCode].equals(insee)) {
					diffNbLgt = Integer.valueOf(line[posNbLgt]);
				}
			}
			if (diffNbLgt > 0) {
				// simulate to fill
			}
			csv.close();
		}
		return null;
	}

	public static void copyAll(File file, File toRoot) throws Exception {
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				File nFolder = new File(toRoot, f.getName());
				nFolder.mkdirs();
				copyAll(f, nFolder);
			} else {
				FileOutputStream out = new FileOutputStream(new File(toRoot, f.getName()));
				Files.copy(f.toPath(), out);
				out.close();
			}
		}
	}

	public void prepareNewGeoFile() throws Exception {
		copyAll((new File(getRootFile(), "dataGeo")), newGeoFile);
		// paste new parcels
		Vectors.copyShp("parcelGenExport", getParcelDepotGenFile().getParentFile(), newGeoFile);

		// merge simulated buildings
		List<File> lBuilding = Arrays.asList(new File(newGeoFile, "building.shp"), getSimPLUDepotGenFile());
		Vectors.mergeVectFiles(lBuilding, new File(newGeoFile, "building.shp"));
	}

	public int fillWithBuilding(String insee, int nbToFill) throws Exception {
		p.set("splitPartRecomp", "N");
		p.set("NC", true);
		// generate new parcels

		// does it has already been treated ?
		File parcelGen = new File(newParcelDepot, "parcelGenExport.shp");
		File parcelCity = new File(newParcelDepot, "parcelPartExport.shp");

		// if the parcels have already been treated (for the reprise of calculations)
		if (parcelGen.exists()) {
			ShapefileDataStore sds = new ShapefileDataStore(parcelGen.toURI().toURL());
			SimpleFeatureCollection parcelIn = sds.getFeatureSource().getFeatures();
			if (!ParcelFonction.getParcelByZip(parcelIn, insee).isEmpty()) {
				System.out.println("compatibility has already been calculated");
				sds.dispose();
				return 0;
			}
			sds.dispose();
		}

		SelectParcels sp = new SelectParcels(getRootFile(), newParcelDepot, getMupOutputFile(), p);
		sp.setGeoFile(newGeoFile);
		sp.selectAndDecompParcels(insee, true, parcelGen);

		// generate new SimPLUSimu
		SimPLUSimulator simPLU = new SimPLUSimulator(new File(getRootFile(), "paramFolder"), getIndicFile(), newGeoFile,
				new File(getRootFile(), "dataRegulation"), tmpFile, parcelCity, p, newOutSimPLU);
		System.out.println("number to fill : " + nbToFill);
		int restObj = simPLU.run(nbToFill, parcelCity);
		if (restObj > 0) {
			System.out.println("reste " + restObj + " logements qui ne pourront pas être construits");
			notPossibleToConstruct.put(insee, restObj);
			return restObj;
		}

		// Count the number of buildings simulated
		for (File f : newOutSimPLU.listFiles()) {
			if (f.getName().startsWith("out") && f.getName().endsWith(".shp") && f.getName().contains(insee)) {
				nbBuildingDifference++;
			}
		}
		return 0;
	}

	public void complete(String insee, File csvResume, SimpleFeatureCollection buildings, File geoFile, BuildingToHousingUnit bht) throws Exception {
		FileWriter csv = new FileWriter(csvResume, true);
		String line = insee;
		System.out.println("for city " + insee);
		SimpleFeatureCollection buildZip = SimuTool.getBuildingByZip(buildings, insee);
		if (!buildZip.isEmpty()) {
			List<SimpleFeature> sortedList = new LinkedList<SimpleFeature>();
			SimpleFeatureIterator it = buildZip.features();
			Hashtable<SimpleFeature, Double> repart = new Hashtable<SimpleFeature, Double>();
			try {
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					repart.put(feat, (Double) feat.getAttribute("EVAL"));
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				it.close();
			}

			// sort that to get the least evaluate buildings in first position to erase
			List<Entry<SimpleFeature, Double>> entryList = new ArrayList<Entry<SimpleFeature, Double>>(repart.entrySet());
			Collections.sort(entryList, new Comparator<Entry<SimpleFeature, Double>>() {
				@Override
				public int compare(Entry<SimpleFeature, Double> obj1, Entry<SimpleFeature, Double> obj2) {
					return obj1.getValue().compareTo(obj2.getValue());
				}
			});

			for (Entry<SimpleFeature, Double> s : entryList) {
				sortedList.add(s.getKey());
			}
			line = line + "," + String.valueOf(SimuTool.getHousingUnitsGoal(geoFile, insee));
			// estimation regarding to the number of buildings estimates
			int diffLgt = bht.getEstimationForACity(insee) - SimuTool.getHousingUnitsGoal(geoFile, insee);
			line = line + "," + String.valueOf(diffLgt);
			if (diffLgt > 0) {
				System.out.println("too much buildings");
				while (diffLgt > 0) {
					if (sortedList.size() > 0) {
						// TODO is a feature represents a building or only a part of it?
						SimpleFeature f = sortedList.remove(0);
						DefaultFeatureCollection tmp = new DefaultFeatureCollection();
						tmp.add(f);
						parcelFree.add((String) f.getAttribute("CODE"));
						nbBuildingDifference = nbBuildingDifference - 1;
						diffLgt = diffLgt - bht.distributionEstimate(tmp.collection());
					} else {
						break;
					}
				}
				line = line + "," + String.valueOf(diffLgt) + "," + nbBuildingDifference + "," + "-" + String.valueOf(calculateParcelSaved()) + ","
						+ true;
			} else if (diffLgt == 0) {
				System.out.println("prefect fit");
				line = line + "," + "";
			} else {
				System.out.println("not enough buildings");
				int rest = fillWithBuilding(insee, -diffLgt);
				// make the statistics
				boolean isRest = false;
				if (rest < 5 && rest > -5) {
					isRest = true;
				}
				line = line + "," + String.valueOf(rest) + "," + nbBuildingDifference + "," + String.valueOf(calculateParcelTaken(insee)) + ","
						+ isRest;
			}
			csv.append(line + "\n");
		} else {
			System.out.println("iz empty of buildings");
		}
		csv.close();
		setCountToZero();
	}

	/**
	 * make compatible a given community
	 * 
	 * @param insee
	 *            : zip/insee code of the community
	 * @throws Exception
	 */
	public void complete(String insee) throws Exception {
		File geoFile = new File(getRootFile(), "dataGeo");

		// bth indicator to get the total building shapefile and do the estimation of household
		BuildingToHousingUnit bht = new BuildingToHousingUnit(getRootFile(), p, scenarName, variantName);
		// get the building file and affect it an evaluation according to MUP-City's cells
		File tmpBati = new File(tmpFile, "batiment.shp");
		if (!tmpBati.exists()) {
			Vectors.exportSFC(SimuTool.giveEvalToBuilding(bht.getBuildingTotalFile(), bht.getMupOutputFile()), tmpBati);
		}
		ShapefileDataStore sds = new ShapefileDataStore(tmpBati.toURI().toURL());
		SimpleFeatureCollection buildings = sds.getFeatureSource().getFeatures();

		File csvResume = new File(getIndicFile(), "resume.csv");
		FileWriter csvW = new FileWriter(csvResume, true);
		csvW.append(fLine + "\n");
		csvW.close();
		complete(insee, csvResume, buildings, geoFile, bht);
		sds.dispose();
	}

	/**
	 * make a complete filling/erasing for each cities
	 * 
	 * @return
	 * @throws Exception
	 */
	public void complete() throws Exception {
		File geoFile = new File(getRootFile(), "dataGeo");

		// list of all insee we analyse
		List<String> listInsee = FromGeom.getInsee(FromGeom.getCommunities(geoFile), "DEPCOM");

		// for each cities
		for (String insee : listInsee) {
			complete(insee);
		}
	}

	private Double calculateParcelTaken(String insee) throws Exception {
		// get the newly simulated parcels code
		for (File f : (new File(getIndicFile(), "newSimPLUDepot")).listFiles()) {
			String name = f.getName();
			if (name.startsWith("out") && name.endsWith(".shp") && name.contains(insee)) {
				parcelTaken.add(name.split("_")[1].split(".shp")[0]);
			}
		}

		// calculate the newly simulated parcels area
		Double areaTaken = 0.0;
		ShapefileDataStore parcelSDS = new ShapefileDataStore((new File(newParcelDepot, "parcelPartExport.shp")).toURI().toURL());
		SimpleFeatureIterator parcelIt = parcelSDS.getFeatureSource().getFeatures().features();
		DefaultFeatureCollection parcelSimuled = new DefaultFeatureCollection();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				if (parcelTaken.contains(feat.getAttribute("CODE"))) {
					areaTaken = areaTaken + ((Geometry) feat.getDefaultGeometry()).getArea();
					parcelSimuled.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		System.out.println("taken : " + areaTaken + " m2");
		parcelSDS.dispose();
		completeParcelle(new File(newParcelDepot, "parcelSimuled.shp"), parcelSimuled);
		return areaTaken;
	}

	public Double calculateParcelSaved() throws Exception {
		Double surfSaved = 0.0;
		ShapefileDataStore parcelSDS = new ShapefileDataStore((new File(newGeoFile, "parcelGenExport.shp")).toURI().toURL());
		SimpleFeatureIterator parcelIt = parcelSDS.getFeatureSource().getFeatures().features();
		DefaultFeatureCollection parcelRejected = new DefaultFeatureCollection();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				if (parcelFree.contains(feat.getAttribute("CODE"))) {
					surfSaved = surfSaved + ((Geometry) feat.getDefaultGeometry()).getArea();
					parcelRejected.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		System.out.println("taken : " + surfSaved + " m2");
		parcelSDS.dispose();
		completeParcelle(new File(newParcelDepot, "parcelRejected.shp"), parcelRejected);
		return surfSaved;
	}

	public void completeParcelle(File parcelRejectedFile, SimpleFeatureCollection parcelRejected) throws Exception {
		if (!parcelRejectedFile.exists()) {
			Vectors.exportSFC(parcelRejected, new File(newParcelDepot, "parcelRejected.shp"));
		} else {
			File tmp = Vectors.exportSFC(parcelRejected, new File(tmpFile, "parcelRejected.shp"));
			List<File> merge = new ArrayList<File>();
			merge.add(tmp);
			merge.add(parcelRejectedFile);
			Vectors.mergeVectFiles(merge, parcelRejectedFile);
		}
	}

	private void setCountToZero() {
		nbBuildingDifference = nbHUDifference = 0;
		aParcelDifference = 0;
		parcelTaken = parcelFree = new ArrayList<String>();
		notPossibleToConstruct = new HashMap<String, Integer>();
	}

}
