package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.DataUtilities;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.createGeom.DensIni;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.DensIniNewComp;
import fr.ign.cogit.map.theseMC.DiffObjDensMap;
import fr.ign.cogit.map.theseMC.DiffObjLgtMap;
import fr.ign.cogit.map.theseMC.nbHU.NbHU;
import fr.ign.cogit.map.theseMC.nbHU.NbHUAU;
import fr.ign.cogit.map.theseMC.nbHU.NbHUDetachedHouse;
import fr.ign.cogit.map.theseMC.nbHU.NbHUMidBlock;
import fr.ign.cogit.map.theseMC.nbHU.NbHUMultiFamilyHouse;
import fr.ign.cogit.map.theseMC.nbHU.NbHUSmallBlock;
import fr.ign.cogit.map.theseMC.nbHU.NbHUSmallHouse;
import fr.ign.cogit.map.theseMC.nbHU.NbHUU;
import fr.ign.cogit.map.theseMC.parcelMaps.ParcelleDensEmprisepHec;
import fr.ign.cogit.map.theseMC.parcelMaps.ParcelleDensHUpHec;
import fr.ign.cogit.map.theseMC.parcelMaps.ParcelleDensSDPpHec;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;
import fr.ign.cogit.util.SimuTool;

public class BuildingToHousingUnit extends Indicators {

	// infos about the buildings
	int nbBuildings, nbHU, nbStairs, nbDetachedHouse, nbSmallHouse, nbMultifamilyHouse, nbSmallBlockFlat, nbMidBlockFlat, nbU, nbAU, nbNC, nbCentre,
			nbBanlieue, nbPeriUrbain, nbRural, objHU, diffHU;
	double sDPtot, empriseTot, averageDensiteHU, averageDensiteSDP, standDevDensiteSDP, averageDensiteEmprise, standDevDensiteEmprise,
			standDevDensiteHU, objDens, diffDens, averageSDPpHU, standDevSDPpHU;
	String housingUnitFirstLine, genFirstLine, genStatFirstLine, numeroParcel;
	File tmpFile;
	static String indicName = "bTH";

	public BuildingToHousingUnit(File rootFile, SimpluParametersJSON par, String scenarName, String variantName) throws Exception {
		super(par, rootFile, scenarName, variantName, indicName);

		tmpFile = new File(getIndicFile(), "tmpFile");
		tmpFile.mkdirs();

		housingUnitFirstLine = "codeParcel," + "SDP," + "emprise," + "nb_housingUnit," + "type_HU," + "zone," + "typo_HU," + "averageSDPPerHU,"
				+ "HUpHectareDensity," + "SDPpHectareDensity," + "EmprisepHectareDensity";

		genStatFirstLine = "code," + "SDPTot," + "empriseTot," + "initial_densite," + "average_densiteHU," + "standardDev_densiteHU,"
				+ "average_densiteSDP," + "standardDev_densiteSDP," + "average_densiteEmprise," + "standardDev_densiteEmprise,"
				+ "objectifSCOT_densite," + "diff_objectifSCOT_densite," + "average_SDP_per_HU," + "standardDev_SDP_per_HU," + "nb_building,"
				+ "nb_housingUnit," + "objectifPLH_housingUnit," + "diff_objectifPLH_housingUnit," + "nbHU_detachedHouse," + "nbHU_smallHouse,"
				+ "nbHU_multiFamilyHouse," + "nbHU_smallBlockFlat," + "nbHU_midBlockFlat," + "nbHU_U," + "nbHU_AU," + "nbHU_NC," + "nbHU_centre,"
				+ "nbHU_banlieue," + "nbHU_periUrbain," + "nbHU_rural";
	}

	public BuildingToHousingUnit(File batiFolder, File paramFile, SimpluParametersJSON par) throws Exception {
		super(par, batiFolder, "", "", indicName);
		setParamFolder(paramFile);
	}

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result2903/");
		File rootParam = new File(rootFile, "paramFolder");
		String scenario = "CDense";
		String variant = "variantMvData1";
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);
		// for (File f : (new File(rootFile, "SimPLUDepot/" + scenario + "/")).listFiles()) {
		// BuildingToHousingUnit bhtU = new BuildingToHousingUnit(rootFile, p, scenario, f.getName());
		BuildingToHousingUnit bhtU = new BuildingToHousingUnit(rootFile, p, scenario, variant);
		// bhtU.distributionEstimate();
		// bhtU.makeGenStat();
		// bhtU.setCountToZero();
		//
		// // for every cities
		// List<String> listInsee = FromGeom.getInsee(new File(bhtU.getRootFile(), "/dataGeo/old/communities.shp"), "DEPCOM");
		// for (String city : listInsee) {
		// bhtU.makeGenStat(city);
		// bhtU.setCountToZero();
		// }
		File parcelleStatFile = bhtU.joinStatoBTHParcels("housingUnits.csv");
		bhtU.createGraphDensity(new File(bhtU.getIndicFile(), "housingUnits.csv"));
		File commStatFile = bhtU.joinStatoBTHCommunities("genStat.csv");
		allOfTheMap(bhtU, commStatFile, parcelleStatFile);

		// bhtU.createGraphCount(new File(bhtU.indicFile, "genStat.csv"));
	}
	// }

	public File createNewDensity(File lgtCountFile, File fileToAddInitialDensity, File zoningFile, File fileOut, String nameFieldLgt,
			String nameFieldCodeCsv) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		// sum the total housing units estimated for each communities
		File tmpCount = new File(tmpFile, "newHUCount.csv");
		CSVWriter csvTmp = new CSVWriter(new FileWriter(tmpCount), ',', '\0');
		String[] fL = { nameFieldCodeCsv, nameFieldLgt };
		csvTmp.writeNext(fL);

		File simuStat = new File(getIndicFile(), "genStat.csv");
		if (!simuStat.exists()) {
			System.out.println("run getEstimationForACity first");
			System.exit(-1);
		}

		CSVReader csvRlgtCount = new CSVReader(new FileReader(lgtCountFile));

		// first line of the initial household counts and set the positions of the interestings fields
		int codeP = 0;
		int lgtP = 0;

		String[] fLine = csvRlgtCount.readNext();
		for (int i = 0; i < fLine.length; i++) {
			String s = fLine[i];
			if (s.equals(nameFieldLgt)) {
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
			if (insee.equals("ALLLL"))
				continue;
			csvRlgtCount = new CSVReader(new FileReader(lgtCountFile));
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

		return DensIni.createCommunitiesWithNewDensity(nameFieldLgt, nameFieldCodeCsv, "DEPCOM", zoningFile, tmpCount, fileToAddInitialDensity,
				fileOut);
	}

	public void createGraphDensity(File distrib) throws IOException {
		makeGraphDens(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, "HUpHectareDensity",
				"densité de logements par hectare", "nombre de parcelle", 0, 100);
		makeGraphDens(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, "SDPpHectareDensity",
				"densité de surface de plancher des bâtiments par hectare", "nombre de parcelle", 0, 6000);
		makeGraphDens(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, "EmprisepHectareDensity",
				"densité de l'emprise des bâtiments par hectare", "nombre de parcelle", 0, 6000);
	}

	public void createGraphCount(File distrib) throws IOException {
		String[] xType = { "nbHU_detachedHouse", "nbHU_smallHouse", "nbHU_multiFamilyHouse", "nbHU_smallBlockFlat", "nbHU_midBlockFlat" };
		makeGraph(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, xType, "Type de bâtiment",
				"Nombre de logements simulés");
		String[] xTypo = { "nbHU_rural", "nbHU_periUrbain", "nbHU_banlieue", "nbHU_centre" };
		makeGraph(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, xTypo, "Typologie des communes",
				"Nombre de logements simulés");
		String[] xZone = { "nbHU_U", "nbHU_AU", "nbHU_NC" };
		makeGraph(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, xZone, "Type de zonage",
				"Nombre de logements simulés");
		String[] xConso = { "SDPTot", "empriseTot" };
		makeGraph(distrib, getGraphDepotFile(), "Scenario : " + scenarName + " - Variante : " + variantName, xConso,
				"Consommation surfacique des bâtiments simulés", "Surface (em km²)");
	}

	public static void makeGraphDens(File csv, File graphDepotFile, String title, String x, String xTitle, String yTitle, int xMin, int xMax)
			throws IOException {
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

		Histogram histo = new Histogram(values, 20, xMin, xMax);
		CategoryChart chart = new CategoryChartBuilder().width(800).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
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

	public static void allOfTheMap(BuildingToHousingUnit bhtU, File commStatFile, File parcelStatFile)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();

		// buildings maps
		MapRenderer diffObjDensMap = new DiffObjDensMap(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(diffObjDensMap);
		MapRenderer diffObjLgtMap = new DiffObjLgtMap(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(diffObjLgtMap);
		MapRenderer nbHU = new NbHU(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHU);
		MapRenderer nbHUDetachedHouse = new NbHUDetachedHouse(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUDetachedHouse);
		MapRenderer nbHUMidBlock = new NbHUMidBlock(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUMidBlock);
		MapRenderer nbHUSmallBlock = new NbHUSmallBlock(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUSmallBlock);
		MapRenderer nbHUSmallHouse = new NbHUSmallHouse(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUSmallHouse);
		MapRenderer nbHUMultiFamilyHouse = new NbHUMultiFamilyHouse(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUMultiFamilyHouse);
		MapRenderer nbHUU = new NbHUU(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUU);
		MapRenderer nbHUAU = new NbHUAU(1000, 1000, bhtU.getMapStyle(), commStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(nbHUAU);

		// parcels maps
		MapRenderer dHuHec = new ParcelleDensHUpHec(1000, 1000, bhtU.getMapStyle(), parcelStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(dHuHec);
		MapRenderer dSDPHec = new ParcelleDensSDPpHec(1000, 1000, bhtU.getMapStyle(), parcelStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(dSDPHec);
		MapRenderer dEmpHec = new ParcelleDensEmprisepHec(1000, 1000, bhtU.getMapStyle(), parcelStatFile, bhtU.getMapDepotFile());
		allOfTheMaps.add(dEmpHec);

		for (MapRenderer map : allOfTheMaps) {
			map.renderCityInfo();
			map.generateSVG();
		}

		File file1 = bhtU.createNewDensity(new File(bhtU.getRootFile(), "dataGeo/base-ic-logement-2012.csv"),
				new File(bhtU.getRootFile(), "dataGeo/old/communities.shp"), new File(bhtU.getRootFile(), "dataRegulation/zoning.shp"),
				new File(bhtU.getMapDepotFile(), "commNewDens.shp"), "P12_LOG", "COM");

		DensIniNewComp map = new DensIniNewComp(1000, 1000, bhtU.getMapStyle(), file1, bhtU.getMapDepotFile());
		map.renderCityInfo();
		map.generateSVG();

		map.makeIniMap(file1);
		map.renderCityInfo("DensIni");
		map.generateSVG(new File(bhtU.getMapDepotFile(), "DensIni.svg"), "DensIni");

		map.makeObjMap(file1);
		map.renderCityInfo("DensObj");
		map.generateSVG(new File(bhtU.getMapDepotFile(), "DensObj.svg"), "DensObj");

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
	public static double existingBuildingDensity(File buildingFile, File parcelsFile, File communitiesFile, String code) throws Exception {
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
			ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelsFile.toURI().toURL());
			SimpleFeatureCollection parcelSFC = DataUtilities
					.collection(ParcelFonction.getParcelByZip(parcelSDS.getFeatureSource().getFeatures(), code));
			ShapefileDataStore buildingSDS = new ShapefileDataStore(buildingFile.toURI().toURL());
			SimpleFeatureCollection buildingSFC = DataUtilities
					.collection(Vectors.snapDatas(buildingSDS.getFeatureSource().getFeatures(), Vectors.unionSFC(parcelSFC)));
			buildingSDS.dispose();
			parcelSDS.dispose();
			iniDens = existingBuildingDensity(buildingSFC, code, parcelSFC);
		}
		commSDS.dispose();
		return iniDens;

	}

	public int getEstimationForACity(String insee) throws IOException {
		int result = -1;
		CSVReader read = new CSVReader(new FileReader(new File(getIndicFile(), "genStat.csv")));
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

	public static double existingBuildingDensity(SimpleFeatureCollection buildingSFC, String code, SimpleFeatureCollection parcelSFC)
			throws Exception {
		if (code.equals("ALLLL")) {
			System.out.println("not concerned");
			return 0;
		}
		String[] attr = { "CODE_DEP", "CODE_COM" };
		parcelSFC = Vectors.getSFCPart(parcelSFC, code, attr);
		buildingSFC = Vectors.snapDatas(buildingSFC, Vectors.unionSFC(parcelSFC));
		return existingBuildingDensity(buildingSFC, parcelSFC, code);
	}

	public static double existingBuildingDensity(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection parcelsSFC, String code)
			throws IOException {
		double surfTot = 0;
		SimpleFeatureIterator itParcel = parcelsSFC.features();
		try {
			parcel: while (itParcel.hasNext()) {
				SimpleFeature feat = itParcel.next();
				SimpleFeatureIterator itbuild = Vectors.snapDatas(buildingSFC, (Geometry) feat.getDefaultGeometry()).features();
				if (!itbuild.hasNext()) {
					continue parcel;
				}
				try {
					while (itbuild.hasNext()) {
						SimpleFeature featBuild = itbuild.next();
						if (!((Geometry) feat.getDefaultGeometry()).intersects(((Geometry) featBuild.getDefaultGeometry()))) {
							continue parcel;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itbuild.close();
				}

				surfTot = surfTot + ((Geometry) feat.getDefaultGeometry()).getArea();
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}

		surfTot = surfTot / 10000;

		int nbLgt = simpleEstimate(buildingSFC, 110.0, 3.5);
		double dens = nbLgt / surfTot;
		System.out.println("nbLgt is " + nbLgt + " and aera is " + surfTot + " so density is : " + dens);

		// CSVWriter csv = new CSVWriter(new FileWriter(initialDensities, true));
		// String[] line = { code, String.valueOf(dens) };
		// csv.writeNext(line);
		// csv.close();

		return nbLgt / surfTot;
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlinePartCsv() {
		return housingUnitFirstLine;
	}

	/**
	 * Surcharge des fonction de générations de csv
	 */
	public String getFirstlineGenCsv() {
		return super.getFirstlineCsv() + genFirstLine;
	}

	public void makeGenStat() throws Exception {
		makeGenStat("ALLLL");
	}

	public void makeGenStat(String code) throws Exception {
		System.out.println();
		System.out.println("city " + code);
		File geoFile = new File(getRootFile(), "dataGeo");
		double iniDensite = existingBuildingDensity(FromGeom.getBuild(geoFile), FromGeom.getParcels(geoFile), FromGeom.getCommunities(geoFile), code);
		String insee = code.substring(0, 5);
		setCountToZero();
		CSVReader stat = new CSVReader(new FileReader(new File(getIndicFile(), "housingUnits.csv")), ',', '\0');
		String[] firstLine = stat.readNext();
		DescriptiveStatistics densityHUStat = new DescriptiveStatistics();
		DescriptiveStatistics densitySDPStat = new DescriptiveStatistics();
		DescriptiveStatistics densityEmpriseStat = new DescriptiveStatistics();
		DescriptiveStatistics sDPPerHUStat = new DescriptiveStatistics();

		int codeParcelP = 0, sdpP = 0, empriseP = 0, nbHousingUnitP = 0, typeHUP = 0, zoneP = 0, hUpHectareDensityP = 0, typoP = 0,
				sDPpHectareDensityP = 0, emprisepHectareDensityP = 0;
		for (int i = 0; i < firstLine.length; i++) {
			switch (firstLine[i]) {
			case "codeParcel":
				codeParcelP = i;
				break;
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
			}
		}

		for (String[] l : stat.readAll()) {
			if (l[codeParcelP].startsWith(insee) || insee.equals("ALLLL")) {
				sDPtot = sDPtot + Double.valueOf(l[sdpP]);
				sDPPerHUStat.addValue(Double.valueOf(l[sdpP]) / Integer.valueOf(l[nbHousingUnitP]));
				nbHU = nbHU + Integer.valueOf(l[nbHousingUnitP]);
				nbBuildings++;
				densityHUStat.addValue(Double.valueOf(l[hUpHectareDensityP]));
				densitySDPStat.addValue(Double.valueOf(l[sDPpHectareDensityP]));
				densityEmpriseStat.addValue(Double.valueOf(l[emprisepHectareDensityP]));
				empriseTot = empriseTot + Double.valueOf(l[empriseP]);
				objDens = SimuTool.getDensityGoal(new File(getRootFile(), "dataGeo"), insee);
				objHU = SimuTool.getHousingUnitsGoal(new File(getRootFile(), "dataGeo"), insee);
				if (insee.equals("ALLLL")) {
					// calculating a sum of the objectives
					List<String> listInsee = FromGeom.getInsee(new File(getRootFile(), "/dataGeo/communities.shp"), "DEPCOM");
					for (String in : listInsee) {
						objHU = objHU + SimuTool.getDensityGoal(new File(getRootFile(), "dataGeo"), in);
					}
				}
				// typo
				switch (l[typoP]) {
				case "centre":
					nbCentre = nbCentre + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "banlieue":
					nbBanlieue = nbBanlieue + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "periUrbain":
					nbPeriUrbain = nbPeriUrbain + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "rural":
					nbRural = nbRural + Integer.valueOf(l[nbHousingUnitP]);
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
				} else if (mainZone.equals("AU")) {
					nbAU = nbAU + Integer.valueOf(l[nbHousingUnitP]);
				} else {
					nbNC = nbNC + Integer.valueOf(l[nbHousingUnitP]);
				}

				// buildingType
				switch (l[typeHUP]) {
				case "DETACHEDHOUSE":
					nbDetachedHouse = nbDetachedHouse + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "SMALLHOUSE":
					nbSmallHouse = nbSmallHouse + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "MULTIFAMILYHOUSE":
					nbMultifamilyHouse = nbMultifamilyHouse + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "SMALLBLOCKFLAT":
					nbSmallBlockFlat = nbSmallBlockFlat + Integer.valueOf(l[nbHousingUnitP]);
					break;
				case "MIDBLOCKFLAT":
					nbMidBlockFlat = nbMidBlockFlat + Integer.valueOf(l[nbHousingUnitP]);
					break;
				}
			}
		}
		averageDensiteHU = densityHUStat.getMean();
		standDevDensiteHU = densityHUStat.getStandardDeviation();
		averageDensiteSDP = densitySDPStat.getMean();
		standDevDensiteSDP = densitySDPStat.getStandardDeviation();
		averageDensiteEmprise = densityEmpriseStat.getMean();
		standDevDensiteEmprise = densityEmpriseStat.getStandardDeviation();
		averageSDPpHU = sDPPerHUStat.getMean();
		standDevSDPpHU = sDPPerHUStat.getStandardDeviation();

		double diffDens = objDens - averageDensiteHU;
		double diffObj = objHU - nbHU;

		String line = insee + "," + round(sDPtot / 1000000, 6) + "," + round(empriseTot / 1000000, 6) + "," + iniDensite + "," + averageDensiteHU
				+ "," + standDevDensiteHU + "," + averageDensiteSDP + "," + standDevDensiteSDP + "," + averageDensiteEmprise + ","
				+ +standDevDensiteEmprise + "," + objDens + "," + diffDens + "," + averageSDPpHU + "," + standDevSDPpHU + "," + nbBuildings + ","
				+ nbHU + "," + objHU + "," + diffObj + "," + nbDetachedHouse + "," + nbSmallHouse + "," + nbMultifamilyHouse + "," + nbSmallBlockFlat
				+ "," + nbMidBlockFlat + "," + nbU + "," + nbAU + "," + nbNC + "," + nbCentre + "," + nbBanlieue + "," + nbPeriUrbain + "," + nbRural;

		toGenCSV("genStat", genStatFirstLine, line);
		stat.close();
	}

	public int distributionEstimate() throws IOException {
		ShapefileDataStore batiSimuledSDS = new ShapefileDataStore(getSimPLUDepotGenFile().toURI().toURL());
		SimpleFeatureCollection batiSimuled = batiSimuledSDS.getFeatureSource().getFeatures();
		int lgt = distributionEstimate(batiSimuled);
		batiSimuledSDS.dispose();
		return lgt;
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

	public int distributionEstimate(SimpleFeatureCollection collec) throws IOException {
		SimpleFeatureIterator it = collec.features();
		List<String> buildingCode = new ArrayList<String>();
		try {
			while (it.hasNext()) {
				SimpleFeature ftBati = it.next();
				String code = (String) ftBati.getAttribute("CODE");
				if (!buildingCode.contains(code)) {

					// typo of the zone
					// String typo = FromGeom.parcelInTypo(FromGeom.getCommunitiesIris(new File(rootFile, "dataGeo")), ftBati).toUpperCase();
					String typo = FromGeom.getTypo(FromGeom.getCommunitiesIris(new File(getRootFile(), "dataGeo")),
							(Geometry) ftBati.getDefaultGeometry());
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
						String lineParticular = numeroParcel + "," + sDPLgt + "," + ftBati.getAttribute("SurfaceSol") + "," + String.valueOf(nbHU)
								+ "," + type + "," + ftBati.getAttribute("TYPEZONE") + "," + typo + "," + String.valueOf(averageSDPpHU) + ","
								+ String.valueOf(averageDensiteHU) + "," + String.valueOf(averageDensiteSDP) + ","
								+ String.valueOf(averageDensiteEmprise);

						toParticularCSV(getIndicFile(), "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
					} else {
						String lineParticular = numeroParcel + "," + sDPLgt + "," + ftBati.getAttribute("SurfaceSol") + "," + String.valueOf(nbHU)
								+ "," + type + "," + ftBati.getAttribute("TYPEZONE") + "," + typo + "," + String.valueOf(averageSDPpHU) + ","
								+ String.valueOf(averageDensiteHU) + "," + String.valueOf(averageDensiteSDP) + ","
								+ String.valueOf(averageDensiteEmprise);

						toParticularCSV(getIndicFile(), "housingUnits.csv", getFirstlinePartCsv(), lineParticular);
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
			if (nbLgtFinal >= minLgt || antiInfinity > 100) {
				// System.out.println("it's enough");
				doRepart = false;
				result.put("smallHU", smallHU);
				result.put("midHU", midHU);
				result.put("largeHU", largeHU);
				HashMap<String, Integer> carac = new HashMap<String, Integer>();
				carac.put("totHU", nbLgtFinal);
				result.put("carac", carac);
				if (antiInfinity > 100) {
					System.out.println("too much loop - this shouldn't happend");
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
			System.out.println("not enough space yee over");
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

	public File joinStatoBTHParcels(String nameFileToJoin) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesOGSDS = new ShapefileDataStore(getParcelDepotGenFile().toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesOGSDS.getFeatureSource().getFeatures();
		File result = joinStatToBTHParcel(communitiesOG, new File(getIndicFile(), nameFileToJoin), new File(getIndicFile(), "parcStat.shp"));
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

		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		int codeParcelP = 0, densHUpHeP = 0, densSDPpHeP = 0, densEmppHeP = 0;
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

		return Vectors.exportSFC(result, outFile);
	}

	// public String setBuildingType(int nbLgt) {
	// if (nbLgt == 0) {
	// return NUTIN;
	// } else if (nbLgt == 1) {
	// return INDIV;
	// } else if (nbLgt == 2) {
	// return DBLINDIV;
	// } else if (nbLgt < 7) {
	// return SDWELLING;
	// }
	// return LDWELLING;
	// }

	// public int runParticularSimpleEstimation() throws IOException {
	// System.out.println("pour " + getnameScenar() + ", la simu selectionnant like " + getSelection());
	// simpleEstimate();
	//
	// return nbHU;
	// }

	// public void simpleCityEstimate() throws IOException {
	// // if particular statistics hasn't been calculated yet
	// File particularSimpleEstimate = new File(indicFile, "housingUnits.csv");
	// if (!particularSimpleEstimate.exists()) {
	// System.out.println("you should have run simpleEstimate() before");
	// simpleEstimate();
	// }
	//
	// // for every cities
	//
	// Hashtable<String, List<String[]>> cities = SimuTool.getCitiesFromparticularHousingUnit(particularSimpleEstimate);
	// String line = "";
	// for (String zipCode : cities.keySet()) {
	// System.out.println("zipcode : " + zipCode);
	// // different values
	// int sumLgt = 0;
	// int sumIndiv = 0;
	// int sumDlbIndiv = 0;
	// int sumSDwell = 0;
	// int sumLDwell = 0;
	// int sumLgtU = 0;
	// int sumLgtAU = 0;
	// int sumLgtOther = 0;
	// DescriptiveStatistics floorAreaStat = new DescriptiveStatistics();
	// DescriptiveStatistics groundAreaStat = new DescriptiveStatistics();
	// DescriptiveStatistics builtDensity = new DescriptiveStatistics();
	//
	// CSVReader csvReader = new CSVReader(new FileReader(particularSimpleEstimate));
	//
	// String[] fLine = csvReader.readNext();
	//
	// for (String[] lineCsv : cities.get(zipCode)) {
	//
	// // make sure that nb of lgt's on
	// int nbLgtLine = 0;
	// for (int i = 0; i < fLine.length; i++) {
	// if (fLine[i].equals("nombre_de_logements")) {
	// nbLgtLine = Integer.valueOf(lineCsv[i]);
	// sumLgt = sumLgt + nbLgtLine;
	// }
	// }
	// // if no household in the building
	// if (nbLgtLine == 0) {
	// System.out.println("dog's house");
	// continue;
	// }
	// for (int i = 0; i < fLine.length; i++) {
	// String nameCol = fLine[i];
	//
	// switch (nameCol) {
	// case "surface_au_sol":
	// groundAreaStat.addValue(Double.valueOf(lineCsv[i]));
	// break;
	//
	// case "surface_de_plancher":
	// floorAreaStat.addValue(Double.valueOf(lineCsv[i]));
	// break;
	// case "type_du_logement":
	// String type = lineCsv[i];
	// if (type.equals(INDIV)) {
	// sumIndiv = sumIndiv + nbLgtLine;
	// } else if (type.equals(DBLINDIV)) {
	// sumDlbIndiv = sumDlbIndiv + nbLgtLine;
	// } else if (type.equals(SDWELLING)) {
	// sumSDwell = sumSDwell + nbLgtLine;
	// } else if (type.equals(LDWELLING)) {
	// sumLDwell = sumLDwell + nbLgtLine;
	// }
	// break;
	//
	// case "zone_de_la_construction":
	// String typeZone = lineCsv[i];
	// if (typeZone.equals("U")) {
	// sumLgtU = sumLgtU + nbLgtLine;
	// } else if (typeZone.equals("AU")) {
	// sumLgtAU = sumLgtAU + nbLgtLine;
	// } else {
	// sumLgtOther = sumLgtOther + nbLgtLine;
	// }
	// break;
	// case "densite_batie":
	// builtDensity.addValue(Double.valueOf(lineCsv[i]));
	// break;
	// }
	// }
	// }
	// csvReader.close();
	//
	// System.out.println("somme_de_la_surface_au_sol_des_logements" + groundAreaStat.getSum());
	//
	// int housingUnitDiff = sumLgt - FromGeom.getHousingUnitsGoals(new File(rootFile, "dataGeo"), zipCode);
	// line = zipCode + "," + sumLgt + "," + sumIndiv + "," + sumDlbIndiv + "," + sumSDwell + "," + sumLDwell + "," + sumLgtU + "," + sumLgtAU
	// + "," + sumLgtOther + "," + groundAreaStat.getSum() + "," + groundAreaStat.getMean() + "," + groundAreaStat.getStandardDeviation()
	// + "," + floorAreaStat.getSum() + "," + floorAreaStat.getMean() + "," + floorAreaStat.getStandardDeviation() + ","
	// + builtDensity.getMean() + "," + builtDensity.getStandardDeviation() + "," + housingUnitDiff;
	//
	// }
	// toGenCSV(indicFile, "BuildingToHouseholdByCity", getFirstlineGenCsv(), line);
	// }

	// public static void runParticularSimpleEstimation(File filebati, File rootFile, File simuFile, SimpluParametersJSON p) throws IOException {
	// BuildingToHousingUnit bth = new BuildingToHousingUnit(filebati, rootFile, simuFile, p, "DDense", "variante0");
	// bth.runParticularSimpleEstimation();
	// }

	public void setCountToZero() {
		nbBuildings = nbHU = nbStairs = nbDetachedHouse = nbSmallHouse = nbMultifamilyHouse = nbSmallBlockFlat = objHU = diffHU = nbMidBlockFlat = nbU = nbAU = nbNC = nbCentre = nbBanlieue = nbPeriUrbain = nbRural = 0;
		sDPtot = empriseTot = averageSDPpHU = standDevSDPpHU = averageDensiteHU = standDevDensiteHU = averageDensiteSDP = standDevDensiteSDP = averageDensiteEmprise = standDevDensiteEmprise = objDens = diffDens = 0.0;
	}

}
