package fr.ign.cogit.indicators;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.util.FromGeom;
import fr.ign.parameters.Parameters;

public class ParcelStat extends Indicators {

	File parcelFile;
	File rootFile;
	File geoFile;

	String firstLine;

	public ParcelStat(Parameters p, File parcelFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		super(p);

		this.parcelFile = parcelFile;

		rootFile = new File(p.getString("rootFile"));
		geoFile = new File(rootFile, "dataGeo");

		firstLine = "INSEE,parcel_selected,parcel_selected_in_U,parcel_selected_in_AU,parcel_selected_in_NC,parcel_constructed,parcel_constructed_in_U,parcel_constructed_in_AU,parcel_constructed_in_NC,";

		isParcelReallyBuilt();

	}

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0MCIgn");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parameterTechnic.xml"));
		lF.add(new File(rootParam, "parameterScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		File parcelFile = new File("/home/mcolomb/informatique/ArtiScales/indic/parcelOut/teststp/variant0/parcelGenExport.shp");

		ParcelStat parc = new ParcelStat(p, parcelFile);
		parc.run();

	}

	/**
	 * for each parcel, set the already existing field "IsBuild" if a new building has been simulated on this parcel
	 * 
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public void isParcelReallyBuilt() throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File simuBuildFiles = new File(rootFile,
				"SimPLUDepot" + "/" + parcelFile.getParentFile().getParentFile().getName() + "/" + parcelFile.getParentFile().getName());
		System.out.println(parcelFile);
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
		DefaultFeatureCollection out = new DefaultFeatureCollection();
		try {
			while (parcelFeaturesIt.hasNext()) {
				SimpleFeature feature = parcelFeaturesIt.next();
				for (File f : simuBuildFiles.listFiles()) {
					if (f.getName().startsWith("out-parcel") && f.getName().endsWith(".shp")) {
						String inseeNum = f.getName().split("_")[1].substring(0, f.getName().split("_")[1].length() - 4);
						if (inseeNum.equals(feature.getAttribute("CODE"))) {
							feature.setAttribute("IsBuild", true);
							out.add(feature);
							break;
						} else {
							feature.setAttribute("IsBuild", false);
						}
					}
					out.add(feature);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelFeaturesIt.close();
		}
		parcelSDS.dispose();
		Vectors.exportSFC(out.collection(), parcelFile);
	}

	public String getFirstlineGenCsv() {

		return super.getFirstlineCsv() + firstLine;
	}

	public void run() throws IOException, NoSuchAuthorityCodeException, FactoryException {

		Indicators.firstLineGen = true;

		for (String city : FromGeom.getInsee(parcelFile)) {
			double surfSelect = 0;
			double surfSelectU = 0;
			double surfSelectAU = 0;
			double surfSelectNC = 0;

			double surfSimulated = 0;
			double surfSimulatedU = 0;
			double surfSimulatedAU = 0;
			double surfSimulatedNC = 0;
			ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
			SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
			try {
				while (parcelFeaturesIt.hasNext()) {
					SimpleFeature feature = parcelFeaturesIt.next();
					if (city.equals((String) feature.getAttribute("INSEE"))) {
						if (((String) feature.getAttribute("DoWeSimul")).equals("true")) {
							double area = ((Geometry) feature.getDefaultGeometry()).getArea();
							surfSelect = surfSelect + area;
							if ((boolean) feature.getAttribute("U")) {
								surfSelectU = surfSelectU + area;
							} else if ((boolean) feature.getAttribute("AU")) {
								surfSelectAU = surfSelectAU + area;
							} else if ((boolean) feature.getAttribute("NC")) {
								surfSelectNC = surfSelectNC + area;
							}
						}
						if ((boolean) feature.getAttribute("IsBuild")) {
							double area = ((Geometry) feature.getDefaultGeometry()).getArea();
							surfSimulated = surfSimulated + area;
							if ((boolean) feature.getAttribute("U")) {
								surfSimulatedU = surfSimulatedU + area;
							} else if ((boolean) feature.getAttribute("AU")) {
								surfSimulatedAU = surfSimulatedAU + area;
							} else if ((boolean) feature.getAttribute("NC")) {
								surfSimulatedNC = surfSimulatedNC + area;
							}
						}
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				parcelFeaturesIt.close();
			}
			parcelSDS.dispose();
			if (surfSelect > 0) {
				String line = city + "," + surfSelect + "," + surfSelectU + "," + surfSelectAU + "," + surfSelectNC + "," + surfSimulated + ","
						+ surfSimulatedU + "," + surfSimulatedAU + "," + surfSimulatedNC;
				System.out.println(line);
				toGenCSV(parcelFile.getParentFile(), "parcelStat", getFirstlineGenCsv(), line);
			}
		}
	}
}
