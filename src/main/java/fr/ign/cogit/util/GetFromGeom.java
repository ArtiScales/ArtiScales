package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.annexeTools.FeaturePolygonizer;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;
import fr.ign.parameters.Parameters;

public class GetFromGeom {

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/yo/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parameterTechnic.xml"));
		lF.add(new File(rootParam, "parameterScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		ShapefileDataStore shpDSZone = new ShapefileDataStore(
				new File("/home/yo/Documents/these/parcTemp.shp").toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = featuresZones.features();
		SimpleFeature waiting = null;
		while (it.hasNext()) {
			SimpleFeature feat = it.next();
			if (((String) feat.getAttribute("NUMERO")).equals("0212")
					&& ((String) feat.getAttribute("SECTION")).equals("AB")) {
				waiting = feat;
			}
		}
		it.close();
		System.out.println(affectToZoneAndTypo(p, waiting, true));

		shpDSZone.dispose();
	}

	public static String affectToZoneAndTypo(Parameters p, IFeature parcel, boolean priorTypoOrZone) throws Exception {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		sfBuilder.add(AdapterFactory.toGeometry(new GeometryFactory(), parcel.getGeom()));

		SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(1));

		return affectToZoneAndTypo(p, feature, priorTypoOrZone);
	}

	/**
	 * 
	 * @param paramLine       : value of the parameter file that concerns the zone
	 *                        repartition
	 * @param parcel          : parcel contained into different zones and/or typs
	 * @param paramName       : name of the scenario
	 * @param dataRegulation  :
	 * @param dataGeo
	 * @param priorTypoOrZone : if true: we prior the typo if two different zones
	 *                        for zone and typo are found
	 * @return
	 * @throws Exception
	 */
	public static String affectToZoneAndTypo(Parameters p, SimpleFeature parcel, boolean priorTypoOrZone)
			throws Exception {
		String occurConcerned = "";

		List<String> mayOccur = new ArrayList<String>();
		// TODO make sure that this returns the best answer
		String typo = GetFromGeom.parcelInBigZone(new File(p.getString("rootFile") + "/dataRegulation"), parcel);
		String zone = GetFromGeom.parcelInTypo(new File(p.getString("rootFile") + "/dataGeo"), parcel);
		String[] tabRepart = p.getString("useRepartition").split("_");

		for (String s : tabRepart) {
			// If the paramFile speak for a particular scenario
			String[] scenarRepart = s.split(":");
			// if its no longer than 1, no particular scénario
			if (scenarRepart.length > 1) {
				// if codes doesn't match, we continue with another one
				if (!scenarRepart[0].equals(p.getString("code"))) {
					continue;
				}
			}
			// seek for the different special locations
			// if both, it's a perfect match !!
			if (s.contains(typo) && s.contains(zone)) {
				occurConcerned = s;
				break;
			}
			// if no perfect match, prior to the typo
			if (s.contains(zone)) {
				mayOccur.add(s);
			}
			if (s.contains(typo)) {
				mayOccur.add(s);
			}
		}
		// if no perfect match found, we seek for a simple zone identifier
		if (occurConcerned.equals("")) {
			// we prior typo infos than zone infos
			if (priorTypoOrZone) {
				for (String s : mayOccur) {
					if (s.equals(typo)) {
						occurConcerned = s;
					}
				}
			}
			// we prior zone to typo
			else if (!priorTypoOrZone) {
				for (String s : mayOccur) {
					if (s.equals(zone)) {
						occurConcerned = s;
					}
				}
			}
			// the type found is not the one priorized. We return it anyway
			else {
				occurConcerned = mayOccur.get(0);
			}
		}
		if (occurConcerned == "") {
			occurConcerned = "default";
		}

		return occurConcerned;
	}

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

	public static File getParcels(File geoFile, File regulFile, File currentFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		return getParcels(geoFile, regulFile, currentFile, new ArrayList<String>());
	}

	/**
	 * get the parcel file and add necessary attributes and informations for an
	 * ArtiScales Simulation
	 * 
	 * @param geoFile    : the folder containing the geographic data
	 * @param regulFile  : the folder containing the urban regulation related data
	 * @param currenFile :
	 * @return the ready to deal with the selection process parcels
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 */
	public static File getParcels(File geoFile, File regulFile, File tmpFile, String zip)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		List<String> lZip = new ArrayList<String>();
		lZip.add(zip);
		return getParcels(geoFile, regulFile, tmpFile, lZip);
	}

	public static File getParcels(File geoFile, File regulFile, File tmpFile, List<String> listZip)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		File result = new File("");
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcel.shp")) {
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

		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());

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
				String numero = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"))
						+ ((String) feat.getAttribute("COM_ABS")) + ((String) feat.getAttribute("SECTION"))
						+ ((String) feat.getAttribute("NUMERO"));
				String INSEE = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));

				boolean isBuild = false;
				SimpleFeatureIterator batiCollectionIt = shpDSBati.getFeatureSource().getFeatures().features();
				try {
					while (batiCollectionIt.hasNext()) {
						if (((Geometry) feat.getDefaultGeometry())
								.intersects(((Geometry) batiCollectionIt.next().getDefaultGeometry()))) {
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

				for (String s : parcelInBigZone(feat, regulFile)) {
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

				Object[] attr = { numero, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"),
						feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"), feat.getAttribute("NUMERO"), INSEE,
						0, "false", isBuild, u, au, nc };

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

	public static File getBuild(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("building") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}

	public static File getCommunities(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("communities") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Communities file not found");
	}

	public static File getRoute(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("road") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Road file not found");
	}

	public static File getIlots(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("ilot") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Ilots file not found");
	}

	public static int getHousingUnitsGoals(File geoFile, String zipCode) throws IOException {

		ShapefileDataStore sds = new ShapefileDataStore(getCommunities(geoFile).toURI().toURL());
		SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();

		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if (feat.getAttribute("DEPCOM").equals(zipCode)) {
					return (int) feat.getAttribute("obj_2035");
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

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

	public static File getParcels(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("parcel") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("parcel file not found");
	}

	public static SimpleFeatureCollection selecParcelZoning(String[] typesZone, String zipcode, File parcelFile,
			File zoningFile) throws Exception {

		ShapefileDataStore shpDSParcel = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcels = shpDSParcel.getFeatureSource().getFeatures();

		return selecParcelZoning(typesZone, zipcode, parcels, zoningFile);

	}

	public static SimpleFeatureCollection selecParcelZoning(String[] typesZone, String zipCode,
			SimpleFeatureCollection parcels, File zoningFile) throws MismatchedDimensionException, CQLException,
			NoSuchAuthorityCodeException, IOException, FactoryException, TransformException, Exception {
		// best exceptions ever

		DefaultFeatureCollection totalParcel = new DefaultFeatureCollection();

		for (String typeZone : typesZone) {
			totalParcel.addAll(selecParcelZoning(typeZone, Vectors.snapDatas(parcels, zoningFile), zoningFile));
		}

		return totalParcel.collection();
	}

	/**
	 * get the insee number from a Simplefeature (that is most of the time, a
	 * parcel)
	 * 
	 * @param cities
	 * @param parcel
	 * @return
	 */
	public static String getInseeFromParcel(SimpleFeatureCollection cities, SimpleFeature parcel) {
		SimpleFeature City = null;
		SimpleFeatureIterator citIt = cities.features();
		String cityInsee = "00000";
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

		if (((String) City.getAttribute("DEPCOM")) != null || ((String) City.getAttribute("DEPCOM")) != "") {
			cityInsee = (String) City.getAttribute("DEPCOM");
		}
		return cityInsee;
	}

	/**
	 * return a single TYPEZONE that a parcels intersect if the parcel intersects
	 * multiple, we select the one that covers the most area
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return
	 * @throws Exception
	 */
	public static String parcelInTypo(File geoFile, SimpleFeature parcelIn) throws Exception {
		return parcelInTypo(parcelIn, geoFile).get(0);
	}

	/**
	 * return the TYPEZONEs that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInTypo(SimpleFeature parcelIn, File geoFile) throws Exception {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getCommunities(geoFile).toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Vectors.snapDatas(shpDSZone.getFeatureSource().getFeatures(),
				(Geometry) parcelIn.getDefaultGeometry());

		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();
		try {
			zone: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				// TODO if same typo in two different typo, won't fall into that trap =>
				// create a big zone shapefile instead?
				if (((Geometry) feat.getDefaultGeometry()).buffer(1)
						.contains((Geometry) parcelIn.getDefaultGeometry())) {
					switch ((String) feat.getAttribute("typo")) {
					case "rural":
						result.add("rural");
						result.remove("periUrbain");
						result.remove("banlieue");
						result.remove("centre");
						break zone;
					case "periUrbain":
						result.add("periUrbain");
						result.remove("rural");
						result.remove("banlieue");
						result.remove("centre");
						break zone;
					case "banlieue":
						result.add("banlieue");
						result.remove("rural");
						result.remove("periUrbain");
						result.remove("centre");
						break zone;
					case "centre":
						result.add("centre");
						result.remove("rural");
						result.remove("periUrbain");
						result.remove("banlieue");
						break zone;
					}
				}
				// maybe the parcel is in between two cities (that must be rare)
				else if (((Geometry) feat.getDefaultGeometry()).intersects((Geometry) parcelIn.getDefaultGeometry())) {
					switch ((String) feat.getAttribute("typo")) {
					case "rural":
						result.add("rural");
						break zone;
					case "periUrbain":
						result.add("periUrbain");
						break zone;
					case "banlieue":
						result.add("banlieue");
						break zone;
					case "centre":
						result.add("centre");
						break zone;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			featuresZones.close();
		}

		// TODO sort from the most represented to the less
		if (result.size() > 1) {
			System.out.println("parcel " + parcelIn.getAttribute("CODE_COM") + parcelIn.getAttribute("SECTION")
					+ parcelIn.getAttribute("NUMERO") + "is IN BETWEEN TWO CITIES OF DIFFERENT TYPOLOGIES");
			System.out.println("thats very rare");
			System.out.println("we randomly use " + result.get(0));
		}

		shpDSZone.dispose();

		return result;

	}

	/**
	 * return a single TYPEZONE that a parcels intersect if the parcel intersects
	 * multiple, we select the one that covers the most area TODO for now, it's
	 * random
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return
	 * @throws Exception
	 */
	public static String parcelInBigZone(File regulFile, SimpleFeature parcelIn) throws Exception {
		List<String> yo = parcelInBigZone(parcelIn, regulFile);
		return yo.get(0);
	}

	/**
	 * return the TYPEZONEs that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInBigZone(SimpleFeature parcelIn, File regulFile) throws Exception {
		List<String> result = new LinkedList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning(regulFile).toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Vectors.snapDatas(shpDSZone.getFeatureSource().getFeatures(),
				(Geometry) parcelIn.getDefaultGeometry());
		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();
		// if there's two zones, we need to sort them by making collection. zis iz évy
		// calculation, but it could worth it
		boolean twoZones = false;
		HashMap<String, Double> repart = new HashMap<String, Double>();

		try {
			zoneLoop: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				if (((Geometry) feat.getDefaultGeometry()).buffer(0.5)
						.contains((Geometry) parcelIn.getDefaultGeometry())) {
					twoZones = false;
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						result.add("U");
						result.remove("AU");
						result.remove("NC");
						break zoneLoop;
					case "AU":
						result.add("AU");
						result.remove("U");
						result.remove("NC");
						break zoneLoop;
					case "N":
					case "NC":
					case "A":
						result.add("NC");
						result.remove("AU");
						result.remove("U");
						break zoneLoop;
					}
				}
				// maybe the parcel is in between two zones (less optimized)
				else if (((Geometry) feat.getDefaultGeometry()).intersects((Geometry) parcelIn.getDefaultGeometry())) {
					twoZones = true;
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						if (repart.containsKey("U")) {
							repart.put("U", repart.get("U") + ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						} else {
							repart.put("U", ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						}
						break;
					case "AU":
						if (repart.containsKey("AU")) {
							repart.put("AU", repart.get("AU") + ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						} else {
							repart.put("AU", ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						}
						break;
					case "N":
					case "NC":
					case "A":
						if (repart.containsKey("NC")) {
							repart.put("NC", repart.get("NC") + ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						} else {
							repart.put("NC", ((Geometry) feat.getDefaultGeometry())
									.intersection((Geometry) parcelIn.getDefaultGeometry()).getArea());
						}
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
		if (twoZones == true) {
			List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(repart.entrySet());
			Collections.sort(entryList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
					return obj2.getValue().compareTo(obj1.getValue());
				}
			});

			for (Entry<String, Double> s : entryList) {
				result.add(s.getKey());
			}

		}

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
	 * Get parcels from a Zoning file matching a certain type of zone (characterized
	 * by the field TYPEZONE)
	 * 
	 * @param typeZone the code of the zone willed to be selected. In a french
	 *                 context, it can either be (A, N, U, AU) or one of its
	 *                 subsection
	 * @param zipCode  the zipcode of the city to select parcels in
	 * @return a SimpleFeatureCollection which contains the parcels that are
	 *         included in the zoning area
	 * @throws IOException
	 * @throws CQLException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws TransformException
	 * @throws MismatchedDimensionException
	 */
	public static SimpleFeatureCollection selecParcelZoning(String typeZone, SimpleFeatureCollection parcelCollection,
			File zoningFile) throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {

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
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName),
				ff.literal(Vectors.unionSFC(featureZoneSelected)));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		shpDSZone.dispose();

		return parcelSelected;
	}

	/**
	 * Overload if the entry is a file. Not very good coz the shapefileDataStore
	 * cannot be disposed.
	 * 
	 * @param typeZone
	 * @param parcelFile
	 * @param zoningFile
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection selecParcelZoning(String typeZone, File parcelFile, File zoningFile)
			throws Exception {
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

	public static SimpleFeatureBuilder setSFBParDefaut(SimpleFeature feat, SimpleFeatureType schema,
			String geometryOutputName) {
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

	public static SimpleFeatureBuilder setSFBWithFeat(SimpleFeature feat, SimpleFeatureType schema,
			String geometryOutputName) {
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

	/**
	 * Merge and recut the to urbanised (AU) zones Cut first the U parcels to keep
	 * them unsplited, then split the AU parcel and remerge them all into the
	 * original parcel file
	 * 
	 * @param parcels
	 * @param zoningFile
	 * @param p          : parametre file s
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection selecParcelZonePLUmergeAU(SimpleFeatureCollection parcels, File tmpFile,
			File zoningFile, Parameters p) throws Exception {

		// parcels to save for after
		DefaultFeatureCollection savedParcels = new DefaultFeatureCollection();
		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		Geometry unionParcel = Vectors.unionSFC(parcels);
		String geometryParcelPropertyName = parcels.getSchema().getGeometryDescriptor().getLocalName();

		// get the AU zones from the zoning file
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filterTypeZone = ff.like(ff.property("TYPEZONE"), "AU");

		Filter filterEmprise = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(unionParcel));
		SimpleFeatureCollection zoneAU = featuresZones.subCollection(filterTypeZone).subCollection(filterEmprise);

		// If no AU zones, we won't bother
		if (zoneAU.isEmpty()) {
			System.out.println("no AU zones");
			return parcels;
		}

		// all the AU zones
		Geometry geomAU = Vectors.unionSFC(zoneAU);
		DefaultFeatureCollection parcelsInAU = new DefaultFeatureCollection();
		SimpleFeatureIterator parcIt = parcels.features();

		// sort in two different collections, the ones that cares and the ones that
		// doesnt
		try {
			while (parcIt.hasNext()) {
				SimpleFeature feat = parcIt.next();
				if (((Geometry) feat.getDefaultGeometry()).intersects(geomAU)) {
					parcelsInAU.add(feat);
				} else {
					savedParcels.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcIt.close();
		}

		// temporary shapefiles
		File fParcelsInAU = Vectors.exportSFC(parcelsInAU, new File(tmpFile, "parcelCible.shp"));
		File fZoneAU = Vectors.exportSFC(zoneAU, new File(tmpFile, "oneAU.shp"));

		// cut and separate parcel according to their spatial relation with the zonnig
		// zones
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

		// parcel intersecting the U zone must not be cuted and keep their attributes
		// intermediary result
		File outU = new File(tmpFile, "polygonU.shp");
		SimpleFeatureBuilder sfBuilder = getParcelSplitSFBuilder();

		DefaultFeatureCollection write = new DefaultFeatureCollection();

		int nFeat = 0;
		// for every polygons situated in between U and AU zones, we cut the parcels
		// regarding to the zone and copy them attributes to keep the existing U parcels
		for (Geometry poly : polygons) {
			// if the polygons are not included on the AU zone
			if (!geomAU.buffer(0.01).contains(poly)) {
				sfBuilder.add(poly);
				SimpleFeatureIterator parcelIt = parcelsInAU.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						if (((Geometry) feat.getDefaultGeometry()).buffer(0.01).contains(poly)) {
							sfBuilder.set("CODE", feat.getAttribute("CODE"));
							sfBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
							sfBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
							sfBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
							sfBuilder.set("SECTION", feat.getAttribute("SECTION"));
							sfBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
							sfBuilder.set("INSEE", feat.getAttribute("INSEE"));
							sfBuilder.set("eval", feat.getAttribute("eval"));
							sfBuilder.set("DoWeSimul", feat.getAttribute("DoWeSimul"));
							sfBuilder.set("SPLIT", 0);
							// @warning the AU Parcels are mostly unbuilt, but maybe not?
							sfBuilder.set("IsBuild", feat.getAttribute("IsBuild"));
							sfBuilder.set("U", feat.getAttribute("U"));
							sfBuilder.set("AU", feat.getAttribute("AU"));
							sfBuilder.set("NC", feat.getAttribute("NC"));
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
				write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
				nFeat++;
			}
		}
		String geometryOutputName = write.getSchema().getGeometryDescriptor().getLocalName();
		SimpleFeatureIterator it = zoneAU.features();
		int numZone = 0;

		// mark the AU zones
		try {
			while (it.hasNext()) {
				SimpleFeature zone = it.next();
				// get the insee number for that zone
				String insee = "";
				insee = (String) zone.getAttribute("INSEE");
				sfBuilder.set("CODE", insee + "000" + "New" + numZone + "Section");
				sfBuilder.set("CODE_DEP", insee.substring(0, 2));
				sfBuilder.set("CODE_COM", insee.substring(2, 5));
				sfBuilder.set("COM_ABS", "000");
				sfBuilder.set("SECTION", "New" + numZone + "Section");
				sfBuilder.set("NUMERO", "");
				sfBuilder.set("INSEE", insee);
				sfBuilder.set("eval", "0");
				sfBuilder.set("DoWeSimul", false);
				sfBuilder.set("SPLIT", 1);
				// @warning the AU Parcels are mostly unbuilt, but maybe not?
				sfBuilder.set("IsBuild", false);
				sfBuilder.set("U", false);
				sfBuilder.set("AU", true);
				sfBuilder.set("NC", false);
				Geometry intersectedGeom = ((Geometry) zone.getDefaultGeometry()).intersection(unionParcel);
				if (!intersectedGeom.isEmpty()) {
					if (intersectedGeom instanceof MultiPolygon) {
						for (int i = 0; i < intersectedGeom.getNumGeometries(); i++) {
							sfBuilder.set(geometryOutputName, intersectedGeom.getGeometryN(i));
							write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
							nFeat++;
							// ugly, but have to do it
							sfBuilder.set("CODE", insee + "000" + "New" + numZone + "Section");
							sfBuilder.set("CODE_DEP", insee.substring(0, 2));
							sfBuilder.set("CODE_COM", insee.substring(2, 5));
							sfBuilder.set("COM_ABS", "000");
							sfBuilder.set("SECTION", "New" + numZone + "Section");
							sfBuilder.set("NUMERO", "");
							sfBuilder.set("INSEE", insee);
							sfBuilder.set("eval", "0");
							sfBuilder.set("DoWeSimul", false);
							sfBuilder.set("SPLIT", 1);
							// @warning the AU Parcels are mostly unbuilt, but maybe not?
							sfBuilder.set("IsBuild", false);
							sfBuilder.set("U", false);
							sfBuilder.set("AU", true);
							sfBuilder.set("NC", false);
						}
						continue;
					} else {
						sfBuilder.set(geometryOutputName, intersectedGeom);
					}
				} else {
					sfBuilder.set(geometryOutputName, zone.getDefaultGeometry());
				}
				write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
				nFeat++;
				numZone++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		Vectors.exportSFC(write.collection(), outU);

		shpDSZone.dispose();

		double roadEpsilon = 0.5;
		double noise = 0;
		double maximalArea = 400;
		double maximalWidth = 50;

		// Exterior from the UrbanBlock if necessary or null
		IMultiCurve<IOrientableCurve> extBlock = null;
		// Roads are created for this number of decomposition level
		int decompositionLevelWithRoad = 2;
		// Road width
		double roadWidth = 5.0;
		// Boolean forceRoadaccess
		boolean forceRoadAccess = false;

		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// get the previously cuted and reshaped shapefile
		ShapefileDataStore pSDS = new ShapefileDataStore(outU.toURI().toURL());
		SimpleFeatureCollection pSFS = pSDS.getFeatureSource().getFeatures();

		SimpleFeatureCollection splitedAUParcelsFile = VectorFct.splitParcels(pSFS, maximalArea, maximalWidth,
				roadEpsilon, noise, extBlock, roadWidth, forceRoadAccess, tmpFile, p);

		pSDS.dispose();

		// Finally, put them all features in a same collec
		SimpleFeatureIterator finalIt = splitedAUParcelsFile.features();
		int cpt = 0;
		try {
			while (finalIt.hasNext()) {
				SimpleFeature feat = finalIt.next();
				SimpleFeatureBuilder finalParcelBuilder = setSFBWithFeat(feat, savedParcels.getSchema(),
						geometryOutputName);
				if (feat.getAttribute("CODE") == null) {
					finalParcelBuilder = setSFBParDefaut(feat, savedParcels.getSchema(), geometryOutputName);
				}
				savedParcels.add(finalParcelBuilder.buildFeature(String.valueOf(cpt)));
				cpt++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			finalIt.close();
		}

		SimpleFeatureCollection result = savedParcels.collection();
		Vectors.exportSFC(result, new File(tmpFile, "parcelFinal.shp"));

		return result;
	}

}
