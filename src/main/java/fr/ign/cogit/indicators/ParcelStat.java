package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.SurfParcelFailedMap;
import fr.ign.cogit.map.theseMC.SurfParcelSimulatedMap;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;
import fr.ign.cogit.util.SimuTool;

public class ParcelStat extends Indicators {

	File parcelOGFile;
	int nbParcelIgnored, nbParcelSimulated, nbParcelSimulFailed, nbParcelSimulatedU, nbParcelSimulFailedU, nbParcelSimulatedAU, nbParcelSimulFailedAU,
			nbParcelSimulatedNC, nbParcelSimulFailedNC, nbParcelSimulatedCentre, nbParcelSimulFailedCentre, nbParcelSimulatedBanlieue,
			nbParcelSimulFailedBanlieue, nbParcelSimulatedPeriUrb, nbParcelSimulFailedPeriUrb, nbParcelSimulatedRural, nbParcelSimulFailedRural;
	double surfParcelIgnored, surfParcelSimulated, surfParcelSimulFailed, surfParcelSimulatedU, surfParcelSimulFailedU, surfParcelSimulatedAU,
			surfParcelSimulFailedAU, surfParcelSimulatedNC, surfParcelSimulFailedNC, surfParcelSimulatedCentre, surfParcelSimulFailedCentre,
			surfParcelSimulatedBanlieue, surfParcelSimulFailedBanlieue, surfParcelSimulatedPeriUrb, surfParcelSimulFailedPeriUrb,
			surfParcelSimulatedRural, surfParcelSimulFailedRural;
	// surfaceSDPParcelle, surfaceEmpriseParcelle;
	SimpleFeatureCollection preciseParcelCollection;
	String firstLine;
	static String indicName = "parcelStat";

	public ParcelStat(SimpluParametersJSON p, File rootFile, String scenarName, String variantName) throws Exception {
		super(p, rootFile, scenarName, variantName, indicName);
		parcelOGFile = FromGeom.getParcels(new File(rootFile, "dataGeo"));
		firstLine = "INSEE,nb_parcel_simulated,nb_parcel_simu_failed,surf_parcel_ignored,surf_parcel_simulated,surf_parcel_simulFailed,nbParcelSimulatedU,nbParcelSimulFailedU,nbParcelSimulatedAU,nbParcelSimulFailedAU,nbParcelSimulatedNC,nbParcelSimulFailedNC,nbParcelSimulatedCentre,nbParcelSimulFailedCentre,nbParcelSimulatedBanlieue,nbParcelSimulFailedBanlieue,nbParcelSimulatedPeriUrb,nbParcelSimulFailedPeriUrb,nbParcelSimulatedRural,nbParcelSimulFailedRural,surfParcelSimulatedU,surfParcelSimulFailedU,surfParcelSimulatedAU,surfParcelSimulFailedAU,surfParcelSimulatedNC,surfParcelSimulFailedNC,surfParcelSimulatedCentre,surfParcelSimulFailedCentre,surfParcelSimulatedBanlieue,surfParcelSimulFailedBanlieue,surfParcelSimulatedPeriUrb,surfParcelSimulFailedPeriUrb,surfParcelSimulatedRural,surfParcelSimulFailedRural";
	}

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result2903/");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		String scenario = "CDense";
		// String variant = "base";

		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
		lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);
		// for (File f : (new File(rootFile, "SimPLUDepot/" + scenario + "/")).listFiles()) {
		// ParcelStat parc = new ParcelStat(p, rootFile, scenario, f.getName());
		ParcelStat parc = new ParcelStat(p, rootFile, scenario, "variantMvData1");
		SimpleFeatureCollection parcelStatSHP = parc.markSimuledParcels();
		parc.caclulateStatParcel();
		// parc.caclulateStatBatiParcel();
		parc.writeLine("AllZone", "ParcelStat");
		parc.setCountToZero();
		List<String> listInsee = FromGeom.getInsee(new File(parc.rootFile, "/dataGeo/old/communities.shp"), "DEPCOM");

		for (String city : listInsee) {
			SimpleFeatureCollection commParcel = ParcelFonction.getParcelByZip(parcelStatSHP, city);
			System.out.println("city " + city);
			parc.caclulateStatParcel(commParcel);
			// parc.caclulateStatBatiParcel(commParcel);
			parc.writeLine(city, "ParcelStat");
			parc.toString();
			parc.setCountToZero();
		}
		File commStatFile = parc.joinStatToCommunities();
		parc.createMap(parc, commStatFile);
		parc.createGraph(new File(parc.getIndicFile(), "ParcelStat.csv"));
	}
	// }

	public void createMap(ParcelStat parc, File commStatFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();
		MapRenderer surfParcelSimulatedMap = new SurfParcelSimulatedMap(1000, 1000, new File(parc.rootFile, "mapStyle"), commStatFile,
				parc.getMapDepotFile());
		allOfTheMaps.add(surfParcelSimulatedMap);
		MapRenderer surfParcelFailedMap = new SurfParcelFailedMap(1000, 1000, parc.getMapStyle(), commStatFile, parc.getMapDepotFile());
		allOfTheMaps.add(surfParcelFailedMap);
		// MapRenderer aParcelSDPSimuMap = new AParcelSDPSimuMap(1000, 1000,parc.mapStyle , commStatFile, parc.mapDepotFile);
		// allOfTheMaps.add(aParcelSDPSimuMap);

		for (MapRenderer map : allOfTheMaps) {
			map.renderCityInfo();
			map.generateSVG();
		}
	}

	public void createGraph(File distrib) throws IOException {
		// Number
		String[] xTypeSimulated = { "nbParcelSimulatedCentre", "nbParcelSimulatedBanlieue", "nbParcelSimulatedPeriUrb", "nbParcelSimulatedRural" };
		String[] xTypeSimulFailed = { "nbParcelSimulFailedCentre", "nbParcelSimulFailedBanlieue", "nbParcelSimulFailedPeriUrb",
				"nbParcelSimulatedRural" };
		String[][] xType = { xTypeSimulated, xTypeSimulFailed };
		makeGraphDouble(distrib, graphDepotFile, "Scenario : " + scenarName + " - Variante : " + variantName, xType, "typologie",
				"Nombre de parcelles");
		String[] xZoneSimulated = { "nbParcelSimulatedU", "nbParcelSimulatedAU", "nbParcelSimulatedNC" };
		String[] xZoneSimulFailed = { "nbParcelSimulFailedU", "nbParcelSimulFailedAU", "nbParcelSimulFailedNC" };
		String[][] xZone = { xZoneSimulated, xZoneSimulFailed };
		makeGraphDouble(distrib, graphDepotFile, "Scenario : " + scenarName + " - Variante : " + variantName, xZone, "type de zone",
				"Nombre de parcelles");

		// Surface
		String[] xTypeSimulatedSurf = { "surfParcelSimulatedCentre", "surfParcelSimulatedBanlieue", "surfParcelSimulatedPeriUrb",
				"surfParcelSimulatedRural" };
		String[] xTypeSimulFailedSurf = { "surfParcelSimulFailedCentre", "surfParcelSimulFailedBanlieue", "surfParcelSimulFailedPeriUrb",
				"surfParcelSimulatedRural" };
		String[][] xTypeSurf = { xTypeSimulatedSurf, xTypeSimulFailedSurf };
		makeGraphDouble(distrib, graphDepotFile, "Scenario : " + scenarName + " - Variante : " + variantName, xTypeSurf, "typologie",
				"Surface de parcelles (km²)");
		String[] xZoneSimulatedSurf = { "surfParcelSimulatedU", "surfParcelSimulatedAU", "surfParcelSimulatedNC" };
		String[] xZoneSimulFailedSurf = { "surfParcelSimulFailedU", "surfParcelSimulFailedAU", "surfParcelSimulFailedNC" };
		String[][] xZoneSurf = { xZoneSimulatedSurf, xZoneSimulFailedSurf };
		makeGraphDouble(distrib, graphDepotFile, "Scenario : " + scenarName + " - Variante : " + variantName, xZoneSurf, "type de zone",
				"Surface de parcelles (km²)");

	}

	public static void makeGraphDouble(File csv, File graphDepotFile, String title, String[][] xes, String xTitle, String yTitle) throws IOException {
		// Create Chart
		CategoryChart chart = new CategoryChartBuilder().width(800).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		int count = 0;
		for (String[] x : xes) {
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
					if (fLine[i].equals("INSEE"))
						iCode = i;
				}

				for (String[] lines : csvR.readAll()) {
					if (lines[iCode].equals("AllZone")) {
						yS.add(Double.valueOf(lines[iX]));
						break;
					}
				}
				csvR.close();
			}
			String simulOrNot = "Simulée";
			if (count == 1) {
				simulOrNot = "Simulation échouée";
			}
			chart.addSeries(simulOrNot, label, yS);
			count++;
		}

		// chart.addSeries(yTitle, label, yS);
		// Histogram histogram1 ;
		// Histogram histogram2 ;
		// chart.addSeries("histogram 1", histogram1.getxAxisData(), histogram1.getyAxisData());
		//
		// chart.addSeries("histogram 2", histogram2.getxAxisData(), histogram2.getyAxisData());
		// Customize Chart
		// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(true);
		chart.getStyler().setHasAnnotations(true);
		chart.getStyler().setXAxisLabelRotation(45);
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
		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + x[0], BitmapFormat.PNG);
		// new SwingWrapper(chart).displayChart();
	}

	public File joinStatToCommunities() throws NoSuchAuthorityCodeException, IOException, FactoryException {
		ShapefileDataStore communitiesSDS = new ShapefileDataStore((new File(rootFile, "/dataGeo/old/communities.shp")).toURI().toURL());
		SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
		File result = joinStatToSFC(communitiesOG, new File(getIndicFile(), "ParcelStat.csv"), new File(getIndicFile(), "commStat.shp"));
		communitiesSDS.dispose();
		return result;
	}

	public File joinStatToSFC(SimpleFeatureCollection collec, File statFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("communities");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("nbParcSimu", Integer.class);
		sfTypeBuilder.add("nbParcFail", Integer.class);
		sfTypeBuilder.add("aParcSimu", Double.class);
		sfTypeBuilder.add("aParcFail", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		SimpleFeatureIterator it = collec.features();

		try {
			while (it.hasNext()) {
				SimpleFeature ftBati = it.next();
				String insee = (String) ftBati.getAttribute("DEPCOM");
				CSVReader stat = new CSVReader(new FileReader(statFile), ',', '\0');
				String[] firstLine = stat.readNext();
				int inseeP = 0, nbParcSimuP = 0, nbParcFailP = 0, aParcSimuP = 0, aParcFailP = 0;
				// surface_SDP_parcelleP = 0, surface_emprise_parcelleP = 0;
				for (int i = 0; i < firstLine.length; i++) {
					switch (firstLine[i]) {
					case "INSEE":
						inseeP = i;
						break;
					case "nb_parcel_simulated":
						nbParcSimuP = i;
						break;
					case "surf_parcel_ignored":
						nbParcFailP = i;
						break;
					case "surf_parcel_simulated":
						aParcSimuP = i;
						break;
					case "surf_parcel_simulFailed":
						aParcFailP = i;
						break;
					}
				}

				for (String[] l : stat.readAll()) {
					if (l[inseeP].equals(insee)) {
						builder.set("the_geom", ftBati.getDefaultGeometry());
						builder.set("INSEE", insee);
						builder.set("nbParcSimu", l[nbParcSimuP]);
						builder.set("nbParcFail", l[nbParcFailP]);
						builder.set("aParcSimu", Double.valueOf(l[aParcSimuP]));
						builder.set("aParcFail", Double.valueOf(l[aParcFailP]));
						result.add(builder.buildFeature(null));
						break;
					}
				}
				stat.close();
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		return Vectors.exportSFC(result, outFile);
	}

	public String writeLine(String geoEntity, String nameFile) throws IOException {
		String result = geoEntity + "," + nbParcelSimulated + "," + nbParcelSimulFailed + "," + round(surfParcelIgnored / 1000000, 3) + ","
				+ round(surfParcelSimulated / 1000000, 3) + "," + round(surfParcelSimulFailed / 1000000, 3) + nbParcelSimulatedU + ","
				+ nbParcelSimulFailedU + "," + nbParcelSimulatedAU + "," + nbParcelSimulFailedAU + "," + nbParcelSimulatedNC + ","
				+ nbParcelSimulFailedNC + "," + nbParcelSimulatedCentre + "," + nbParcelSimulFailedCentre + "," + nbParcelSimulatedBanlieue + ","
				+ nbParcelSimulFailedBanlieue + "," + nbParcelSimulatedPeriUrb + "," + nbParcelSimulFailedPeriUrb + "," + nbParcelSimulatedRural + ","
				+ nbParcelSimulFailedRural + "," + round(surfParcelSimulatedU / 1000000, 3) + "," + round(surfParcelSimulFailedU / 1000000, 3) + ","
				+ round(surfParcelSimulatedAU / 1000000, 3) + "," + round(surfParcelSimulFailedAU / 1000000, 3) + ","
				+ round(surfParcelSimulatedNC / 1000000, 3) + "," + round(surfParcelSimulFailedNC / 1000000, 3) + ","
				+ round(surfParcelSimulatedCentre / 1000000, 3) + "," + round(surfParcelSimulFailedCentre / 1000000, 3) + ","
				+ round(surfParcelSimulatedBanlieue / 1000000, 3) + "," + round(surfParcelSimulFailedBanlieue / 1000000, 3) + ","
				+ round(surfParcelSimulatedPeriUrb / 1000000, 3) + "," + round(surfParcelSimulFailedPeriUrb / 1000000, 3) + ","
				+ round(surfParcelSimulatedRural / 1000000, 3) + "," + round(surfParcelSimulFailedRural / 1000000, 3);
		// + "," + surfaceSDPParcelle + "," + surfaceEmpriseParcelle;
		toGenCSV(nameFile, firstLine, result);
		return result;
	}

	// public void caclulateStatBatiParcel() throws IOException {
	// File parcelStatShapeFile = new File(indicFile, "parcelStatted.shp");
	// if (!parcelStatShapeFile.exists()) {
	// markSimuledParcels();
	// }
	//
	// ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelStatShapeFile.toURI().toURL());
	// SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
	// caclulateStatBatiParcel(parcelSimuled);
	// parcelSimuledSDS.dispose();
	//
	// }
	//
	// public void caclulateStatBatiParcel(SimpleFeatureCollection parcelSimuled) throws IOException {
	//
	// ShapefileDataStore batiSDS = new ShapefileDataStore(simPLUDepotGenFile.toURI().toURL());
	// SimpleFeatureCollection batiColl = batiSDS.getFeatureSource().getFeatures();
	// SimpleFeatureIterator itParcel = parcelSimuled.features();
	//
	// try {
	// while (itParcel.hasNext()) {
	// SimpleFeature ft = itParcel.next();
	// if (((String) ft.getAttribute("DoWeSimul")).equals("simulated")) {
	// SimpleFeatureIterator batiIt = Vectors.snapDatas(batiColl, (Geometry) ft.getDefaultGeometry()).features();
	// try {
	// while (batiIt.hasNext()) {
	// SimpleFeature ftBati = batiIt.next();
	// if (((Geometry) ftBati.getDefaultGeometry()).intersects((Geometry) ft.getDefaultGeometry())) {
	// this.surfaceSDPParcelle = surfaceSDPParcelle + (double) ftBati.getAttribute("SDPShon");
	// this.surfaceEmpriseParcelle = surfaceEmpriseParcelle + (double) ftBati.getAttribute("SurfaceSol");
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// batiIt.close();
	// }
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// itParcel.close();
	// }
	//
	// batiSDS.dispose();
	// }

	public void caclulateStatParcel() throws IOException {
		File parcelStatShapeFile = new File(getIndicFile(), "parcelStatted.shp");
		if (!parcelStatShapeFile.exists()) {
			markSimuledParcels();
		}
		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelStatShapeFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		caclulateStatParcel(parcelSimuled);
		parcelSimuledSDS.dispose();
	}

	public void caclulateStatParcel(SimpleFeatureCollection parcelSimuled) throws IOException {

		SimpleFeatureIterator itParcel = parcelSimuled.features();
		// nbParcelSimulatedCentre, nbParcelSimulFailedCentre, nbParcelSimulatedPeriUrb,
		// nbParcelSimulFailedPeriUrb, nbParcelSimulatedRural, nbParcelSimulFailedRural
		try {
			while (itParcel.hasNext()) {
				SimpleFeature ft = itParcel.next();
				double area = ((Geometry) ft.getDefaultGeometry()).getArea();
				switch ((String) ft.getAttribute("DoWeSimul")) {
				case "noSelection":
					surfParcelIgnored = surfParcelIgnored + area;
					nbParcelIgnored++;
					break;
				case "simulated":
					surfParcelSimulated = surfParcelSimulated + area;
					nbParcelSimulated++;
					if ((boolean) ft.getAttribute("U")) {
						surfParcelSimulatedU = surfParcelSimulatedU + area;
						nbParcelSimulatedU++;
					}
					if ((boolean) ft.getAttribute("AU")) {
						surfParcelSimulatedAU = surfParcelSimulatedAU + area;
						nbParcelSimulatedAU++;
					}
					if ((boolean) ft.getAttribute("NC")) {
						surfParcelSimulatedNC = surfParcelSimulatedNC + area;
						nbParcelSimulatedNC++;
					}
					// System.out.println(FromGeom.getTypo(FromGeom.getCommunitiesIris(new File(rootFile, "dataGeo")), (Geometry) ft.getDefaultGeometry()));
					switch (FromGeom.getTypo(FromGeom.getCommunitiesIris(new File(rootFile, "dataGeo")), (Geometry) ft.getDefaultGeometry())) {
					case "rural":
						nbParcelSimulatedRural++;
						surfParcelSimulatedRural = surfParcelSimulatedRural + area;

						break;
					case "periUrbain":
						nbParcelSimulatedPeriUrb++;
						surfParcelSimulatedPeriUrb = surfParcelSimulatedPeriUrb + area;

						break;
					case "centre":
						nbParcelSimulatedCentre++;
						surfParcelSimulatedCentre = surfParcelSimulatedCentre + area;

						break;
					case "banlieue":
						nbParcelSimulatedBanlieue++;
						surfParcelSimulatedBanlieue = surfParcelSimulatedBanlieue + area;

						break;
					}
					break;
				case "simuFailed":
					surfParcelSimulFailed = surfParcelSimulFailed + area;
					nbParcelSimulFailed++;
					if ((boolean) ft.getAttribute("U")) {
						nbParcelSimulFailedU++;
						surfParcelSimulFailedU = surfParcelSimulFailedU + area;
					}
					if ((boolean) ft.getAttribute("AU")) {
						nbParcelSimulFailedAU++;
						surfParcelSimulFailedAU = surfParcelSimulFailedAU + area;
					}
					if ((boolean) ft.getAttribute("NC")) {
						nbParcelSimulFailedNC++;
						surfParcelSimulFailedNC = surfParcelSimulFailedNC + area;
					}
					switch (FromGeom.getTypo(FromGeom.getCommunitiesIris(new File(rootFile, "dataGeo")), (Geometry) ft.getDefaultGeometry())) {
					case "rural":
						nbParcelSimulFailedRural++;
						surfParcelSimulFailedRural = surfParcelSimulFailedRural + area;
						break;
					case "periUrbain":
						nbParcelSimulFailedPeriUrb++;
						surfParcelSimulFailedPeriUrb = surfParcelSimulFailedPeriUrb + area;
						break;
					case "centre":
						nbParcelSimulFailedCentre++;
						surfParcelSimulFailedCentre = surfParcelSimulFailedCentre + area;
						break;
					case "banlieue":
						nbParcelSimulFailedBanlieue++;
						surfParcelSimulFailedBanlieue = surfParcelSimulFailedBanlieue + area;
						break;
					}
					break;
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
	}

	/**
	 * mark the parcels that has been selected or not (noSelection) and where a building has been simulated (simulated) or not (simuFailed)
	 * 
	 * @return the newly marked parcel shapeFile
	 * @throws IOException
	 */
	public SimpleFeatureCollection markSimuledParcels() throws IOException {

		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator itParcel = parcelSimuled.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		try {
			while (itParcel.hasNext()) {
				SimpleFeature ft = itParcel.next();
				String field = "noSelection";
				if (isParcelReallySimulated(ft)) {
					field = "simulated";
				} else if (ft.getAttribute("DoWeSimul").equals("true")) {
					field = "simuFailed";
				}
				ft.setAttribute("DoWeSimul", field);
				result.add(ft);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
		Vectors.exportSFC(result, new File(getIndicFile(), "parcelStatted.shp"));

		parcelSimuledSDS.dispose();

		return DataUtilities.collection(result.collection());
	}

	/**
	 * for each parcel, set the already existing field "IsBuild" if a new building has been simulated on this parcel
	 * 
	 * @throws Exception
	 */
	public boolean isParcelReallySimulated(SimpleFeature parcel) throws Exception {
		File simuBuildFiles = new File(super.rootFile + "/SimPLUDepot" + "/" + scenarName + "/" + variantName + "/TotBatSimuFill.shp");
		ShapefileDataStore batiSDS = new ShapefileDataStore(simuBuildFiles.toURI().toURL());
		SimpleFeatureCollection batiColl = batiSDS.getFeatureSource().getFeatures();

		Geometry parcelGeometry = (Geometry) parcel.getDefaultGeometry();

		SimpleFeatureCollection snapBatiCollec = Vectors.snapDatas(batiColl, parcelGeometry);
		SimpleFeatureIterator batiFeaturesIt = snapBatiCollec.features();
		try {
			while (batiFeaturesIt.hasNext()) {
				SimpleFeature bati = batiFeaturesIt.next();
				if (((Geometry) bati.getDefaultGeometry()).intersects(parcelGeometry)) {
					return true;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			batiFeaturesIt.close();
		}
		batiSDS.dispose();
		return false;
	}

	// public void run() throws IOException, NoSuchAuthorityCodeException, FactoryException {
	//
	// for (String city : FromGeom.getInsee(parcelDepotGenFile)) {
	// double surfSelect = 0;
	// double surfSelectU = 0;
	// double surfSelectAU = 0;
	// double surfSelectNC = 0;
	//
	// double surfSimulated = 0;
	// double surfSimulatedU = 0;
	// double surfSimulatedAU = 0;
	// double surfSimulatedNC = 0;
	// ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
	// SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
	// try {
	// while (parcelFeaturesIt.hasNext()) {
	// SimpleFeature feature = parcelFeaturesIt.next();
	// if (city.equals((String) feature.getAttribute("INSEE"))) {
	// if (((String) feature.getAttribute("DoWeSimul")).equals("true")) {
	// double area = ((Geometry) feature.getDefaultGeometry()).getArea();
	// surfSelect = surfSelect + area;
	// if ((boolean) feature.getAttribute("U")) {
	// surfSelectU = surfSelectU + area;
	// } else if ((boolean) feature.getAttribute("AU")) {
	// surfSelectAU = surfSelectAU + area;
	// } else if ((boolean) feature.getAttribute("NC")) {
	// surfSelectNC = surfSelectNC + area;
	// }
	// }
	// if ((boolean) feature.getAttribute("IsBuild")) {
	// double area = ((Geometry) feature.getDefaultGeometry()).getArea();
	// surfSimulated = surfSimulated + area;
	// if ((boolean) feature.getAttribute("U")) {
	// surfSimulatedU = surfSimulatedU + area;
	// } else if ((boolean) feature.getAttribute("AU")) {
	// surfSimulatedAU = surfSimulatedAU + area;
	// } else if ((boolean) feature.getAttribute("NC")) {
	// surfSimulatedNC = surfSimulatedNC + area;
	// }
	// }
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// parcelFeaturesIt.close();
	// }
	// parcelSDS.dispose();
	// if (surfSelect > 0) {
	// String line = city + "," + surfSelect + "," + surfSelectU + "," + surfSelectAU + "," + surfSelectNC + "," + surfSimulated + ","
	// + surfSimulatedU + "," + surfSimulatedAU + "," + surfSimulatedNC;
	// System.out.println(line);
	// toGenCSV("parcelStat", firstLine, line);
	// }
	// }
	// }

	public void setCountToZero() {
		nbParcelIgnored = nbParcelSimulated = nbParcelSimulFailed = nbParcelSimulatedU = nbParcelSimulFailedU = nbParcelSimulatedAU = nbParcelSimulFailedAU = nbParcelSimulatedNC = nbParcelSimulFailedNC = nbParcelSimulatedCentre = nbParcelSimulFailedCentre = nbParcelSimulatedBanlieue = nbParcelSimulFailedBanlieue = nbParcelSimulatedPeriUrb = nbParcelSimulFailedPeriUrb = nbParcelSimulatedRural = nbParcelSimulFailedRural = 0;
		surfParcelIgnored = surfParcelSimulated = surfParcelSimulFailed = surfParcelSimulatedU = surfParcelSimulFailedU = surfParcelSimulatedAU = surfParcelSimulFailedAU = surfParcelSimulatedNC = surfParcelSimulFailedNC = surfParcelSimulatedCentre = surfParcelSimulFailedCentre = surfParcelSimulatedBanlieue = surfParcelSimulFailedBanlieue = surfParcelSimulatedPeriUrb = surfParcelSimulFailedPeriUrb = surfParcelSimulatedRural = surfParcelSimulFailedRural = 0;
		// surfaceSDPParcelle = surfaceEmpriseParcelle =
	}

	/**
	 * this method aims to select the simulated parcels, the parcel that haven't been selected and if no building have been simulated on the selected and/or cuted parcel, get the
	 * older ones. This is not finished nor working
	 * 
	 * @return
	 * @throws IOException
	 */
	public SimpleFeatureCollection reuniteParcelOGAndSimuled() throws IOException {
		DefaultFeatureCollection reuniteParcel = new DefaultFeatureCollection();

		ShapefileDataStore parcelOGSDS = new ShapefileDataStore(parcelOGFile.toURI().toURL());
		SimpleFeatureCollection parcelOG = parcelOGSDS.getFeatureSource().getFeatures();
		List<String> oGCode = ParcelFonction.getCodeParcels(parcelOG);

		ShapefileDataStore parcelSimuledSDS = new ShapefileDataStore(parcelDepotGenFile.toURI().toURL());
		SimpleFeatureCollection parcelSimuled = parcelSimuledSDS.getFeatureSource().getFeatures();
		List<String> simuledCode = ParcelFonction.getCodeParcels(parcelSimuled);

		List<String> intactParcels = new ArrayList<String>();
		List<String> cuttedButIntactParcels = new ArrayList<String>();
		List<String> changedParcels = new ArrayList<String>();

		List<String> simuledParcels = new ArrayList<String>();

		for (String simuC : simuledCode) {
			if (oGCode.contains(simuC)) {
				intactParcels.add(simuC);
			} else {
				changedParcels.add(simuC);
			}
		}

		changedP: for (String changedParcel : simuledCode) {
			SimpleFeatureIterator itParcel = parcelSimuled.features();
			try {
				while (itParcel.hasNext()) {
					SimpleFeature ft = itParcel.next();
					String codeTmp = (String) ft.getAttribute("CODE");
					if (codeTmp.equals(changedParcel)) {
						// no construction has been simulated in this parcel
						if (isParcelReallySimulated(ft)) {
							simuledParcels.add(codeTmp);
						} else {
							cuttedButIntactParcels.add(codeTmp);
						}
						continue changedP;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				itParcel.close();
			}
		}
		System.out.println("isolated problematic parcels");
		DefaultFeatureCollection toMergeIftouch = new DefaultFeatureCollection();
		SimpleFeatureIterator itParcel = parcelSimuled.features();
		try {
			while (itParcel.hasNext()) {
				SimpleFeature f = itParcel.next();
				if (cuttedButIntactParcels.contains((String) f.getAttribute("CODE"))) {
					toMergeIftouch.add(f);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}

		Vectors.exportSFC(toMergeIftouch, new File("/tmp/toMergeIfTouch.shp"));

		SimpleFeatureIterator ItToMergeIftouch = toMergeIftouch.features();

		try {
			while (ItToMergeIftouch.hasNext()) {
				SimpleFeature f = ItToMergeIftouch.next();
				Geometry aggregate = mergeIfTouch((Geometry) f.getDefaultGeometry(), toMergeIftouch);

				// find attribute infos
				SimpleFeatureIterator getAttributeIt = Vectors.snapDatas(parcelOG, aggregate).features();
				try {
					while (getAttributeIt.hasNext()) {
						SimpleFeature model = getAttributeIt.next();
						if (((Geometry) model.getDefaultGeometry()).intersects(aggregate)) {
							SimpleFeatureBuilder sfbuild = FromGeom.setSFBParcelWithFeat(model);
							sfbuild.set(model.getFeatureType().getGeometryDescriptor().getName().toString(), aggregate);
							reuniteParcel.add(sfbuild.buildFeature(null));
							break;
						}

					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					getAttributeIt.close();
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			ItToMergeIftouch.close();
		}

		Vectors.exportSFC(reuniteParcel, new File("/tmp/unitedParcels.shp"));

		return toMergeIftouch;

		// maybe some created parcels are made of OG parcels. We then have to make some particular stuff
	}

	/**
	 * This method recursively add geometries to a solo one if they touch each other not sure this is working
	 * 
	 * not safe at work
	 * 
	 * @param geomIn
	 * @param df
	 * @return
	 * @throws IOException
	 */
	public Geometry mergeIfTouch(Geometry geomIn, DefaultFeatureCollection df) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(df.collection());

		SimpleFeatureIterator features = df.features();

		Geometry aggreg = geomIn;

		try {
			while (features.hasNext()) {
				SimpleFeature f = features.next();
				Geometry geomTemp = (((Geometry) f.getDefaultGeometry()));
				if (geomIn.intersects(geomTemp) && !geomIn.equals(geomTemp)) {
					result.remove(f);
					aggreg = Vectors.unionGeom(geomIn, geomTemp);
					aggreg = mergeIfTouch(aggreg, result);
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			features.close();
		}
		return aggreg;

	}
}
