package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
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
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.annexeTools.FeaturePolygonizer;
import fr.ign.parameters.Parameters;

public class GetFromGeom {

	/**
	 * get the parcel file and add necessary files for an ArtiScales Simulation
	 * 
	 * @param geoFile
	 * @return
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 */
	public static File getParcels(File geoFile, File regulFile, File currentFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		File result = new File("");
		for (File f : geoFile.listFiles()) {
			// TODO if full emprise : if (f.toString().contains("parcelle.shp")) {
			if (f.toString().contains("parcel.shp")) {
				result = f;
			}
		}
		ShapefileDataStore parcelSDS = new ShapefileDataStore(result.toURI().toURL());
		SimpleFeatureCollection parcels = parcelSDS.getFeatureSource().getFeatures();

		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();

		// on snap la couche de batiment et la met dans une géométrie unique
		Geometry batiUnion = Vectors.unionSFC(batiCollection);

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
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				// put the best cell evaluation into the parcel
				String numero = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")) + ((String) feat.getAttribute("COM_ABS"))
						+ ((String) feat.getAttribute("SECTION")) + ((String) feat.getAttribute("NUMERO"));
				String INSEE = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));

				boolean isBuild = false;
				if (((Geometry) feat.getDefaultGeometry()).contains(batiUnion)) {
					isBuild = true;
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
					case "AU":
						result.add("AU");
					case "N":
					case "NC":
					case "A":
						result.add("NC");
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
	public static SimpleFeatureCollection selecParcelZonePLUmergeAU(SimpleFeatureCollection parcels, File zoningFile, Parameters p) throws Exception {

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		// get the AU zones from the zoning file
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("TYPEZONE"), "AU");
		SimpleFeatureCollection zoneAU = featuresZones.subCollection(filter);

		// Filter to select parcels that intersects the selected zonnig zone
		String geometryParcelPropertyName = parcels.getSchema().getGeometryDescriptor().getLocalName();
		Geometry geomAU = Vectors.unionSFC(zoneAU);
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(geomAU));
		SimpleFeatureCollection parcelsInAU = parcels.subCollection(inter);

		Filter contains = ff.contains(ff.property(geometryParcelPropertyName), ff.literal(geomAU.buffer(0.01)));
		SimpleFeatureCollection parcelsAbsoInAU = parcels.subCollection(contains);
		Geometry geomParcAU = Vectors.unionSFC(parcelsAbsoInAU);

		// temporary
		File fParcelsInAU = Vectors.exportSFC(parcelsInAU, new File("/tmp/parcelCible.shp"));
		File fZoneAU = Vectors.exportSFC(zoneAU, new File("/tmp/zoneAU.shp"));

		// cut and separate parcel according to their spatial relation with the zonnig zones
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);
		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();

		// parcel intersecting the U zone must not be cuted and keep their attributes
		File outU = new File("/tmp/polygonU.shp");
		FileDataStore dataStoreU = factory.createDataStore(outU.toURI().toURL());

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

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		dataStoreU.createSchema(sfBuilder.getFeatureType());
		String typeName = dataStoreU.getTypeNames()[0];
		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStoreU.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT);

		System.setProperty("org.geotools.referencing.forceXY", "true");
		System.out.println(Calendar.getInstance().getTime() + " write shapefile");

		// List<Polygon> polygonsFinal = FeaturePolygonizer.getPolygons(polyFiles);
		// //delete polygon if the parcel
		// for (Polygon poly : polygons) {
		// SimpleFeatureIterator parcelIt = parcelsInAU.features();
		// boolean keepPoly = false;
		// try {
		// while (parcelIt.hasNext()) {
		// SimpleFeature feat = parcelIt.next();
		// if (((Geometry) feat.getDefaultGeometry()).equals(poly)) {
		// keepPoly = true;
		// }
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// parcelIt.close();
		// }
		// if (keepPoly) {
		// polygonsFinal.add(poly);
		// }
		// }
		//
		// //temp block
		// String specs = "geom:Polygon:srid=2154";
		// File out = new File("/tmp/polygon.shp");
		// ShapefileDataStoreFactory factory2 = new ShapefileDataStoreFactory();
		// FileDataStore dataStore = factory2.createDataStore(out.toURI().toURL());
		// String featureTypeName = "Object";
		// SimpleFeatureType featureType = DataUtilities.createType(featureTypeName, specs);
		// dataStore.createSchema(featureType);
		// String typeName2 = dataStore.getTypeNames()[0];
		// FeatureWriter<SimpleFeatureType, SimpleFeature> writer2 = dataStore.getFeatureWriterAppend(typeName2, Transaction.AUTO_COMMIT);
		// System.setProperty("org.geotools.referencing.forceXY", "true");
		// System.out.println(Calendar.getInstance().getTime() + " write shapefile");
		// for (Polygon po : polygonsFinal) {
		//
		// SimpleFeature feature = writer2.next();
		// feature.setAttributes(new Object[] { po });
		// writer2.write();
		//
		// }
		// System.out.println(Calendar.getInstance().getTime() + " done");
		// writer2.close();
		// dataStore.dispose();
		//
		//
		//

		for (Polygon poly : polygons) {
			if (!geomAU.buffer(0.01).contains(poly)) {
				Object[] attr = new Object[11];
				SimpleFeatureIterator parcelIt = parcelsInAU.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						if (((Geometry) feat.getDefaultGeometry()).contains(poly)) {
							attr = feat.getAttributes().toArray();
							attr[10] = 0;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
				SimpleFeature feature = writer.next();
				feature.setAttributes(attr);
				feature.setDefaultGeometry(poly);
				writer.write();
			}
		}

		// parcel within the AU zone must be merged and prepared to be cuted (with the value 1 at the SPLIT attribute)
		// MultiPolygon mp = (MultiPolygon) geomAU;
		// for (int i = 0; i < mp.getNumGeometries(); i++) {
		// SimpleFeature feature = writer.next();
		// feature.setAttribute("SPLIT", 1);
		// feature.setDefaultGeometry(mp.getGeometryN(i));
		// writer.write();
		// }

		GeometryCollection collec = (GeometryCollection) geomParcAU;
		for (int i = 0; i < collec.getNumGeometries(); i++) {
			SimpleFeature feature = writer.next();
			feature.setAttribute("SPLIT", 1);
			feature.setDefaultGeometry(collec.getGeometryN(i));
			writer.write();
		}

		writer.close();
		dataStoreU.dispose();

		double roadEpsilon = 0.5;
		double noise = 0;
		double maximalArea = 1500;
		double maximalWidth = 15;
		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// get the previously cuted and reshaped shapefile
		ShapefileDataStore pSDS = new ShapefileDataStore(outU.toURI().toURL());
		SimpleFeatureCollection pSFS = pSDS.getFeatureSource().getFeatures();

		SimpleFeatureCollection splitedAUParcels = VectorFct.splitParcels(pSFS, maximalArea, maximalWidth, roadEpsilon, noise, p);
		Vectors.exportSFC(splitedAUParcels, new File("/tmp/parcelCuted.shp"));

		shpDSZone.dispose();

		return splitedAUParcels;

	}

}
