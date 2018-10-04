package fr.ign.cogit.indicators;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.SimPLUSimulator;
import fr.ign.parameters.Parameters;

public class BuildingToHousehold extends Indicators {
	// infos from the Artiscales simulations
	File fileBati;
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

	public BuildingToHousehold(File filebati, Parameters par) {
		super(par);
		fileBati = filebati;
		simuFile = filebati.getParentFile().getParentFile();
		zipCode = simuFile.getParentFile().getName();
		rootFile = new File(p.getString("rootFile"));
		surfaceLogDefault = p.getInteger("HousingUnitSize");
		particularFirstLine = "numero parcelle,surface de plancher,surface au sol, nombre de logements,moyenne de surface par logements, densite batie";
		//new first line to fill (completer avec le doc de cécile
		//genFirstLine = "nombre de logements,nombre de mainsons individuelles, nombre de maisons jumelés, nombre de logements collectifs,nombre de batiments construits dans les zones U, nombre de batiment construit dans les zones AU, nombre de batiments construit dans les zones N et A ,somme de la surface au sol des logements, moyenne de surface au sol des batiments, densite batie";
		genFirstLine = "surface de plancher,surface au sol, nombre de logements,moyenne de surface par logements, densite batie";
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

		for (File f : batisSimulatedFile.listFiles()) {
			if (f.getName().startsWith("out-parcelle_") && f.getName().endsWith(".shp")) {
				BuildingToHousehold bhtU = new BuildingToHousehold(f, p);
				bhtU.run();

			}
		}
	}

	public static void run(File filebati, Parameters p) throws IOException {
		BuildingToHousehold bth = new BuildingToHousehold(filebati, p);
		bth.run();
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlinePartCsv() {
		return super.getFirstlineCsv() + particularFirstLine;
	}
	
	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlineGenCsv() {
		return super.getFirstlineCsv() + particularFirstLine;
	}

	public int run() throws IOException {
		System.out.println("pour " + getnameScenar() + ", la simu selectionnant like " + getSelection() + " avec le code zip " + zipCode);

		simpleEstimate(fileBati);

		return nbLogements;
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
	public void simpleEstimate(File f) throws IOException {

		if (!f.exists()) {
			System.out.println("pas de batiments..."); // bof, c'est un peu logique mais bon..
		} else {
			ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureIterator buildingCollectionIt = shpDSBuilding.getFeatureSource().getFeatures().features();

			try {
				SimpleFeature build = buildingCollectionIt.next();
				double surfaceLgt = (double) build.getAttribute("SDPShon");
				numeroParcel = String.valueOf(build.getAttribute("NUMEROPARC"));
				System.out.println("le batiment de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");
				nbLogements = (int) Math.round((surfaceLgt / surfaceLogDefault));

				surfaceLogements = surfaceLgt / nbLogements;

				surfaceParcel = (double) build.getAttribute("SurfacePar");
				densite = nbLogements / (surfaceParcel / 10000);

				System.out.println("on peux ici construire " + nbLogements + " logements à une densité de " + densite);
				
				String lineParticular =numeroParcel + ","+surfaceLgt+","+build.getAttribute("SurfaceSol") +","+ String.valueOf(nbLogements) + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite);

				
				toParticularCSV(f.getParentFile(), "housingUnits.csv", getFirstlinePartCsv(),lineParticular);
						
				// aggregate at the city's level -- find a way to make the aggregation
				toGenCSV("BuildingToHousehold",getFirstlineGenCsv(),String.valueOf(nbLogements) + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite));
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
