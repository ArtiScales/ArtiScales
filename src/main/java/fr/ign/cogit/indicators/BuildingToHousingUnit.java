package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.SimuTool;
import fr.ign.parameters.Parameters;

public class BuildingToHousingUnit extends Indicators {

	// infos from the Artiscales simulations

	List<File> buildingList;
	double surfaceLogDefault;

	// infos about the buildings
	String numeroParcel;
	double surfaceParcel;
	int nbHU;
	double surfaceLogements; // c'est une moyenne
	int nbStairs;
	double densite;
	String particularFirstLine;
	String genFirstLine;

	// different housing unit types
	final String NUTIN = "une niche pour ton iench";
	final String INDIV = "logement individuel";
	final String DBLINDIV = "double logement individuel";
	final String SDWELLING = "petit logement collectif";
	final String LDWELLING = "grand logement collectif";

	public BuildingToHousingUnit(List<File> buildingList, File simuFile, Parameters par) {
		super(par);
		this.buildingList = buildingList;
		this.simuFile = simuFile;
		rootFile = new File(p.getString("rootFile"));
		surfaceLogDefault = p.getInteger("HousingUnitSize");
		particularFirstLine = "numero_parcelle,surface_de_plancher," + "surface_au_sol," + "nombre_de_logements," + "type_du_logement," + "zone_de_la_construction,"
				+ "moyenne_de_la_surface_plancher_par_logements," + "densite_batie";

		genFirstLine = "code INSEE," + "nombre_de_logements," + "nombre_de_logements_individuels," + "nombre_de_logements_doubles," + "nombre_de_logements_en_petit_collectif,"
				+ "nombre_de_logements_en_grand_collectif," + "nombre_de_batiments_construits_dans_les_zones_U," + "nombre_de_batiment_construit_dans_les_zones_AU,"
				+ "nombre_de_batiments_construit_dans_les_zones_N_et_A," + "somme_de_la_surface_au_sol_des_logements," + "moyenne_de_surface_au_sol_des_bâtiments,"
				+ "ecart_type_de_la_surface_au_sol_des_bâtiments," + "somme_de_la_surface_plancher_des_logements," + "moyenne_de_la_surface_plancher_des_bâtiments,"
				+ "ecart_type_de_la_surface_plancher_des_bâtiments," + "moyenne_de_la_densité_batie," + "ecart_type_de_la_densité_batie,"
				+ "différence_avec_les_objectifs_régionaux";

	}

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parameterTechnic.xml"));
		lF.add(new File(rootParam, "parameterScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		File batisSimulatedFile = new File("/home/mcolomb/informatique/ArtiScales/SimPLUDepot/exScenar/variant0");
		File simuFile = new File("/home/mcolomb/informatique/ArtiScales/indic/bTH/teststp/variant1");
		simuFile.mkdirs();
		List<File> listFile = new ArrayList<File>();
		for (File f : batisSimulatedFile.listFiles()) {
			if (f.getName().startsWith("out-parcel_") && f.getName().endsWith(".shp")) {
				listFile.add(f);
			}
		}
		BuildingToHousingUnit bhtU = new BuildingToHousingUnit(listFile, simuFile, p);
		bhtU.distributionEstimate();
		// bhtU.simpleCityEstimate();
	}

	public static void runParticularSimpleEstimation(List<File> filebati, File simuFile, Parameters p) throws IOException {
		BuildingToHousingUnit bth = new BuildingToHousingUnit(filebati, simuFile, p);
		bth.runParticularSimpleEstimation();
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlinePartCsv() {
		return particularFirstLine;
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlineGenCsv() {
		return super.getFirstlineCsv() + genFirstLine;
	}

	public int runParticularSimpleEstimation() throws IOException {
		System.out.println("pour " + getnameScenar() + ", la simu selectionnant like " + getSelection());
		if (buildingList.size() > 0) {
			simpleEstimate();
		}

		return nbHU;
	}

	public void simpleCityEstimate() throws IOException {
		// if particular statistics hasn't been calculated yet
		File particularSimpleEstimate = new File(simuFile, "housingUnits.csv");
		if (!particularSimpleEstimate.exists()) {
			System.out.println("you should have run simpleEstimate() before");
			simpleEstimate();
		}

		// for every cities

		Hashtable<String, List<String[]>> cities = SimuTool.getCitiesFromparticularHousingUnit(particularSimpleEstimate);
		String line = "";
		for (String zipCode : cities.keySet()) {
			System.out.println("zipcode : " + zipCode);
			// different values
			int sumLgt = 0;
			int sumIndiv = 0;
			int sumDlbIndiv = 0;
			int sumSDwell = 0;
			int sumLDwell = 0;
			int sumLgtU = 0;
			int sumLgtAU = 0;
			int sumLgtOther = 0;
			DescriptiveStatistics floorAreaStat = new DescriptiveStatistics();
			DescriptiveStatistics groundAreaStat = new DescriptiveStatistics();
			DescriptiveStatistics builtDensity = new DescriptiveStatistics();

			CSVReader csvReader = new CSVReader(new FileReader(particularSimpleEstimate));

			String[] fLine = csvReader.readNext();

			for (String[] lineCsv : cities.get(zipCode)) {

				// make sure that nb of lgt's on
				int nbLgtLine = 0;
				for (int i = 0; i < fLine.length; i++) {
					if (fLine[i].equals("nombre_de_logements")) {
						nbLgtLine = Integer.valueOf(lineCsv[i]);
						sumLgt = sumLgt + nbLgtLine;
					}
				}
				// if no household in the building
				if (nbLgtLine == 0) {
					System.out.println("dog's house");
					continue;
				}
				for (int i = 0; i < fLine.length; i++) {
					String nameCol = fLine[i];

					switch (nameCol) {
					case "surface_au_sol":
						groundAreaStat.addValue(Double.valueOf(lineCsv[i]));
						break;

					case "surface_de_plancher":
						floorAreaStat.addValue(Double.valueOf(lineCsv[i]));
						break;
					case "type_du_logement":
						String type = lineCsv[i];
						if (type.equals(INDIV)) {
							sumIndiv = sumIndiv + nbLgtLine;
						} else if (type.equals(DBLINDIV)) {
							sumDlbIndiv = sumDlbIndiv + nbLgtLine;
						} else if (type.equals(SDWELLING)) {
							sumSDwell = sumSDwell + nbLgtLine;
						} else if (type.equals(LDWELLING)) {
							sumLDwell = sumLDwell + nbLgtLine;
						}
						break;

					case "zone_de_la_construction":
						String typeZone = lineCsv[i];
						if (typeZone.equals("U")) {
							sumLgtU = sumLgtU + nbLgtLine;
						} else if (typeZone.equals("AU")) {
							sumLgtAU = sumLgtAU + nbLgtLine;
						} else {
							sumLgtOther = sumLgtOther + nbLgtLine;
						}
						break;
					case "densite_batie":
						builtDensity.addValue(Double.valueOf(lineCsv[i]));
						break;
					}
				}
			}
			csvReader.close();

			System.out.println("somme_de_la_surface_au_sol_des_logements" + groundAreaStat.getSum());

			int housingUnitDiff = sumLgt - GetFromGeom.getHousingUnitsGoals(new File(rootFile, "dataGeo"), zipCode);
			line = zipCode + "," + sumLgt + "," + sumIndiv + "," + sumDlbIndiv + "," + sumSDwell + "," + sumLDwell + "," + sumLgtU + "," + sumLgtAU + "," + sumLgtOther + ","
					+ groundAreaStat.getSum() + "," + groundAreaStat.getMean() + "," + groundAreaStat.getStandardDeviation() + "," + floorAreaStat.getSum() + ","
					+ floorAreaStat.getMean() + "," + floorAreaStat.getStandardDeviation() + "," + builtDensity.getMean() + "," + builtDensity.getStandardDeviation() + ","
					+ housingUnitDiff;

		}
		toGenCSV(simuFile, "BuildingToHouseholdByCity", getFirstlineGenCsv(), line);
	}

	public void distributionEstimate() throws IOException {
		for (File bFile : buildingList) {
			if (!(bFile.exists() || bFile.getName().endsWith(".shp"))) {
				System.out.println("pas de batiments..."); // bof, c'est un peu logique mais bon..
			} else {
				ShapefileDataStore shpDSBuilding = new ShapefileDataStore(bFile.toURI().toURL());
				SimpleFeatureIterator buildingCollectionIt = shpDSBuilding.getFeatureSource().getFeatures().features();
				try {
					SimpleFeature build = buildingCollectionIt.next();

					BuildingType type = BuildingType.valueOf((String) build.getAttribute("BUILDTYPE"));
					boolean collectiveHousing = false;
					double surfaceLgt = 0;
					HashMap<String, HashMap<String, Integer>> repartition;
					// for a single house, there's only a single housing unit
					switch (type) {
					case DETACHEDHOUSE:
					case SMALLHOUSE:
						nbHU = 1;
						surfaceLgt = (double) build.getAttribute("SDPShon");
						surfaceLogements = surfaceLgt / nbHU;
						System.out.println("le batiment" + type + " de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");

						break;
					// for collective buildings
					default:
						collectiveHousing = true;
						repartition = makeCollectiveHousingRepartition(build, type);
						nbHU = repartition.get("carac").get("totHU");
					}
					numeroParcel = String.valueOf(build.getAttribute("CODE"));

					surfaceParcel = (double) build.getAttribute("SurfacePar");
					densite = nbHU / (surfaceParcel / 10000);

					System.out.println("on peux ici construire " + nbHU + " logements à une densité de " + densite);
					if (collectiveHousing) {
						String lineParticular = numeroParcel + "," + surfaceLgt + "," + build.getAttribute("SurfaceSol") + "," + String.valueOf(nbHU) + "," + type + ","
								+ build.getAttribute("TYPEZONE") + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite);

						toParticularCSV(simuFile, "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
					} else {
						String lineParticular = numeroParcel + "," + surfaceLgt + "," + build.getAttribute("SurfaceSol") + "," + String.valueOf(nbHU) + "," + type + ","
								+ build.getAttribute("TYPEZONE") + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite);

						toParticularCSV(simuFile, "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
					}
					System.out.println("");

				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					buildingCollectionIt.close();
				}
				shpDSBuilding.dispose();
			}
		}
	}

	/**
	 * Basic method to estimate the number of households that can fit into a set of cuboid known as a building The total area of ground is predefined in the class SimPLUSimulator
	 * and calculated with the object SDPCalc. This calculation estimates 3meters needed for one floor. It's the same for all the boxes; so it should be taken only once.
	 * 
	 * @param f
	 *            : direction to the shapefile of the building
	 * @return : number of households
	 * @throws IOException
	 */
	public void simpleEstimate() throws IOException {
		for (File bFile : buildingList) {
			if (bFile.exists() && bFile.getName().endsWith(".shp")) {
				ShapefileDataStore shpDSBuilding = new ShapefileDataStore(bFile.toURI().toURL());
				SimpleFeatureIterator buildingCollectionIt = shpDSBuilding.getFeatureSource().getFeatures().features();

				try {
					SimpleFeature build = buildingCollectionIt.next();
					double surfaceLgt = (double) build.getAttribute("SDPShon");
					numeroParcel = String.valueOf(build.getAttribute("CODE"));
					System.out.println("le batiment de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");
					nbHU = (int) Math.round((surfaceLgt / surfaceLogDefault));

					surfaceLogements = surfaceLgt / nbHU;

					surfaceParcel = (double) build.getAttribute("SurfacePar");
					densite = nbHU / (surfaceParcel / 10000);

					System.out.println("on peux ici construire " + nbHU + " logements à une densité de " + densite);

					String lineParticular = numeroParcel + "," + surfaceLgt + "," + build.getAttribute("SurfaceSol") + "," + String.valueOf(nbHU) + "," + setBuildingType(nbHU)
							+ "," + build.getAttribute("TYPEZONE") + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite);

					toParticularCSV(simuFile, "housingUnits.csv", getFirstlinePartCsv(), lineParticular);

					System.out.println("");
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					buildingCollectionIt.close();
				}
				shpDSBuilding.dispose();
			}
		}
	}

	/**
	 * 
	 * @param bati
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, HashMap<String, Integer>> makeCollectiveHousingRepartition(SimpleFeature bati, BuildingType type) throws Exception {

		Parameters pType = RepartitionBuildingType.getParam(new File(this.getClass().getClassLoader().getResource("profileBuildingType").getFile()), type);

		int minLgt = pType.getInteger("minHousingUnit");

		// each key means a different repartition of housing unit type per building
		HashMap<String, HashMap<String, Integer>> result = new HashMap<String, HashMap<String, Integer>>();

		Double sizeSmallDwellingMin = Double.valueOf(pType.getString("sizeSmallDwelling").split("-")[0]);
		Double sizeSmallDwellingMax = Double.valueOf(pType.getString("sizeSmallDwelling").split("-")[1]);
		Double sizeMidDwellingMin = Double.valueOf(pType.getString("sizeMidDwelling").split("-")[0]);
		Double sizeMidDwellingMax = Double.valueOf(pType.getString("sizeMidDwelling").split("-")[1]);
		Double sizeLargeDwellingMin = Double.valueOf(pType.getString("sizeLargeDwelling").split("-")[0]);
		Double sizeLargeDwellingMax = Double.valueOf(pType.getString("sizeLargeDwelling").split("-")[1]);

		Double freqSmallDwelling = pType.getDouble("freqSmallDwelling");
		Double freqMidDwelling = pType.getDouble("freqMidDwelling");
		Double freqLargeDwelling = pType.getDouble("freqLargeDwelling");

		if (!((freqSmallDwelling + freqMidDwelling + freqLargeDwelling) == 1)) {
			System.out.println("problem in the sum of housing unit frequencies");
		}

		double totSDP = (double) bati.getAttribute("SDPShon");
		// percentage of the building that will be for common spaces (not for multifamily houses)
		if (type != BuildingType.MULTIFAMILYHOUSE) {
			totSDP = 0.9 * totSDP;
		}
		boolean doRepart = true;

		while (doRepart) {

			int nbLgtFinal = 0;

			HashMap<String, Integer> smallHU = new HashMap<String, Integer>();
			HashMap<String, Integer> midHU = new HashMap<String, Integer>();
			HashMap<String, Integer> largeHU = new HashMap<String, Integer>();
			double leftSDP = totSDP;
			String typeHU = "";

			// integer randomness to chose the range
			Random rand = new Random();

			boolean enoughSpace = true;
			while (enoughSpace) {
				// ponderated randomness
				double rd = Math.random();
				if (rd < freqSmallDwelling) {
					// this is a small house
					System.out.println("small dwelling");
					//// look at the left space
					// not enough room
					if (leftSDP - sizeSmallDwellingMin < 0) {
						enoughSpace = false;
					}
					// this is a minimum construction type of situation
					else if (leftSDP - sizeSmallDwellingMax < 0 || leftSDP - sizeSmallDwellingMin >= 0) {
						// Housing Unit is at the minimum size
						Double sdp = sizeSmallDwellingMin;

						nbLgtFinal++;

						// put in collec
						if (smallHU.containsKey(String.valueOf(sdp))) {
							smallHU.put(String.valueOf(sdp), smallHU.get(String.valueOf(sdp)) + 1);
						} else {
							smallHU.put(String.valueOf(sdp), 1);
						}
						enoughSpace = false;
					}
					// nothing to declare
					else {
						// we chose a random range
						int range = rand.nextInt(((int) (sizeSmallDwellingMax - sizeSmallDwellingMin) / 5) + 1);
						Double sdp = (double) (range * 5) + sizeSmallDwellingMin;

						// suppress the aera
						leftSDP = leftSDP - sdp;
						nbLgtFinal++;

						// put in collec
						if (smallHU.containsKey(String.valueOf(sdp))) {
							smallHU.put(String.valueOf(sdp), smallHU.get(String.valueOf(sdp)) + 1);
						} else {
							smallHU.put(String.valueOf(sdp), 1);
						}
						System.out.println("new HU of  " + sdp + "m2");
						System.out.println("sdp left : " + leftSDP);
					}
				} else if (rd < (freqSmallDwelling + freqMidDwelling)) {
					// this is a medium house
					System.out.println("mid dwelling");
					//// look at the left space
					// not enough room
					if (leftSDP - sizeMidDwellingMin < 0) {

						enoughSpace = false;
					}
					// this is a minimum construction type of situation
					else if (leftSDP - sizeMidDwellingMax < 0 || leftSDP - sizeMidDwellingMin >= 0) {
						// Housing Unit is at the minimum size
						Double sdp = sizeMidDwellingMin;

						nbLgtFinal++;

						// put in collec
						if (midHU.containsKey(String.valueOf(sdp))) {
							midHU.put(String.valueOf(sdp), midHU.get(String.valueOf(sdp)) + 1);
						} else {
							midHU.put(String.valueOf(sdp), 1);
						}
						enoughSpace = false;
					}
					// nothing to declare
					else {
						int range = rand.nextInt(((int) (sizeMidDwellingMax - sizeMidDwellingMin) / 5) + 1);
						Double sdp = (double) (range * 5) + sizeMidDwellingMax;

						// suppress the aera
						leftSDP = leftSDP - sdp;
						nbLgtFinal++;

						// put in collec
						if (midHU.containsKey(String.valueOf(sdp))) {
							midHU.put(String.valueOf(sdp), midHU.get(String.valueOf(sdp)) + 1);
						} else {
							midHU.put(String.valueOf(sdp), 1);
						}
						System.out.println("new HU of  " + sdp + "m2");
						System.out.println("sdp left : " + leftSDP);
					}
				} else {
					// this is a large house
					System.out.println("large dwelling");

					//// look at the left space
					// not enough room
					if (leftSDP - sizeLargeDwellingMin < 0) {

						enoughSpace = false;
					}
					// this is a minimum construction type of situation
					else if (leftSDP - sizeLargeDwellingMax < 0 || (leftSDP - sizeLargeDwellingMin) >= 0) {
						// Housing Unit is at the minimum size
						Double sdp = sizeLargeDwellingMin;

						nbLgtFinal++;

						// put in collec
						if (largeHU.containsKey(String.valueOf(sdp))) {
							largeHU.put(String.valueOf(sdp), largeHU.get(String.valueOf(sdp)) + 1);
						} else {
							largeHU.put(String.valueOf(sdp), 1);
						}
						enoughSpace = false;
					}
					// nothing to declare
					else {
						int range = rand.nextInt(((int) (sizeLargeDwellingMax - sizeLargeDwellingMin) / 5) + 1);
						Double sdp = (double) (range * 5) + sizeLargeDwellingMax;

						// suppress the aera
						leftSDP = leftSDP - sdp;
						nbLgtFinal++;

						// put in collec
						if (largeHU.containsKey(String.valueOf(sdp))) {
							largeHU.put(String.valueOf(sdp), largeHU.get(String.valueOf(sdp)) + 1);
						} else {
							largeHU.put(String.valueOf(sdp), 1);
						}
						System.out.println("new HU of  " + sdp + "m2");
						System.out.println("sdp left : " + leftSDP);
					}
				}

			}
			// if the limit of minimum housing units is outpassed
			System.out.println("minLgt"+minLgt+" contre "+nbLgtFinal);
			if (nbLgtFinal >= minLgt) {
				System.out.println("it's enough");
				doRepart = false;
				result.put("smallHU", smallHU);
				result.put("midHU", midHU);
				result.put("largeHU", largeHU);
				HashMap<String, Integer> carac = new HashMap<String, Integer>();
				carac.put("totHU", nbLgtFinal);
				result.put("carac", carac);
			} else {
				System.out.println("it's not enough");
			}
		}
		return result;
	}

	public String setBuildingType(int nbLgt) {
		if (nbLgt == 0) {
			return NUTIN;
		} else if (nbLgt == 1) {
			return INDIV;
		} else if (nbLgt == 2) {
			return DBLINDIV;
		} else if (nbLgt < 7) {
			return SDWELLING;
		}
		return LDWELLING;
	}

}
