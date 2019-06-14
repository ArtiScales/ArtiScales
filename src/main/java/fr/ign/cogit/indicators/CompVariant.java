package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CSVImporter;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.internal.series.Series.DataType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.compVariant.CVNbBat;
import fr.ign.cogit.map.theseMC.compVariant.CVParcelFailed;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class CompVariant extends Indicators {
	String[] baseVariant;
	List<String> variantNames = new LinkedList<String>();
	String indicStatFile;
	static String indicName = "compVariant";
	String spec = "";

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result2903/");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		String scenario = "CDense";
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterTechnic.xml"));
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterScenario.xml"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);
		run(p, rootFile, scenario);
	}

	public static void run(SimpluParametersJSON p, File rootFile, String scenario) throws Exception {
		String[] analyzes = { "parcelStat", "bTH" };
		// String[] analyzes = { "parcelStat"};

		for (String analyzed : analyzes) {
			CompVariant compVar = new CompVariant(p, rootFile, scenario);
			compVar.createStat(analyzed, "genStat.csv");

			if (analyzed.equals("bTH")) {
				compVar.createGraphBHT(new File(compVar.getIndicFolder(), "compVariant" + analyzed + "Gen.csv"));
				compVar.setCommStatFile(compVar.joinStatBTHtoCommunities("compVariant" + analyzed + "CityCoeffVar.csv"));
				compVar.allOfTheMapsBTH();

			} else if (analyzed.equals("parcelStat")) {
				compVar.createGraphParcelStat(new File(compVar.getIndicFolder(), "compVariant" + analyzed + "Gen.csv"));
				compVar.setCommStatFile(compVar.joinStatParcelToCommunities("compVariant" + analyzed + "CityCoeffVar.csv"));
				compVar.allOfTheMapsParcelStat();
			}

			// for every different variant
			List<LinkedList<String>> lists = compVar.ForEachVariantType();

			for (LinkedList<String> soloList : lists) {
				CompVariant compVarSolo = new CompVariant(p, rootFile, scenario);
				String prefix = soloList.get(0).substring(7, 11);
				compVarSolo.spec = "solo/" + analyzed + "/" + prefix;
				compVarSolo.setIndicFile(new File(compVarSolo.getIndicFolder(), compVarSolo.spec));
				compVarSolo.getIndicFolder().mkdirs();
				File graphDepot = new File(compVarSolo.getIndicFolder(), "graphDepot");
				graphDepot.mkdirs();
				File mapDepot = new File(compVarSolo.getIndicFolder(), "mapDepot");
				mapDepot.mkdirs();

				compVarSolo.createStat(analyzed, "genStat.csv", soloList);

				if (analyzed.equals("bTH")) {
					compVarSolo.createGraphBHT(new File(compVarSolo.getIndicFolder(), "compVariant" + analyzed + "Gen.csv"), graphDepot);
					compVarSolo.setCommStatFile(compVarSolo.joinStatBTHtoCommunities("compVariant" + analyzed + "CityCoeffVar.csv"));
					compVarSolo.allOfTheMapsBTH(mapDepot, prefix);
				} else if (analyzed.equals("parcelStat")) {
					compVarSolo.createGraphParcelStat(new File(compVarSolo.getIndicFolder(), "compVariant" + analyzed + "Gen.csv"), graphDepot);
					compVarSolo.setCommStatFile(compVarSolo.joinStatParcelToCommunities("compVariant" + analyzed + "CityCoeffVar.csv",
							new File(compVarSolo.getIndicFolder(), "commStatCoeffVar.shp")));
					compVarSolo.allOfTheMapsParcelStat(mapDepot, prefix);

				}
			}
		}
	}

	public CompVariant(SimpluParametersJSON p, File rootfile, String scenarname) throws Exception {
		super(p, rootfile, scenarname, "", indicName);
	}

	/**
	 * thats a bit ugly but it sorts the variants by the same type for a solo comparison
	 * 
	 * @return
	 */
	public List<LinkedList<String>> ForEachVariantType() {
		List<LinkedList<String>> result = new ArrayList<LinkedList<String>>();
		for (String name : variantNames) {
			if (name.equals("base")) {
				continue;
			}
			LinkedList<String> add = new LinkedList<String>();
			if (result.isEmpty()) {
				add.add(name);
				result.add(add);
			} else {
				boolean addNew = true;
				LinkedList<String> tp = new LinkedList<String>();
				LinkedList<String> tpRm = new LinkedList<String>();
				for (LinkedList<String> list : result) {
					if (list.get(0).substring(7, 10).startsWith(name.substring(7, 10))) {
						addNew = false;
						tp.add(name);
						tp.addAll(list);
						tpRm = list;
					}
				}
				if (addNew) {
					add.add(name);
					result.add(add);
				} else {
					result.remove(tpRm);
					result.add(tp);
				}
			}
		}

		for (LinkedList<String> list : result) {
			list.add("base");
		}

		return result;
	}

	public void SortVariantNames() {
		// TODO faire ça
	}

	public void createStat(String nameCompared, String nameFileStat) throws IOException {
		List<String> listVariant = new ArrayList<String>();

		for (File f : new File(super.getRootFile(), "indic/" + nameCompared + "/" + super.scenarName + "/").listFiles()) {
			listVariant.add(f.getName());
		}
		createStat(nameCompared, nameFileStat, listVariant);
	}

	public void createStat(String nameCompared, String nameFileStat, List<String> listVariant) throws IOException {

		String nameGen = indicName + nameCompared + "Gen.csv";
		String nameCity = indicName + nameCompared + "City.csv";

		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameGen)), ',', '\u0000');
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameCity)), ',', '\u0000');
		boolean firstLine = true;
		// for (File f : (new File(super.getRootFile(), "indic/" + nameCompared + "/" + super.scenarName + "/")).listFiles()) {
		for (String variant : listVariant) {
			File f = new File(super.getRootFile(), "indic/" + nameCompared + "/" + super.scenarName + "/" + variant);
			System.out.println(f);
			File statFile = new File(f, nameFileStat);
			if (f.isDirectory() && statFile.exists()) {
				if (!variantNames.contains(f.getName())) {
					variantNames.add(f.getName());
				}

				CSVReader csvR = new CSVReader(new FileReader(statFile));
				if (firstLine) {
					String[] fLine = csvR.readNext();
					csvWGen.writeNext(fLine);
					csvWCity.writeNext(fLine);
					firstLine = false;

				} else {
					csvR.readNext();
				}

				for (String[] l : csvR.readAll()) {
					String insee = l[2];
					// System.out.println(insee + ",");
					// System.out.println(insee + "," + String.valueOf(nbVariant) + ",");
					if (insee.equals("AllZone") || insee.equals("ALLLL")) {
						csvWGen.writeNext(l);
					} else {
						csvWCity.writeNext(l);
					}
				}
				csvR.close();
			}
		}
		csvWGen.close();
		csvWCity.close();
		genCoeffVar(nameGen, nameCity, getIndicFolder());
	}

	public void genCoeffVar(String nameGen, String nameCity, File indicFolder) throws IOException {
		CSVReader csvGen = new CSVReader(new FileReader(new File(indicFolder, nameGen)));
		String[] fLine = csvGen.readNext();
		CSVReader csvCity = new CSVReader(new FileReader(new File(indicFolder, nameCity)));
		csvCity.readNext();
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(indicFolder, nameGen.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWGen.writeNext(fLine);
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(indicFolder, nameCity.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWCity.writeNext(fLine);
		DescriptiveStatistics[] listStatGen = new DescriptiveStatistics[fLine.length - 3];
		// boolean to get the existing DS
		boolean ini = true;
		for (String[] l : csvGen.readAll()) {
			for (int i = 3; i < fLine.length; i++) {
				DescriptiveStatistics ds = new DescriptiveStatistics();
				if (!ini) {
					ds = listStatGen[i - 3];
				}
				ds.addValue(Double.valueOf(l[i]));
				listStatGen[i - 3] = ds;
			}
			ini = false;
		}
		String[] line = new String[fLine.length];
		line[0] = scenarName;
		line[1] = "AllVariant";
		line[2] = "AllZone";
		for (int i = 3; i < fLine.length; i++) {
			double sal = listStatGen[i - 3].getStandardDeviation() / listStatGen[i - 3].getMean();
			if (Double.isNaN(sal)) {
				sal = 0;
			}
			line[i] = String.valueOf(sal);
		}
		csvWGen.writeNext(line);

		// for the cities
		Hashtable<String, DescriptiveStatistics[]> listStatCity = new Hashtable<String, DescriptiveStatistics[]>();
		for (String[] l : csvCity.readAll()) {
			String city = l[2];
			if (listStatCity.containsKey(city)) {
				DescriptiveStatistics[] listStat = listStatCity.remove(city);
				for (int i = 3; i < fLine.length; i++) {
					DescriptiveStatistics ds = listStat[i - 3];
					ds.addValue(Double.valueOf(l[i]));
					listStat[i - 3] = ds;
				}
				listStatCity.put(city, listStat);
			} else {
				DescriptiveStatistics[] newListStat = new DescriptiveStatistics[fLine.length - 3];
				for (int i = 3; i < fLine.length; i++) {
					DescriptiveStatistics ds = new DescriptiveStatistics();
					ds.addValue(Double.valueOf(l[i]));
					newListStat[i - 3] = ds;
				}
				listStatCity.put(city, newListStat);
			}
		}
		for (String city : listStatCity.keySet()) {
			String[] lineCity = new String[fLine.length];
			lineCity[0] = scenarName;
			lineCity[1] = "AllVariant";
			lineCity[2] = city;
			DescriptiveStatistics[] cityStat = listStatCity.get(city);
			for (int i = 3; i < fLine.length; i++) {
				double sal = cityStat[i - 3].getStandardDeviation() / cityStat[i - 3].getMean();
				if (Double.isNaN(sal)) {
					sal = 0;
				}
				lineCity[i] = String.valueOf(sal);
			}
			csvWCity.writeNext(lineCity);
		}
		csvCity.close();
		csvGen.close();
		csvWCity.close();
		csvWGen.close();
	}

	// public void createGraph(File csvInj, Stage stage ) throws IOException {
	// //from http://lauraliparulo.altervista.org/data-science-java-part-2-csv-data-charts/
	// stage.setTitle("Index Chart Sample");
	// final NumberAxis yAxis = new NumberAxis(0, 5000000, 1);
	// final CategoryAxis xAxis = new CategoryAxis();
	// yAxis.setLabel("People without job");
	//
	// final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
	// xAxis.setLabel("year");
	// lineChart.setTitle("Unemployment in Germnay");
	//
	// XYChart.Series series = new XYChart.Series();
	// XYChart.Series seriesWest = new XYChart.Series();
	// XYChart.Series seriesEast = new XYChart.Series();
	//
	// series.setName("Germany");
	// seriesWest.setName("West Germany");
	// seriesEast.setName("East Germany");
	//
	// try (CSVReader dataReader = new CSVReader(new FileReader("docs/unemployment_germany.csv"))) {
	// String[] nextLine;
	// while ((nextLine = dataReader.readNext()) != null) {
	// String year = String.valueOf(nextLine[0]);
	// int population = Integer.parseInt(nextLine[1]);
	// series.getData().add(new XYChart.Data(year, population));
	// int populationWest = Integer.parseInt(nextLine[2]);
	// ;
	// seriesWest.getData().add(new XYChart.Data(year, populationWest));
	// int populationEast = Integer.parseInt(nextLine[3]);
	// seriesEast.getData().add(new XYChart.Data(year, populationEast));
	// }
	// }
	//
	// lineChart.getData().addAll(series, seriesWest, seriesEast);
	// Scene scene = new Scene(lineChart, 500, 400);
	// stage.setScene(scene);
	// stage.show();
	// }

	public void createGraphBHT(File distrib) throws IOException {
		createGraphBHT(distrib, getGraphDepotFolder());
	}

	public void createGraphBHT(File distrib, File graphDepot) throws IOException {
		System.out.println("distr ib " + distrib);
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans une commune de type rurale", "nameVariant", "Variante ", "nbBuild_rural",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans une commune de type péri-urbain", "nameVariant", "Variante ",
				"nbBuild_periUrbain", "Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans un quartier de type banlieue", "nameVariant", "Variante ",
				"nbBuild_banlieue", "nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans un quartier de type centre", "nameVariant", "Variante ", "nbBuild_centre",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans une zone non constructible (NC)", "nameVariant", "Variante ", "nbBuild_NC",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans une zone à urbaniser (AU)", "nameVariant", "Variante ", "nbBuild_AU",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés dans une zone urbanisable (U)", "nameVariant", "Variante ", "nbBuild_U",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés de type maison isolée", "nameVariant", "Variante ", "nbBuild_detachedHouse",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés de type pavillon de lotissement", "nameVariant", "Variante ",
				"nbBuild_smallHouse", "Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés de type immeuble d'habitat intermédiaire", "nameVariant", "Variante ",
				"nbBuild_multiFamilyHouse", "Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés de type petit immeuble collectif", "nameVariant", "Variante ",
				"nbBuild_smallBlockFlat", "Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés de type immeuble collectif de taille moyenne", "nameVariant", "Variante ",
				"nbBuild_midBlockFlat", "Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de bâtiments simulés par variante", "nameVariant", "Variante ", "nb_building",
				"Nombre de bâtiments simulés");
		makeGraph(distrib, graphDepot, "Nombre de logements simulés par variante", "nameVariant", "Variante ", "nb_housingUnit",
				"Nombre de logements simulés", 700, 420);
	}

	public void createGraphParcelStat(File distrib) throws IOException {
		createGraphParcelStat(distrib, getGraphDepotFolder());
	}

	public void createGraphParcelStat(File distrib, File graphDepot) throws IOException {
		System.out.println("distr ib " + distrib);
		makeGraph(distrib, graphDepot, "Parcelles simulées dans une commune de type rurale", "nameVariant", "Variante ", "nbParcelSimulatedRural",
				"Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles simulées dans une commune de type péri-urbain", "nameVariant", "Variante ",
				"nbParcelSimulatedPeriUrb", "Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles simulées dans un quartier de type banlieue", "nameVariant", "Variante ",
				"nbParcelSimulatedBanlieue", "nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles simulées dans un quartier de type centre", "nameVariant", "Variante ", "nbParcelSimulatedCentre",
				"Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles simulées dans une zone à urbaniser (AU)", "nameVariant", "Variante ", "nbParcelSimulatedAU",
				"parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles simulées dans une zone urbanisable (U)", "nameVariant", "Variante ", "nbParcelSimulatedU",
				"Nombre de parcelles simulés");

		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les commune de type rurale", "nameVariant", "Variante ",
				"nbParcelSimulFailedRural", "Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les commune de type péri-urbain", "nameVariant", "Variante ",
				"nbParcelSimulFailedPeriUrb", "Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les quartier de type banlieue", "nameVariant", "Variante ",
				"nbParcelSimulFailedBanlieue", "nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les quartier de type centre", "nameVariant", "Variante ",
				"nbParcelSimulFailedCentre", "Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les zone à urbaniser (AU)", "nameVariant", "Variante ", "nbParcelSimulFailedAU",
				"Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées pour les zone urbanisable (U)", "nameVariant", "Variante ", "nbParcelSimulFailedU",
				"Nombre de parcelles simulés");

		makeGraph(distrib, graphDepot, "Nombre de parcelles simulées par variante", "nameVariant", "Variante ", "nb_parcel_simulated",
				"Nombre de parcelles simulés");
		makeGraph(distrib, graphDepot, "Parcelles non simulées par variante", "nameVariant", "Variante ", "nb_parcel_simu_failed",
				"Nombre de logements simulés", 700, 420);
	}

	public static void makeGraph(File csv, File graphDepotFile, String title, String x, String xTitle, String y, String yTitle) throws IOException {
		makeGraph(csv, graphDepotFile, title, x, xTitle, y, yTitle, 600, 475);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void makeGraph(File csv, File graphDepotFile, String title, String x, String xTitle, String y, String yTitle, int width, int height)
			throws IOException {
		CategorySeries csvData = CSVImporter.getCategorySeriesFromCSVFile(csv, x, y, yTitle + "-" + xTitle, DataType.String);
		// Create Chart
		CategoryChart chart = new CategoryChartBuilder().width(width).height(height).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		// chart.addSeries(yTitle, csvDaa.getxAxisData(), csvDaa.getyAxisData());
		List<String> goodNames = new LinkedList<String>();
		Iterator<?> it = csvData.getXData().iterator();
		while (it.hasNext()) {
			String s = ((String) it.next()).replace("variant", "");
			// System.out.println("position varianteComp" + s);
			goodNames.add(s);
		}
		chart.addSeries(yTitle, goodNames, (List) csvData.getYData());
		// Customize Chart
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(true);

		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + y, BitmapFormat.PNG);

	}

	public void allOfTheMapsBTH() throws IOException, NoSuchAuthorityCodeException, FactoryException {
		allOfTheMapsBTH(this.getMapDepotFolder(), "");
	}

	public void allOfTheMapsBTH(File depot, String prefix) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();

		MapRenderer mapNbHUCV = new CVNbBat(1000, 1000, this.getMapStyle(), this.getCommStatFile(), depot, prefix);
		mapNbHUCV.renderCityInfo();
		mapNbHUCV.generateSVG();
		allOfTheMaps.add(mapNbHUCV);
	}

	public void allOfTheMapsParcelStat() throws IOException, NoSuchAuthorityCodeException, FactoryException {
		allOfTheMapsParcelStat(this.getMapDepotFolder(), "");
	}

	public void allOfTheMapsParcelStat(File depot, String prefix) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();

		MapRenderer mapNbHUCV = new CVParcelFailed(1000, 1000, this.getMapStyle(), this.getCommStatFile(), depot, prefix);
		mapNbHUCV.renderCityInfo();
		mapNbHUCV.generateSVG();
		allOfTheMaps.add(mapNbHUCV);
	}

	// public void createGraph(File csvIn) throws IOException {
	// Table distrib = Table.read().csv(csvIn.toString());
	// // see tutorial here https://dzone.com/articles/learn-data-science-with-java-and-tablesaw
	// // NumberColumn toPlot = distrib.numberColumn(nameColumn);
	// // Table distribClean = distrib;
	// // Table fatalities1 =
	// // distrib.summarize("SDPTot", sum).by("nameVariant");
	// // System.out.println(fatalities1);
	//
	// // distrib.sortAscendingOn("");
	// // Plot.show(Histogram.create("surf de pl", distrib, "SDPTot"),outFile);
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans une commune de type rurale", distrib, "nameVariant", "nbHU_rural"),
	// new File(graphDepotFile, "nbHU_rural"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans une commune de type péri-urbain", distrib, "nameVariant",
	// "nbHU_periUrbain"), new File(graphDepotFile, "nbHU_periUrbain"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans un quartier de type banlieue", distrib, "nameVariant", "nbHU_banlieue"),
	// new File(graphDepotFile, "nbHU_banlieue"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans un quartier de type centre", distrib, "nameVariant", "nbHU_centre"),
	// new File(graphDepotFile, "nbHU_centre"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans une zone non constructible (NC)", distrib, "nameVariant", "nbHU_NC"),
	// new File(graphDepotFile, "nbHU_NC"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans une zone à urbaniser (AU)", distrib, "nameVariant", "nbHU_AU"),
	// new File(graphDepotFile, "nbHU_AU"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés dans une zone urbanisable (U)", distrib, "nameVariant", "nbHU_U"),
	// new File(graphDepotFile, "nbHU_U"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés de type maison isolée", distrib, "nameVariant", "nbHU_detachedHouse"),
	// new File(graphDepotFile, "nbHU_detachedHouse"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés de type pavillon de lotissement", distrib, "nameVariant", "nbHU_smallHouse"),
	// new File(graphDepotFile, "nbHU_smallHouse"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés de type immeuble d'habitat intermédiaire", distrib, "nameVariant",
	// "nbHU_multiFamilyHouse"), new File(graphDepotFile, "nbHU_multiFamilyHouse"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés de type petit immeuble collectif", distrib, "nameVariant",
	// "nbHU_smallBlockFlat"), new File(graphDepotFile, "nbHU_smallBlockFlat"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés de type immeuble collectif de taille moyenne", distrib, "nameVariant",
	// "nbHU_midBlockFlat"), new File(graphDepotFile, "nbHU_midBlockFlat"));
	// Plot.show(HorizontalBarPlot.create("Surface de plancher par variantes", distrib, "nameVariant", "SDPTot"),
	// new File(graphDepotFile, "compVariantSDP"));
	// Plot.show(HorizontalBarPlot.create("Nombre de logements simulés par variantes", distrib, "nameVariant", "nb_housingUnit"),
	// new File(graphDepotFile, "compVariantNbHU"));
	// }
}
