package fr.ign.cogit.Indicators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

public class BuildingToHousehold extends indicators {
	List<File> listSelection;
	double surfaceLog;

	public BuildingToHousehold(List<File> listselection, double surfacelog) {
		listSelection = listselection;
		surfaceLog = surfacelog;
	}

	public static void main(String[] args) throws Exception {
		List<File> listSimu = new ArrayList<File>();
		listSimu.add(new File("/home/mcolomb/donnee/couplage/output/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0/25245/notBuilt/simu0"));
		run(listSimu, 100);
	}

	public static void run(List<File> listfile, double surfacelog) throws IOException {
		BuildingToHousehold bth = new BuildingToHousehold(listfile, surfacelog);
		bth.run();
	}

	public void run() throws IOException {
	
		for (File f : listSelection) {
			putSimuNames(f);
			System.out.println(f);
			System.out.println("la simu est " + simPLUSimu + " avec le code zip " + zipCode + " frome the Mupsimu: " + mUPSimu);
			int totLgt =0;
			firstLine=true;
			for (File batiFile : f.listFiles()){
				if (batiFile.toString().endsWith(".shp")){
				System.out.println("bati file : "+ batiFile);
				int lgt = simpleEstimate(batiFile);
				totLgt = totLgt + lgt;
			}
			}
			toGenCSV(getInfoSimuCsv() + totLgt);
		}

	}

	public int simpleEstimate(File f) throws IOException {
		double surface = 0;
		double hauteur = 0;
		long etage;
		int logements = 0;
		Double totParcelArea = 0.0;
		// pas encore de prise en compte de batiments s'intersectant

		ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
		SimpleFeatureCollection buildingCollection = shpDSBuilding.getFeatureSource().getFeatures();
		for (Object feature : buildingCollection.toArray()) {
			SimpleFeature feat = (SimpleFeature) feature;
			surface = (double) feat.getAttribute("Surface");
			hauteur = (double) feat.getAttribute("Hauteur");
			System.out.println("le batiment de la parcelle "
					+ f.toString().substring(f.toString().length() - 6, f.toString().length() - 4) + " fait " + surface
					+ " mcarré et " + hauteur + "m de haut");
			etage = Math.round((hauteur / 2.5));
			int logement = (int) Math.round((surface * etage) / surfaceLog);
			System.out.println("on peux ici construire " + logement + " logements de " + etage + " étages");
			logements = logements + logement;
			double areaParcel = (double) feat.getAttribute("areaParcel");
			totParcelArea = totParcelArea + areaParcel ;
			
			String firstline = new String("building surface, building height, number of stairs, number of households, housing units per hectare \n");
			toCSV(f.getParentFile(), "housingUnits.csv",firstline, surface + "," + hauteur + "," + etage + "," + logement+"," +(logement/(areaParcel/100000)));
			
		}
		System.out.println("construction totale de " + logements + " logements pour une densité de "+(logements/(totParcelArea/100000)) );
		return logements;
	}
}
