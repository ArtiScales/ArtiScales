package fr.ign.cogit.createGeom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;

public class DensIni {

	public static void main(String[] args) throws Exception {
		String nameFieldLgt = "P12_LOG";
		String nameFieldCodeCsv = "COM";
		String nameInseeFileOut = "DEPCOM";
		File zoningFile = new File("/home/ubuntu/boulot/these/result2903/tmp/dataRegulation/zoning.shp");
		File nbLgtFile = new File("/home/ubuntu/Téléchargements/base-ic-logement-2012.csv");
		List<File> filesToAddInitialDensity = new ArrayList<File>();
		filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/old/communities.shp"));
		filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp"));
		createCommunitiesWithIniDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, filesToAddInitialDensity);

	}

	public static void createCommunitiesWithIniDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, List<File> filesToAddInitialDensity) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		// step1 : isolate constructible zones and merge them into a single SimpleFeature
		ShapefileDataStore zoningSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureIterator zoneIt = zoningSDS.getFeatureSource().getFeatures().features();
		HashMap<String, SimpleFeatureCollection> list = new HashMap<String, SimpleFeatureCollection>();
		while (zoneIt.hasNext()) {
			SimpleFeature feat = zoneIt.next();
			if (feat.getAttribute("TYPEZONE").equals("U") || feat.getAttribute("TYPEZONE").equals("ZC")) {
				String insee = (String) feat.getAttribute("INSEE");
				if (list.containsKey(insee)) {
					DefaultFeatureCollection tmp = new DefaultFeatureCollection();
					tmp.addAll(list.remove(insee));
					tmp.add(feat);
					list.put(insee, tmp.collection());
				} else {
					DefaultFeatureCollection add = new DefaultFeatureCollection();
					add.add(feat);
					list.put(insee, add.collection());
				}
			}
		}
		zoneIt.close();
		zoningSDS.dispose();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("INSEE", String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		DefaultFeatureCollection zones = new DefaultFeatureCollection();
		for (String com : list.keySet()) {
			builder.set("the_geom", Vectors.unionSFC(list.get(com)));
			builder.set("INSEE", com);
			zones.add(builder.buildFeature(null));
		}
		// Vectors.exportSFC(zones, new File("/tmp/step1.shp"));

		// step2 calculate the density
		HashMap<String, Double> iniDensVal = new HashMap<String, Double>();
		SimpleFeatureIterator com = zones.features();
		while (com.hasNext()) {
			SimpleFeature feature = com.next();
			String insee = (String) feature.getAttribute("INSEE");
			CSVReader csvR = new CSVReader(new FileReader(nbLgtFile));
			String[] fline = csvR.readNext();
			double obj = 0;
			double dens = 0;
			int field = 0;
			int code = 0;
			for (int i = 0; i < fline.length; i++) {
				if (fline[i].equals(nameFieldLgt)) {
					field = i;
				}
				if (fline[i].equals(nameFieldCodeCsv)) {
					code = i;
				}
			}
			for (String[] line : csvR.readAll()) {
				if (line[code].equals(insee)) {
					obj = obj + Double.valueOf(line[field].replace(",", "."));
				}
			}
			dens = obj / (((Geometry) feature.getDefaultGeometry()).getArea() / 10000);
			iniDensVal.put(insee, dens);
			csvR.close();
		}
		com.close();
		// //step 3 : affect the density of housing units per hectare to a list of administrative file (could either be communities or Iris)
		for (File f : filesToAddInitialDensity) {
			DefaultFeatureCollection result = new DefaultFeatureCollection();

			ShapefileDataStore inSDS = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureIterator inIt = inSDS.getFeatureSource().getFeatures().features();

			SimpleFeatureType schema = inSDS.getSchema();
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName(schema.getName());
			b.setSuperType((SimpleFeatureType) schema.getSuper());
			b.addAll(schema.getAttributeDescriptors());
			b.add("iniDens", Double.class);
			SimpleFeatureType nSchema = b.buildFeatureType();
			while (inIt.hasNext()) {
				SimpleFeature feat = inIt.next();
				SimpleFeature featFin = DataUtilities.reType(nSchema, feat);
				if (iniDensVal.containsKey(feat.getAttribute(nameInseeFileOut))) {
					featFin.setAttribute("iniDens", iniDensVal.get(feat.getAttribute(nameInseeFileOut)));
					result.add(featFin);
				}
			}
			inIt.close();
			Vectors.exportSFC(result, new File(f.getParentFile(), f.getName().replace(".shp", "-dens.shp")));
		}

		//
		// for (File irisFile : filesToAddInitialDensity ) {
		// ShapefileDataStore communitiesSDS = new ShapefileDataStore(irisFile.toURI().toURL());
		//
		// ShapefileDataStore densSDS = new ShapefileDataStore(densFile.toURI().toURL());
		// SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(densSDS.getSchema());
		// SimpleFeatureIterator it = communitiesSDS.getFeatureSource().getFeatures().features();
		//
		// //
		// while (it.hasNext()) {
		// SimpleFeature feat = it.next();
		// System.out.println(feat.getAttribute("DEPCOM"));
		// SimpleFeatureIterator itDens = densSDS.getFeatureSource().getFeatures().features();
		// List<Object> values = feat.getAttributes();
		// while (itDens.hasNext()) {
		// SimpleFeature fdens = itDens.next();
		// // System.out.println( f.getDefaultGeometry());
		// // System.out.println(fdens.getDefaultGeometry());
		// if (fdens.getAttribute("DEPCOM").equals(feat.getAttribute("DEPCOM"))) {
		// if (fdens.getAttribute("DEPCOM").equals("25056")) {
		// if (fdens.getAttribute("IRIS").equals(feat.getAttribute("IRIS"))) {
		// System.out.println(fdens.getAttribute("DEPCOM"));
		// values.add(fdens.getAttribute("densIni"));
		// break;
		// }
		// } else {
		// System.out.println(fdens.getAttribute("DEPCOM"));
		// values.add(fdens.getAttribute("densIni"));
		// break;
		// }
		// }
		// }
		// itDens.close();
		// if (values != null && !values.isEmpty()) {
		// result.add(finalParcelBuilder.buildFeature(null, values.toArray()));
		// }
		// }
		// it.close();
		// densSDS.dispose();
		// communitiesSDS.dispose();
		// }
		// Vectors.exportSFC(result, new File("/tmp/bye.shp"));
		//
	}
}
