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
	 
	//Default line for regulation (to parse the list and to export the regulation)
	 public final static  String fLine = "libelle_zone,insee,libelle_de_base,libelle_de_dul,fonctions,oap,zonage_coherent,correction_zonage,art_3,art_4,art_5,art_6,art_6_opt,art_6_optD,art_71,art_72,art_73,art_74,art_8,art_9,art_10_top,art_101,art_12,art_13,art_14";

	 
	public static void main(String[] args) throws IOException {
		//Folder where to generate the fake data
		String folderOut = "/home/mcolomb/tmp/fakeworld/";
		(new File(folderOut)).mkdirs();
		generateTestForArticle5(folderOut+"art5/");
		generateTestForArticle6(folderOut+"art6/");
		generateTestForArticle71(folderOut+"art71/");
		generateTestForArticle72(folderOut+"art72/");
		generateTestForArticle73(folderOut+"art73/");
		generateTestForArticle74(folderOut+"art74/");
		generateTestForArticle8(folderOut+"art8/");
		generateTestForArticle9(folderOut+"art9/");
		generateTestForArticle13(folderOut+"art10/");	
		generateTestForArticle13(folderOut+"art13/");	
	}
	
	
	public static void generateTestForArticle5(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_5(25000);
		
		
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_5(99);
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	public static void generateTestForArticle10(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		
		//CAS ARTICLE 10 = 1 && ARTICLE_10_1 = 5
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_10(1);
		regulationDefault2.setArt_10_top(5);	
		
		
		//CAS ARTICLE 10 = 2 && ARTICLE_10_2 = 12
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_10(2);
		regulationDefault3.setArt_10_top(12);	
		
		//CAS ARTICLE 10 = 6 && ARTICLE_10_2 = 12
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_10(6);
		regulationDefault4.setArt_10_top(12);	
		
		//CAS ARTICLE 10 = 7 && ARTICLE_10_2 = 12
		ArtiScalesRegulation regulationDefault5 = regulationDefault.clone();
		regulationDefault5.setLibelle_de_dul("U3");
		regulationDefault5.setArt_10(7);
		regulationDefault5.setArt_10_top(12);	
		
		//CAS ARTICLE 10 = 8 && ARTICLE_10_2 = 12
		ArtiScalesRegulation regulationDefault6 = regulationDefault.clone();
		regulationDefault6.setLibelle_de_dul("U4");
		regulationDefault6.setArt_10(8);
		regulationDefault6.setArt_10_top(12);	
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		regulations.add(regulationDefault5);
		regulations.add(regulationDefault6);
		
		generateFakeData(regulations, folderOut);
	}
	
	
	public static void generateTestForArticle6(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		
		//CAS ARTICLE 6 = 0
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_6(0);
		
		//CAS ARTICLE 6 = 44
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_6(44);
		
		//CAS ARTICLE 6 = 55
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_6(55);
		
		//CAS ARTICLE 6 = n (ici 6)
		ArtiScalesRegulation regulationDefault5 = regulationDefault.clone();
		regulationDefault5.setLibelle_de_dul("U3");
		regulationDefault5.setArt_6(6);
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		regulations.add(regulationDefault5);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	public static void generateTestForArticle71(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 71 = 1
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_71(1);
		
		//CAS ARTICLE 71 = 2
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_71(2);
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	
	public static void generateTestForArticle72(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 72 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_72(5);
		
		//CAS ARTICLE 72 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_72(10);
		
		//CAS ARTICLE 72 = 99
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_72(99);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	public static void generateTestForArticle73(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 73 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_73(5);
		
		//CAS ARTICLE 73 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_73(10);
		
		//CAS ARTICLE 73 = 99
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_73(99);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	public static void generateTestForArticle74(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 74 = 1
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_74(1);
		
		//CAS ARTICLE 74 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_74(3);
		
		//CAS ARTICLE 74 = 5
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_74(5);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	public static void generateTestForArticle8(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();
		
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 8 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_8(1);
		
		//CAS ARTICLE 8 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_8(3);
		
		//CAS ARTICLE 8 = 6
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_8(99);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	public static void generateTestForArticle9(String folderOut) throws IOException {
		
		(new File(folderOut)).mkdirs();
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 9 = 0.333
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_9(0.333);
		
		//CAS ARTICLE 9 = 0.6666
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_9(0.666);
		
		//CAS ARTICLE 8 = 0
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_9(0);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	
	
	
	public static void generateTestForArticle13(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();
		
		
		in = new FileWriter( new File(folderOut+"predicate.csv")); 
		in.write(fLine +  "\n");
		
		//Having a default regulation is very pratical to see the influence of varying a parameter
			String defaultRegulation = "0,0,U0,U0,0,0,0,0,0,0,0,0.1,0,0,0,0,0,0,0,1.0,2,10,0,0,10";
	
			//The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		
		//CAS ARTICLE 9 = 0.333
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, defaultRegulation);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		//Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_13(0.333);
		
		//CAS ARTICLE 9 = 0.6666
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_13(0.666);
		
		//CAS ARTICLE 8 = 0
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_13(0);
		
		
		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);
		
		generateFakeData(regulations, folderOut);
		
	}
	
	public static void generateFakeData(List<ArtiScalesRegulation> regulationList, String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();
		 
		 IFeatureCollection<IFeature> parcels = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> buildings = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> zones = new FT_FeatureCollection<>();
		 IFeatureCollection<IFeature> roads = new FT_FeatureCollection<>();
		 
		 int count = 0;
		 
		for(ArtiScalesRegulation regulation: regulationList) {
			//We create a zone with the right name
			FakeZone fZ = new FakeZone(count,  regulation.getLibelle_de_dul(), 843126.29, 6519466);
			
			//We export the regulation
		    in.write(regulation.toCSVLine()+  "\n");
		    
		    //Adding objects to the final list
		    parcels.addAll(fZ.getParcels());
		    zones.addAll(fZ.getUrbaZones());
		    buildings.addAll(fZ.getBuildings());
		    roads.addAll(fZ.getRoads());
		    
		    count++;
		}
		

   
        
        in.close();
        
        ShapefileWriter.write(parcels, folderOut + "parcelle.shp"); 
        ShapefileWriter.write(buildings, folderOut + "batiment.shp");    
        ShapefileWriter.write(zones, folderOut + "zoneUrba.shp");   
        ShapefileWriter.write(roads, folderOut + "route.shp");  
		
		
	}

}
