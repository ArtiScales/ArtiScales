package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.SimPLUSimulator;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.parameters.Parameters;

public class BuildingToHousingUnit extends Indicators {

	// infos from the Artiscales simulations

	List<File> buildingList;
	double surfaceLogDefault;

	// infos about the buildings
	String numeroParcel;
	double surfaceParcel;
	int nbLogements;
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

	public BuildingToHousingUnit(List<File> buildingList, Parameters par) {
		super(par);
		this.buildingList = buildingList;
		simuFile = buildingList.get(0).getParentFile().getParentFile();
		zipCode = simuFile.getParentFile().getName();
		rootFile = new File(p.getString("rootFile"));
		surfaceLogDefault = p.getInteger("HousingUnitSize");
		particularFirstLine = "numero_parcelle,surface_de_plancher," + "surface_au_sol," + "nombre_de_logements," + "type_du_logement," + "zone_de_la_construction,"
				+ "moyenne_de_la_surface_plancher_par_logements," + "densite_batie";

		genFirstLine = "nombre_de_logements," + "nombre_de_logements_individuels," + "nombre_de_logements_doubles," + "nombre_de_logements_en_petit_collectif,"
				+ "nombre_de_logements_en_grand_collectif," + "nombre_de_batiments_construits_dans_les_zones_U," + "nombre_de_batiment_construit_dans_les_zones_AU,"
				+ "nombre_de_batiments_construit_dans_les_zones_N_et_A," + "somme_de_la_surface_au_sol_des_logements," + "moyenne_de_surface_au_sol_des_bâtiments,"
				+ "ecart_type_de_la_surface_au_sol_des_bâtiments," + "somme_de_la_surface_plancher_des_logements," + "moyenne_de_la_surface_plancher_des_bâtiments,"
				+ "ecart_type_de_la_surface_plancher_des_bâtiments," + "moyenne_de_la_densité_batie," + "ecart_type_de_la_densité_batie,"
				+ "différence_avec_les_objectifs_régionaux";

	}

	public static void main(String[] args) throws Exception {
		List<File> listParameters = new ArrayList<>();

		// Folder root with parameters
		String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenar0/").getPath();

		listParameters.add(new File(rootParam + "parametreTechnique.xml"));
		listParameters.add(new File(rootParam + "parametreScenario.xml"));
		Parameters p = Parameters.unmarshall(listParameters);
		File batisSimulatedFile = new File(
				"/home/mcolomb/informatique/ArtiScales/output/Stability-dataAutomPhy-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_9015629222324914404-evalAnal-20.0/25265/ZoningAllowed/simu/");
		List<File> listFile = new ArrayList<File>();
		for (File f : batisSimulatedFile.listFiles()) {
			if (f.getName().startsWith("out-parcelle_") && f.getName().endsWith(".shp")) {
				listFile.add(f);
			}
		}
		BuildingToHousingUnit bhtU = new BuildingToHousingUnit(listFile, p);
		bhtU.runParticularSimpleEstimation();
		bhtU.simpleCityEstimate();
	}

	public static void runParticularSimpleEstimation(List<File> filebati, Parameters p) throws IOException {
		BuildingToHousingUnit bth = new BuildingToHousingUnit(filebati, p);
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
		System.out.println("pour " + getnameScenar() + ", la simu selectionnant like " + getSelection() + " avec le code zip " + zipCode);

		simpleEstimate();

		return nbLogements;
	}

	public void simpleCityEstimate() throws IOException {
		// if particular statistics hasn't been calculated yet
		File particularSimpleEstimate = new File(simuFile, "housingUnits.csv");
		if (!particularSimpleEstimate.exists()) {
			System.out.println("you should have run simpleEstimate() before");
			simpleEstimate();
		}

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

		for (String[] lineCsv : csvReader.readAll()) {
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
		int housingUnitDiff = sumLgt - GetFromGeom.getHousingUnitsGoals(new File(p.getString("geoFile")), zipCode);
		String line = sumLgt + "," + sumIndiv + "," + sumDlbIndiv + "," + sumSDwell + "," + sumLDwell + "," + sumLgtU + "," + sumLgtAU + "," + sumLgtOther + ","
				+ groundAreaStat.getSum() + "," + groundAreaStat.getMean() + "," + groundAreaStat.getStandardDeviation() + "," + floorAreaStat.getSum() + ","
				+ floorAreaStat.getMean() + "," + floorAreaStat.getStandardDeviation() + "," + builtDensity.getMean() + "," + builtDensity.getStandardDeviation() + ","
				+ housingUnitDiff;

		toGenCSV("BuildingToHousehold", getFirstlineGenCsv(), line);

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
			if (!(bFile.exists() || bFile.getName().endsWith(".shp"))) {
				System.out.println("pas de batiments..."); // bof, c'est un peu logique mais bon..
			} else {
				ShapefileDataStore shpDSBuilding = new ShapefileDataStore(bFile.toURI().toURL());
				SimpleFeatureIterator buildingCollectionIt = shpDSBuilding.getFeatureSource().getFeatures().features();

				try {
					SimpleFeature build = buildingCollectionIt.next();
					double surfaceLgt = (double) build.getAttribute("SDPShon");
					numeroParcel = String.valueOf(build.getAttribute("CODE"));
					System.out.println("le batiment de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");
					nbLogements = (int) Math.round((surfaceLgt / surfaceLogDefault));

					surfaceLogements = surfaceLgt / nbLogements;

					surfaceParcel = (double) build.getAttribute("SurfacePar");
					densite = nbLogements / (surfaceParcel / 10000);

					System.out.println("on peux ici construire " + nbLogements + " logements à une densité de " + densite);

					String lineParticular = numeroParcel + "," + surfaceLgt + "," + build.getAttribute("SurfaceSol") + "," + String.valueOf(nbLogements) + ","
							+ setBuildingType(nbLogements) + "," + build.getAttribute("TYPEZONE") + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite);

					toParticularCSV(simuFile, "housingUnits.csv", getFirstlinePartCsv(), lineParticular);

					// aggregate at the city's level -- find a way to make the aggregation

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
