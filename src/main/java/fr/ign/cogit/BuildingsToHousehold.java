package fr.ign.cogit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

public class BuildingsToHousehold {
	List<File> listFile;
	double surfaceLog;
	String mUPSimu;
	String zipCode;
	String selection;
	String simPLUSimu;
	File rootFile;
	boolean firstLine = true;

	public BuildingsToHousehold(List<File> listfile, double surfacelog) {
		listFile = listfile;
		surfaceLog = surfacelog;
	}

	public static void main(String[] args) throws Exception {
		List<File> listBat = new ArrayList<File>();
		File fi = new File(
				"/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N3_Ba_Moy_ahpx_seed42-analyse-20.0/25245/multipleParcels--notBuilt/simu0");
		for (File f : fi.listFiles()) {
			if (f.toString().endsWith("shp")) {
				listBat.add(f);
			}
		}
		run(listBat, 100);
	}

	public static int run(List<File> listfile, double surfacelog) throws IOException {
		BuildingsToHousehold bth = new BuildingsToHousehold(listfile, surfacelog);
		return bth.run();
	}

	public void run() throws IOException {
		putSimuNames();
		System.out.println();
		System.out.println("la simu est " + simPLUSimu + " ou zip de " + zipCode + " ou supsimu de " + mUPSimu);
		for (File f : listFile){
		int lgt = simpleEstimate(f);
		toGenCSV(lgt);
		}

	}

	private void putSimuNames() {
		File fileRef = listFile.get(0);
		simPLUSimu = fileRef.getParentFile().getName();
		selection = fileRef.getParentFile().getParentFile().getName();
		zipCode = fileRef.getParentFile().getParentFile().getParentFile().getName();
		mUPSimu = fileRef.getParentFile().getParentFile().getParentFile().getParentFile().getName();
		rootFile = fileRef.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
		System.out.println(rootFile);
	}

	public int simpleEstimate(File f) throws IOException {
		double surface = 0;
		double hauteur = 0;
		long etage;
		int logements = 0;
		// pas encore de prise en compte de batiments s'intersectant

			ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureCollection buildingCollection = shpDSBuilding.getFeatureSource().getFeatures();
			for (Object feature : buildingCollection.toArray()) {
				SimpleFeature feat = (SimpleFeature) feature;
				surface = (double) feat.getAttribute("Surface");
				hauteur = (double) feat.getAttribute("Hauteur");
				System.out.println("le batiment de la parcelle "
						+ f.toString().substring(f.toString().length() - 6, f.toString().length() - 4) + " fait "
						+ surface + " mcarré et " + hauteur + "m de haut");
				etage = Math.round((hauteur / 2.5));
				int logement = (int) Math.round((surface * etage) / surfaceLog);
				System.out.println("on peux ici construire " + logement + " logements de " + etage + " étages");
				logements = logements + logement;
				toCSV(surface, hauteur, etage, logement);
			}

		
		System.out.println("on peux ici construire " + logements + " logements");
		return logements;
	}

	public void toGenCSV(int nbLgt) throws IOException {
		File fileName = new File(rootFile, "results.csv");

		FileWriter writer = new FileWriter(fileName, true);

		writer.append(mUPSimu + "," + zipCode + "," + selection + "," + simPLUSimu + "," + nbLgt);
		writer.append("\n");
		writer.close();
	}

	public void toCSV(double surfaceBat, double hauteur, long nbStairs, int nbLgt) throws IOException {
		File fileName = new File(listFile.get(0).getParentFile(), "households.csv");
		FileWriter writer = new FileWriter(fileName, true);
		if (firstLine == true) {
			writer.append("building surface, building height, number of stairs, number of households \n");
			firstLine = false;
		}
		writer.append(surfaceBat + "," + hauteur + "," + nbStairs + "," + nbLgt);
		writer.append("\n");
		writer.close();
	}

}
