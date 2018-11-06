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
import fr.ign.parameters.Parameters;

public class GetFromGeom {

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
	public static File getParcels(File geoFile, File regulFile, File currentFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File result = new File("");
		for (File f : geoFile.listFiles()) {
			// if (f.toString().contains("parcelle.shp")) {
			if (f.toString().contains("parcel.shp")) {
				result = f;
			}
		}
		ShapefileDataStore parcelSDS = new ShapefileDataStore(result.toURI().toURL());
		SimpleFeatureCollection parcels = parcelSDS.getFeatureSource().getFeatures();

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
				boolean casse = false;

				for (String s : bigZoneinParcel(feat, regulFile)) {
					if (s.equals("AU")) {
						au = true;
					} else if (s.equals("U")) {
						u = true;
					} else if (s.equals("NC")) {
						nc = true;
					} else {
						casse = true;
					}
				}
				// if the parcel is outside of the zoning file, we don't keep it
				if (casse) {
					continue;
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

		return Vectors.exportSFC(newParcel.collection(), new File(currentFile, "parcels.shp"));
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

	/**
	 * get the negative file of the parcels (ugly method to deal with it)
	 * 
	 * @param geoFile
	 * @return
	 * @throws FileNotFoundException
	 */
	public static File getNegParcels(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("negParcel") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
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

	public static List<String> bigZoneinParcel(SimpleFeature parcelIn, File regulFile) throws Exception {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning(regulFile).toURI().toURL());
		SimpleFeatureIterator featuresZones = shpDSZone.getFeatureSource().getFeatures().features();
		try {
			while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				if (((Geometry) feat.getDefaultGeometry()).intersects((Geometry) parcelIn.getDefaultGeometry())) {
					// TODO prendre en compte le multizone et fermer le sds
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

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0MCIgn");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parametreTechnique.xml"));
		lF.add(new File(rootParam, "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);
		ShapefileDataStore shpDSZone = new ShapefileDataStore((new File("/home/mcolomb/informatique/ArtiScales/tmp/parcelGenExport.shp")).toURI().toURL());
		SimpleFeatureCollection parcel = shpDSZone.getFeatureSource().getFeatures();

		selecParcelZonePLUmergeAU(parcel, new File("/home/mcolomb/informatique/ArtiScales/tmp"), new File("/home/mcolomb/informatique/ArtiScales/dataRegul/zoningRegroupe.shp"),
				new File("/home/mcolomb/informatique/ArtiScales/dataGeo/negParcel.shp"), p);

	}

	/**
	 * Merge and recut the to urbanised (AU) zones. TODO : faire apparaitre les routes car elles ne sont pas prises en comptes et le découpage est faite indépendament d'elle. TODO
	 * essayé avec les polygones ou en faisant le merge avec les parcelles au lieu de prendre les zones, ça ne marche pas, j'y reviendrais
	 * 
	 * @param parcels
	 * @param zoningFile
	 * @param p
	 *            : parametre file s
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection selecParcelZonePLUmergeAU(SimpleFeatureCollection parcels, File tmpFile, File zoningFile, File negParcel, Parameters p) throws Exception {

		// parcels to save for after
		DefaultFeatureCollection savedParcels = new DefaultFeatureCollection();
		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		Geometry unionParcel = Vectors.unionSFC(parcels);
		Vectors.exportGeom(unionParcel, new File("/home/mcolomb/tmp/parcelMerge.shp"));

		// get the AU zones from the zoning file
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("TYPEZONE"), "AU");
		SimpleFeatureCollection zoneAU = featuresZones.subCollection(filter);

		// Filter to select parcels that intersects the selected zonnig zone
		String geometryParcelPropertyName = parcels.getSchema().getGeometryDescriptor().getLocalName();
		// all the AU zones
		Geometry geomAU = Vectors.unionSFC(zoneAU);
		DefaultFeatureCollection parcelsInAU = new DefaultFeatureCollection();
		SimpleFeatureIterator parcIt = parcels.features();
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

		// cut and separate parcel according to their spatial relation with the zonnig zones
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

		// parcel intersecting the U zone must not be cuted and keep their attributes
		// intermediary result
		File outU = new File(tmpFile, "polygonU.shp");
		SimpleFeatureBuilder sfBuilder = getParcelSplitSFBuilder();

		DefaultFeatureCollection write = new DefaultFeatureCollection();

		int nFeat = 0;
		// for every polygons situated in between U and AU zones, we cut the parcels regarding to the zone and copy them attributes to keep the existing U parcels
		for (Geometry poly : polygons) {
			// if the polygons are not included on the AU zone
			if (!geomAU.buffer(0.01).contains(poly)) {

				sfBuilder.add(poly);
				Object[] attr = new Object[14];
				SimpleFeatureIterator parcelIt = parcelsInAU.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						if (((Geometry) feat.getDefaultGeometry()).buffer(0.01).contains(poly)) {
							for (int i = 0; i < feat.getAttributes().toArray().length; i++) {
								attr[i] = feat.getAttributes().toArray()[i];
							}
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
				sfBuilder.set("CODE", insee + "000" + "NewSection" + numZone);
				sfBuilder.set("CODE_DEP", insee.substring(0, 2));
				sfBuilder.set("CODE_COM", insee.substring(2, 5));
				sfBuilder.set("COM_ABS", "000");
				sfBuilder.set("SECTION", "NewSection" + numZone);
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
						System.out.println("multi" + intersectedGeom);
						for (int i = 0; i < intersectedGeom.getNumGeometries(); i++) {
							sfBuilder.set(geometryOutputName, intersectedGeom.getGeometryN(i));
							write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
							nFeat++;
							// ugly, but have to do it
							sfBuilder.set("CODE", insee + "000" + "NewSection" + numZone);
							sfBuilder.set("CODE_DEP", insee.substring(0, 2));
							sfBuilder.set("CODE_COM", insee.substring(2, 5));
							sfBuilder.set("COM_ABS", "000");
							sfBuilder.set("SECTION", "NewSection" + numZone);
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

		double roadEpsilon = 0.5;
		double noise = 0;
		double maximalArea = 400;
		double maximalWidth = 50;
//		if (!(p == null)) {
//			maximalArea = p.getDouble("maximalAreaSplitParcel");
//			maximalWidth = p.getDouble("maximalWidthSplitParcel");
//		}
		
		
		// get the previously cuted and reshaped shapefile
		ShapefileDataStore pSDS = new ShapefileDataStore(outU.toURI().toURL());
		SimpleFeatureCollection pSFS = pSDS.getFeatureSource().getFeatures();

		SimpleFeatureCollection splitedAUParcels = VectorFct.splitParcels(pSFS, maximalArea, maximalWidth, roadEpsilon, noise, p);
		Vectors.exportSFC(splitedAUParcels, new File(tmpFile, "parcelCuted.shp"));
		// Final, put them all in a same SHP
		SimpleFeatureBuilder finalParcelBuilder = getParcelSFBuilder();
		SimpleFeatureIterator finalIt = splitedAUParcels.features();
		int cpt = 0;
		try {
			while (finalIt.hasNext()) {
				SimpleFeature feat = finalIt.next();
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
				savedParcels.add(finalParcelBuilder.buildFeature(String.valueOf(cpt)));
				cpt++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		Vectors.exportSFC(savedParcels, new File(tmpFile, "parcelFinal.shp"));

		shpDSZone.dispose();

		return savedParcels;

	}

}
