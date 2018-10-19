package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

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

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.parameters.Parameters;

public class GetFromGeom {

	
	public static File getParcels(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcelle.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Parcel file not found");
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

	public static int getHousingUnitsGoals(File geoFile, String zipCode) throws IOException {
		File donneGen = new File(geoFile, "donnecommune.csv"); // A mettre dans le fichier de paramètres?
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

	public static File getZoning(File pluFile, String zipCode) throws FileNotFoundException {
		for (File f : pluFile.listFiles()) {
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		
		for (File f : pluFile.listFiles()) {
			Pattern insee = Pattern.compile("zoneUrba");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		
		throw new FileNotFoundException("Zoning file not found");
	}

//	public static File getPAUzone(File pluFile, File geoFile, File tmpFile, String zipCode) throws Exception {
//		String type = "zone";
//		return getPAU(type, pluFile, geoFile, tmpFile, zipCode);
//	}
//	
//	
//	public static File getPAUparcel(File pluFile, File geoFile, File tmpFile, String zipCode) throws Exception {
//		String type = "parcel";
//		return getPAU(type, pluFile, geoFile, tmpFile, zipCode);
//	}
//
//
//	public static File getPAU(String type, File pluFile, File geoFile, File tmpFile, String zipCode) throws Exception {
//
//		tmpFile.mkdir();
//String test = "TypeZone";
//		File pauFile = new File(pluFile, type+"PAU.shp");
//		ShapefileDataStore shpDSpau = new ShapefileDataStore(pauFile.toURI().toURL());
//		SimpleFeatureCollection pau = shpDSpau.getFeatureSource().getFeatures();
//
//		File adminFile = new File(geoFile, "admin_typo.shp");
//		ShapefileDataStore shpDSadmin = new ShapefileDataStore(adminFile.toURI().toURL());
//		SimpleFeatureCollection admin = shpDSadmin.getFeatureSource().getFeatures();
//		SimpleFeatureIterator adminIt = admin.features();
//
//		SimpleFeature sf = null;
//
//		try {
//			while (adminIt.hasNext()) {
//				SimpleFeature sFeat = adminIt.next();
//				if (((String) sFeat.getAttribute("DEPCOM")).equals(zipCode)) {
//					sf = sFeat;
//				}
//			}
//		} catch (Exception problem) {
//			problem.printStackTrace();
//		} finally {
//			adminIt.close();
//		}
//
//		Vectors.exportSFC(Vectors.snapDatas(pau, (Geometry) sf.getDefaultGeometry()), new File(tmpFile, "pau_" + zipCode + ".shp"));
//		shpDSpau.dispose();
//		return new File(tmpFile, "pau_" + zipCode + ".shp");
//	}

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
			totalParcel.addAll(selecParcelZoning(typeZone, Vectors.snapDatas(parcels, zoningFile), zipCode, zoningFile));
		}

		return totalParcel.collection();
	}

	/**
	 * Choppe les parcelles d'une certaine zone du PLU
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
	public static SimpleFeatureCollection selecParcelZoning(String typeZone, String zipcode, File parcelFile, File zoningFile) throws Exception {
		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(parcelFile.toURI().toURL());
		return selecParcelZoning(typeZone, Vectors.snapDatas(shpDSParcel.getFeatureSource().getFeatures(), zoningFile), zipcode, zoningFile);
	}

	public static SimpleFeatureCollection selecParcelZoning(String typeZone, SimpleFeatureCollection parcelCollection, String zipCode, File zoningFile)
			throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		// verificaiton
		System.out.println("Pour la commune " + zipCode + " on a " + featuresZones.size() + " zones différentes");


		
		// creation of the filter to select only wanted type of zone in the PLU
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("TYPEZONE"), (typeZone.contains("AU") ? "*" : "") + typeZone + "*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
	
		if(featureZoneSelected.getSchema().getType("TYPEZONE") == null) {
			System.out.println("ATTRIBUTE TYPEZONE IS MISSING in data " + zoningFile);
			return null;
		}
		
		System.out.println("zones " + typeZone + " au nombre de : " + featureZoneSelected.size());

		// Filter to select parcels that intersects the selected zonnig zone
		String geometryParcelPropertyName = parcelCollection.getSchema().getGeometryDescriptor().getLocalName();

		// TODO opérateur géométrique pas terrible, mais rattrapé par le
		// découpage de SimPLU
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(Vectors.unionSFC(featureZoneSelected)));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		System.out.println("parcelSelected : " + parcelSelected.size());
		shpDSZone.dispose();

		return parcelSelected;
	}

	// public static SimpleFeatureCollection selecParcelZonePLUmergeAU(File parcelFile, String zipCode, File zoningFile, Parameters p)
	// throws Exception {
	//
	// // import the parcel files
	// ShapefileDataStore shpDSParc = new ShapefileDataStore(parcelFile.toURI().toURL());
	// SimpleFeatureCollection featuresParc = shpDSParc.getFeatureSource().getFeatures();
	//
	// // import of the zoning file
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
	// SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
	//
	// // output
	// DefaultFeatureCollection parcelAUCuted = new DefaultFeatureCollection();
	//
	// // verificaiton
	// System.out.println("Pour la commune " + zipCode + " on a " + featuresZones.size() + " zones différentes");
	//
	// // creation of the filter to select only wanted type of zone in the PLU
	// // for the 'AU' zones, a temporality attribute is usually pre-fixed, we
	// // need to search after
	// FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
	// Filter filter = ff.like(ff.property("TYPEZONE"), "*AU*");
	// SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
	// System.out.println("zones " + "AU" + " au nombre de : " + featureZoneSelected.size());
	// int i = 0;
	// SimpleFeatureIterator zoneIt = featureZoneSelected.features();
	// try {
	// while (zoneIt.hasNext()) {
	// SimpleFeature zone = zoneIt.next();
	// // Filter to select parcels that intersects the selected zonnig zone
	// String geometryParcelPropertyName = featuresParc.getSchema().getGeometryDescriptor().getLocalName();
	//
	// // TODO opérateur géométrique pas terrible, mais rattrapé par le
	// // découpage de SimPLU
	// Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(zone.getDefaultGeometry()));
	// SimpleFeatureCollection parcelSelected = featuresParc.subCollection(inter);
	// Vectors.exportSFC(parcelSelected, new File("/home/mcolomb/tmp/samere"+i+".shp"));
	//
	// SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
	//
	// CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
	// sfTypeBuilder.setName("mergeAUParcels");
	// sfTypeBuilder.setCRS(sourceCRS);
	// sfTypeBuilder.add("the_geom", Polygon.class);
	// sfTypeBuilder.setDefaultGeometry("the_geom");
	//
	// SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	//
	// sfBuilder.add(Vectors.unionSFC(parcelSelected));
	// parcelAUCuted.add(sfBuilder.buildFeature(String.valueOf(i)));
	// i = i + 1;
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// zoneIt.close();
	// }
	//
	// System.out.println("parcelSelected : " + parcelAUCuted.size());
	// shpDSZone.dispose();
	// Vectors.exportSFC(parcelAUCuted.collection(), new File("/home/mcolomb/tmp/samereTot.shp"));
	// return VectorFct.generateSplitedParcels(parcelAUCuted.collection(), p);
	// }

	public static SimpleFeatureCollection selecParcelZonePLUmergeAU(File parcelFile, String zipCode, File zoningFile, Parameters p) throws Exception {

		// import the parcel files
		ShapefileDataStore shpDSParc = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection featuresParc = shpDSParc.getFeatureSource().getFeatures();

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		// output
		DefaultFeatureCollection parcelAUCuted = new DefaultFeatureCollection();

		// verificaiton
		System.out.println("Pour la commune " + zipCode + " on a " + featuresZones.size() + " zones différentes");

		// creation of the filter to select only wanted type of zone in the PLU
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("TYPEZONE"), "*AU*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
		System.out.println("zones " + "AU" + " au nombre de : " + featureZoneSelected.size());

		// Filter to select parcels that intersects the selected zonnig zone
		String geometryParcelPropertyName = featuresParc.getSchema().getGeometryDescriptor().getLocalName();

		// TODO opérateur géométrique pas terrible, mais rattrapé par le
		// découpage de SimPLU
		File selectedParcelFile = new File(p.getString("selectedParcelFile"));
		File folderParent = selectedParcelFile.getParentFile();
		if(!folderParent.exists()) {
			folderParent.mkdirs();
		}
		
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(Vectors.unionSFC(featureZoneSelected)));
		SimpleFeatureCollection parcelSelected = featuresParc.subCollection(inter);
		Vectors.exportSFC(parcelSelected, selectedParcelFile);

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("mergeAUParcels");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("NUMERO", Integer.class);

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		// TODO ne marche pas car renvoie juste quelques parcelles découpés. Surement la faute au multipolygone dans l'algo de découpage. Repasser en simple polygones, mais
		// comment?
		sfBuilder.add(Vectors.unionSFC(parcelSelected));
		String[] attr = { "1" };
		parcelAUCuted.add(sfBuilder.buildFeature(String.valueOf(0), attr));

		Vectors.exportSFC(parcelAUCuted.collection(), new File(folderParent.getAbsolutePath()+"/parcelMerged.shp"));

		Vectors.exportSFC(VectorFct.generateSplitedParcels(parcelAUCuted, p), new File(folderParent.getAbsolutePath()+ "parcelCuted.shp"));

		System.out.println("parcelSelected : " + parcelAUCuted.size());
		shpDSZone.dispose();

		return VectorFct.generateSplitedParcels(parcelAUCuted.collection(), p);
	}

}
