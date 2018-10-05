package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.annexeTools.fakeWorld.geo.FakeZone;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;

/**
 * The aim of this code is to generate a FakeWorld that can be used to test the
 * different rules for the simulation at a local scale
 * 
 * @author mbrasebin
 *
 */
public class FakeWorldGenerator {

	 static FileWriter in; 
	 
	public static void main(String[] args) throws IOException {
		//Folder where to generate the fake data
		String folderOut = "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScalesTest/Donnees/";
		//Default line for regulation (to parse the list and to export the regulation)
		String fLine = "libelle_zone,insee,libelle_de_base,libelle_de_dul,fonctions,oap,zonage_coherent,correction_zonage,art_3,art_4,art_5,art_6,art_6_opt,art_6_optD,art_71,art_72,art_73,art_74,art_8,art_9,art_10_top,art_101,art_102,art_12,art_13,art_14";
		
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_8(5);
		
		
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault2.setLibelle_de_dul("U2");
		regulationDefault3.setArt_71(5);
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		
		generateFakeData(regulations, folderOut);
		
		


	}
	
	
	public static void generateFakeData(List<ArtiScalesRegulation> regulationList, String folderOut) throws IOException {
		
		 
		 IFeatureCollection<IFeature> parcels = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> buildings = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> zones = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> roads = new FT_FeatureCollection<>();
		 
		 int count = 0;
		 
		for(ArtiScalesRegulation regulation: regulationList) {
			//We create a zone with the right name
			FakeZone fZ = new FakeZone((++count),  regulation.getLibelle_de_dul());
			
			//We export the regulation
		    in.write(regulation.toCSVLine()+  "\n");
		    
		    //Adding objects to the final list
		    parcels.addAll(fZ.getParcels());
		    zones.addAll(fZ.getUrbaZones());
		    buildings.addAll(fZ.getBuildings());
		    roads.addAll(fZ.getRoads());
		}
		

   
        
        in.close();
        
        ShapefileWriter.write(parcels, folderOut + "parcelle.shp"); 
        ShapefileWriter.write(buildings, folderOut + "batiment.shp");    
        ShapefileWriter.write(zones, folderOut + "zoneUrba.shp");   
        ShapefileWriter.write(roads, folderOut + "route.shp");  
		
		
	}

}
