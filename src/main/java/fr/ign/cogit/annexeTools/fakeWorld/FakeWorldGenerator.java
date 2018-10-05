package fr.ign.cogit.annexeTools.fakeWorld;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import fr.ign.cogit.annexeTools.fakeWorld.geo.FakeZone;
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

	public static void main(String[] args) throws IOException {

		String folderOut = "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScalesTest/Donnees/";
		
		String fLine = "libelle_zone,insee,libelle_de_base,libelle_de_dul,fonctions,oap,zonage_coherent,correction_zonage,art_3,art_4,art_5,art_6,art_6_opt,art_6_optD,art_71,art_72,art_73,art_74,art_8,art_9,art_10_top,art_101,art_102,art_12,art_13,art_14";
		String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,0,10";
		
		ArtiScalesRegulation regulation = new ArtiScalesRegulation(fLine, defaultRegulation);

		FakeZone fZ = new FakeZone(0, "U0");
		

        File outputFile = new File(folderOut+"predicate.csv"); 
        
        
        FileWriter in = new FileWriter(outputFile); 
		
        
        in.write(fLine +  "\n");
        in.write(regulation.toCSVLine()+  "\n");
        
        in.close();
        
        ShapefileWriter.write(fZ.getParcels(), folderOut + "parcelle.shp"); 
        ShapefileWriter.write(fZ.getBuildings(), folderOut + "batiment.shp");    
        ShapefileWriter.write(fZ.getUrbaZones(), folderOut + "zoneUrba.shp");   
        ShapefileWriter.write(fZ.getRoads(), folderOut + "route.shp");  
	}

}
