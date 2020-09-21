package fr.ign.artiscales.main.annexeTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import fr.ign.artiscales.pm.goal.ConsolidationDivision;
import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Shp;
import fr.ign.artiscales.tools.parameter.ProfileUrbanFabric;

public class PAUDigger {
	// cut cluster polygons with limits

	public static void main(String[] args) throws Exception {
//		File rootFile = new File("/home/ubuntu/boulot/these/newZoning/");
//		File communitiesFile = new File(rootFile, "dataGeo/communities.shp");
//		ShapefileDataStore morphoSDS = new ShapefileDataStore(communitiesFile.toURI().toURL());
//		DefaultFeatureCollection df = new DefaultFeatureCollection();
//		try (SimpleFeatureIterator it = morphoSDS.getFeatureSource().getFeatures().features()){
//			while (it.hasNext()) {
//				SimpleFeature feat = it.next();
//				Geometry g = ((Geometry) feat.getDefaultGeometry());
//				PreciseConvexHull pch = new PreciseConvexHull(g);
//				feat.setAttribute("the_geom", pch.getConvexHull());
//				df.add(feat);
//			}
//		} catch (Exception problem) {
//			problem.printStackTrace();
//		}
//		Collec.exportSFC(df.collection(), new File(rootFile, "newComm.shp"));
		createPAU();
	}

	public static File createPAU() throws Exception {

		File tmpFile = new File("/tmp/");
		File rootFile = new File("/home/ubuntu/boulot/these/newZoning/");
		File dataIn = new File(rootFile, "dataGeo");
		File outFile = new File(rootFile, "final");

		File buildFile = new File(dataIn , "building.shp");
		File parcelFile = new File(dataIn , "parcel.shp");
		File communitiesFile = new File(dataIn , "communities.shp");
		File morphoLimFile = new File(dataIn , "PAU-morpholimEnv.shp");
		File roadFile = new File(dataIn , "roadPAU.shp");
		File riverFile = new File(dataIn , "river.shp");
		File railFile = new File(dataIn , "TRONCON_VOIE_FERREE.shp");
		
		// zones NU
		List<File> nU = new ArrayList<File>();
		File NUroot = new File(rootFile, "/NU/");
		for (File f : NUroot.listFiles()) {
			if (f.getName().endsWith(".shp"))
				nU.add(f);
		}
		File fileNU = Shp.mergeVectFiles(nU, new File(tmpFile, "zonesNU.shp"), false);
		ShapefileDataStore nUSDS = new ShapefileDataStore(fileNU.toURI().toURL());
		Geometry unionNU = Geom.unionSFC(nUSDS.getFeatureSource().getFeatures());

		File[] buildResult = prepareClusterBuild(buildFile, tmpFile, "");
		File buildAllegeCluster = buildResult[1];
		File buildAllege = buildResult[0];

		File limit = prepareLimit(roadFile, riverFile, railFile, tmpFile);
		File splitedCluster = splitLimClus(limit, buildAllegeCluster, buildAllege, tmpFile);
		ShapefileDataStore clusterSDS = new ShapefileDataStore(splitedCluster.toURI().toURL());
		Geometry clusterUnion = Geom.unionSFC(clusterSDS.getFeatureSource().getFeatures());

		// morphology
		ShapefileDataStore morphoSDS = new ShapefileDataStore(morphoLimFile.toURI().toURL());
		Geometry morphoUnion = Geom.unionSFC(morphoSDS.getFeatureSource().getFeatures());

		// selection with geographical filters
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(morphoSDS.getSchema().getGeometryDescriptor().getLocalName());
		Filter filCluster = ff.intersects(pName, ff.literal(clusterUnion));
		Filter filMorpho = ff.intersects(pName, ff.literal(morphoUnion));
		Filter filNU = ff.not(ff.intersects(pName, ff.literal(unionNU)));

		// parcels
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcelPreSelected = (new SpatialIndexFeatureCollection(parcelSDS.getFeatureSource().getFeatures()))
				.subCollection(filCluster).subCollection(filMorpho);

		// TODO mark all (anf
		SimpleFeatureCollection parcelSplitted = (new ConsolidationDivision()).consolidationDivision(parcelPreSelected, null, tmpFile, new ProfileUrbanFabric() );
		SimpleFeatureCollection out = makeEnvelopePAU(parcelSplitted.subCollection(filNU).subCollection(filMorpho).subCollection(filCluster),
				communitiesFile);
		nUSDS.dispose();
		parcelSDS.dispose();
		morphoSDS.dispose();
		clusterSDS.dispose();

		return Collec.exportSFC(out, new File(outFile, "zonePAU"));
	}

	public static SimpleFeatureBuilder pAUBuilder() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setName("PAU");
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("nom_zone", String.class);
		sfTypeBuilder.add("LIBELLE", String.class);
		sfTypeBuilder.add("TYPEZONE", String.class);
		sfTypeBuilder.add("TYPEPLAN", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	private static SimpleFeatureCollection makeEnvelopePAU(SimpleFeatureCollection pau, File communitiesFile)
			throws NoSuchAuthorityCodeException, FactoryException, IOException, SchemaException {
		DefaultFeatureCollection df = new DefaultFeatureCollection();
		SimpleFeatureBuilder sfBuilder = pAUBuilder();
		
		MultiPolygon mp = (MultiPolygon) Geom.unionSFC(pau);
		int nbGeom = mp.getNumGeometries();
		ShapefileDataStore communeSDS = new ShapefileDataStore(communitiesFile.toURI().toURL());
		SimpleFeatureCollection communeSFC = communeSDS.getFeatureSource().getFeatures();

		for (int i = 0; i < nbGeom; i++) {
			Geometry geom = mp.getGeometryN(i);
			sfBuilder.set("the_geom", geom.buffer(0));
			sfBuilder.set("LIBELLE", "ZC");
			sfBuilder.set("TYPEZONE", "ZC");
			sfBuilder.set("TYPEPLAN", "RNU");
			String insee = "";
			try (SimpleFeatureIterator it = communeSFC.features()) {
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					if (((Geometry) feat.getDefaultGeometry()).contains(geom)) {
						insee = (String) feat.getAttribute("DEPCOM");
						break;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} 
			// TODO some insee are set to null - check
			sfBuilder.set("INSEE", insee);
			df.add(sfBuilder.buildFeature(null));
		}
		communeSDS.dispose();
		return df.collection();
	}

	private static File prepareLimit(File roadFile, File riverFile, File railFile, File tmpFile)
			throws NoSuchAuthorityCodeException, FactoryException, IOException {
		DefaultFeatureCollection collecLimit = new DefaultFeatureCollection();
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaMultiPolygon("limit");

		ShapefileDataStore roadSDS = new ShapefileDataStore(roadFile.toURI().toURL());
		try (SimpleFeatureIterator roadIt = roadSDS.getFeatureSource().getFeatures().features()) {
			while (roadIt.hasNext()) {
				SimpleFeature road = roadIt.next();
				if (Integer.valueOf((String) road.getAttribute("IMPORTANCE")) >= 1
						&& Integer.valueOf((String) road.getAttribute("IMPORTANCE")) <= 4) {
					sfBuilder.add(((Geometry) road.getDefaultGeometry()));
					collecLimit.add(sfBuilder.buildFeature(Attribute.makeUniqueId()));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} 
		roadSDS.dispose();

		ShapefileDataStore trainSDS = new ShapefileDataStore(railFile.toURI().toURL());
		try (SimpleFeatureIterator trainIt = trainSDS.getFeatureSource().getFeatures().features()){
			while (trainIt.hasNext()) {
				sfBuilder.add(((Geometry) trainIt.next().getDefaultGeometry()));
				collecLimit.add(sfBuilder.buildFeature(Attribute.makeUniqueId()));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} 		
		trainSDS.dispose();

		ShapefileDataStore riverSDS = new ShapefileDataStore(riverFile.toURI().toURL());
		try (SimpleFeatureIterator riverIt = riverSDS.getFeatureSource().getFeatures().features()) {
			while (riverIt.hasNext()) {
				SimpleFeature river = riverIt.next();
				if (((String) river.getAttribute("REGIME")).equals("Permanent")) {
					sfBuilder.add(((Geometry) river.getDefaultGeometry()));
					collecLimit.add(sfBuilder.buildFeature(Attribute.makeUniqueId()));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} 
		riverSDS.dispose();
		return Collec.exportSFC(collecLimit.collection(), new File(tmpFile, "limit.shp"));
	}

	/**
	 * prepare the clusters of buildings
	 * 
	 * @param fBuild
	 * @return a tab coinaining [0] : the selected build files [1] the buffered builed file
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static File[] prepareClusterBuild(File fBuild, File tmpFile, String bdTopoVersion) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore buildSDS = new ShapefileDataStore(fBuild.toURI().toURL());
		DefaultFeatureCollection collecBuild = new DefaultFeatureCollection();
		DefaultFeatureCollection bufferBuild = new DefaultFeatureCollection();
		SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaMultiPolygon("buildBuffer");

		List<Geometry> lG = new ArrayList<Geometry>();
		Arrays.stream(buildSDS.getFeatureSource().getFeatures().toArray(new SimpleFeature[0])).forEach(feat -> {
			// if the building is from an old version of the BD Topo and we need to sort the industrial and farmer buildings
			if (bdTopoVersion.equals("old")) {
				if (!(((String) feat.getAttribute("NATURE")).equals("Bâtiment agricole") || ((String) feat.getAttribute("NATURE")).equals("Silo")
						|| ((String) feat.getAttribute("NATURE")).equals("Bâtiment industriel")
						|| ((Geometry) feat.getDefaultGeometry()).getArea() < 20.0)) {
					collecBuild.add(feat);
					lG.add(((Geometry) feat.getDefaultGeometry()).buffer(25));
				}
			} else {
				collecBuild.add(feat);
				lG.add(((Geometry) feat.getDefaultGeometry()).buffer(25));
			}
		});
		sfBuilder.add(Geom.unionGeom(lG));
		bufferBuild.add(sfBuilder.buildFeature(Attribute.makeUniqueId()));
		buildSDS.dispose();
		File[] result = { Collec.exportSFC(collecBuild.collection(), new File(tmpFile, "batiAllege.shp")),
				Collec.exportSFC(bufferBuild.collection(), new File(tmpFile, "batiAllegeBuffer.shp")) };
		return result;
	}

	/**
	 * Cut the cluster regarding the important limits limits
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws SchemaException
	 */
	public static File splitLimClus(File fLimit, File fCluster, File fBuild, File tmpFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException, SchemaException {

		ShapefileDataStore buildSDS = new ShapefileDataStore(fBuild.toURI().toURL());
		SimpleFeatureCollection buildSFC = buildSDS.getFeatureSource().getFeatures();
		CoordinateReferenceSystem crs = buildSFC.getSchema().getCoordinateReferenceSystem();

		// split
		File[] polyFiles = { fLimit, fCluster };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);
		String specs = "geom:Polygon:srid=2154";
		File out = new File(tmpFile, "polygon.shp");
		ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
		FileDataStore dataStore = factory.createDataStore(out.toURI().toURL());
		String featureTypeName = "Object";
		SimpleFeatureType featureType = DataUtilities.createType(featureTypeName, specs);
		dataStore.createSchema(featureType);
		String typeName = dataStore.getTypeNames()[0];
		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT);
		System.setProperty("org.geotools.referencing.forceXY", "true");
		System.out.println(Calendar.getInstance().getTime() + " write shapefile");

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

		for (Polygon p : polygons) {
			// filtre un peu
			ReferencedEnvelope env = new ReferencedEnvelope(p.getEnvelopeInternal(), crs);
			Filter filter = ff.bbox(ff.property(buildSFC.getSchema().getGeometryDescriptor().getLocalName()), env);
			int count = 0;
			try (SimpleFeatureIterator bIt = buildSFC.subCollection(filter).features()) {
				while (bIt.hasNext()) {
					if (p.contains((Geometry) bIt.next().getDefaultGeometry())) {
						count++;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} 
			if (count >= 5) {
				SimpleFeature feature = writer.next();
				feature.setAttributes(new Object[] { p });
				writer.write();
			}
		}
		System.out.println(Calendar.getInstance().getTime() + " done");
		writer.close();
		dataStore.dispose();
		return out;
	}
}
