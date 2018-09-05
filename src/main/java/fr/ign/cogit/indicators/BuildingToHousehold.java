package fr.ign.cogit.indicators;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.util.VectorFct;
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
	String particularFirstLine ;
	
	public BuildingToHousehold(File filebati, Parameters par) {
		fileBati = filebati;
		p = par;
		simuFile = filebati.getParentFile().getParentFile(); 
		zipCode = simuFile.getParentFile().getName();
		rootFile = new File(p.getString("rootFile"));
		surfaceLogDefault = p.getInteger("HousingUnitSize");
		particularFirstLine = "Numero Parcelle,Nombre de logements,moyenne de surface par logements, densite batie";
	}

	public static void main(String[] args) throws Exception {
		String paramFile = "/home/yo/workspace/ArtiScales/src/main/resources/paramSet/param0.xml";
		Parameters par = Parameters.unmarshall(new File(paramFile));

		BuildingToHousehold bhtU = new BuildingToHousehold(new File(
				"/home/yo/Documents/these/ArtiScales/output/Stability-dataAutomPhy-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_9015629222324914404-evalAnal-20.0/25495/ZoningAllowed/simu0"),
				par);
		bhtU.run();
	}

	public static void run(File filebati, Parameters p) throws IOException {
		BuildingToHousehold bth = new BuildingToHousehold(filebati, p);
		bth.run();
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlineCsv() {
		return super.getFirstlineCsv()
				+ particularFirstLine ;
	}

	public int run() throws IOException {
		System.out.println("pour " + getnameScenar() + ", la simu selectionnant like " + getSelection()
				+ " avec le code zip " + zipCode);
		// si on pointe vers un directory contenant tous les batiments
		if (fileBati.isDirectory()) {
			for (File singleBatiFile : fileBati.listFiles()) {
				if (singleBatiFile.getName().endsWith(".shp") && singleBatiFile.getName().startsWith("out")) {
					simpleEstimate(singleBatiFile);
				}
			}
		}
		// Si l'on pointe directement vers le shp
		else {
			simpleEstimate(fileBati);
		}
		toGenCSV(numeroParcel + "," +String.valueOf(nbLogements) + "," + String.valueOf(surfaceLogements) + "," + String.valueOf(densite),getFirstlineCsv());
		System.out.println("");
		
		return nbLogements;
	}

	/**
	 * Basic method to estimate the number of households that can fit into a set of
	 * cuboid known as a building The total area of ground is predefined in the
	 * class SimPLUSimulator and calculated with the object SDPCalc. This
	 * calculation estimates 3meters needed for one floor. It's the same for all the
	 * boxes; so it should be taken only once.
	 * 
	 * @param f : direction to the shapefile of the building
	 * @return : number of households
	 * @throws IOException
	 */
	public void simpleEstimate(File f) throws IOException {

		if (!f.exists()) {
			System.out.println("pas de batiments..."); // bof, c'est un peu logique mais bon..
		} else {
			ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureIterator buildingCollectionIt = shpDSBuilding.getFeatureSource().getFeatures().features();
			int i = 0;
			try {
				while (i < 1) {
					SimpleFeature build = buildingCollectionIt.next();
					double surfaceLgt = (double) build.getAttribute("SDPShon");
					numeroParcel = String.valueOf(build.getAttribute("num"));
					System.out.println("le batiment de la parcelle " + numeroParcel + " fait " + surfaceLgt
							+ " mcarré ");
					nbLogements = (int) Math.round((surfaceLgt / surfaceLogDefault));

					surfaceLogements = surfaceLgt / nbLogements;

					surfaceParcel = (double) build.getAttribute("areaParcel");
					densite = nbLogements / (surfaceParcel / 10000);

					System.out.println(
							"on peux ici construire " + nbLogements + " logements à une densité de " + densite);

					toCSV(f.getParentFile(), "housingUnits.csv", particularFirstLine,
							numeroParcel + "," + String.valueOf(nbLogements) + "," + String.valueOf(surfaceLogements)
									+ "," + String.valueOf(densite));
					i = 1;
				}

			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				buildingCollectionIt.close();
			}
			shpDSBuilding.dispose();
		}
	}
}
