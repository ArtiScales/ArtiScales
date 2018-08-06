package fr.ign.cogit.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.resources.coverage.IntersectUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;

import fr.ign.cogit.SelectParcels;

public class PAUDigger {
//cut cluster polygons with limits
	
	public static void main(String[] args) throws Exception {
splitLimClus();
	}
	
	public static void splitLimClus() throws IOException, NoSuchAuthorityCodeException, FactoryException{
	File limit = new File("/home/mcolomb/doc_de_travail/PAU/snap/snapLim.shp");
	File cluster = new File("/home/mcolomb/doc_de_travail/PAU/snap/snapCluster.shp");
	
	DefaultFeatureCollection cutedCluster = new DefaultFeatureCollection();
	
	SimpleFeatureTypeBuilder finalBuilder = new SimpleFeatureTypeBuilder();
	CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
	finalBuilder.setName("cutedCluster");
	finalBuilder.setCRS(sourceCRS);
	finalBuilder.add("the_geom", MultiPolygon.class);
	finalBuilder.setDefaultGeometry("the_geom");
	SimpleFeatureType featureFinalType = finalBuilder.buildFeatureType();
	SimpleFeatureBuilder finalFeatureBuilder = new SimpleFeatureBuilder(featureFinalType);
	
	ShapefileDataStore dSLimit = new ShapefileDataStore(limit.toURI().toURL());
	SimpleFeatureCollection limitsSFC = dSLimit.getFeatureSource().getFeatures();

	Geometry limite = SelectParcels.unionSFC(limitsSFC);
	
	DefaultFeatureCollection limDF = new DefaultFeatureCollection();
	
	SimpleFeatureTypeBuilder lBuilder = new SimpleFeatureTypeBuilder();
	lBuilder.setName("limit");
	lBuilder.setCRS(sourceCRS);
	lBuilder.add("the_geom", MultiLineString.class);
	lBuilder.setDefaultGeometry("the_geom");

	SimpleFeatureType featurelType = lBuilder.buildFeatureType();
	SimpleFeatureBuilder lFeatureBuilder = new SimpleFeatureBuilder(featurelType);
	
	lFeatureBuilder.add(limite);
	SimpleFeature limF = lFeatureBuilder.buildFeature("1");
	limDF.add(limF);
	SelectParcels.exportSFC(limDF.collection(), new File("/home/mcolomb/tmp/testlim.shp"));
	
	ShapefileDataStore dSCluster = new ShapefileDataStore(cluster.toURI().toURL());
	SimpleFeatureCollection clusterSFC = dSLimit.getFeatureSource().getFeatures();
	
	SimpleFeatureIterator clusIt = clusterSFC.features();
	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	SpatialIndexFeatureCollection sifc = new SpatialIndexFeatureCollection(clusterSFC);
	int finalId = 0;
	try {
		while (clusIt.hasNext()) {
			SimpleFeature feat = clusIt.next();
			Geometry clusterGeom = ((Geometry) feat.getDefaultGeometry());

			BBOX boundsCheck = ff.bbox(ff.property("the_geom"), limitsSFC.getBounds());
			SimpleFeatureIterator chosenFeatIterator = sifc.subCollection(boundsCheck).features();
			Geometry unionGeom = null;
			List<Geometry> list = new ArrayList<>();
			while (chosenFeatIterator.hasNext()) {
				SimpleFeature f = chosenFeatIterator.next();
				Geometry g = (Geometry) f.getDefaultGeometry();

				if (g.intersects(limite)) {

					list.add(g);
				}
			}
			GeometryCollection coll = limite.getFactory().createGeometryCollection(list.toArray(new Geometry[list.size()]));
			try {
				Geometry y = coll.union();
				if (y.isValid()){
		//			coll = y;
				}
			} catch (Exception e) {
			}
			unionGeom = IntersectUtils.intersection(clusterGeom,coll);
			try {
				Geometry y = unionGeom.buffer(0);
				if (y.isValid()) {
					unionGeom = y;
				}
			} catch (Exception e) {
			}
			
			if (unionGeom != null) {
				finalFeatureBuilder.add(unionGeom);
				SimpleFeature feature = finalFeatureBuilder.buildFeature(String.valueOf(finalId++));
				cutedCluster.add(feature);
			}
		}
	} catch (Exception problem) {
		problem.printStackTrace();
	} finally {
		clusIt.close();
	}
	SelectParcels.exportSFC(cutedCluster.collection(),new File("/home/mcolomb/tmp/test.shp"));
	
	dSLimit.dispose();
	dSCluster.dispose();
}
	}
