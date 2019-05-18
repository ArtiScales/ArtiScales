package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CSVImporter;
import org.knowm.xchart.CSVImporter.DataOrientation;
import org.knowm.xchart.CSVImporter.SeriesData;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.compVariant.MapNbHUCV;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class CompVariant extends Indicators {
	int nbVariant = 0;
	String[] baseVariant;
	String indicStatFile;
	static String indicName = "compVariant";

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result2903/");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		String scenario = "CDense";
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterTechnic.xml"));
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterScenario.xml"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		CompVariant parc = new CompVariant(p, rootFile, scenario);
		parc.createStat("bTH", "genStat.csv");
		List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();

		File commStatFile = parc.joinStatoBTHCommunities("compVariantbTHCityCoeffVar.csv");

		parc.createGraph(new File(parc.getIndicFile(), "compVariantbTHGen.csv"));

		MapRenderer mapNbHUCV = new MapNbHUCV(1000, 1000, parc.getMapStyle(), commStatFile, parc.getMapDepotFile());
		mapNbHUCV.renderCityInfo();
		mapNbHUCV.generateSVG();
		allOfTheMaps.add(mapNbHUCV);
	}

	public CompVariant(SimpluParametersJSON p, File rootfile, String scenarname) throws Exception {
		super(p, rootfile, scenarname, "", indicName);
	}

	public void createStat(String nameCompared, String nameFileStat) throws IOException {
		String nameGen = indicName + nameCompared + "Gen.csv";
		String nameCity = indicName + nameCompared + "City.csv";
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(getIndicFile(), nameGen)), ',', '\u0000');
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(getIndicFile(), nameCity)), ',', '\u0000');
		boolean firstLine = true;
		for (File f : (new File(super.rootFile, "indic/" + nameCompared + "/" + super.scenarName + "/")).listFiles()) {
			File statFile = new File(f, nameFileStat);
			if (f.isDirectory() && statFile.exists()) {
				CSVReader csvR = new CSVReader(new FileReader(statFile));
				if (firstLine) {
					String[] fLineTmp = csvR.readNext();
					// adding a count of the variant
					String[] fLine = new String[fLineTmp.length + 1];
					int y = 0;
					for (int i = 0; i < fLine.length; i++) {
						if (i == 3) {
							fLine[i] = "nbVariant";
						} else {
							fLine[i] = fLineTmp[y++];
						}
					}
					csvWGen.writeNext(fLine);
					csvWCity.writeNext(fLine);
					firstLine = false;
				} else {
					csvR.readNext();
				}
				for (String[] l : csvR.readAll()) {
					String insee = l[2];
					String lineTmp = "";
					for (String s : l) {
						lineTmp = lineTmp + s + ",";
					}
					// System.out.println(insee + ",");
					// System.out.println(insee + "," + String.valueOf(nbVariant) + ",");
					String[] line = lineTmp.replace(insee + ",", insee + "," + String.valueOf(nbVariant) + ",").split(",");
					if (insee.equals("AllZone") || insee.equals("ALLLL")) {
						csvWGen.writeNext(line);
					} else {
						csvWCity.writeNext(line);
					}
				}
				csvR.close();
			}
			nbVariant++;
		}
		csvWGen.close();
		csvWCity.close();
		genCoeffVar(nameGen, nameCity);

	}

	public void genCoeffVar(String nameGen, String nameCity) throws IOException {
		CSVReader csvGen = new CSVReader(new FileReader(new File(getIndicFile(), nameGen)));
		String[] fLine = csvGen.readNext();
		CSVReader csvCity = new CSVReader(new FileReader(new File(getIndicFile(), nameCity)));
		csvCity.readNext();
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(getIndicFile(), nameGen.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWGen.writeNext(fLine);
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(getIndicFile(), nameCity.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWCity.writeNext(fLine);
		DescriptiveStatistics[] listStatGen = new DescriptiveStatistics[fLine.length - 4];
		// boolean to get the existing DS
		boolean ini = true;
		for (String[] l : csvGen.readAll()) {
			for (int i = 4; i < fLine.length; i++) {
				DescriptiveStatistics ds = new DescriptiveStatistics();
				if (!ini) {
					ds = listStatGen[i - 4];
				}
				ds.addValue(Double.valueOf(l[i]));
				listStatGen[i - 4] = ds;
			}
			ini = false;
		}
		String[] line = new String[fLine.length];
		line[0] = scenarName;
		line[1] = "AllVariant";
		line[2] = "AllZone";
		line[3] = String.valueOf(nbVariant);
		for (int i = 4; i < fLine.length; i++) {
			line[i] = String.valueOf(listStatGen[i - 4].getStandardDeviation() / listStatGen[i - 4].getMean());
		}
		csvWGen.writeNext(line);

		// for the cities
		Hashtable<String, DescriptiveStatistics[]> listStatCity = new Hashtable<String, DescriptiveStatistics[]>();
		for (String[] l : csvCity.readAll()) {
			String city = l[2];
			if (listStatCity.containsKey(city)) {
				DescriptiveStatistics[] listStat = listStatCity.remove(city);
				for (int i = 4; i < fLine.length; i++) {
					DescriptiveStatistics ds = listStat[i - 4];
					ds.addValue(Double.valueOf(l[i]));
					listStat[i - 4] = ds;
				}
				listStatCity.put(city, listStat);
			} else {
				DescriptiveStatistics[] newListStat = new DescriptiveStatistics[fLine.length - 4];
				for (int i = 4; i < fLine.length; i++) {
					DescriptiveStatistics ds = new DescriptiveStatistics();
					ds.addValue(Double.valueOf(l[i]));
					newListStat[i - 4] = ds;
				}
				listStatCity.put(city, newListStat);
			}
		}
		for (String city : listStatCity.keySet()) {
			String[] lineCity = new String[fLine.length];
			lineCity[0] = scenarName;
			lineCity[1] = "AllVariant";
			lineCity[2] = city;
			line[3] = String.valueOf(nbVariant);
			DescriptiveStatistics[] cityStat = listStatCity.get(city);
			for (int i = 4; i < fLine.length; i++) {
				lineCity[i] = String.valueOf(cityStat[i - 4].getStandardDeviation() / cityStat[i - 4].getMean());
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
	//
	// final LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
	// yAxis.setLabel("People without job");
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

	public void createGraph(File distrib) throws IOException {
		makeGraph(distrib, graphDepotFile, "exemple on SDPTot", "nbVariant", "Variante", "SDPTot", "Surface De Plancher Totale");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans une commune de type rurale", "nbVariant", "Variante ", "nbHU_rural",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans une commune de type péri-urbain", "nbVariant", "Variante ",
				"nbHU_periUrbain", "Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans un quartier de type banlieue", "nbVariant", "Variante ", "nbHU_banlieue",
				"nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans un quartier de type centre", "nbVariant", "Variante ", "nbHU_centre",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans une zone non constructible (NC)", "nbVariant", "Variante ", "nbHU_NC",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans une zone à urbaniser (AU)", "nbVariant", "Variante ", "nbHU_AU",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés dans une zone urbanisable (U)", "nbVariant", "Variante ", "nbHU_U",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés de type maison isolée", "nbVariant", "Variante ", "nbHU_detachedHouse",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés de type pavillon de lotissement", "nbVariant", "Variante ", "nbHU_smallHouse",
				"Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés de type immeuble d'habitat intermédiaire", "nbVariant", "Variante ",
				"nbHU_multiFamilyHouse", "Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés de type petit immeuble collectif", "nbVariant", "Variante ",
				"nbHU_smallBlockFlat", "Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés de type immeuble collectif de taille moyenne", "nbVariant", "Variante ",
				"nbHU_midBlockFlat", "Nombre de logements simulés");
		makeGraph(distrib, graphDepotFile, "Nombre de logements simulés par variantes", "nbVariant", "Variante ", "nb_housingUnit",
				"Nombre de logements simulés");
	}

	public static void makeGraph(File csv, File graphDepotFile, String title, String x, String xTitle, String y, String yTitle) throws IOException {

		SeriesData csvData = CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, x, y);

		// Create Chart
		CategoryChart chart = new CategoryChartBuilder().width(800).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		chart.addSeries(yTitle, csvData.getxAxisData(), csvData.getyAxisData());
		// Customize Chart
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(true);

		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + y, BitmapFormat.PNG);

		// new SwingWrapper(chart).displayChart();
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
