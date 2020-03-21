package fr.ign.cogit.annexeTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
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
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import fr.ign.cogit.FeaturePolygonizer;
import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.geoToolsFunctions.vectors.Geom;
import fr.ign.cogit.geoToolsFunctions.vectors.Shp;
import goal.ConsolidationDivision;

public class PAUDigger {
	// cut cluster polygons with limits

	public static void main(String[] args) throws Exception {
		// File rootFile = new File("/home/ubuntu/boulot/these/newZoning/");
		// File communitiesFile = new File(rootFile, "dataGeo/communities.shp");
		// ShapefileDataStore morphoSDS = new ShapefileDataStore(communitiesFile .toURI().toURL());
		// SimpleFeatureIterator it = morphoSDS.getFeatureSource().getFeatures().features();
		// DefaultFeatureCollection df = new DefaultFeatureCollection();
		// try {
		// while (it.hasNext()) {
		// SimpleFeature feat = it.next();
		// Geometry g = ((Geometry)feat.getDefaultGeometry());
		// PreciseConvexHull pch = new PreciseConvexHull(g);
		//
		// feat.setAttribute("the_geom", pch.getConvexHull());
		// df.add(feat);
		//
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// it.close();
		// }
		// Vectors.exportSFC(df.collection(), new File(rootFile, "newComm.shp"));

		createPAU();
	}

	public static File createPAU() throws Exception {

		File tmpFile = new File("/tmp/");
		File rootFile = new File("/home/ubuntu/boulot/these/newZoning/");

		File outFile = new File("/home/ubuntu/boulot/these/newZoning/final");

		File buildFile = new File(rootFile, "dataGeo/building.shp");
		File parcelFile = new File(rootFile, "dataGeo/parcel.shp");
		File communitiesFile = new File(rootFile, "dataGeo/communities.shp");
		File morphoLimFile = new File(rootFile, "dataGeo/PAU-morpholimEnv.shp");

		// zones NU
		List<File> nU = new ArrayList<File>();
		File NUroot = new File("/home/ubuntu/boulot/these/newZoning/PAU/");
		nU.add(new File(NUroot, "PPRI_Ognon_AU.shp"));
		nU.add(new File(NUroot, "PPRI_Loue_AU.shp"));
		nU.add(new File(NUroot, "PPRI_Doubs_AU.shp"));
		nU.add(new File(NUroot, "ZNIEFF1_AU.shp"));
		File fileNU = Shp.mergeVectFiles(nU, new File(tmpFile, "zonesNU.shp"), false);
		ShapefileDataStore nUSDS = new ShapefileDataStore(fileNU.toURI().toURL());
		SimpleFeatureCollection nUSFC = nUSDS.getFeatureSource().getFeatures();
		Geometry unionNU = Geom.unionSFC(nUSFC);
		Geom.exportGeom(unionNU, new File("/tmp/unionNU.shp"));

		// cluster of buildings
		// limits
		File roadFile = new File(rootFile, "dataGeo/roadPAU.shp");
		File riverFile = new File(rootFile, "dataGeo/river.shp");
		File railFile = new File(NUroot, "TRONCON_VOIE_FERREE.shp");

		File[] buildResult = prepareClusterBuild(buildFile, tmpFile);
		File buildAllegeCluster = buildResult[1];
		File buildAllege = buildResult[0];

		File limit = prepareLimit(roadFile, riverFile, railFile, tmpFile);
		File splitedCluster = splitLimClus(limit, buildAllegeCluster, buildAllege, tmpFile);
		ShapefileDataStore clusterSDS = new ShapefileDataStore(splitedCluster.toURI().toURL());
		SimpleFeatureCollection clusterSFC = clusterSDS.getFeatureSource().getFeatures();
		Geometry clusterUnion = Geom.unionSFC(clusterSFC);

		// morphology
		ShapefileDataStore morphoSDS = new ShapefileDataStore(morphoLimFile.toURI().toURL());
		SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
		Geometry morphoUnion = Geom.unionSFC(morphoSFC);

		// selection with geographical filters
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(clusterSFC.getSchema().getGeometryDescriptor().getLocalName());
		Filter filCluster = ff.intersects(pName, ff.literal(clusterUnion));
		Filter filMorpho = ff.intersects(pName, ff.literal(morphoUnion));
		Filter filNU = ff.not(ff.intersects(pName, ff.literal(unionNU)));

		// parcels
		ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcelSFC = parcelSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection parcelPreSelected = parcelSFC.subCollection(filCluster).subCollection(filMorpho);

		// TODO mark all (anf
		SimpleFeatureCollection parcelSplitted = ConsolidationDivision.consolidationDivision(parcelPreSelected, tmpFile, 2000.0, 500, 7, 99, 99);

		SimpleFeatureCollection out = makeEnvelopePAU(parcelSplitted.subCollection(filNU).subCollection(filMorpho).subCollection(filCluster),
				communitiesFile);

		nUSDS.dispose();
		clusterSDS.dispose();
		parcelSDS.dispose();
		morphoSDS.dispose();
		return Collec.exportSFC(out, new File(outFile, "zonePAU"));
	}

	public static SimpleFeatureBuilder pAUBuilder() throws NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("PAU");
		sfTypeBuilder.setCRS(sourceCRS);
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
			SimpleFeatureIterator it = communeSFC.features();
			String insee = "";
			try {
				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					if (((Geometry) feat.getDefaultGeometry()).contains(geom)) {
						insee = (String) feat.getAttribute("DEPCOM");
						break;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				it.close();
			}
			// TODO some insee are set to null
			sfBuilder.set("INSEE", insee);
			df.add(sfBuilder.buildFeature(null));
		}
		communeSDS.dispose();
		return df.collection();
	}

	private static File prepareLimit(File roadFile, File riverFile, File railFile, File tmpFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

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
	public static File[] prepareClusterBuild(File fBuild, File tmpFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore buildSDS = new ShapefileDataStore(fBuild.toURI().toURL());
		SimpleFeatureCollection buildSFC = buildSDS.getFeatureSource().getFeatures();
		SimpleFeatureIterator bIt = buildSFC.features();

		// if the building is from an old version of the BD Topo and we need to sort the industrial and farmer buildings
		boolean isOld = false;
		for (AttributeDescriptor attr : buildSDS.getSchema().getAttributeDescriptors()) {
			if (attr.getLocalName().equals("NATURE")) {
				isOld = true;
			}
		}

		DefaultFeatureCollection collecBuild = new DefaultFeatureCollection();
		DefaultFeatureCollection bufferBuild = new DefaultFeatureCollection();

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("buildBuffer");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		List<Geometry> lG = new ArrayList<Geometry>();
		int i = 0;
		try {
			while (bIt.hasNext()) {
				SimpleFeature build = bIt.next();
				// if the building is from an old version of the BD Topo and we need to sort the industrial and farmer buildings
				if (isOld) {
					if (!(((String) build.getAttribute("NATURE")).equals("Bâtiment agricole")
							|| ((String) build.getAttribute("NATURE")).equals("Silo")
							|| ((String) build.getAttribute("NATURE")).equals("Bâtiment industriel")
							|| ((Geometry) build.getDefaultGeometry()).getArea() < 20.0)) {
						collecBuild.add(build);
						lG.add(((Geometry) build.getDefaultGeometry()).buffer(25));
						i++;
					}
				} else {
					collecBuild.add(build);
					lG.add(((Geometry) build.getDefaultGeometry()).buffer(25));
					i++;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			bIt.close();
		}

		sfBuilder.add(Geom.unionGeom(lG));
		bufferBuild.add(sfBuilder.buildFeature(String.valueOf(i)));

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
			SimpleFeatureIterator bIt = buildSFC.subCollection(filter).features();

			int count = 0;
			try {
				while (bIt.hasNext()) {
					if (p.contains((Geometry) bIt.next().getDefaultGeometry())) {
						count++;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				bIt.close();
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
