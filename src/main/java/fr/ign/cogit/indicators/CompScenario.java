package fr.ign.cogit.indicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CSVImporter;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.internal.series.Series.DataType;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.compVariant.CVNbBat;

public class CompScenario extends Indicators {
	String[] baseScenario;
	String indicStatFile;
	static String variante = "base";
	static String indicName = "compScenario";

	public static void main(String[] args) throws Exception {
		 File rootFile = new File("./result2903/");
		 run(rootFile);
	}
	public static void run(File rootFile) throws Exception {
		
		 CompScenario compScen = new CompScenario(rootFile);
		
		 compScen.createStat("bTH", "genStat.csv");
		 List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();
		
		 compScen.setCommStatFile(compScen.joinStatBTHtoCommunities("compScenariobTHCity.csv"));
		 compScen.createGraph(new File(compScen.getIndicFolder(), "compScenariobTHGen.csv"));
		
		 MapRenderer mapNbHUCV = new CVNbBat(1000, 1000, compScen.getMapStyle(), compScen.getCommStatFile(), compScen.getMapDepotFolder(), "");
		 mapNbHUCV.renderCityInfo();
		 mapNbHUCV.generateSVG();
		 allOfTheMaps.add(mapNbHUCV);

		mergeDenialStat(new File("/home/ubuntu/boulot/these/result2903/indic/bTH/"), "base", new File("/tmp/stat"));
	}

	public CompScenario(File rootfile) throws Exception {
		super(null, rootfile, "", variante, indicName);
		setIndicFile(new File(getRootFile(), "indic/" + indicName + "/" + scenarName + "/" + variantName));
		getIndicFolder().mkdirs();
		setMapStyle(new File(getRootFile(), "mapStyle"));
		setMapDepotFolder(new File(getIndicFolder(), "mapDepot"));
		getMapDepotFolder().mkdir();
		setGraphDepotFolder(new File(getIndicFolder(), "graphDepot"));
		getGraphDepotFolder().mkdir();
	}

	public void createStat(String nameCompared, String nameFileStat) throws IOException {
		String nameGen = indicName + nameCompared + "Gen.csv";
		String nameCity = indicName + nameCompared + "City.csv";
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameGen)), ',', '\u0000');
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameCity)), ',', '\u0000');
		boolean firstLine = true;
		for (File fRetarded : (new File(super.getRootFile(), "indic/" + nameCompared + "/")).listFiles()) {
			File indicFile = new File(fRetarded, variante);
			if (indicFile.exists()) {
				File statFile = new File(indicFile, nameFileStat);
				if (indicFile.isDirectory() && statFile.exists()) {
					CSVReader csvR = new CSVReader(new FileReader(statFile));
					String[] fLine = csvR.readNext();
					if (firstLine) {
						csvWGen.writeNext(fLine);
						csvWCity.writeNext(fLine);
						firstLine = false;
					}

					for (String[] l : csvR.readAll()) {
						String insee = l[2];
						if (insee.equals("AllZone") || insee.equals("ALLLL")) {
							csvWGen.writeNext(l);
						} else {
							csvWCity.writeNext(l);
						}
					}
					csvR.close();
				}
			}
		}
		csvWGen.close();
		csvWCity.close();
		// genCoeffVar(nameGen, nameCity);
	}

	public void genCoeffVar(String nameGen, String nameCity) throws IOException {
		CSVReader csvGen = new CSVReader(new FileReader(new File(getIndicFolder(), nameGen)));
		String[] fLine = csvGen.readNext();
		CSVReader csvCity = new CSVReader(new FileReader(new File(getIndicFolder(), nameCity)));
		csvCity.readNext();
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameGen.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
		csvWGen.writeNext(fLine);
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(getIndicFolder(), nameCity.replace(".csv", "") + "CoeffVar.csv")), ',', '\u0000');
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
		line[1] = "AllScenario";
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
			lineCity[1] = "AllScenario";
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

	public void createGraph(File distrib) throws IOException {
		makeGraph(distrib, getGraphDepotFolder(), "exemple on SDPTot", "nbScenario", "Scenario", "SDPTot", "Surface De Plancher Totale");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans une commune de type rurale", "nameScenario", "Scenario ",
				"nbHU_rural", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans une commune de type péri-urbain", "nameScenario", "Scenario ",
				"nbHU_periUrbain", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans un quartier de type banlieue", "nameScenario", "Scenario ",
				"nbHU_banlieue", "nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans un quartier de type centre", "nameScenario", "Scenario ",
				"nbHU_centre", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans une zone non constructible (NC)", "nameScenario", "Scenario ",
				"nbHU_NC", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans une zone à urbaniser (AU)", "nameScenario", "Scenario ",
				"nbHU_AU", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés dans une zone urbanisable (U)", "nameScenario", "Scenario ", "nbHU_U",
				"Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés de type maison isolée", "nameScenario", "Scenario ",
				"nbHU_detachedHouse", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés de type pavillon de lotissement", "nameScenario", "Scenario ",
				"nbHU_smallHouse", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés de type immeuble d'habitat intermédiaire", "nameScenario", "Scenario ",
				"nbHU_multiFamilyHouse", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés de type petit immeuble collectif", "nameScenario", "Scenario ",
				"nbHU_smallBlockFlat", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés de type immeuble collectif de taille moyenne", "nameScenario",
				"Scenario ", "nbHU_midBlockFlat", "Nombre de logements simulés");
		makeGraph(distrib, getGraphDepotFolder(), "Nombre de logements simulés par Scenarios", "nameScenario", "Scenario ", "nb_housingUnit",
				"Nombre de logements simulés");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void makeGraph(File csv, File graphDepotFile, String title, String x, String xTitle, String y, String yTitle) throws IOException {

		// SeriesData csvDaa = CSVImporter.getSeriesDataFromCSVFile(csv, DataOrientation.Columns, x, y);
		CategorySeries csvData = CSVImporter.getCategorySeriesFromCSVFile(csv, x, y, yTitle + "-" + xTitle, DataType.String);
		// Create Chart
		CategoryChart chart = new CategoryChartBuilder().width(800).height(600).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		// chart.addSeries(yTitle, csvDaa.getxAxisData(), csvDaa.getyAxisData());

		chart.addSeries(yTitle, (List) csvData.getXData(), (List) csvData.getYData());
		// Customize Chart
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(true);

		BitmapEncoder.saveBitmap(chart, graphDepotFile + "/" + y, BitmapFormat.PNG);

		// new SwingWrapper(chart).displayChart();
	}

	public static void mergeDenialStat(File indicFile, String variant, File outFile) throws IOException {
		HashMap<String, Long> tot = new HashMap<String, Long>();

		System.out.println(indicFile);
		for (File f : indicFile.listFiles()) {
			System.out.println(new File(f, variant + "/StatDenialCuboid.csv"));
			CSVReader read = new CSVReader(new FileReader(new File(f, variant + "/StatDenialCuboid.csv")), ',', '\u0000');
			for (String[] l : read.readAll()) {
				if (tot.containsKey(l[0])) {
					Long tmp = tot.remove(l[0]);
					tot.put(l[0], tmp + Long.valueOf(l[1].replace("\"", "")));
				} else {
					tot.put(l[0], Long.valueOf(l[1].replace("\"", "")));
				}
			}
			read.close();
		}
		FileWriter w = new FileWriter(outFile);
		for (String s : tot.keySet()) {
			w.write(s + "," + tot.get(s) + "\n");
		}
		w.close();
	}
}
