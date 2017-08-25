package fr.ign.cogit;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;

public class Test {

	public static void main(String[] args){
		IFeatureCollection<IFeature> featC = ShapefileReader.read("/home/mcolomb/tmp/splited.shp");
		
		for(IFeature feat: featC){
			System.out.println(feat.getGeom().toString());
		}
		
	}
}
