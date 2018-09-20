package fr.ign.cogit.annexeTools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import fr.ign.cogit.GTFunctions.Vectors;

public class PAUDigger {
	// cut cluster polygons with limits
	public static File tmpFile = new File("/home/mcolomb/tmp/pau/");

	public static void main(String[] args) throws Exception {
		File buildFile = new File("/home/mcolomb/informatique/ArtiScales/donneeGeographiques/batimentPro.shp");
		File parcelFile = new File("/home/mcolomb/informatique/ArtiScales/donneeGeographiques/parcelle.shp");
		File morphoLimFile = new File("/home/mcolomb/informatique/ArtiScales/depotConfigSpatMUP/PAU-morpholimEnv.shp");

		File roadFile = new File("/home/mcolomb/informatique/ArtiScales/donneeGeographiques/rroute.shp");
		File riverFile = new File("/home/mcolomb/informatique/ArtiScales/donneeGeographiques/eau.shp");
		File railFile = new File("/media/mcolomb/Data_2/donnee/autom/besancon/dataIn/train/TRONCON_VOIE_FERREE.shp");

		File buildAllegeCluster= prepareClusterBuild(buildFile);
		File limit = prepareLimit(roadFile, riverFile, railFile);
		File splitedCluster = splitLimClus(limit,buildAllegeCluster);
	
		countBuildInCluster(buildAllege, splitedCluster );
	}

	private static File prepareLimit(File roadFile, File riverFile, File railFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		DefaultFeatureCollection collecLimit = new DefaultFeatureCollection();

		ShapefileDataStore roadSDS = new ShapefileDataStore(roadFile.toURI().toURL());
		SimpleFeatureCollection roadSFC = roadSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator roadIt = roadSFC.features();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("limit");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		int i = 1;

		try {
			while (roadIt.hasNext()) {
				SimpleFeature build = roadIt.next();
				if (((String) build.getAttribute("IMPORTANCE")).equals("4") || ((String) build.getAttribute("IMPORTANCE")).equals("3")
						|| ((String) build.getAttribute("IMPORTANCE")).equals("2") || ((String) build.getAttribute("IMPORTANCE")).equals("1")) {
					sfBuilder.add(((Geometry) build.getDefaultGeometry()));
					collecLimit.add(sfBuilder.buildFeature(String.valueOf(i)));
					i++;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			roadIt.close();
		}

		ShapefileDataStore trainSDS = new ShapefileDataStore(railFile.toURI().toURL());
		SimpleFeatureCollection trainSFC = trainSDS.getFeatureSource().getFeatures();

		SimpleFeatureIterator trainIt = trainSFC.features();

		try {
			while (trainIt.hasNext()) {
				sfBuilder.add(((Geometry) trainIt.next().getDefaultGeometry()));
				collecLimit.add(sfBuilder.buildFeature(String.valueOf(i)));
				i++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			trainIt.close();
		}

		ShapefileDataStore riverSDS = new ShapefileDataStore(riverFile.toURI().toURL());
		SimpleFeatureCollection riverSFC = riverSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator riverIt = riverSFC.features();

		try {
			while (riverIt.hasNext()) {
				SimpleFeature river = riverIt.next();
				if (((String) river.getAttribute("REGIME")).equals("Permanent")) {
					sfBuilder.add(((Geometry) river.getDefaultGeometry()));
					collecLimit.add(sfBuilder.buildFeature(String.valueOf(i)));
					i++;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			riverIt.close();
		}

		roadSDS.dispose();
		trainSDS.dispose();
		riverSDS.dispose();

		return Vectors.exportSFC(collecLimit.collection(), new File(tmpFile, "limit.shp"));
		
	}

	public static File prepareClusterBuild(File fBuild) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore buildSDS = new ShapefileDataStore(fBuild.toURI().toURL());
		SimpleFeatureCollection buildSFC = buildSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator bIt = buildSFC.features();

		DefaultFeatureCollection collecBuild = new DefaultFeatureCollection();
		DefaultFeatureCollection bufferBuild = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("buildBuffer");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		int i = 0;
		try {
			while (bIt.hasNext()) {
				SimpleFeature build = bIt.next();
				if (!(((String) build.getAttribute("NATURE")).equals("Bâtiment agricole") || ((String) build.getAttribute("NATURE")).equals("Silo")
						|| ((String) build.getAttribute("NATURE")).equals("Bâtiment industriel") || ((Geometry) build.getDefaultGeometry()).getArea() < 20.0)) {
					collecBuild.add(build);
					sfBuilder.add(((Geometry) build.getDefaultGeometry()).buffer(25));
					bufferBuild.add(sfBuilder.buildFeature(String.valueOf(i)));
					i++;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			bIt.close();
		}
		buildSDS.dispose();
		Vectors.exportSFC(collecBuild.collection(), new File(tmpFile, "batiAllege.shp"));
		return Vectors.exportSFC(bufferBuild.collection(), new File(tmpFile, "batiAllegeBuffer.shp"));

	}

	/**
	 * Cut the cluster regarding the important limits limits TODO doesnt work from now
	 * @return 
	 * 
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File splitLimClus(File fLimit, File fCluster) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		DefaultFeatureCollection cutedCluster = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder finalClusterBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		finalClusterBuilder.setName("cutedCluster");
		finalClusterBuilder.setCRS(sourceCRS);
		finalClusterBuilder.add("the_geom", MultiPolygon.class);
		finalClusterBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureFinalType = finalClusterBuilder.buildFeatureType();
		SimpleFeatureBuilder finalFeatureBuilder = new SimpleFeatureBuilder(featureFinalType);

		ShapefileDataStore dSLimit = new ShapefileDataStore(fLimit.toURI().toURL());
		SimpleFeatureCollection limitsSFC = dSLimit.getFeatureSource().getFeatures();


		DefaultFeatureCollection limDF = new DefaultFeatureCollection();


		ShapefileDataStore dSCluster = new ShapefileDataStore(fCluster.toURI().toURL());
		SimpleFeatureCollection clusterSFC = dSLimit.getFeatureSource().getFeatures();

	
		
		File outFile = new File("/home/mcolomb/tmp/test.shp");
		Vectors.exportSFC(cutedCluster.collection(), outFile);

		dSLimit.dispose();
		dSCluster.dispose();
		return outFile;
	}
}
