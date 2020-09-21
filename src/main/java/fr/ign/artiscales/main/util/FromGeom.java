package fr.ign.artiscales.main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.crypto.dsig.TransformException;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import fr.ign.artiscales.pm.fields.french.FrenchParcelSchemas;
import fr.ign.artiscales.pm.parcelFunction.ParcelState;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Shp;
import fr.ign.artiscales.tools.geometryGeneration.CityGeneration;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;

public class FromGeom {

	public static void main(String[] args) throws Exception {
		mergeBatis(new File("/home/ubuntu/boulot/these/result2903/indic/compatibleResult/CDense/variantMvData1/newSimPLUDepot/"));
	}
	// File rootParam = new
	// File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
	// List<File> lF = new ArrayList<>();
	// lF.add(new File(rootParam, "parameterTechnic.xml"));
	// lF.add(new File(rootParam, "parameterScenario.xml"));
	//
	// Parameters p = Parameters.unmarshall(lF);
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// new
	// File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/21/parcelle.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones =
	// shpDSZone.getFeatureSource().getFeatures();
	// SimpleFeatureIterator it = featuresZones.features();
	// SimpleFeature waiting = null;
	// while (it.hasNext()) {
	// SimpleFeature feat = it.next();
	// if (feat.getAttribute("NUMERO") != null) {
	// if (((String) feat.getAttribute("NUMERO")).equals("0287") && ((String)
	// feat.getAttribute("SECTION")).equals("AB")) {
	// waiting = feat;
	// }
	// }
	// }
	// it.close();
	// System.out.println(affectZoneAndTypoToLocation(p.getString("useRepartition"),
	// "exScenar", waiting, new File("/home/mcolomb/informatique/ArtiScales/"),
	// true));
	//
	// shpDSZone.dispose();
	// }

	public static String affectZoneAndTypoToLocation(String mainLine, String code, IFeature parcel, File rootFile, boolean priorTypoOrZone)
			throws Exception {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		sfBuilder.add(AdapterFactory.toGeometry(new GeometryFactory(), parcel.getGeom()));

		SimpleFeature feature = sfBuilder.buildFeature(null);

		File zoningFile = FromGeom.getZoning(new File(rootFile, "dataRegulation"));
		File communeFile = FromGeom.getCommunities(new File(rootFile, "dataGeo"));

		return affectZoneAndTypoToLocation(mainLine, code, feature, zoningFile, communeFile, priorTypoOrZone);
	}

	/**
	 * affect a repartition file for a specific parcel overload to accept geotool's IFeature
	 * 
	 * @param mainLine
	 * @param code
	 * @param parcel
	 * @param zoningFile
	 * @param communeFile
	 * @param priorTypoOrZone
	 * @return
	 * @throws Exception
	 */
	public static String affectZoneAndTypoToLocation(String mainLine, String code, IFeature parcel, File zoningFile, File communeFile,
			boolean priorTypoOrZone) throws Exception {

		SimpleFeatureBuilder sfBuilder = FrenchParcelSchemas.getSFBFrenchParcel();
		sfBuilder.set("the_geom", AdapterFactory.toGeometry(new GeometryFactory(), parcel.getGeom()));
		SimpleFeature feature = sfBuilder.buildFeature(null);

		return affectZoneAndTypoToLocation(mainLine, code, feature, zoningFile, communeFile, priorTypoOrZone);
	}

	/**
	 * affect a repartition file for a specific parcel
	 * 
	 * @param paramLine
	 *            : value of the parameter file that concerns the zone repartition
	 * @param parcel
	 *            : parcel contained into different zones and/or typs
	 * @param paramName
	 *            : name of the scenario
	 * @param dataRegulation
	 *            :
	 * @param dataGeo
	 * @param priorTypoOrZone
	 *            : if true: we prior the typo if two different zones for zone and typo are found
	 * @return
	 * @throws Exception
	 */
	public static String affectZoneAndTypoToLocation(String mainLine, String code, SimpleFeature parcel, File zoningFile, File communeFile,
			boolean priorTypoOrZone) throws Exception {
		String result = "";
		String isCode = "";
		List<String> mayOccur = new ArrayList<String>();
		// TODO make sure that this returns the best answer
		String zone = ParcelState.parcelInBigZone(zoningFile, parcel);
		if (zone.equals("null")) {
			System.out.println("Could not affect zonez in " + zoningFile + " to " + parcel);
			return "null";
		}
		String typo = ParcelState.parcelInTypo(communeFile, parcel);

		String[] tabRepart = mainLine.split("_");

		// for all the options specified in the parameters file
		for (String s : tabRepart) {
			// If the paramFile speak for a particular scenario
			String[] scenarRepart = s.split(":");
			// if its no longer than 1, no particular scenario
			if (scenarRepart.length > 1) {
				// if codes doesn't match, we continue with another one
				if (!scenarRepart[0].equals(code)) {
					continue;
				}
				s = scenarRepart[1];
				isCode = scenarRepart[0] + ":";
			}
			// seek for the different special locations
			// if both, it's a perfect match !!
			if (s.split("-").length > 1) {
				if (s.split("-")[0].toLowerCase().equals(typo.toLowerCase()) && s.split("-")[1].toLowerCase().equals(zone.toLowerCase())) {
					result = isCode + s;
					break;
				}
			} else if (s.equals(zone)) {
				mayOccur.add(isCode + s);
			} else if (s.equals(typo)) {
				mayOccur.add(isCode + s);
			}
		}
		// if no perfect match found, we seek for a simple zone identifier
		if (result.equals("")) {
			// we prior typo infos than zone infos
			if (priorTypoOrZone) {
				for (String s : mayOccur) {
					if (s.contains(typo)) {
						result = s;
					}
				}
			}
			// we prior zone to typo
			else if (!priorTypoOrZone) {
				for (String s : mayOccur) {
					if (s.contains(zone)) {
						result = s;
					}
				}
			}
			// the type found is not the one priorized. We return it anyway
			else {
				result = mayOccur.get(0);
			}
		}
		return result;
	}
	
	public static List<String> getZips(File regulFile) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureIterator cityIt = sds.getFeatureSource().getFeatures().features();
		List<String> result = new ArrayList<String>();
		try {
			while (cityIt.hasNext()) {
				SimpleFeature city = cityIt.next();
				String insee = (String) city.getAttribute("INSEE");
				if (!result.contains(insee)) {
					result.add(insee);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cityIt.close();
		}
		return result;
	}

	public static List<String> getZipByTypeDoc(File regulFile, String typeDoc) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureIterator cityIt = sds.getFeatureSource().getFeatures().features();
		List<String> result = new ArrayList<String>();
		try {
			while (cityIt.hasNext()) {
				SimpleFeature city = cityIt.next();
				String typlan = (String) city.getAttribute("TYPEPLAN");
				String insee = (String) city.getAttribute("INSEE");
				if (typlan.equals(typeDoc) && !result.contains(insee)) {
					result.add(insee);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cityIt.close();
		}
		return result;
	}

	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile
	 * 
	 * @param file2MergeIn
	 *            : list of files containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(List<File> file2MergeIn) throws Exception {
		File out = new File(file2MergeIn.get(0).getParentFile(), "TotBatSimuFill.shp");
		return Shp.mergeVectFiles(file2MergeIn, out);
	}

	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile with a recursive method.
	 * 
	 * @param folder2MergeFilesIn
	 *            Folder containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(File folder2MergeFilesIn) throws Exception {
		List<File> listBatiFile = addBati(folder2MergeFilesIn);
		File outFile = new File(folder2MergeFilesIn, "TotBatSimuFill.shp");
		return Shp.mergeVectFiles(listBatiFile, outFile);
	}

	public static List<File> addBati(File motherF) {
		ArrayList<File> tmpRes = new ArrayList<File>();
		for (File f : motherF.listFiles()) {
			if (f.isDirectory()) {
				tmpRes.addAll(addBati(f));
			} else {
				if (f.getName().endsWith(".shp") && f.getName().startsWith("out")) {
					tmpRes.add(f);
				}
			}
		}
		return tmpRes;
	}

	public static boolean isBuilt(SimpleFeature parcel, SimpleFeatureCollection batiSFC) throws Exception {
		return isBuilt(parcel, batiSFC, 0.0);
	}

	public static boolean isBuilt(SimpleFeature parcel, SimpleFeatureCollection batiSFC, double bufferParcel) throws Exception {
		SimpleFeatureCollection batiCollec = Collec.snapDatas(batiSFC, (Geometry) parcel.getDefaultGeometry());
		boolean isBuild = false;
		SimpleFeatureIterator batiCollectionIt = batiCollec.features();
		try {
			while (batiCollectionIt.hasNext()) {
				if (((Geometry) parcel.getDefaultGeometry()).buffer(bufferParcel)
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
		return isBuild;
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

	public static File getIlots(File geoFile, SimpleFeatureCollection parcelCollection) throws Exception {
		File ilots = getIlots(geoFile);
		return Shp.snapDatas(ilots,ilots, parcelCollection);
	}

	/**
	 * If an ilot file is contained in the data folder, it returns it. Otherwise, it creates ilots 
	 * 
	 * @param geoFile
	 * @return
	 * @throws NoSuchAuthorityCodeException
	 * @throws IOException
	 * @throws FactoryException
	 */
	public static File getIlots(File geoFile) throws NoSuchAuthorityCodeException, IOException, FactoryException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("ilot") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		System.out.println("ilots not found: auto-generation of them");
		return CityGeneration.createUrbanIslet(getParcel(geoFile), geoFile);
	}

	public static File getZoning(File regulFile) throws FileNotFoundException {
		for (File f : regulFile.listFiles()) {
			if (f.getName().startsWith("zoning") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}

	public static File getParcel(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("parcel") && f.getName().endsWith(".shp")) {
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
		if (regulFile != null)
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
			throws MismatchedDimensionException, CQLException, NoSuchAuthorityCodeException, IOException, FactoryException, TransformException,
			Exception {
		// best exceptions ever

		DefaultFeatureCollection totalParcel = new DefaultFeatureCollection();

		for (String typeZone : typesZone) {
			totalParcel.addAll(selecParcelZoning(typeZone, Collec.snapDatas(parcels, zoningFile), zoningFile));
		}

		return totalParcel.collection();
	}

	/**
	 * get the insee number from a Simplefeature (that is most of the time, a parcel)
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

		String attribute = (String) City.getAttribute("DEPCOM");
		if (attribute != null && !attribute.isEmpty()) {
			cityInsee = attribute;
		}
		return cityInsee;
	}

	/**
	 * return a single TYPEZONE that a parcels intersect if the parcel intersects multiple, we select the one that covers the most area
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return
	 * @throws Exception
	 */
	public static String parcelInTypo(File communeFile, SimpleFeature parcelIn) throws Exception {
		return parcelInTypo(parcelIn, communeFile).get(0);
	}

	/**
	 * return the TYPEZONEs that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInTypo(SimpleFeature parcelIn, File communeFile) throws Exception {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(communeFile.toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Collec.snapDatas(shpDSZone.getFeatureSource().getFeatures(),
				(Geometry) parcelIn.getDefaultGeometry());

		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();
		try {
			zone: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				// TODO if same typo in two different typo, won't fall into that trap =>
				// create a big zone shapefile instead?
				if (((Geometry) feat.getDefaultGeometry()).buffer(1).contains((Geometry) parcelIn.getDefaultGeometry())) {
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
			System.out.println("parcel " + parcelIn.getAttribute("CODE_COM") + parcelIn.getAttribute("SECTION") + parcelIn.getAttribute("NUMERO")
					+ "is IN BETWEEN TWO CITIES OF DIFFERENT TYPOLOGIES");
			System.out.println("thats very rare");
			System.out.println("we randomly use " + result.get(0));
		}

		shpDSZone.dispose();

		return result;

	}

	/**
	 * return the TYPEZONEs that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param zoningFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInBigZone(SimpleFeature parcelIn, File zoningFile) throws Exception {
		List<String> result = new LinkedList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Collec.snapDatas(DataUtilities.collection(shpDSZone.getFeatureSource().getFeatures()), (Geometry) parcelIn.getDefaultGeometry());
		if (shpDSZoneReduced.isEmpty()) {
		  System.out.println("parcelInBigZone = " + zoningFile);
		  System.out.println("ParcelIn = " + parcelIn.getDefaultGeometry());
		}
		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();
		// if there's two zones, we need to sort them by making collection. zis iz évy
		// calculation, but it could worth it
		boolean twoZones = false;
		HashMap<String, Double> repart = new HashMap<String, Double>();

		try {
			zoneLoop: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				PrecisionModel precMod = new PrecisionModel(100);
				Geometry featGeometry = GeometryPrecisionReducer.reduce((Geometry) feat.getDefaultGeometry(), precMod);
				Geometry parcelInGeometry = GeometryPrecisionReducer.reduce((Geometry) parcelIn.getDefaultGeometry(), precMod);

				if (featGeometry.buffer(0.5).contains(parcelInGeometry)) {
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
				// maybe the parcel is in between two zones (less optimized) intersection
				else if ((featGeometry).intersects(parcelInGeometry)) {
					twoZones = true;
					double area = Geom.scaledGeometryReductionIntersection(Arrays.asList(featGeometry, parcelInGeometry)).getArea();
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						if (repart.containsKey("U")) {
							repart.put("U", repart.get("U") + area);
						} else {
							repart.put("U", area);
						}
						break;
					case "AU":
						if (repart.containsKey("AU")) {
							repart.put("AU", repart.get("AU") + area);
						} else {
							repart.put("AU", area);
						}
						break;
					case "N":
					case "NC":
					case "A":
						if (repart.containsKey("NC")) {
							repart.put("NC", repart.get("NC") + area);
						} else {
							repart.put("NC", area);
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

	/**
>>>>>>> geotools-21
	 * return all the insee numbers from a parcel shapeFile
	 * 
	 * @param parcelFile
	 * @return
	 * @throws IOException
	 */
	public static List<String> getInsee(File parcelFile) throws IOException {
		return getInsee(parcelFile, "INSEE");
	}

	/**
	 * return all the insee numbers from a shapeFile
	 * 
	 * @param shpFile
	 * @return
	 * @throws IOException
	 */
	public static List<String> getInsee(File shpFile, String field) throws IOException {

		List<String> result = new ArrayList<String>();
		ShapefileDataStore parcelSDS = new ShapefileDataStore(shpFile.toURI().toURL());
		SimpleFeatureIterator parcelFeaturesIt = parcelSDS.getFeatureSource().getFeatures().features();
		try {
			while (parcelFeaturesIt.hasNext()) {
				SimpleFeature feat = parcelFeaturesIt.next();
				if (!result.contains(feat.getAttribute(field))) {
					result.add((String) feat.getAttribute(field));
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
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(Geom.unionSFC(featureZoneSelected)));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		shpDSZone.dispose();

		return parcelSelected;
	}

	/**
	 * Get parcels from a Zoning file matching a certain type of zone (characterized by the field TYPEZONE) Overload if the entry is a file.
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
		SimpleFeatureCollection result = selecParcelZoning(typeZone, shpDSParcel.getFeatureSource().getFeatures(), zoningFile);
		shpDSParcel.dispose();
		return result;
	}

	public static File getCommunitiesIris(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.isDirectory()) {
				return getCommunitiesIris(f);
			} else if (f.getName().startsWith("communitiesIris") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("CommunitiesIris file not found");
	}

}
