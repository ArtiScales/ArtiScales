package fr.ign.artiscales.main.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.artiscales.main.createGeom.Density;
import fr.ign.artiscales.main.map.MapRenderer;
import fr.ign.artiscales.main.map.theseMC.DensIniNewComp;
import fr.ign.artiscales.main.map.theseMC.DiffObjLgtMap;
import fr.ign.artiscales.main.map.theseMC.nbHU.BuildingCollRatio;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHU;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUAU;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUDetachedHouse;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUMidBlock;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUMultiFamilyHouse;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUSmallBlock;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUSmallHouse;
import fr.ign.artiscales.main.map.theseMC.nbHU.NbHUU;
import fr.ign.artiscales.main.map.theseMC.parcelMaps.ParcelleDensEmprisepHec;
import fr.ign.artiscales.main.map.theseMC.parcelMaps.ParcelleDensHUpHec;
import fr.ign.artiscales.main.map.theseMC.parcelMaps.ParcelleDensSDPpHec;
import fr.ign.artiscales.main.rules.regulation.buildingType.BuildingType;
import fr.ign.artiscales.main.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.artiscales.main.util.FromGeom;
import fr.ign.artiscales.main.util.SimuTool;
import fr.ign.artiscales.pm.parcelFunction.ParcelAttribute;
import fr.ign.artiscales.pm.parcelFunction.ParcelState;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Shp;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class BuildingToHousingUnit extends Indicators {

	// infos about the buildings
	int nbBuildings, nbHU, nbDetachedHouse, nbSmallHouse, nbMultifamilyHouse, nbSmallBlockFlat, nbMidBlockFlat, nbU, nbAU, nbNC, nbCentre, nbBanlieue,
			nbPeriUrbain, nbRural, objHU, diffHU, nbBuildDetachedHouse, nbBuildSmallHouse, nbBuildMultifamilyHouse, nbBuildSmallBlockFlat,
			nbBuildMidBlockFlat, nbBuildU, nbBuildAU, nbBuildNC, nbBuildCentre, nbBuildBanlieue, nbBuildPeriUrbain, nbBuildRural;
	double sDPtot, empriseTot, averageDensiteHU, averageDensiteSDP, standDevDensiteSDP, averageDensiteEmprise, standDevDensiteEmprise,
			standDevDensiteHU, objDens, diffDens, averageSDPpHU, standDevSDPpHU, averageEval, standDevEval, ratioHUcol, ratioHUind;
	String housingUnitFirstLine, genStatFirstLine, numeroParcel;
	File tmpFile;
	private static String indicName = "bTH";

	public BuildingToHousingUnit(File rootFile, SimpluParametersJSON par, String scenarName, String variantName) throws Exception {
		super(par, rootFile, scenarName, variantName, getIndicName());
		setBasics();
	}

	public BuildingToHousingUnit(File rootFile, SimpluParametersJSON par, String scenarName, String variantName, List<String> listCities)
			throws Exception {
		super(par, rootFile, scenarName, variantName, getIndicName(), listCities);
		setBasics();
	}

	private void setBasics() {
		tmpFile = new File(getIndicFolder(), "tmpFile");
		tmpFile.mkdirs();

		housingUnitFirstLine = "codeParcel," + "armature," + "SDP," + "emprise," + "nb_housingUnit," + "type_HU," + "zone," + "typo_HU,"
				+ "averageSDPPerHU," + "HUpHectareDensity," + "SDPpHectareDensity," + "EmprisepHectareDensity," + "eval";

		genStatFirstLine = "code," + "SDPTot," + "empriseTot," + "average_densiteHU," + "standardDev_densiteHU," + "average_densiteSDP,"
				+ "standardDev_densiteSDP," + "average_densiteEmprise," + "standardDev_densiteEmprise," + "objectifSCOT_densite,"
				+ "diff_objectifSCOT_densite," + "average_SDP_per_HU," + "standardDev_SDP_per_HU," + "nb_building," + "nb_housingUnit,"
				+ "objectifPLH_housingUnit," + "diff_objectifPLH_housingUnit," + "nbHU_detachedHouse," + "nbHU_smallHouse," + "nbHU_multiFamilyHouse,"
				+ "nbHU_smallBlockFlat," + "nbHU_midBlockFlat," + "nbHU_U," + "nbHU_AU," + "nbHU_NC," + "nbHU_centre," + "nbHU_banlieue,"
				+ "nbHU_periUrbain," + "nbHU_rural," + "nbBuild_detachedHouse," + "nbBuild_smallHouse," + "nbBuild_multiFamilyHouse,"
				+ "nbBuild_smallBlockFlat," + "nbBuild_midBlockFlat," + "nbBuild_U," + "nbBuild_AU," + "nbBuild_NC," + "nbBuild_centre,"
				+ "nbBuild_banlieue," + "nbBuild_periUrbain," + "nbBuild_rural," + "averageEval," + "standardDevEval," + "ratioHUcol," + "ratioHUind";
	}

	public BuildingToHousingUnit(File batiFolder, File paramFile, SimpluParametersJSON par) throws Exception {
		super(par, batiFolder, "", "", getIndicName());
		setParamFolder(paramFile);
	}

//	public static void main(String[] args) throws Exception {
//		File rootFile = new File("./result2903/");
//		File rootParam = new File(rootFile, "paramFolder");
//
//		String[] scenarios = { "CDense", "CPeuDense", "DDense", "DPeuDense" };
//		// String[] scenarios = { "CDense" };
//		String variant = "base";
//
//		for (String scenario : scenarios) {
//			// for (File f : (new File(rootFile, "SimPLUDepot/" + scenario + "/")).listFiles()) {
//			// String variant = f.getName();
//			//
//			// String variant = "varianteSeed2";
//
//			List<File> lF = new ArrayList<>();
//			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
//			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));
//
//			SimpluParametersJSON p = new SimpluParametersJSON(lF);
//			System.out.println("run " + scenario + " variant: " + variant);
//			run(p, rootFile, scenario, variant);
//			// }
//		}
//
//	}

	 // // main for analysis by different documents
	 public static void main(String[] args) throws Exception {
	 File rootFile = new File("./result2903/");
	 File rootParam = new File(rootFile, "paramFolder");
	 // String scenario = "CPeuDense";
	 String variant = "base";
	 String[] typeDocs = { "RNU", "PLU", "CC" };
	 String[] scenarios = { "DDense", "CDense", "CPeuDense", "DPeuDense" };
	 for (String typeDoc : typeDocs) {
	 List<String> listDoc = FromGeom.getZipByTypeDoc(new File(rootFile, "dataRegulation"), typeDoc);
	 setIndicName("bTH-" + typeDoc);
	
	 // String[] scenarios = { "CDense", "CPeuDense", "DDense", "DPeuDense" };
	 // String[] scenarios = { "CPeuDense", "DDense" };
	
	 for (String scenario : scenarios) {
	
	 List<File> lF = new ArrayList<>();
	 lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
	 lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));
	
	 SimpluParametersJSON p = new SimpluParametersJSON(lF);
	 System.out.println("run " + scenario + " variant: " + variant + " doc ? " + typeDoc);
	 run(p, rootFile, scenario, variant, listDoc);
	 }
	 }
	 }

	public static void run(SimpluParametersJSON p, File rootFile, String scenario, String variant) throws Exception {
		run(p, rootFile, scenario, variant, null);
	}

	public static void run(SimpluParametersJSON p, File rootFile, String scenario, String variant, List<String> listCities) throws Exception {

		BuildingToHousingUnit bhtU;
		if (listCities != null && !listCities.isEmpty()) {
			bhtU = new BuildingToHousingUnit(rootFile, p, scenario, variant, listCities);
		} else {
			bhtU = new BuildingToHousingUnit(rootFile, p, scenario, variant);
		}

		// // statistics about denials
		 SimuTool.getStatDenialBuildingType(bhtU.getSimPLUDepotGenFile().getParentFile(),
		 new File(bhtU.getIndicFolder(), "StatDenialBuildingType.csv"));
//		 SimuTool.getStatDenialCuboid(bhtU.getSimPLUDepotGenFile().getParentFile(), new File(bhtU.getIndicFolder(), "StatDenialCuboid.csv"));
		//
		// // main general statistics
//		bhtU.distributionEstimate();
		// bhtU.makeGenStat();
		// bhtU.setCountToZero();
		//
		// // for every cities
		// List<String> listInsee = FromGeom.getInsee(new File(bhtU.getRootFile(), "/dataGeo/old/communities.shp"), "DEPCOM");
		// for (String city : listInsee) {
		// bhtU.makeGenStat(city);
		// bhtU.setCountToZero();
		// }

		// for every armature
//		List<String> listArmature = FromGeom.getInsee(new File(bhtU.getRootFile(), "/dataGeo/old/communities.shp"), "armature");
//		for (String armat : listArmature) {
//			bhtU.makeArmatureStat(armat);
//			bhtU.setCountToZero();
//		}

		// // new shapefile with stats
		// bhtU.setParcelStatFile(bhtU.joinStatBTHtoParcels("housingUnits.csv"));
		// bhtU.setCommStatFile(bhtU.joinStatBTHtoCommunities("genStat.csv"));
		//
		// bhtU.createDensityCommunities(new File(bhtU.getRootFile(), "dataGeo/base-ic-logement-2012.csv"),
		// new File(bhtU.getRootFile(), "dataGeo/old/communities.shp"), bhtU.getRootFile(), new File(bhtU.getIndicFolder(), "commNewDens.shp"),
		// "P12_LOG", "COM", "DEPCOM");
		//
		// // graphs
		 bhtU.createGraphNetDensity(new File(bhtU.getIndicFolder(), "housingUnits.csv"));
		 bhtU.createGraphCount(new File(bhtU.getIndicFolder(), "genStat.csv"), listCities);
		// bhtU.makeStatHU();
		// // maps
		// allOfTheMap(bhtU);
	}

	public File createDensityCommunities(File countHUFile, File fileToAddInitialDensity, File rootFile, File fileOut, String nameFieldHUCsv,
			String nameFieldCodeCsv, String nameFieldCodeShp) throws Exception {
		// sum the total housing units estimated for each communities
		File tmpCount = new File(tmpFile, "newHUCount.csv");
		CSVWriter csvTmp = new CSVWriter(new FileWriter(tmpCount), ',', '\0');
		String[] fL = { nameFieldCodeCsv, nameFieldHUCsv };
		csvTmp.writeNext(fL);

		File simuStat = new File(getIndicFolder(), "genStat.csv");
		if (!simuStat.exists()) {
			System.out.println("run getEstimationForACity first");
			System.exit(-1);
		}

		CSVReader csvRlgtCount = new CSVReader(new FileReader(countHUFile));

		// first line of the initial household counts and set the positions of the interesting fields
		int codeP = 0;
		int lgtP = 0;

		String[] fLine = csvRlgtCount.readNext();
		for (int i = 0; i < fLine.length; i++) {
			String s = fLine[i];
			if (s.equals(nameFieldHUCsv)) {
				lgtP = i;
			}
			if (s.equals(nameFieldCodeCsv)) {
				codeP = i;
			}
		}

		// get the accounts of the simulation's HU created
		CSVReader csvRStatGen = new CSVReader(new FileReader(simuStat), ',', '\0');

		int codePResult = 0;
		int lgtPResult = 0;
		String[] fLineResult = csvRStatGen.readNext();
		for (int i = 0; i < fLineResult.length; i++) {
			String s = fLineResult[i];
			if (s.equals("nb_housingUnit")) {
				lgtPResult = i;
			}
			if (s.equals("code")) {
				codePResult = i;
			}
		}
		csvRlgtCount.close();

		for (String[] lineResult : csvRStatGen.readAll()) {
			String insee = lineResult[codePResult];
			if (insee.equals("ALLLL")) {
				continue;
			}
			csvRlgtCount = new CSVReader(new FileReader(countHUFile));
			int nb = Math.round(Float.valueOf(lineResult[lgtPResult].replace(",", ".")));
			for (String[] lineIni : csvRlgtCount.readAll()) {
				if (insee.equals(lineIni[codeP])) {
					nb = nb + Math.round(Float.valueOf(lineIni[lgtP].replace(",", ".")));
				}
			}
			String[] nextLine = { insee, String.valueOf(nb) };
			csvTmp.writeNext(nextLine);
			csvRlgtCount.close();
		}

		csvRStatGen.close();
		csvTmp.close();

		File zoningFile = new File(rootFile, "/dataRegulation/zoning.shp");

		File result = Density.createCommunitiesWithInitialBrutDensity(nameFieldHUCsv, nameFieldCodeCsv, nameFieldCodeShp, zoningFile, countHUFile,
				fileToAddInitialDensity, fileOut);
		result = Density.createCommunitiesWithNewBrutDensity(nameFieldHUCsv, nameFieldCodeCsv, nameFieldCodeShp, zoningFile, tmpCount, result,
				fileOut);

		File buildingIniFile = new File(rootFile, "dataGeo/building.shp");
		File parcelIniFile = new File(rootFile, "dataGeo/parcel.shp");

		File parcelNewFile = getParcelDepotGenFile();

		List<File> lBuilding = Arrays.asList(buildingIniFile, getSimPLUDepotGenFile());
		File buildingNewFile = Shp.mergeVectFiles(lBuilding, new File(tmpFile, "buildingTot.shp"));

		result = Density.createCommunitiesWithInitialNetDensity(nameFieldHUCsv, nameFieldCodeCsv, nameFieldCodeShp, parcelIniFile, buildingIniFile,
				countHUFile, result, fileOut);

		result = Density.createCommunitiesWithNewNetDensity(nameFieldHUCsv, nameFieldCodeCsv, nameFieldCodeShp, parcelNewFile, buildingNewFile,
				tmpCount, result, fileOut);
		return result;
	}

	public void createGraphNetDensity(File statFile) throws IOException {
		makeGraphDens(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName, "HUpHectareDensity",
				"Densité nette de logements par hectare", "Nombre de parcelles", 0, 100);
		makeGraphDens(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName, "SDPpHectareDensity",
				"Densité nette de surface de plancher des bâtiments par hectare", "Nombre de parcelles", 0, 6000);
		makeGraphDens(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName,
				"EmprisepHectareDensity", "Densité nette de l'emprise des bâtiments par hectare", "Nombre de parcelles", 0, 6000);
		makeGraphDens(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName, "eval",
				"Évaluation de l'intêret à être urbanisé", "Nombre de parcelles", 0, 1, 10);
	}

	public void createGraphCount(File statFile, List<String> listCities) throws IOException {

		// un peu bancal, mais ce sera mieux fait one day
		String addon = "";
		if (listCities == null || listCities.isEmpty()) {
			listCities = FromGeom.getZips(new File(getRootFile(), "dataRegulation"));
		} else {
			switch (getIndicName().split("-")[1]) {
			case "RNU":
				addon = " - communes couvertes par le RNU";
				break;
			case "CC":
				addon = " - communes couvertes par une carte communale";
				break;
			case "PLU":
				addon = " - communes couvertes par un PLU";
				break;
			}
		}

		String[] xType = { "nbHU_detachedHouse", "nbHU_smallHouse", "nbHU_multiFamilyHouse", "nbHU_smallBlockFlat", "nbHU_midBlockFlat" };
		makeGraph(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName + addon, xType,
				"Type de bâtiment", "Nombre de logements simulés" + addon);
		String[] xTypo = { "nbHU_rural", "nbHU_periUrbain", "nbHU_banlieue", "nbHU_centre" };
		makeGraph(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName + addon, xTypo,
				"Typologie des communes", "Nombre de logements simulés");
		String[] xZone = { "nbHU_U", "nbHU_AU", "nbHU_NC" };
		makeGraph(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName + addon, xZone,
				"Type de zonage", "Nombre de logements simulés");
		String[] xConso = { "SDPTot", "empriseTot" };
		makeGraph(statFile, getGraphDepotFolder(), SimuTool.makeWordPHDable(scenarName) + " - Variante : " + variantName + addon, xConso,
				"Consommation surfacique des bâtiments simulés", "Surface (em km²)");

		makeGraphForEachCities(statFile, getGraphDepotFolder(), "Différence des objectifs de logements et des logements simulés" + addon, listCities,
				"Code INSEE des communes", "Différence entre l'objectif et le nombre de logements simulés");

	}

	public static void makeGraphDens(File csv, File graphDepotFile, String title, String x, String xTitle, String yTitle, int xMin, int xMax)
			throws IOException {
		makeGraphDens(csv, graphDepotFile, title, x, xTitle, yTitle, xMin, xMax, 20);
	}

	public static void makeGraphDens(File csv, File graphDepotFile, String title, String x, String xTitle, String yTitle, int xMin, int xMax,
			int range) throws IOException {
		// Create Chart
		List<Double> values = new ArrayList<Double>();

		// List<Double> yS = new ArrayList<Double>();

		// SeriesData csvData= CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, s, y);
		CSVReader csvR = new CSVReader(new FileReader(csv));
		int iX = 0;
		String[] fLine = csvR.readNext();
		// get them first line
		for (int i = 0; i < fLine.length; i++) {
			if (fLine[i].equals(x))
				iX = i;
		}
		for (String[] lines : csvR.readAll()) {
			values.add(Double.valueOf(lines[iX]));
		}

		csvR.close();

		Histogram histo = new Histogram(values, range, xMin, xMax);
		CategoryChart chart = new CategoryChartBuilder().width(600).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		chart.addSeries(makeLabelPHDable(x), histo.getxAxisData(), histo.getyAxisData());

		// Customize Chart
		// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(false);
		chart.getStyler().setXAxisLabelRotation(45);
		chart.getStyler().setXAxisLogarithmicDecadeOnly(true);
		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + SimuTool.makeCamelWordOutOfPhrases(xTitle + yTitle), BitmapFormat.PNG);
		// new SwingWrapper(chart).displayChart();
	}

	/**
	 * create statistics about the Housing Units within a Building
	 * 
	 * @throws IOException
	 */
	public void makeStatHU() throws IOException {

		// List<Double> yS = new ArrayList<Double>();
		String[] types = { "MULTIFAMILYHOUSE", "SMALLHOUSE", "SMALLBLOCKFLAT", "MIDBLOCKFLAT", "DETACHEDHOUSE" };

		File graphDepotFileHU = new File(this.getGraphDepotFolder(), "HU");
		graphDepotFileHU.mkdir();

		for (String type : types) {

			List<Double> valuesSDPpHU = new ArrayList<Double>();
			List<Double> valuesnbHousing = new ArrayList<Double>();

			CSVReader csvR = new CSVReader(new FileReader(new File(this.getIndicFolder(), "housingUnits.csv")));

			// SeriesData csvData= CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, s, y);
			int typeP = 0, nbHousingP = 0, sDPpHUP = 0;
			String[] fLine = csvR.readNext();
			// get them first line
			for (int i = 0; i < fLine.length; i++) {
				if (fLine[i].equals("averageSDPPerHU"))
					sDPpHUP = i;
				else if (fLine[i].equals("nb_housingUnit"))
					nbHousingP = i;
				else if (fLine[i].equals("type_HU"))
					typeP = i;
			}
			for (String[] line : csvR.readAll()) {
				if (line[typeP].equals(type)) {
					valuesSDPpHU.add(Double.valueOf(line[sDPpHUP]));
					valuesnbHousing.add(Double.valueOf(line[nbHousingP]));
				}
			}
			csvR.close();
			int xMax = 200;
			int xMin = 50;
			if (type.equals("DETACHEDHOUSE")) {
				xMax = 260;
			} else if (type.equals("SMALLBLOCKFLAT") || type.equals("MIDBLOCKFLAT")) {
				xMin = 30;
				xMax = 180;
			}

			Histogram histoSDPpHU = new Histogram(valuesSDPpHU, 15, xMin, xMax);
			String titreSPDpHU = "moyenne de surface de plancher par logements";

			CategoryChart chartSPDpHU = new CategoryChartBuilder().width(460).height(400).title(titreSPDpHU)
					.xAxisTitle("moyenne de surface de plancher").yAxisTitle("nombre de bâtiments").build();
			chartSPDpHU.addSeries(titreSPDpHU, histoSDPpHU.getxAxisData(), histoSDPpHU.getyAxisData());

			// Customize Chart
			// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
			chartSPDpHU.getStyler().setLegendVisible(false);
			chartSPDpHU.getStyler().setHasAnnotations(false);
			chartSPDpHU.getStyler().setXAxisLabelRotation(45);
			chartSPDpHU.getStyler().setXAxisLogarithmicDecadeOnly(true);
			BitmapEncoder.saveBitmap(chartSPDpHU, graphDepotFileHU + "/" + titreSPDpHU + "-" + type, BitmapFormat.PNG);
			// new SwingWrapper(chart).displayChart();

			if (type.equals("SMALLHOUSE") || type.equals("DETACHEDHOUSE")) {
				continue;
			}

			Histogram histonbH = new Histogram(valuesnbHousing, 10, 1, 31);
			String titrenbH = "nombre de logements par bâtiments";

			CategoryChart chartnbH = new CategoryChartBuilder().width(400).height(400).title(titrenbH).xAxisTitle("nombre de logements")
					.yAxisTitle("nombre de bâtiments").build();
			chartnbH.addSeries("nombre de logements par bâtiments", histonbH.getxAxisData(), histonbH.getyAxisData());

			// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
			chartnbH.getStyler().setLegendVisible(false);
			chartnbH.getStyler().setHasAnnotations(false);
			chartnbH.getStyler().setXAxisLabelRotation(45);
			chartnbH.getStyler().setXAxisLogarithmicDecadeOnly(true);
			BitmapEncoder.saveBitmap(chartnbH, graphDepotFileHU + "/" + titrenbH + "-" + type, BitmapFormat.PNG);
			// new SwingWrapper(chart).displayChart();
		}
	}

	public static void makeGraph(File csv, File graphDepotFile, String title, String[] x, String xTitle, String yTitle) throws IOException {
		// Create Chart
		CategoryChart chart = new CategoryChartBuilder().width(800).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		List<String> label = new ArrayList<String>();
		List<Double> yS = new ArrayList<Double>();
		for (String s : x) {
			label.add(makeLabelPHDable(s));
			// SeriesData csvData= CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, s, y);
			CSVReader csvR = new CSVReader(new FileReader(csv));
			int iX = 0;
			int iCode = 0;
			String[] fLine = csvR.readNext();
			// get them first line
			for (int i = 0; i < fLine.length; i++) {
				if (fLine[i].equals(s))
					iX = i;
				if (fLine[i].equals("code"))
					iCode = i;
			}
			for (String[] lines : csvR.readAll()) {
				if (lines[iCode].equals("ALLLL")) {
					yS.add(Double.valueOf(lines[iX]));
					break;
				}
			}
			csvR.close();
		}

		chart.addSeries(yTitle, label, yS);

		// Customize Chart
		// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(true);
		chart.getStyler().setXAxisLabelRotation(45);
		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + SimuTool.makeCamelWordOutOfPhrases(xTitle + yTitle), BitmapFormat.PNG);
		// new SwingWrapper(chart).displayChart();
	}

	public static void makeGraphForEachCities(File csv, File graphDepotFile, String title, List<String> x, String xTitle, String yTitle)
			throws IOException {
		// Create Chart
		// delete those Besançon data
		x.remove("25056");
		CategoryChart chart = new CategoryChartBuilder().width(1300).height(500).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		List<Double> yS = new LinkedList<Double>();
		for (String s : x) {
			// SeriesData csvData= CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, s, y);
			CSVReader csvR = new CSVReader(new FileReader(csv));
			int iX = 0;
			int iCode = 0;
			String[] fLine = csvR.readNext();
			// get them first line
			for (int i = 0; i < fLine.length; i++) {
				if (fLine[i].equals("diff_objectifPLH_housingUnit"))
					iX = i;
				if (fLine[i].equals("code"))
					iCode = i;
			}
			for (String[] lines : csvR.readAll()) {
				if (lines[iCode].equals(s)) {
					yS.add(Double.valueOf(lines[iX]));
					break;
				}
			}
			csvR.close();
		}

		chart.addSeries(yTitle, x, yS);

		// Customize Chart
		// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(true);
		chart.getStyler().setXAxisLabelRotation(90);
		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + SimuTool.makeCamelWordOutOfPhrases(xTitle + yTitle), BitmapFormat.PNG);
		// new SwingWrapper(chart).displayChart();
	}

	/**
	 * shortCut method
	 * 
	 * @param bhtU
	 * @throws MalformedURLException
	 * @throws NoSuchAuthorityCodeException
	 * @throws IOException
	 * @throws FactoryException
	 */
	public static void allOfTheMap(BuildingToHousingUnit bhtU)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		allOfTheMap(bhtU, new File(bhtU.getIndicFolder(), "commStatBTH.shp"), new File(bhtU.getIndicFolder(), "parcStat.shp"),
				new File(bhtU.getIndicFolder(), "commNewDens.shp"));
	}

	public static void allOfTheMap(BuildingToHousingUnit bhtU, File commStat, File parcStat, File newBrutDensityFile)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {

		bhtU.setCommStatFile(commStat);
		bhtU.setParcelStatFile(parcStat);

		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();
		// buildings maps
		MapRenderer diffObjLgtMap = new DiffObjLgtMap(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(diffObjLgtMap);
		MapRenderer nbHU = new NbHU(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHU);
		MapRenderer nbHUDetachedHouse = new NbHUDetachedHouse(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUDetachedHouse);
		MapRenderer nbHUMidBlock = new NbHUMidBlock(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUMidBlock);
		MapRenderer nbHUSmallBlock = new NbHUSmallBlock(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUSmallBlock);
		MapRenderer nbHUSmallHouse = new NbHUSmallHouse(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUSmallHouse);
		MapRenderer nbHUMultiFamilyHouse = new NbHUMultiFamilyHouse(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUMultiFamilyHouse);
		MapRenderer nbHUU = new NbHUU(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUU);
		MapRenderer nbHUAU = new NbHUAU(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(nbHUAU);
		MapRenderer buildingCollRatio = new BuildingCollRatio(1000, 1000, bhtU.getMapStyle(), bhtU.getCommStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(buildingCollRatio);

		// parcels maps
		MapRenderer dHuHec = new ParcelleDensHUpHec(1000, 1000, bhtU.getMapStyle(), bhtU.getParcelStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(dHuHec);
		MapRenderer dSDPHec = new ParcelleDensSDPpHec(1000, 1000, bhtU.getMapStyle(), bhtU.getParcelStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(dSDPHec);
		MapRenderer dEmpHec = new ParcelleDensEmprisepHec(1000, 1000, bhtU.getMapStyle(), bhtU.getParcelStatFile(), bhtU.getMapDepotFolder());
		allOfTheMaps.add(dEmpHec);

		for (MapRenderer map : allOfTheMaps) {
			map.renderCityInfo();
			map.generateSVG();
		}

		// net density maps
		DensIniNewComp map = new DensIniNewComp(1000, 1000, bhtU.getMapStyle(), newBrutDensityFile, bhtU.getMapDepotFolder());
		map.makeDensIniNetMap();
		map.renderCityInfo("DensNetIni");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DensNetIni.svg"), "DensNetIni");

		map.makeDensNewNetMap();
		map.renderCityInfo("DensNetNew");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DensNetNew.svg"), "DensNetNew");

		map.makeObjMap();
		map.renderCityInfo("DensObj");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DensObj.svg"), "DensObj");

		map.makeDensIniBrutMap();
		map.renderCityInfo("DensBrtIni");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DensBrtIni.svg"), "DensBrtIni");

		map.makeDensNewBrutMap();
		map.renderCityInfo("DensBrtNew");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DensBrtNew.svg"), "DensBrtNew");

		map.makeDiffObjMap();
		map.renderCityInfo("DifDObjN");
		map.generateSVG(new File(bhtU.getMapDepotFolder(), "DifDObjN.svg"), "DifDObjN");
	}

	public int getEstimationForACity(String insee) throws IOException {
		int result = -1;
		CSVReader read = new CSVReader(new FileReader(new File(getIndicFolder(), "genStat.csv")));
		String[] fLine = read.readNext();
		int nbCode = 0;
		int nbHU = 0;
		for (int i = 0; i < fLine.length; i++) {
			if (fLine[i].equals("code")) {
				nbCode = i;
			}
			if (fLine[i].equals("nb_housingUnit")) {
				nbHU = i;
			}
		}
		for (String[] line : read.readAll()) {
			if (line[nbCode].equals(insee)) {
				result = Integer.valueOf(line[nbHU]);
			}
		}
		read.close();
		if (result == -1) {
			System.out.println("beware : estimation from getEstimationForACity() has not been found");
		}
		return result;
	}

	/**
	 * get the initial density. If it's not in the corresponding shapefile, it calculates it from a building estimation
	 * 
	 * @param buildingFile
	 * @param parcelsFile
	 * @param initialDensities
	 * @param code
	 * @return
	 * @throws Exception
	 */
	public static double getInitialNetDensity(File buildingFile, File parcelsFile, File communitiesFile, String code) throws Exception {
		if (code.equals("ALLLL")) {
			System.out.println("not concerned");
			return 0;
		}

		// if this value has already been calculated (for all of them scenarios and vairants - it could tho depends on the scenario parameters for the housing unit estimation
		ShapefileDataStore commSDS = new ShapefileDataStore(communitiesFile.toURI().toURL());
		SimpleFeatureIterator commIt = commSDS.getFeatureSource().getFeatures().features();
		double iniDens = 0.0;
		try {
			while (commIt.hasNext()) {
				SimpleFeature feat = commIt.next();
				if (feat.getAttribute("DEPCOM").equals(code)) {
					iniDens = (Double) feat.getAttribute("densIni");
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			commIt.close();
		}

		if (iniDens == 0.0) {
			System.out.println("densIni's missing");
			System.out.println(
					"compute it with the createGeom.Density.createCommunitiesWithInitialNetDensity() class and add it to the community shapefile");
		}
		commSDS.dispose();
		return iniDens;

	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlinePartCsv() {
		return housingUnitFirstLine;
	}

	public void makeGenStat() throws Exception {
		makeGenStat("ALLLL");
	}

	public void makeGenStat(String code) throws Exception {
		System.out.println("city " + code);
		String insee = code.substring(0, 5);
		makeGenStat(insee, "codeParcel");

		double diffDens = objDens - averageDensiteHU;
		double diffObj = objHU - nbHU;

		// put all that into csv
		String line = insee + "," + round(sDPtot / 1000000, 6) + "," + round(empriseTot / 1000000, 6) + "," + averageDensiteHU + ","
				+ standDevDensiteHU + "," + averageDensiteSDP + "," + standDevDensiteSDP + "," + averageDensiteEmprise + "," + +standDevDensiteEmprise
				+ "," + objDens + "," + diffDens + "," + averageSDPpHU + "," + standDevSDPpHU + "," + nbBuildings + "," + nbHU + "," + objHU + ","
				+ diffObj + "," + nbDetachedHouse + "," + nbSmallHouse + "," + nbMultifamilyHouse + "," + nbSmallBlockFlat + "," + nbMidBlockFlat
				+ "," + nbU + "," + nbAU + "," + nbNC + "," + nbCentre + "," + nbBanlieue + "," + nbPeriUrbain + "," + nbRural + ","
				+ nbBuildDetachedHouse + "," + nbBuildSmallHouse + "," + nbBuildMultifamilyHouse + "," + nbBuildSmallBlockFlat + ","
				+ nbBuildMidBlockFlat + "," + nbBuildU + "," + nbBuildAU + "," + nbBuildNC + "," + nbBuildCentre + "," + nbBuildBanlieue + ","
				+ nbBuildPeriUrbain + "," + nbBuildRural + "," + averageEval + "," + standDevEval + "," + ratioHUcol + "," + ratioHUind;

		toGenCSV("genStat", genStatFirstLine, line);

	}

	public void makeArmatureStat(String code) throws Exception {
		makeGenStat(code, "armature");
		double diffDens = objDens - averageDensiteHU;
		double diffObj = objHU - nbHU;

		// put all that into csv
		String line = code + "," + round(sDPtot / 1000000, 6) + "," + round(empriseTot / 1000000, 6) + "," + averageDensiteHU + ","
				+ standDevDensiteHU + "," + averageDensiteSDP + "," + standDevDensiteSDP + "," + averageDensiteEmprise + "," + +standDevDensiteEmprise
				+ "," + objDens + "," + diffDens + "," + averageSDPpHU + "," + standDevSDPpHU + "," + nbBuildings + "," + nbHU + "," + objHU + ","
				+ diffObj + "," + nbDetachedHouse + "," + nbSmallHouse + "," + nbMultifamilyHouse + "," + nbSmallBlockFlat + "," + nbMidBlockFlat
				+ "," + nbU + "," + nbAU + "," + nbNC + "," + nbCentre + "," + nbBanlieue + "," + nbPeriUrbain + "," + nbRural + ","
				+ nbBuildDetachedHouse + "," + nbBuildSmallHouse + "," + nbBuildMultifamilyHouse + "," + nbBuildSmallBlockFlat + ","
				+ nbBuildMidBlockFlat + "," + nbBuildU + "," + nbBuildAU + "," + nbBuildNC + "," + nbBuildCentre + "," + nbBuildBanlieue + ","
				+ nbBuildPeriUrbain + "," + nbBuildRural + "," + averageEval + "," + standDevEval + "," + ratioHUcol + "," + ratioHUind;

		toGenCSV("armatureStat", genStatFirstLine, line);
	}

	public void makeGenStat(String code, String codeMainElement) throws Exception {
		setCountToZero();
		CSVReader stat = new CSVReader(new FileReader(new File(getIndicFolder(), "housingUnits.csv")), ',', '\0');
		String[] firstLine = stat.readNext();
		DescriptiveStatistics densityHUStat = new DescriptiveStatistics();
		DescriptiveStatistics densitySDPStat = new DescriptiveStatistics();
		DescriptiveStatistics densityEmpriseStat = new DescriptiveStatistics();
		DescriptiveStatistics sDPPerHUStat = new DescriptiveStatistics();
		DescriptiveStatistics evalStat = new DescriptiveStatistics();

		int codeParcelP = 0, sdpP = 0, empriseP = 0, nbHousingUnitP = 0, typeHUP = 0, zoneP = 0, hUpHectareDensityP = 0, typoP = 0,
				sDPpHectareDensityP = 0, emprisepHectareDensityP = 0, evalP = 0;
		for (int i = 0; i < firstLine.length; i++) {
			if (firstLine[i].equals(codeMainElement)) {
				codeParcelP = i;
				continue;
			} else {
				switch (firstLine[i]) {
				case "SDP":
					sdpP = i;
					break;
				case "emprise":
					empriseP = i;
					break;
				case "nb_housingUnit":
					nbHousingUnitP = i;
					break;
				case "type_HU":
					typeHUP = i;
					break;
				case "typo_HU":
					typoP = i;
					break;
				case "zone":
					zoneP = i;
					break;
				case "HUpHectareDensity":
					hUpHectareDensityP = i;
					break;
				case "SDPpHectareDensity":
					sDPpHectareDensityP = i;
					break;
				case "EmprisepHectareDensity":
					emprisepHectareDensityP = i;
					break;
				case "eval":
					evalP = i;
					break;
				}
			}
		}

		for (String[] l : stat.readAll()) {
			if (l[codeParcelP].startsWith(code) || code.equals("ALLLL")) {
				sDPtot = sDPtot + Double.valueOf(l[sdpP]);
				sDPPerHUStat.addValue(Double.valueOf(l[sdpP]) / Integer.valueOf(l[nbHousingUnitP]));
				nbHU = nbHU + Integer.valueOf(l[nbHousingUnitP]);
				nbBuildings++;
				densityHUStat.addValue(Double.valueOf(l[hUpHectareDensityP]));
				densitySDPStat.addValue(Double.valueOf(l[sDPpHectareDensityP]));
				densityEmpriseStat.addValue(Double.valueOf(l[emprisepHectareDensityP]));
				evalStat.addValue(Double.valueOf(l[evalP]));
				empriseTot = empriseTot + Double.valueOf(l[empriseP]);

				// typo
				switch (l[typoP]) {
				case "centre":
					nbCentre = nbCentre + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildCentre++;
					break;
				case "banlieue":
					nbBanlieue = nbBanlieue + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildBanlieue++;
					break;
				case "periUrbain":
					nbPeriUrbain = nbPeriUrbain + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildPeriUrbain++;
					break;
				case "rural":
					nbRural = nbRural + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildRural++;
					break;
				}
				// zone
				String mainZone = "";
				if (l[zoneP].contains("+")) {
					mainZone = l[zoneP].split("\\+")[0];
				} else {
					mainZone = l[zoneP];
				}

				if (mainZone.equals("U") || mainZone.equals("ZC")) {
					nbU = nbU + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildU++;
				} else if (mainZone.equals("AU")) {
					nbAU = nbAU + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildAU++;
				} else {
					nbNC = nbNC + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildNC++;
				}

				// buildingType
				switch (l[typeHUP]) {
				case "DETACHEDHOUSE":
					nbDetachedHouse = nbDetachedHouse + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildDetachedHouse++;
					break;
				case "SMALLHOUSE":
					nbSmallHouse = nbSmallHouse + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildSmallHouse++;
					break;
				case "MULTIFAMILYHOUSE":
					nbMultifamilyHouse = nbMultifamilyHouse + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildMultifamilyHouse++;
					break;
				case "SMALLBLOCKFLAT":
					nbSmallBlockFlat = nbSmallBlockFlat + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildSmallBlockFlat++;
					break;
				case "MIDBLOCKFLAT":
					nbMidBlockFlat = nbMidBlockFlat + Integer.valueOf(l[nbHousingUnitP]);
					nbBuildMidBlockFlat++;
					break;
				}
			}
		}

		stat.close();

		// ratio collective and individual buildings
		try {
			int col = (nbMidBlockFlat + nbSmallBlockFlat + nbMultifamilyHouse);
			int indiv = (nbSmallHouse + nbDetachedHouse);
			int tot = (nbMidBlockFlat + nbSmallBlockFlat + nbMultifamilyHouse + nbSmallHouse + nbDetachedHouse);
			ratioHUcol = ((double) col) / ((double) tot);
			ratioHUind = ((double) indiv) / ((double) tot);
		} catch (ArithmeticException divZero) {
			ratioHUcol = -1;
			ratioHUind = -1;
		}
		// densities stats
		averageDensiteHU = densityHUStat.getMean();
		standDevDensiteHU = densityHUStat.getStandardDeviation();
		averageDensiteSDP = densitySDPStat.getMean();
		standDevDensiteSDP = densitySDPStat.getStandardDeviation();
		averageDensiteEmprise = densityEmpriseStat.getMean();
		standDevDensiteEmprise = densityEmpriseStat.getStandardDeviation();
		averageSDPpHU = sDPPerHUStat.getMean();
		standDevSDPpHU = sDPPerHUStat.getStandardDeviation();
		averageEval = evalStat.getMean();
		standDevEval = evalStat.getStandardDeviation();

		// relations with the region objectives
		objDens = SimuTool.getDensityGoal(new File(getRootFile(), "dataGeo"), code);
		objHU = SimuTool.getHousingUnitsGoal(new File(getRootFile(), "dataGeo"), code);
		if (code.equals("ALLLL")) {
			// calculating a sum of the objectives
			List<String> listInsee = FromGeom.getInsee(new File(getRootFile(), "/dataGeo/communities.shp"), "DEPCOM");
			for (String in : listInsee) {
				objHU = objHU + SimuTool.getDensityGoal(new File(getRootFile(), "dataGeo"), in);
			}
		}

	}

	public int simpleDistributionEstimate(SimpleFeatureCollection collec) throws Exception {
		SimpleFeatureIterator it = collec.features();
		List<String> buildingCode = new ArrayList<String>();
		while (it.hasNext()) {
			SimpleFeature ftBati = it.next();
			if (!buildingCode.contains((String) ftBati.getAttribute("CODE"))) {
				BuildingType type = BuildingType.valueOf((String) ftBati.getAttribute("BUILDTYPE"));
				HashMap<String, HashMap<String, Integer>> repartition;
				// for a single house, there's only a single housing unit
				switch (type) {
				case DETACHEDHOUSE:
				case SMALLHOUSE:
					nbHU = 1;
					// System.out.println("le batiment" + type + " de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");
					break;
				// for collective buildings
				default:
					repartition = makeCollectiveHousingRepartition(ftBati, type, getParamFolder());
					nbHU = repartition.get("carac").get("totHU");
				}
			}
		}
		return nbHU;
	}

	public int distributionEstimate() throws IOException, NoSuchAuthorityCodeException, FactoryException {
		return distributionEstimate(SimuTool.giveEvalToBuilding(getSimPLUDepotGenFile(), getMupOutputFile()));
	}

	public int distributionEstimate(SimpleFeatureCollection collec) throws IOException {
		ShapefileDataStore citiesSds = new ShapefileDataStore(FromGeom.getCommunities(new File(getRootFile(), "dataGeo")).toURI().toURL());
		SimpleFeatureIterator it = collec.features();
		List<String> buildingCode = new ArrayList<String>();
		try {
			while (it.hasNext()) {
				SimpleFeature ftBati = it.next();
				String code = (String) ftBati.getAttribute("CODE");
				if (!buildingCode.contains(code)) {
					double eval = (double) ftBati.getAttribute("EVAL");
					if (eval == 0.0) {
						eval = 0.001;
					}
					// typo of the zone
					String typo = ParcelState.parcelInTypo(FromGeom.getCommunitiesIris(new File(getRootFile(), "dataGeo")), ftBati);

					String armature = ParcelAttribute.getCommunityTypeFromSFC(
							Collec.snapDatas(citiesSds.getFeatureSource().getFeatures(), (Geometry) ftBati.getDefaultGeometry()), ftBati);
					BuildingType type = BuildingType.valueOf((String) ftBati.getAttribute("BUILDTYPE"));
					boolean collectiveHousing = false;
					double sDPLgt = (double) ftBati.getAttribute("SDPShon");
					HashMap<String, HashMap<String, Integer>> repartition;
					// for a single house, there's only a single housing unit
					switch (type) {
					case DETACHEDHOUSE:
					case SMALLHOUSE:
						nbHU = 1;
						averageSDPpHU = sDPLgt / nbHU;
						// System.out.println("le batiment" + type + " de la parcelle " + numeroParcel + " fait " + surfaceLgt + " mcarré ");
						break;
					// for collective buildings
					default:
						collectiveHousing = true;
						repartition = makeCollectiveHousingRepartition(ftBati, type, getParamFolder());
						nbHU = repartition.get("carac").get("totHU");
						averageSDPpHU = sDPLgt / nbHU;
					}
					numeroParcel = code;

					averageDensiteHU = nbHU / ((double) ftBati.getAttribute("SurfacePar") / 10000);
					averageDensiteSDP = sDPLgt / ((double) ftBati.getAttribute("SurfacePar") / 10000);
					averageDensiteEmprise = (double) ftBati.getAttribute("SurfaceSol") / ((double) ftBati.getAttribute("SurfacePar") / 10000);

					// System.out.println("on peux ici construire " + nbHU + " logements à une densité de " + averageDensite);
					if (collectiveHousing) {
						String lineParticular = numeroParcel + "," + armature + "," + sDPLgt + "," + ftBati.getAttribute("SurfaceSol") + ","
								+ String.valueOf(nbHU) + "," + type + "," + ftBati.getAttribute("TYPEZONE") + "," + typo + ","
								+ String.valueOf(averageSDPpHU) + "," + String.valueOf(averageDensiteHU) + "," + String.valueOf(averageDensiteSDP)
								+ "," + String.valueOf(averageDensiteEmprise) + "," + String.valueOf(eval);

						toParticularCSV(getIndicFolder(), "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
					} else {
						String lineParticular = numeroParcel + "," + armature + "," + sDPLgt + "," + ftBati.getAttribute("SurfaceSol") + ","
								+ String.valueOf(nbHU) + "," + type + "," + ftBati.getAttribute("TYPEZONE") + "," + typo + ","
								+ String.valueOf(averageSDPpHU) + "," + String.valueOf(averageDensiteHU) + "," + String.valueOf(averageDensiteSDP)
								+ "," + String.valueOf(averageDensiteEmprise) + "," + String.valueOf(eval);

						toParticularCSV(getIndicFolder(), "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
					}
					// System.out.println("");

					buildingCode.add(code);
				}
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		citiesSds.dispose();
		return nbHU;
	}

	/**
	 * Basic method to estimate the number of households that can fit into a set of cuboid known as a building The total area of ground is predefined in the class SimPLUSimulator
	 * and calculated with the object SDPCalc. This calculation estimates 3meters needed for one floor. It's the same for all the boxes; so it should be taken only once.
	 *
	 * @param f
	 *            : direction to the shapefile of the building
	 * @return : number of households
	 * @throws IOException
	 */
	public static int simpleEstimate(SimpleFeatureCollection batiSFC, double surfaceLgtDefault, double heightStorey) throws IOException {

		int nbHousingUnit = 0;
		SimpleFeatureIterator batiIt = batiSFC.features();
		try {
			while (batiIt.hasNext()) {
				SimpleFeature build = batiIt.next();
				double stairs = 0;
				try {
					stairs = Math.round((((Integer) build.getAttribute("HAUTEUR") / heightStorey)));
				} catch (NullPointerException np) {
					stairs = Math.round((((Double) build.getAttribute("Hauteur") / heightStorey)));
				}
				// lot of houses - we trim the last stairs
				if (stairs > 1) {
					stairs = stairs - 0.5;
				}
				double sdp = ((Geometry) build.getDefaultGeometry()).getArea() * stairs;

				nbHousingUnit = nbHousingUnit + (int) Math.round((sdp / surfaceLgtDefault));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			batiIt.close();
		}
		return nbHousingUnit;
	}

	/**
	 * 
	 * @param bati
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, HashMap<String, Integer>> makeCollectiveHousingRepartition(SimpleFeature bati, BuildingType type, File paramFolder)
			throws Exception {

		int antiInfinity = 0;
		SimpluParametersJSON pType = RepartitionBuildingType.getParamBuildingType(new File(paramFolder, "profileBuildingType"), type);

		int minLgt = pType.getInteger("minHousingUnit");

		// each key means a different repartition of housing unit type per building
		HashMap<String, HashMap<String, Integer>> result = new HashMap<String, HashMap<String, Integer>>();

		Double sizeSmallDwellingMin = Double.valueOf(pType.getString("sizeSmallDwelling").split("-")[0]);
		Double sizeSmallDwellingMax = Double.valueOf(pType.getString("sizeSmallDwelling").split("-")[1]);
		Double sizeMidDwellingMin = Double.valueOf(pType.getString("sizeMidDwelling").split("-")[0]);
		Double sizeMidDwellingMax = Double.valueOf(pType.getString("sizeMidDwelling").split("-")[1]);
		Double sizeLargeDwellingMin = Double.valueOf(pType.getString("sizeLargeDwelling").split("-")[0]);
		Double sizeLargeDwellingMax = Double.valueOf(pType.getString("sizeLargeDwelling").split("-")[1]);
		Double freqSmallDwelling = pType.getDouble("freqSmallDwelling");
		Double freqMidDwelling = pType.getDouble("freqMidDwelling");
		Double freqLargeDwelling = pType.getDouble("freqLargeDwelling");

		if (!((freqSmallDwelling + freqMidDwelling + freqLargeDwelling) == 1)) {
			System.out.println("problem in the sum of housing unit frequencies");
		}

		double totSDP = (double) bati.getAttribute("SDPShon");
		// percentage of the building that will be for common spaces (not for multifamily houses)
		if (type != BuildingType.MULTIFAMILYHOUSE) {
			totSDP = 0.9 * totSDP;
		}
		boolean doRepart = true;

		while (doRepart) {

			int nbLgtFinal = 0;
			HashMap<String, Integer> smallHU = new HashMap<String, Integer>();
			HashMap<String, Integer> midHU = new HashMap<String, Integer>();
			HashMap<String, Integer> largeHU = new HashMap<String, Integer>();
			double leftSDP = totSDP;
			// System.out.println("this one is " + totSDP);
			boolean enoughSpace = true;
			while (enoughSpace) {
				// ponderated randomness
				double rd = Math.random();
				if (rd < freqSmallDwelling) {
					// this is a small house

					// System.out.println("small dwelling");
					// HashMap<String, Integer> smallHUTemp = smallHU;
					Object[] repart = doDwellingRepart(smallHU, leftSDP, sizeSmallDwellingMax, sizeSmallDwellingMin);
					smallHU = (HashMap<String, Integer>) repart[0];
					leftSDP = (double) repart[1];
					boolean conti = (boolean) repart[2];
					// if nothing has changed, it's time to end that
					if (!conti) {
						enoughSpace = false;
					} else {
						nbLgtFinal++;
					}
				} else if (rd < (freqSmallDwelling + freqMidDwelling)) {
					// this is a medium house
					// System.out.println("mid dwelling");
					// HashMap<String, Integer> midHUTemp = midHU;
					Object[] repart = doDwellingRepart(midHU, leftSDP, sizeMidDwellingMax, sizeMidDwellingMin);
					midHU = (HashMap<String, Integer>) repart[0];
					leftSDP = (double) repart[1];
					// if nothing has changed, it's time to end that
					if (!(boolean) repart[2]) {
						enoughSpace = false;
						// System.out.println("same size");
					} else {
						nbLgtFinal++;
					}
				} else {
					// this is a large house
					// System.out.println("large dwelling");
					// HashMap<String, Integer> largeHUTemp = largeHU;
					Object[] repart = doDwellingRepart(largeHU, leftSDP, sizeLargeDwellingMax, sizeLargeDwellingMin);
					largeHU = (HashMap<String, Integer>) repart[0];
					leftSDP = (double) repart[1];
					boolean conti = (boolean) repart[2];
					// if nothing has changed, it's time to end that
					if (!conti) {
						enoughSpace = false;
					} else {
						nbLgtFinal++;
					}
				}
				// System.out.println("nbLgtFinal : " + nbLgtFinal);

			}
			// if the limit of minimum housing units is outpassed
			// System.out.println("minLgt : " + minLgt + " contre " + nbLgtFinal);
			if (nbLgtFinal >= minLgt || antiInfinity > 42) {
				// System.out.println("it's enough");
				doRepart = false;
				result.put("smallHU", smallHU);
				result.put("midHU", midHU);
				result.put("largeHU", largeHU);
				HashMap<String, Integer> carac = new HashMap<String, Integer>();
				carac.put("totHU", nbLgtFinal);
				result.put("carac", carac);
				if (antiInfinity > 42) {
					System.err.println("too much loops on building estimation - this shouldn't happend");
					System.err.println("building " + bati.getAttribute("CODE"));
				}

			} else {
				antiInfinity++;
				// System.out.println("it's not enough");
			}
		}
		return result;
	}

	// private int totLgt(HashMap<String, Integer> hu) {
	// int result = 0;
	// for (String key : hu.keySet()) {
	// result = result + hu.get(key);
	// }
	// return result;
	// }

	/**
	 * the returned object is composed of 0: the collection 1: the left sdp
	 * 
	 * @param smallHU
	 * @param leftSDP
	 * @param sizeSmallDwellingMax
	 * @param sizeSmallDwellingMin
	 * @return
	 */
	private Object[] doDwellingRepart(HashMap<String, Integer> smallHU, double leftSDP, double sizeSmallDwellingMax, double sizeSmallDwellingMin) {
		Object[] result = new Object[3];
		boolean conti = true;
		Random rand = new Random();
		//// look at the left space
		// not enough room
		if (leftSDP - sizeSmallDwellingMin < 0) {
			// System.out.println("not enough space yee over");
			conti = false;
		}
		// this is a minimum construction type of situation
		else if (leftSDP - sizeSmallDwellingMax < 0) {
			// Housing Unit is at the minimum size
			Double sdp = sizeSmallDwellingMin;
			leftSDP = leftSDP - sdp;
			// put in collec
			if (smallHU.containsKey(String.valueOf(sdp))) {
				smallHU.put(String.valueOf(sdp), smallHU.get(String.valueOf(sdp)) + 1);
			} else {
				smallHU.put(String.valueOf(sdp), 1);
			}
			// System.out.println("new HU of " + sdp + "m2 - sdp left : " + leftSDP);
			// System.out.println("this is the last one");
		}
		// nothing to declare
		else {
			// we chose a random range
			int range = rand.nextInt(((int) (sizeSmallDwellingMax - sizeSmallDwellingMin) / 5) + 1);
			Double sdp = (double) (range * 5) + sizeSmallDwellingMin;
			leftSDP = leftSDP - sdp;
			// put in collec
			if (smallHU.containsKey(String.valueOf(sdp))) {
				smallHU.put(String.valueOf(sdp), smallHU.get(String.valueOf(sdp)) + 1);
			} else {
				smallHU.put(String.valueOf(sdp), 1);
			}
			// System.out.println("new HU of " + sdp + "m2 - sdp left : " + leftSDP);
		}
		result[0] = smallHU;
		result[1] = leftSDP;
		result[2] = conti;
		return result;
	}

	public File joinStatBTHtoParcels(String nameFileToJoin) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesOGSDS = new ShapefileDataStore(getParcelDepotGenFile().toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesOGSDS.getFeatureSource().getFeatures();
		File result = joinStatToBTHParcel(communitiesOG, new File(getIndicFolder(), nameFileToJoin), new File(getIndicFolder(), "parcStat.shp"));
		communitiesOGSDS.dispose();
		return result;
	}

	public File joinStatToBTHParcel(SimpleFeatureCollection collec, File statFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("parcels");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("codeParcel", String.class);
		sfTypeBuilder.add("densHUpHe", Double.class);
		sfTypeBuilder.add("densSDPpHe", Double.class);
		sfTypeBuilder.add("densEmppHe", Double.class);
		sfTypeBuilder.add("eval", Double.class);

		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		int codeParcelP = 0, densHUpHeP = 0, densSDPpHeP = 0, densEmppHeP = 0, evalP = 0;
		CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
		String[] firstLine = stat.readNext();
		for (int i = 0; i < firstLine.length; i++) {
			switch (firstLine[i]) {
			case "codeParcel":
				codeParcelP = i;
				break;
			case "HUpHectareDensity":
				densHUpHeP = i;
				break;
			case "SDPpHectareDensity":
				densSDPpHeP = i;
				break;
			case "EmprisepHectareDensity":
				densEmppHeP = i;
				break;
			case "eval":
				evalP = i;
				break;
			}
		}
		List<String[]> totLines = stat.readAll();
		stat.close();
		// set first names
		try {
			while (it.hasNext()) {
				SimpleFeature featCity = it.next();
				String insee = (String) featCity.getAttribute("CODE");
				for (String[] l : totLines) {
					if (l[codeParcelP].equals(insee)) {
						builder.set("the_geom", featCity.getDefaultGeometry());
						builder.set("INSEE", l[codeParcelP].substring(0, 5));
						builder.set("codeParcel", l[codeParcelP]);
						builder.set("densHUpHe", Double.valueOf(l[densHUpHeP]));
						builder.set("densSDPpHe", Double.valueOf(l[densSDPpHeP]));
						builder.set("densEmppHe", Double.valueOf(l[densEmppHeP]));
						builder.set("eval", Double.valueOf(l[evalP]));
						result.add(builder.buildFeature(null));
						break;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		return Collec.exportSFC(result, outFile);
	}

	public void setCountToZero() {
		nbBuildings = nbHU = nbDetachedHouse = nbSmallHouse = nbMultifamilyHouse = nbSmallBlockFlat = objHU = diffHU = nbMidBlockFlat = nbU = nbAU = nbNC = nbCentre = nbBanlieue = nbPeriUrbain = nbRural = nbBuildDetachedHouse = nbBuildSmallHouse = nbBuildMultifamilyHouse = nbBuildSmallBlockFlat = nbBuildMidBlockFlat = nbBuildU = nbBuildAU = nbBuildNC = nbBuildCentre = nbBuildBanlieue = nbBuildPeriUrbain = nbBuildRural = 0;
		ratioHUcol = ratioHUind = averageEval = standDevEval = sDPtot = empriseTot = averageSDPpHU = standDevSDPpHU = averageDensiteHU = standDevDensiteHU = averageDensiteSDP = standDevDensiteSDP = averageDensiteEmprise = standDevDensiteEmprise = objDens = diffDens = 0.0;
	}

	public static String getIndicName() {
		return indicName;
	}

	public static void setIndicName(String indicName) {
		BuildingToHousingUnit.indicName = indicName;
	}
}
