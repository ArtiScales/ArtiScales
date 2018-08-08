package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;

public class GetFromGeom {

	public static File getParcels(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcelle.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Parcel file not found");
	}

	// TODO préciser quelle couche de batiment on utilise (si l'on explore plein de jeux de données)
	public static File getBati(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("batiment") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}

	// TODO préciser quelle couche de batiment on utilise (si l'on explore plein de jeux de données)
	public static File getRoute(File geoFile) throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.getName().startsWith("route") && f.getName().endsWith(".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}
	
	public static File getZoning(File rootFile, String zipCode) throws FileNotFoundException {
		File zoningsFile = new File(rootFile, "/donneeGeographiques/PLU/");
		for (File f : zoningsFile.listFiles()) {
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}

	public static int getHousingUnitsGoals(File rootFile, String zipCode) throws IOException {
		File donneGen = new File(rootFile, "donneeGeographiques/donnecommune.csv"); // A mettre dans le fichier de paramètres
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
				if (s.contains("zipcode")) {
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

	/**
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

	public static SimpleFeatureCollection selecParcelZonePLU(String typeZone, String zipcode, File parcelFile, File zoningFile) throws Exception {
		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcelCollection = shpDSParcel.getFeatureSource().getFeatures();
		return selecParcelZonePLU(typeZone, parcelCollection, shpDSParcel, zipcode, zoningFile);
	}

	public static SimpleFeatureCollection selecParcelZonePLU(String typeZone, SimpleFeatureCollection parcelCollection, ShapefileDataStore shpDSParcel, String zipCode,
			File zoningFile) throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		// verificaiton
		System.out.println("Pour la commune " + zipCode);
		System.out.println("on a " + featuresZones.size() + " zones");

		// creation of the filter to select only wanted type of zone in the PLU
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("LIBELLE"), (typeZone.contains("AU") ? "*" : "") + typeZone + "*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
		System.out.println("zones " + typeZone + " au nombre de : " + featureZoneSelected.size());

		// Filter to select parcels that intersects the selected zonnig zone

		Geometry union = Vectors.unionSFC(featureZoneSelected);

		String geometryParcelPropertyName = shpDSParcel.getSchema().getGeometryDescriptor().getLocalName();
		// TODO opérateur géométrique pas terrible, mais rattrapé par le
		// découpage de SimPLU
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(union));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		System.out.println("parcelSelected : " + parcelSelected.size());
		shpDSZone.dispose();
		return parcelSelected;
	}
}
