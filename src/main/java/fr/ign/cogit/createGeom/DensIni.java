package fr.ign.cogit.createGeom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
		File zoningFile = new File("/home/ubuntu/boulot/these/result2903/dataRegulation/zoning.shp");
		File nbLgtFile = new File("/home/ubuntu/boulot/these/result2903/dataGeo/base-ic-logement-2012.csv");
		File fileToAddInitialDensity = new File("/home/ubuntu/boulot/these/result2903/dataGeo/old/communities.shp");
		// filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp"));
		createCommunitiesWithInitialDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				new File(fileToAddInitialDensity.getParentFile(), fileToAddInitialDensity.getName().replace(".shp", "-densIni.shp")));
		fileToAddInitialDensity = new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp");
		// filesToAddInitialDensity.add(new File("/home/ubuntu/boulot/these/result2903/dataGeo/communities.shp"));
		createCommunitiesWithInitialDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				new File(fileToAddInitialDensity.getParentFile(), fileToAddInitialDensity.getName().replace(".shp", "-densIni.shp")));
	}

	/**
	 * merge all the zones that are potentially containing housing units Defaults values of zones that contains industries, activites, equipments, are excluded
	 * 
	 * @param zoningFile
	 * @param typeOfZoneToAdd
	 * @return
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, String typeOfZoneToAdd)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> string = new ArrayList<String>();
		string.add(typeOfZoneToAdd);
		return mergeConstructedZone(zoningFile, string);
	}

	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, List<String> typeOfZoneToAdd)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		String[] tmpTab = { "UX", "UY", "UZ", "UE", "UL", "AUX", "AUY", "AUZ", "AUE", "AUL", };
		return mergeConstructedZone(zoningFile, typeOfZoneToAdd, Arrays.asList(tmpTab));
	}

	public static boolean libelleLookLike(List<String> list, String libelle) {
		boolean result = false;
		for (String s : list) {
			if (libelle.toUpperCase().contains(s.toUpperCase())) {
				return true;
			}
		}
		return result;
	}

	public static SimpleFeatureCollection mergeConstructedZone(File zoningFile, List<String> typeOfZoneToAdd, List<String> libelleToExclude)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore zoningSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureIterator zoneIt = zoningSDS.getFeatureSource().getFeatures().features();
		HashMap<String, SimpleFeatureCollection> list = new HashMap<String, SimpleFeatureCollection>();
		while (zoneIt.hasNext()) {
			SimpleFeature feat = zoneIt.next();
			if (typeOfZoneToAdd.contains(feat.getAttribute("TYPEZONE"))
					&& !libelleLookLike(libelleToExclude, (String) feat.getAttribute("LIBELLE"))) {

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
		Vectors.exportSFC(zones, new File("/tmp/step1.shp"));
		return zones;
	}

	public static HashMap<String, Double> calculDensityForConstructibleCommunities(String nameFieldLgt, String nameFieldCodeCsv,
			SimpleFeatureCollection zoneZoning, String nameCodeSFC, File nbLgtFile) throws NumberFormatException, IOException {
		HashMap<String, Double> iniDensVal = new HashMap<String, Double>();
		SimpleFeatureIterator com = zoneZoning.features();
		while (com.hasNext()) {
			SimpleFeature feature = com.next();
			String insee = (String) feature.getAttribute(nameCodeSFC);
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
		return iniDensVal;
	}

	public static File createCommunitiesWithInitialDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, File fileToAddInitialDensity, File outFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> zoneName = new ArrayList<String>();
		zoneName.add("U");
		zoneName.add("ZC");
		return createCommunitiesWithDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				zoneName, "densIni", outFile);
	}

	public static File createCommunitiesWithNewDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, File fileToAddInitialDensity, File outFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> zoneName = new ArrayList<String>();
		zoneName.add("U");
		zoneName.add("AU");
		zoneName.add("ZC");

		return createCommunitiesWithDensity(nameFieldLgt, nameFieldCodeCsv, nameInseeFileOut, zoningFile, nbLgtFile, fileToAddInitialDensity,
				zoneName, "densNew", outFile);
	}

	public static File createCommunitiesWithDensity(String nameFieldLgt, String nameFieldCodeCsv, String nameInseeFileOut, File zoningFile,
			File nbLgtFile, File fileToAddInitialDensity, List<String> zoneName, String typeOfDensFieldName, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		// step1 : isolate constructible zones and merge them into a single SimpleFeature
		SimpleFeatureCollection zones = mergeConstructedZone(zoningFile, zoneName);

		// step2 calculate the density
		HashMap<String, Double> iniDensVal = calculDensityForConstructibleCommunities(nameFieldLgt, nameFieldCodeCsv, zones, "INSEE", nbLgtFile);

		// step 3 : affect the density of housing units per hectare to a list of administrative file (could either be communities or Iris)

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore inSDS = new ShapefileDataStore(fileToAddInitialDensity.toURI().toURL());
		SimpleFeatureIterator inIt = inSDS.getFeatureSource().getFeatures().features();

		SimpleFeatureType schema = inSDS.getSchema();
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.setName(schema.getName());
		b.setSuperType((SimpleFeatureType) schema.getSuper());
		b.addAll(schema.getAttributeDescriptors());
		b.add(typeOfDensFieldName, Double.class);
		SimpleFeatureType nSchema = b.buildFeatureType();
		while (inIt.hasNext()) {
			SimpleFeature feat = inIt.next();
			SimpleFeature featFin = DataUtilities.reType(nSchema, feat);
			if (iniDensVal.containsKey(feat.getAttribute(nameInseeFileOut))) {

				featFin.setAttribute(typeOfDensFieldName, iniDensVal.get(feat.getAttribute(nameInseeFileOut)));
				result.add(featFin);
			}
		}
		inIt.close();
		return Vectors.exportSFC(result, outFile);
	}
}
