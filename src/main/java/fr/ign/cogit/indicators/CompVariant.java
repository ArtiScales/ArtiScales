package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class CompVariant extends Indicators {

	String[] baseVariant;
	String indicStatFile;
	static String indicName = "compVariant";

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result2903/tmp");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		String scenario = "CDense";
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterTechnic.xml"));
		lF.add(new File(rootParam, "paramSet/" + scenario + "/parameterScenario.xml"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		CompVariant parc = new CompVariant(p, "compVariant", "ParcelStat.csv", rootFile, scenario);
		// parc.createGraph(new File(parc.indicFile, "compVariantbTHGen.csv"));

		// parc.createStat("bTH", "genStat.csv");
		// List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();
		//
		// File commStatFile = parc.joinStatoBTHCommunnities("compVariantbTHCityCoeffVar.csv");
		//
		// MapRenderer mapNbHUCV = new MapNbHUCV(1000, 1000, parc.mapStyle, commStatFile, parc.mapDepotFile);
		// mapNbHUCV.renderCityInfo();
		// mapNbHUCV.generateSVG();
		// allOfTheMaps.add(MapNbHUCV);
	}

	public CompVariant(SimpluParametersJSON p, String indicname, String indicstatfile, File rootfile, String scenarname) throws Exception {
		super(p, rootfile, scenarname, "", indicName);

		this.indicStatFile = indicstatfile;
		super.mapDepotFile = new File(indicFile, "mapDepot");
		super.mapDepotFile.mkdir();
		super.graphDepotFile = new File(indicFile, "graphDepot");
		super.graphDepotFile.mkdir();
	}

	public void createStat(String nameCompared, String nameFileStat) throws IOException {
		String nameGen = "compVariant" + nameCompared + "Gen.csv";
		String nameCity = "compVariant" + nameCompared + "City.csv";
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(indicFile, nameGen)), ',', '\u0000');
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(indicFile, nameCity)), ',', '\u0000');
		boolean firstLine = true;
		for (File f : (new File(super.rootFile, "indic/" + nameCompared + "/" + super.scenarName + "/")).listFiles()) {
			File statFile = new File(f, nameFileStat);
			if (f.isDirectory() && statFile.exists()) {
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
					if (l[2].equals("AllZone") || l[2].equals("ALLLL")) {
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
		genCoeffVar(nameGen, nameCity);

	}

	public void genCoeffVar(String nameGen, String nameCity) throws IOException {
		CSVReader csvGen = new CSVReader(new FileReader(new File(indicFile, nameGen)));
		String[] fLine = csvGen.readNext();
		CSVReader csvCity = new CSVReader(new FileReader(new File(indicFile, nameCity)));
		csvCity.readNext();
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(indicFile, nameGen.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWGen.writeNext(fLine);
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(indicFile, nameCity.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
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
			line[i] = String.valueOf(listStatGen[i - 3].getStandardDeviation() / listStatGen[i - 3].getMean());
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
				lineCity[i] = String.valueOf(cityStat[i - 3].getStandardDeviation() / cityStat[i - 3].getMean());
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
