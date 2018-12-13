package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.annexeTools.FeaturePolygonizer;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.GeOxygeneGeoToolsTypes;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.parameters.Parameters;

public class GetFromGeom {

	// public static void main(String[] args) throws Exception {
	// System.out.println(rnuZip(new File("/home/mcolomb/informatique/ArtiScales/dataRegul")));
	// }

	public static List<String> rnuZip(File regulFile) throws IOException {
		List<String> result = new ArrayList<String>();
		File fCsv = new File("");
		try {
			for (File f : regulFile.listFiles()) {
				if (f.getName().equals("listRNUCities.csv")) {
					fCsv = f;
					break;
				}
			}
		} catch (NullPointerException np) {
			System.out.println("no RNU list");
			return null;
		}
		CSVReader read = new CSVReader(new FileReader(fCsv));
		// entete
		read.readNext();
		for (String[] line : read.readAll()) {
			result.add(line[1]);
		}
		read.close();
		return result;
	}

	public static File getParcels(File geoFile, File regulFile, File currentFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		return getParcels(geoFile, regulFile, currentFile, new ArrayList<String>());
	}

	/**
	 * get the parcel file and add necessary attributes and informations for an ArtiScales Simulation
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param currenFile
	 *            :
	 * @return the ready to deal with the selection process parcels
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 */
	public static File getParcels(File geoFile, File regulFile, File tmpFile, String zip) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> lZip = new ArrayList<String>();
		lZip.add(zip);
		return getParcels(geoFile, regulFile, tmpFile, lZip);
	}

	public static File getParcels(File geoFile, File regulFile, File tmpFile, List<String> listZip) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		File result = new File("");
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcelle.shp")) {
				result = f;
			}
		}

		ShapefileDataStore parcelSDS = new ShapefileDataStore(result.toURI().toURL());
		SimpleFeatureCollection parcels = parcelSDS.getFeatureSource().getFeatures();

		// if we decided to work on a set of cities
		if (!listZip.isEmpty()) {
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			DefaultFeatureCollection df = new DefaultFeatureCollection();
			for (String zip : listZip) {
				String codep = zip.substring(0, 2);
				String cocom = zip.substring(2, 5);
				Filter filterDep = ff.like(ff.property("CODE_DEP"), codep);
				Filter filterCom = ff.like(ff.property("CODE_COM"), cocom);
				df.addAll(parcels.subCollection(filterDep).subCollection(filterCom));
			}
			parcels = df.collection();
		}

		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("eval", String.class);
		sfTypeBuilder.add("DoWeSimul", String.class);
		sfTypeBuilder.add("IsBuild", Boolean.class);
		sfTypeBuilder.add("U", Boolean.class);
		sfTypeBuilder.add("AU", Boolean.class);
		sfTypeBuilder.add("NC", Boolean.class);

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();

		int i = 0;
		// int tot = parcels.size();
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				// put the best cell evaluation into the parcel
				String numero = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")) + ((String) feat.getAttribute("COM_ABS"))
						+ ((String) feat.getAttribute("SECTION")) + ((String) feat.getAttribute("NUMERO"));
				String INSEE = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));

				boolean isBuild = false;
				SimpleFeatureIterator batiCollectionIt = shpDSBati.getFeatureSource().getFeatures().features();
				try {
					while (batiCollectionIt.hasNext()) {
						if (((Geometry) feat.getDefaultGeometry()).intersects(((Geometry) batiCollectionIt.next().getDefaultGeometry()))) {
							isBuild = true;
							break;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					batiCollectionIt.close();
				}

				// say if the parcel intersects a particular zoning type
				boolean u = false;
				boolean au = false;
				boolean nc = false;

				for (String s : bigZoneinParcel(feat, regulFile)) {
					if (s.equals("AU")) {
						au = true;
					} else if (s.equals("U")) {
						u = true;
					} else if (s.equals("NC")) {
						nc = true;
					} else {
						// if the parcel is outside of the zoning file, we don't keep it
						continue;
					}
				}

				Object[] attr = { numero, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"),
						feat.getAttribute("NUMERO"), INSEE, 0, "false", isBuild, u, au, nc };

				sfBuilder.add(feat.getDefaultGeometry());

				SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
				newParcel.add(feature);
				// System.out.println(i+" on "+tot);
				i = i + 1;
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		parcelSDS.dispose();
		shpDSBati.dispose();

		return Vectors.exportSFC(newParcel.collection(), new File(tmpFile, "parcelle.shp"));
	}

	// TODO préciser quelle couche de batiment on utilise (si l'on explore plein de
	// jeux de données)
	public static File getBati(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("batiment") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}

	public static File getCities(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("cities") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Cities file not found");
	}

	// TODO préciser quelle couche de route on utilise (si l'on explore plein de
	// jeux de données)
	public static File getRoute(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("route") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Route file not found");
	}

	public static File getIlots(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("ilot") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Ilots file not found");
	}

	public static int getHousingUnitsGoals(File regulFile, String zipCode) throws IOException {
		File donneGen = new File(regulFile, "donnecommune.csv"); // A mettre dans le fichier de paramètres?
		CSVReader csvReader = new CSVReader(new FileReader(donneGen));
		List<String[]> content = csvReader.readAll();
		int ColLog = 0;
		int ColZip = 0;

		for (String[] row : content) {
			int i = 0;
			for (String s : row) {
				if (s.contains("nbLogObjectif")) {
					ColLog = i;
				}
				if (s.contains("DEPCOM")) {
					ColZip = i;
				}
				i = i + 1;
			}
			if (row[ColZip].equals(zipCode)) {
				int nb = Integer.parseInt(row[ColLog]);
				csvReader.close();
				return (nb);
			}
		}
		csvReader.close();
		throw new FileNotFoundException("Housing units objectives not found");
	}

	public static File getZoning(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("zoning") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}

	public static File getPredicate(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("predicate") && f.getName().endsWith(".csv")) {
				return f;
			}
		}
		throw new FileNotFoundException("Predicate not found");
	}

	public static File getPrescPonct(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("prescPonct") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("prescPonct file not found");
	}

	public static File getPrescSurf(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("prescSurf") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("prescSurf file not found");
	}

	public static File getPrescLin(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("prescLin") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("prescLin file not found");
	}

	public static SimpleFeatureCollection selecParcelZoning(String[] typesZone, String zipcode, File parcelFile, File zoningFile) throws Exception {

		ShapefileDataStore shpDSParcel = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcels = shpDSParcel.getFeatureSource().getFeatures();

		return selecParcelZoning(typesZone, zipcode, parcels, zoningFile);

	}

	public static SimpleFeatureCollection selecParcelZoning(String[] typesZone, String zipCode, SimpleFeatureCollection parcels, File zoningFile)
			throws MismatchedDimensionException, CQLException, NoSuchAuthorityCodeException, IOException, FactoryException, TransformException, Exception {
		// best exceptions ever

		DefaultFeatureCollection totalParcel = new DefaultFeatureCollection();

		for (String typeZone : typesZone) {
			totalParcel.addAll(selecParcelZoning(typeZone, Vectors.snapDatas(parcels, zoningFile), zoningFile));
		}

		return totalParcel.collection();
	}
/**
 * get the insee number from a Simplefeature (that is most of the time, a parcel)
 * @param cities
 * @param parcel
 * @return
 */
	public static String getInseeFromParcel(SimpleFeatureCollection cities, SimpleFeature parcel) {
		SimpleFeature City = null;
		SimpleFeatureIterator citIt = cities.features();
		try {
			while (citIt.hasNext()) {
				SimpleFeature cit = citIt.next();
				if (((Geometry) cit.getDefaultGeometry()).contains((Geometry) parcel.getDefaultGeometry())) {
					City = cit;
					break;
				}
				// if the parcel is in between two cities, we randomly add the first met
				else if (((Geometry) cit.getDefaultGeometry()).intersects((Geometry) parcel.getDefaultGeometry())) {
					City = cit;
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			citIt.close();
		}
		return (String) City.getAttribute("INSEE");
	}

	/**
	 * return the typezones that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return
	 * @throws Exception
	 */
	public static List<String> bigZoneinParcel(SimpleFeature parcelIn, File regulFile) throws Exception {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning(regulFile).toURI().toURL());
		SimpleFeatureIterator featuresZones = shpDSZone.getFeatureSource().getFeatures().features();
		try {
			while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				if (((Geometry) feat.getDefaultGeometry()).buffer(0.1).contains((Geometry) parcelIn.getDefaultGeometry())) {
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						result.add("U");
						continue;
					case "AU":
						result.add("AU");
						continue;
					case "N":
					case "NC":
					case "A":
						result.add("NC");
						continue;
					}
				}
				// maybe the parcel is in between two zones
				else if (((Geometry) feat.getDefaultGeometry()).intersects((Geometry) parcelIn.getDefaultGeometry())) {
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						result.add("U");
						break;
					case "AU":
						result.add("AU");
						break;
					case "N":
					case "NC":
					case "A":
						result.add("NC");
						break;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			featuresZones.close();
		}

		shpDSZone.dispose();

		return result;

	}

	public static List<String> getInsee(File parcelFile) throws IOException {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
		try {
			while (parcelFeaturesIt.hasNext()) {
				SimpleFeature feat = parcelFeaturesIt.next();
				if (!result.contains(feat.getAttribute("INSEE"))) {
					result.add((String) feat.getAttribute("INSEE"));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelFeaturesIt.close();
		}
		parcelSDS.dispose();
		return result;
	}

	/**
	 * Get parcels from a Zoning file matching a certain type of zone (characterized by the field TYPEZONE)
	 * 
	 * @param typeZone
	 *            the code of the zone willed to be selected. In a french context, it can either be (A, N, U, AU) or one of its subsection
	 * @param zipCode
	 *            the zipcode of the city to select parcels in
	 * @return a SimpleFeatureCollection which contains the parcels that are included in the zoning area
	 * @throws IOException
	 * @throws CQLException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws TransformException
	 * @throws MismatchedDimensionException
	 */
	public static SimpleFeatureCollection selecParcelZoning(String typeZone, SimpleFeatureCollection parcelCollection, File zoningFile)
			throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		// creation of the filter to select only wanted type of zone in the PLU
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("TYPEZONE"), (typeZone.contains("AU") ? "*" : "") + typeZone + "*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);

		if (featureZoneSelected.getSchema().getType("TYPEZONE") == null) {
			System.out.println("ATTRIBUTE TYPEZONE IS MISSING in data " + zoningFile);
			return null;
		}

		// Filter to select parcels that intersects the selected zonnig zone
		String geometryParcelPropertyName = parcelCollection.getSchema().getGeometryDescriptor().getLocalName();

		// TODO opérateur géométrique pas terrible, mais rattrapé par le
		// découpage de SimPLU
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(Vectors.unionSFC(featureZoneSelected)));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		shpDSZone.dispose();

		return parcelSelected;
	}

	/**
	 * Overload if the entry is a file. Not very good coz the shapefileDataStore cannot be disposed.
	 * 
	 * @param typeZone
	 * @param parcelFile
	 * @param zoningFile
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection selecParcelZoning(String typeZone, File parcelFile, File zoningFile) throws Exception {
		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(parcelFile.toURI().toURL());
		return selecParcelZoning(typeZone, shpDSParcel.getFeatureSource().getFeatures(), zoningFile);
	}

	public static SimpleFeatureBuilder getParcelSplitSFBuilder() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("eval", String.class);
		sfTypeBuilder.add("DoWeSimul", String.class);
		sfTypeBuilder.add("SPLIT", Integer.class);
		sfTypeBuilder.add("IsBuild", Boolean.class);
		sfTypeBuilder.add("U", Boolean.class);
		sfTypeBuilder.add("AU", Boolean.class);
		sfTypeBuilder.add("NC", Boolean.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder getParcelSFBuilder() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("eval", String.class);
		sfTypeBuilder.add("DoWeSimul", String.class);
		sfTypeBuilder.add("IsBuild", Boolean.class);
		sfTypeBuilder.add("U", Boolean.class);
		sfTypeBuilder.add("AU", Boolean.class);
		sfTypeBuilder.add("NC", Boolean.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBParDefaut(SimpleFeature feat, SimpleFeatureType schema, String geometryOutputName) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder.set(geometryOutputName, (Geometry) feat.getDefaultGeometry());
		finalParcelBuilder.set("CODE", "unknow");
		finalParcelBuilder.set("CODE_DEP", "unknow");
		finalParcelBuilder.set("CODE_COM", "unknow");
		finalParcelBuilder.set("COM_ABS", "unknow");
		finalParcelBuilder.set("SECTION", "unknow");
		finalParcelBuilder.set("NUMERO", "unknow");
		finalParcelBuilder.set("INSEE", "unknow");
		finalParcelBuilder.set("eval", "0");
		finalParcelBuilder.set("DoWeSimul", false);
		finalParcelBuilder.set("IsBuild", false);
		finalParcelBuilder.set("U", false);
		finalParcelBuilder.set("AU", false);
		finalParcelBuilder.set("NC", false);

		return finalParcelBuilder;
	}

	/**
	 * not very nice overload
	 * 
	 * @param feat
	 * @param schema
	 * @return
	 */
	public static SimpleFeatureBuilder setSFBWithFeat(SimpleFeature feat, SimpleFeatureType schema) {
		return setSFBWithFeat(feat, schema, schema.getGeometryDescriptor().getName().toString());

	}

	public static SimpleFeatureBuilder setSFBWithFeat(SimpleFeature feat, SimpleFeatureType schema, String geometryOutputName) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder.set(geometryOutputName, (Geometry) feat.getDefaultGeometry());
		finalParcelBuilder.set("CODE", feat.getAttribute("CODE"));
		finalParcelBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		finalParcelBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		finalParcelBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		finalParcelBuilder.set("SECTION", feat.getAttribute("SECTION"));
		finalParcelBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
		finalParcelBuilder.set("INSEE", feat.getAttribute("INSEE"));
		finalParcelBuilder.set("eval", feat.getAttribute("eval"));
		finalParcelBuilder.set("DoWeSimul", feat.getAttribute("DoWeSimul"));
		finalParcelBuilder.set("IsBuild", feat.getAttribute("IsBuild"));
		finalParcelBuilder.set("U", feat.getAttribute("U"));
		finalParcelBuilder.set("AU", feat.getAttribute("AU"));
		finalParcelBuilder.set("NC", feat.getAttribute("NC"));

		return finalParcelBuilder;
	}

}
