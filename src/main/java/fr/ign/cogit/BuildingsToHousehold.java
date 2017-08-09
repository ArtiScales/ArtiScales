package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureImpl;
import org.opengis.feature.simple.SimpleFeature;

public class BuildingsToHousehold {

	public static void run(File BuildingFolder, int sqMperBuilding) {

		getBuildings(BuildingFolder);
		
		
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
