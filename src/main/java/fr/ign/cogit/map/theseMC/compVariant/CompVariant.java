package fr.ign.cogit.map.theseMC.compVariant;

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
import fr.ign.cogit.indicators.ParcelStat;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class CompVariant extends ParcelStat {

	String[] baseVariant;
	String indicName, indicStatFile;

	public static void main(String[] args) throws Exception {
		File rootFile = new File("./result0308/");
		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "paramSet/DDense/parameterTechnic.xml"));
		lF.add(new File(rootParam, "paramSet/DDense/parameterScenario.xml"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		CompVariant parc = new CompVariant(p, "bTH", "genStat.csv", rootFile, "DDense");
		parc.createStatParcel();
	}

	public CompVariant(SimpluParametersJSON p, String indicname, String indicstatfile, File rootfile, String scenarname) throws Exception {
		super(p, rootfile, scenarname, "");
		this.indicName = indicname;
		this.indicStatFile = indicstatfile;

		super.indicFile = new File(rootFile, "indic/" + indicName + "/" + scenarName + "/compVariant");
		super.indicFile.mkdirs();
		super.mapDepotFile = new File(indicFile, "mapDepot");
		super.mapDepotFile.mkdir();
	}

	public void createStatParcel() throws IOException {
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(indicFile, "compVariantParcelStatGen.csv")));
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(indicFile, "compVariantParcelStatCity.csv")));
		boolean firstLine = true;
		for (File f : (new File(super.rootFile, "indic/" + indicName + "/" + super.scenarName)).listFiles()) {
			File statFile = new File(f, indicStatFile);
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

		genCoeffVar();

	}

	public void genCoeffVar() throws IOException {
		CSVReader csvGen = new CSVReader(new FileReader(new File(indicFile, "compVariantParcelStatGen.csv")));
		String[] fLine = csvGen.readNext();
		CSVReader csvCity = new CSVReader(new FileReader(new File(indicFile, "compVariantParcelStatCity.csv")));
		csvCity.readNext();
		CSVWriter csvWGen = new CSVWriter(new FileWriter(new File(indicFile, "compVariantParcelStatGenCoeffVar.csv")));
		csvWGen.writeNext(fLine);
		CSVWriter csvWCity = new CSVWriter(new FileWriter(new File(indicFile, "compVariantParcelStatCityCoeffVar.csv")));
		csvWGen.writeNext(fLine);
		DescriptiveStatistics[] listStatGen = new DescriptiveStatistics[fLine.length - 3];
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
}
