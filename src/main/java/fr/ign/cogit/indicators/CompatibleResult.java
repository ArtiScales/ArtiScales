package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.modules.SelectParcels;
import fr.ign.cogit.modules.SimPLUSimulator;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.SimuTool;

public class CompatibleResult extends Indicators {
	File newGeoFile, newParcelDepot;
	static String indicName = "compatibleResult";
	List<String> parcelFree = new ArrayList<String>();
	HashMap<String, Integer> notPossibleToConstruct = new HashMap<String, Integer>();

	public CompatibleResult(File rootFile, SimpluParametersJSON par, String scenarName, String variantName) throws Exception {
		super(par, rootFile, scenarName, variantName, indicName);
		newGeoFile = new File(indicFile, "dataGeo");
		newGeoFile.mkdirs();
		newParcelDepot = new File(indicFile, "newParcelDepot");
		newParcelDepot.mkdir();
		prepareNewGeoFile();
	}

	public static void main(String[] args) throws Exception {

		File root = new File("./result2903/tmp/");
		File rootParam = new File(root, "paramFolder");
		String scenario = "CDense";
		String variant = "variantMvData1";
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		CompatibleResult cr = new CompatibleResult(root, p, scenario, variant);
		cr.completeAll();
		cr.calculateParcelSaved();
		System.out.println(cr.notPossibleToConstruct);
		// cr.prepareNewGeoFile();
		// cr.fillWithBuilding("25112", 138);
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
		copyAll((new File(rootFile, "dataGeo")), newGeoFile);
		// paste new parcels
		Vectors.copyShp("parcelGenExport", parcelDepotGenFile.getParentFile(), newGeoFile);

		// merge simulated buildings
		List<File> lBuilding = Arrays.asList(new File(newGeoFile, "building.shp"), simPLUDepotGenFile);
		Vectors.mergeVectFiles(lBuilding, new File(newGeoFile, "building.shp"));
	}

	public void fillWithBuilding(String insee, int nbToFill) throws Exception {
		p.set("splitPartRecomp", "N");
		p.set("NC", true);
		// generate new parcels
		SelectParcels sp = new SelectParcels(rootFile, newParcelDepot, mupOutputFile, p);
		sp.setGeoFile(newGeoFile);
		File parcelNew = new File(newParcelDepot, "parcelGenExport.shp");
		sp.selectAndDecompParcels(insee, true, parcelNew);

		// generate new SimPLUSimu
		File outSimPLU = new File(indicFile, "newSimPLUDepot");
		File tmpFile = new File(indicFile, "tmpFile");
		tmpFile.mkdirs();
		SimPLUSimulator simPLU = new SimPLUSimulator(new File(rootFile, "paramFolder"), indicFile, newGeoFile, new File(rootFile, "dataRegulation"),
				tmpFile, parcelNew, p, outSimPLU);
		int restObj = simPLU.run(nbToFill, parcelNew);
		if (restObj > 0) {
			System.out.println("reste " + restObj + " logements qui ne pourront pas être construits");
			notPossibleToConstruct.put(insee, restObj);
		}
	}

	/**
	 * make a complete filling/erasing for each cities
	 * 
	 * @return
	 * @throws Exception
	 */
	public void completeAll() throws Exception {
		File geoFile = new File(rootFile, "dataGeo");

		// list of all insee we analyse
		List<String> listInsee = FromGeom.getInsee(FromGeom.getCommunities(geoFile), "DEPCOM");

		// bth indicator to get the total building shapefile and do the estimation of household
		BuildingToHousingUnit bht = new BuildingToHousingUnit(rootFile, p, scenarName, variantName);
		// get the building file and affect it an evaluation according to MUP-City's cells
		SimpleFeatureCollection buildings = SimuTool.giveEvalToBuilding(bht.getBuildingTotalFile(), bht.mupOutputFile);
		// for each cities
		for (String insee : listInsee) {
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

				// estimation regarding to the number of buildings estimates
				int diffLgt = bht.getEstimationForACity(insee) - SimuTool.getHousingUnitsGoal(geoFile, insee);
				if (diffLgt > 0) {
					System.out.println("too much buildings");
					while (diffLgt > 0) {
						if (sortedList.size() > 0) {
							SimpleFeature f = sortedList.remove(0);
							DefaultFeatureCollection tmp = new DefaultFeatureCollection();
							tmp.add(f);
							parcelFree.add((String) f.getAttribute("CODE"));
							diffLgt = diffLgt - bht.distributionEstimate(tmp.collection());
						} else {
							break;
						}
					}
				} else if (diffLgt == 0) {
					System.out.println("prefect fit");
				} else {
					System.out.println("not enough buildings");
					fillWithBuilding(insee, diffLgt);
				}
			}
		}
	}

	public Double calculateParcelSaved() throws IOException {
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
		System.out.println("saved : " + surfSaved);
		parcelSDS.dispose();
		Vectors.exportSFC(parcelRejected, new File(newParcelDepot, "parcelRejected.shp"));
		return surfSaved;
	}

}
