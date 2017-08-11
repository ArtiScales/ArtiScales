package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.opengis.feature.simple.SimpleFeature;

public class BuildingsToHousehold {
	List<File> listFile;
	
	public BuildingsToHousehold(List<File> listfile){
		listFile = listfile;
	}
	
	public void run() throws IOException {

		double surface;
		double hauteur;
		for (File f : listFile){
			ShapefileDataStore shpDSBuilding = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureCollection buildingCollection = shpDSBuilding.getFeatureSource().getFeatures();
			System.out.println("taille de l'échantillon"+buildingCollection.size());
			for ( Object feature : buildingCollection.toArray()){
				SimpleFeature feat = (SimpleFeature) feature;
				surface = (double) feat.getAttribute("Surface");
				hauteur = (double) feat.getAttribute("Hauteur");
				System.out.println("le batiment fait "+surface + "mcarré et "+ hauteur + "m de haut");
			}
		}
	}

	public static ArrayList<File> getBuildings(File BuildingFolder) {
		ArrayList<File> buildings = new ArrayList<File>();
		for (File buildingFile : BuildingFolder.listFiles()) {
			if (buildingFile.toString().endsWith(".shp")) {
				buildings.add(buildingFile);
			}
		}
		return buildings;
	}
	public static double getArea (File building) throws IOException{
		ShapefileDataStore shpBuilding = new ShapefileDataStore(building.toURI().toURL());
		SimpleFeatureCollection buildingSFC = shpBuilding.getFeatureSource().getFeatures();
		double area = 0;
		for ( Object cube : buildingSFC.toArray()){
			SimpleFeatureImpl eachCube = (SimpleFeatureImpl) cube;
		}

		return area;
	}
	
}
