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
 * The aim of this code is to generate a FakeWorld that can be used to test the different rules for the simulation at a local scale
 * 
 * @author mbrasebin
 *
 */
public class FakeWorldGenerator {

	static FileWriter in;
	// RNU rule (which is the basicalest basic)
	static String RNURow = "0,42400,U,U0,0,0,0,0,99,1,99,99,0,3,3,2,3,99,7,21,1,99,99";
	static String neutralRow = "0,42400,U,U0,0,0,0,0,99,99,99,99,99,99,99,99,99,1,1,2,99,99,99";

	// static String basicRow = "0,42400,U,U0,0,0,0,0,99,4,99,99,0,3,3,2,3,99,6,9,1m60_2,99,99" ;
	// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

	// Default line for regulation (to parse the list and to export the regulation)
	public final static String fLine = "libelle_zone,insee,libelle_de_base,libelle_de_dul,OAP,fonction,art_3,art_4,art_5,art_6_defaut,art_6_type,art_6_optionel,art_71,art_72,art_73,art_74,art_8,art_9,art_10_top,art_101,art_12,art_13,art_14";

	public static void main(String[] args) throws IOException {
		// Folder where to generate the fake data
		String folderOut = "./fakeWorld/";
		generateTestForBuildingType(folderOut);
		// generateTestForArticle5(folderOut + "art5/");
		// generateTestForArticle6(folderOut + "art6/");
		// generateTestForArticle6spec(folderOut + "art6spec/");
		// generateTestForArticle71(folderOut + "art71/");
		// generateTestForArticle72(folderOut + "art72/");
		// generateTestForArticle73(folderOut + "art73/");
		// generateTestForArticle74(folderOut + "art74/");
		// generateTestForArticle8(folderOut + "art8/");
		// generateTestForArticle9(folderOut + "art9/");
		// generateTestForArticle10(folderOut + "art10/");
		// generateTestForArticle12(folderOut + "art12/");
		// generateTestForArticle13(folderOut + "art13/");

	}

	// , MIDBLOCKFLATS, MULTIFAMILYHOUSE, DETACHEDHOUSE, SMALLBLOCKFLAT
	public static void generateTestForBuildingType(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);

		regulations.add(regulationDefault);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle5(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_5("11000");

		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_5("99");

		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_5("_225");

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle10(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized

		// CAS ARTICLE 10 = 1 && ARTICLE_10_1 = 5
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_10("5");
		regulationDefault2.setArt_10_top(1);

		// CAS ARTICLE 10 = 2 && ARTICLE_10_1 = 12
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_10("12");
		regulationDefault3.setArt_10_top(2);

		// CAS ARTICLE 10 = 6 && ARTICLE_10_1 = 12
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_10("12");
		regulationDefault4.setArt_10_top(6);

		// CAS ARTICLE 10 = 7 && ARTICLE_10_1 = 12
		ArtiScalesRegulation regulationDefault5 = regulationDefault.clone();
		regulationDefault5.setLibelle_de_dul("U4");
		regulationDefault5.setArt_10("12");
		regulationDefault5.setArt_10_top(7);

		// CAS ARTICLE 10 = 8 && ARTICLE_10_1 = 12
		ArtiScalesRegulation regulationDefault6 = regulationDefault.clone();
		regulationDefault6.setLibelle_de_dul("U5");
		regulationDefault6.setArt_10("12");
		regulationDefault6.setArt_10_top(8);

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

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized

		// CAS ARTICLE 6 = 0
		// TODO tout les coboides doivent être collées.. Est ce bien logique?
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_6_defaut("0");
		regulationDefault2.setArt_6_type(99);
		regulationDefault2.setArt_6_optionel("99");

		// CAS ARTICLE 6 = 44
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_6_defaut("44");
		regulationDefault3.setArt_6_type(99);
		regulationDefault3.setArt_6_optionel("99");

		// CAS ARTICLE 6 = normal number
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_6_defaut("3");
		regulationDefault4.setArt_6_type(99);
		regulationDefault4.setArt_6_optionel("99");

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle6spec(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);

		// Important to change to get the right regulation recognized

		// CAS ARTICLE 6 is either sticked on the edge or has a recoil of 3 meters
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_6_defaut("0");
		regulationDefault2.setArt_6_type(10);
		regulationDefault2.setArt_6_optionel("3");

		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_6_defaut("0");
		regulationDefault3.setArt_6_type(10);
		regulationDefault3.setArt_6_optionel("30");

		// // CAS ARTICLE 6 must follow one side building, or a 3 meters recoil
		// ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		// regulationDefault3.setLibelle_de_dul("U2");
		// regulationDefault3.setArt_6("3");
		// regulationDefault3.setArt_6_opt("20");
		// regulationDefault3.setArt_6_optD("99");
		//
		// // CAS ARTICLE 6 must follow the current alignment, or a 3 meters recoil
		// ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		// regulationDefault4.setLibelle_de_dul("U3");
		// regulationDefault4.setArt_6("3");
		// regulationDefault4.setArt_6_opt("30");
		// regulationDefault4.setArt_6_optD("99");

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		// regulations.add(regulationDefault4);
		// regulations.add(regulationDefault5);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle71(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,3,3,2,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 71 = 1
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_71(1);

		// CAS ARTICLE 71 = 2
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_71(2);

		// CAS ARTICLE 71 = 3
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_71(3);

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle72(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 72 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_72(5);

		// CAS ARTICLE 72 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_72(10);

		// CAS ARTICLE 72 = 99
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

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 73 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_73(5);

		// CAS ARTICLE 73 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_73(10);
		regulationDefault3.setArt_71(1);

		// CAS ARTICLE 73 = 99
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");

		regulationDefault4.setArt_73(99);
		regulationDefault4.setArt_71(1);

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle74(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 74 = 1
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_74(1);

		// CAS ARTICLE 74 = 2
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_74(2);

		// CAS ARTICLE 74 has no limitation
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_71(1);
		regulationDefault4.setArt_74(2);

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle8(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();
		// TODO problem
		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 8 = 0
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_8(1);

		// CAS ARTICLE 8 = 3
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_8(3);

		// CAS ARTICLE 8 = 6
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_8(10);

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle9(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 9 = 0.333
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_9(0.333);

		// CAS ARTICLE 9 = 0.6666
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_9(0.666);

		// CAS ARTICLE 8 = 0
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_9(99);

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle12(String folderOut) throws IOException {

		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// SIMPLE CASE
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_12("1");

		// CASE ARTICLE 12 related to housing unit limit
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_12("0l2_2");

		// CAS ARTICLE 12 related to building surface
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_12("1m60_2");

		regulations.add(regulationDefault);
		regulations.add(regulationDefault2);
		regulations.add(regulationDefault3);
		regulations.add(regulationDefault4);

		generateFakeData(regulations, folderOut);

	}

	public static void generateTestForArticle13(String folderOut) throws IOException {
		(new File(folderOut)).mkdirs();

		in = new FileWriter(new File(folderOut + "snapPredicate.csv"));
		in.write(fLine + "\n");

		// Having a default regulation is very pratical to see the influence of varying a parameter
		// String defaultRegulation = "0,42400,U,U0,0,0,0,0,0,99,99,99,0,0,0,0,0,1.0,0,0,0,0,0";

		// The list of regulation for which building generation will be testsed
		List<ArtiScalesRegulation> regulations = new ArrayList<ArtiScalesRegulation>();

		// CAS ARTICLE 13 = 0.333
		ArtiScalesRegulation regulationDefault = new ArtiScalesRegulation(fLine, RNURow);
		ArtiScalesRegulation regulationDefault2 = regulationDefault.clone();
		// Important to change to get the right regulation recognized
		regulationDefault2.setLibelle_de_dul("U1");
		regulationDefault2.setArt_13("0.333");

		// CAS ARTICLE 13 has a area constraint
		ArtiScalesRegulation regulationDefault3 = regulationDefault.clone();
		regulationDefault3.setLibelle_de_dul("U2");
		regulationDefault3.setArt_13("0.7>300");

		// CAS ARTICLE 13 = 0
		ArtiScalesRegulation regulationDefault4 = regulationDefault.clone();
		regulationDefault4.setLibelle_de_dul("U3");
		regulationDefault4.setArt_13("0");

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

		for (ArtiScalesRegulation regulation : regulationList) {
			// We create a zone with the right name
			FakeZone fZ = new FakeZone(count, regulation.getLibelle_de_dul(), 817195.2, 6488529.3);

			// We export the regulation
			in.write(regulation.toCSVLine() + "\n");

			// Adding objects to the final list
			parcels.addAll(fZ.getParcels());
			zones.addAll(fZ.getUrbaZones());
			buildings.addAll(fZ.getBuildings());
			roads.addAll(fZ.getRoads());

			count++;
		}

		in.close();
		new File(folderOut + "/geoSnap").mkdirs();
		ShapefileWriter.write(parcels, folderOut + "/parcelle.shp");
		ShapefileWriter.write(buildings, folderOut + "/geoSnap/batiment.shp");
		ShapefileWriter.write(zones, folderOut + "/geoSnap/zone_urba.shp");
		ShapefileWriter.write(roads, folderOut + "/geoSnap/route.shp");

	}

}
