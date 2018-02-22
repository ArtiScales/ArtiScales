package fr.ign.cogit;

import java.io.File;
import java.io.IOException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
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
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class Test {

	public static void main(String[] args) throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException {
		SimpleFeatureCollection geoFlaSFC = (new ShapefileDataStore((new File("/home/mcolomb/tmp/cleaned2.shp")).toURI().toURL())).getFeatureSource().getFeatures();
		DefaultFeatureCollection villeColl = new DefaultFeatureCollection();
		DefaultFeatureCollection emprise = new DefaultFeatureCollection();

		for (Object obj : geoFlaSFC.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			villeColl.add(feat);
		}

		Geometry geo = SelectParcels.unionSFC(villeColl);
		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("NU-union");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);
		sfBuilder.add(wktReader.read(geo.toString()));
		SimpleFeature feature = sfBuilder.buildFeature("0");
		emprise.add(feature);

		SelectParcels.exportSFC(emprise.collection(), new File("/home/mcolomb/workspace/mupcity-openMole/data/nonUrbaSys2.shp"));

	}
}
