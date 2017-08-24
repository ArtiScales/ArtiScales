package fr.ign.cogit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.herault.parcel.ParcelSplitting;
import fr.ign.random.Random;

public class SelectParcels {

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"),
				new File("/home/mcolomb/donnee/couplage/output/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0"), "25245", true, true);
	}

	File rootFile;
	File spatialConfiguration;
	File zoningsFile;
	File geoFile;
	File selecFiles;
	String zipCode;
	boolean notBuilt;
	boolean splitParcel;

	public SelectParcels(File rootfile, File spatialconfiguration, String zipcode, boolean notbuilt, boolean splitparcel)
			throws IOException, CQLException {
		rootFile = rootfile;
		spatialConfiguration = spatialconfiguration;
		zipCode = zipcode;
		notBuilt = notbuilt;
		splitParcel= splitparcel;
		zoningsFile = new File(rootFile, "pluZoning");
		geoFile = new File(rootFile, "donneeGeographiques");

		File zipFiles = new File(spatialconfiguration, zipcode);

		// name of selection folders
		String nBuilt = "built";
		if (notbuilt) {
			nBuilt = "notBuilt";
		}
		String nSplit = "notSplit";
		if (notbuilt) {
			nSplit = "Split";
		}
		selecFiles = new File(zipFiles, nBuilt+"-"+nSplit);
		selecFiles.mkdirs();
	}

	public static ArrayList<File> run(File rootfile, File testFile, String zipcode, boolean notbuilt, boolean splitparcel) throws Exception {
		SelectParcels sp = new SelectParcels(rootfile, testFile, zipcode, notbuilt,splitparcel);
		return sp.runBrownfield();
	}

	public ArrayList<File> runBrownfield() throws Exception {
		ArrayList<File> selectionList = new ArrayList<File>();
		SimpleFeatureCollection parcelU = selecParcelZonePLU("U");
		if (splitParcel) {
			parcelU = generateSplitedParcels(parcelU);
		}
		if (notBuilt) {
			parcelU = parcelNoBuilt(parcelU);
		}
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(parcelU);
		selectionList.add(putEvalInParcel(parcelInCell));
		return selectionList;
	}

	public ArrayList<File> runGreenfield(int missingHousingUnit) throws Exception {
		ArrayList<File> selectionList = new ArrayList<File>();
		SimpleFeatureCollection parcelAU = selecParcelZonePLU("AU");
		SimpleFeatureCollection spiltedParcels = generateSplitedParcels(parcelAU);
		if (notBuilt) {
			spiltedParcels = parcelNoBuilt(spiltedParcels);
		}
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(spiltedParcels);
		selectionList.add(putEvalInParcel(parcelInCell));
		return selectionList;
	}

	/**
	 * 
	 * @param typeZone
	 *            the code of the zone willed to be selected. In a french
	 *            context, it can either be ( A, N, U, AU) or one of its
	 *            subsection
	 * @param zipCode
	 *            the zipcode of the city to select parcels in
	 * @return a SimpleFeatureCollection which contains the parcels that are
	 *         included in the zoning area
	 * @throws IOException
	 * @throws CQLException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 * @throws TransformException
	 * @throws MismatchedDimensionException
	 */
	public SimpleFeatureCollection selecParcelZonePLU(String typeZone) throws IOException, CQLException,
			NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(getParcels().toURI().toURL());
		SimpleFeatureCollection parcelCollection = shpDSParcel.getFeatureSource().getFeatures();

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning().toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
		// CoordinateReferenceSystem sourceZoneCRS = CRS.decode("epsg:3947");
		// CoordinateReferenceSystem targetZoneCRS =
		// shpDSParcel.getSchema().getCoordinateReferenceSystem();
		//
		// MathTransform transform = CRS.findMathTransform(sourceZoneCRS,
		// targetZoneCRS);

		// verificaiton
		System.out.println("Pour la commune " + zipCode);
		System.out.println("on a " + featuresZones.size() + " zones");

		// creation of the filter to select only wanted type of zone in the PLU
		// zoning shp
		// for the 'AU' zones, a temporality attribute is usually pre-fixed, we
		// need to search after
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filter = ff.like(ff.property("LIBELLE"), (typeZone.contains("AU") ? "*" : "") + typeZone + "*");
		SimpleFeatureCollection featureZoneSelected = featuresZones.subCollection(filter);
		System.out.println("zones U au nombre de : " + featureZoneSelected.size());

		// Filter to select parcels that intersects the selected zonnig zone

		// Geometry union =
		// JTS.transform(unionSFC(featureZoneSelected),transform);
		Geometry union = unionSFC(featureZoneSelected);

		String geometryParcelPropertyName = shpDSParcel.getSchema().getGeometryDescriptor().getLocalName();
		// TODO opérateur géométrique pas bon
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(union));

		// deuxième méthode qui pourrait marcher?
		// String geometryZonePropertyName =
		// shpDSZone.getSchema().getGeometryDescriptor().getLocalName();
		// Filter in =
		// ff.(ff.property(geometryZonePropertyName),ff.literal(union));
		// SimpleFeatureCollection parcelSelected2 =
		// parcelCollection.subCollection(in);
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);
		// SimpleFeatureCollection parcelSelected2 =
		// parcelSelectedtemp.subCollection(touch);
		System.out.println("parcelSelected : " + parcelSelected.size());
		// System.out.println("parcelSelected deuxième méthode : " +
		// parcelSelected2.size());
		// File newParcelSelection = new File(selecFiles, "parcelIn" + typeZone
		// + ".shp");
		// exportSFC(parcelSelected, newParcelSelection);
		// File newParcelSelection2 = new File(selecFiles, "parcelIn" + typeZone
		// + ".shp");
		exportSFC(parcelSelected, new File("/home/mcolomb/tmp/test/parcel2SplitIn.shp"));

		return parcelSelected;
	}

	public SimpleFeatureCollection selecMultipleParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {

		// import of the MUP-City outputs
		SimpleFeatureCollection cellsCollection = getCellSFC();

		Geometry cellsUnion = unionSFC(cellsCollection);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(cellsUnion));
		SimpleFeatureCollection parcelSelected = parcelIn.subCollection(inter);

		File newParcelSelection = new File(selecFiles + "/parcelSelected.shp");
		System.out.println("parcelSelected with cells: " + parcelSelected.size());
		System.out.println("");
		exportSFC(parcelSelected, newParcelSelection);
		return parcelSelected;
	}
	
	public SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn) throws Exception{
		// splitting method option
		double maximalArea=3000; 
		double maximalWidth=50;
		double roadEpsilon=0.5; 
		double noise = 10;
		
		// putting the need of splitting into attribute
		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("SPLIT", Integer.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
		int i =0;

		for (Object obj : parcelIn.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			Object[] attr= { 0 };
			if(((Geometry) feat.getDefaultGeometry()).getArea()>maximalArea){
				attr[0]= 1;
			}
			
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			toSplit.add(feature);
			System.out.println("parcels to split"+toSplit.size());
			i=i+1;
		}
		//return splitParcels(toSplit,maximalArea, maximalWidth, roadEpsilon, noise );
		return null;
	}
	
	/**
	 * largely inspired from the simPLU.ParcelSplitting class but rewrote to work with geotools SimpleFeatureCollection objects
	 * @param toSplit
	 * @param maximalArea
	 * @param maximalWidth
	 * @param roadEpsilon
	 * @param noise
	 * @return
	 */
//	public SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit,double maximalArea, double maximalWidth, double roadEpsilon, double noise ){
//
//		String attNameToTransform = "SPLIT";
//
//		IFeatureCollection<IFeature> ifeatColl = new IFeatureCollection();
//		IFeatureCollection<IFeature> ifeatColl = ShapefileReader.read(toSplit.toString());
//		IFeatureCollection<IFeature> ifeatCollOut = new FT_FeatureCollection<>();
//
//		for (IFeature feat : ifeatColl) {
//
//			Object o = feat.getAttribute(attNameToTransform);
//			if (o == null) {
//				ifeatCollOut.add(feat);
//				continue;
//			}
//			if (Integer.parseInt(o.toString()) != 1) {
//				ifeatCollOut.add(feat);
//				continue;
//			}
//			IPolygon pol = (IPolygon) FromGeomToSurface.convertGeom(feat.getGeom()).get(0);
//
//			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, Random.random(),
//					roadEpsilon, noise);
//
//			IFeatureCollection<IFeature> featCollTemp = obb.decompParcel();
//
//			for (IFeature featNew : featCollTemp) {
//				IFeature featTemp = feat.cloneGeom();
//				featTemp.setGeom(featNew.getGeom());
//				ifeatCollOut.add(featTemp);
//			}
//		}
//		ShapefileWriter.write(ifeatCollOut, fileOut.toString());
//	
//	}
	
	public File putEvalInParcel(SimpleFeatureCollection parcelIn)
			throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {
		SimpleFeatureCollection cellsCollection = getCellSFC();
		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("eval", Float.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = cellsCollection.getSchema().getGeometryDescriptor().getLocalName();
		int i = 0;
		for (Object obj : parcelIn.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(feat.getDefaultGeometry()));
			SimpleFeatureCollection onlyCells = cellsCollection.subCollection(inter);
			Double bestEval = 0.0;
			// put the best cell evaluation into the parcel
			for (Object obje : onlyCells.toArray()) {
				SimpleFeature featu = (SimpleFeature) obje;
				if ((Double) featu.getAttribute("eval") > bestEval) {
					bestEval = (Double) featu.getAttribute("eval");
				}
			}
			Object[] attr = { bestEval };
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));

			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			newParcel.add(feature);
		}
		System.out.println("parcelEval : "+newParcel.size());
		File exportedFile = new File("/home/mcolomb/tmp/test/parcelWeval.shp");
		exportSFC(newParcel.collection(),exportedFile );
		return exportedFile;
	}

	public File selecOneParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {
		// TODO finir cette méthode : pourquoi est ce que la méthode dessous
		// requied d'etre statique alors qu'elle est utilisé dans la classe
		// SimPLUSimulator?
		// mettre le recouvrement des cellules dans un attribut et favoriser
		// selon le plus gros pourcentage?
		// spatialConfiguration =
		// SimPLUSimulator.snapDatas(spatialConfiguration, new
		// File(spatialConfiguration, "snap"));

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();

		SimpleFeatureCollection cellsCollection = getCellSFC();

		for (Object obj : cellsCollection.toArray()) {

			SimpleFeature feat = (SimpleFeature) obj;
			Filter inter = ff.intersects(ff.property(geometryParcelPropertyName),
					ff.literal(feat.getDefaultGeometry()));
			SimpleFeatureCollection parcelMultipleSelection = parcelIn.subCollection(inter);
			if (!parcelMultipleSelection.isEmpty()) {
				SimpleFeature bestFeature = null;
				for (Object parc : parcelMultipleSelection.toArray()) {
					SimpleFeature featParc = (SimpleFeature) parc;
					System.out.println(featParc.getAttribute("eval"));
				}
			}
		}

		return null;
	}

	public SimpleFeatureCollection getCellSFC() throws IOException {
		File shpCellIn = new File(spatialConfiguration, spatialConfiguration.getName() + "-vectorized.shp");
		ShapefileDataStore shpDSCells = new ShapefileDataStore(shpCellIn.toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
		return cellsCollection;
	}

	public SimpleFeatureCollection parcelNoBuilt(SimpleFeatureCollection parcelIn) throws IOException {
		// getBatiFiles
		ShapefileDataStore shpDSBati = new ShapefileDataStore(getBati().toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		Geometry batiUnion = unionSFC(batiCollection);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(batiUnion));
		// ExcludeFilter yo = inter.EXCLUDE;
		// SimpleFeatureCollection parcelSelected = parcelIn.subCollection(yo);

		SimpleFeatureCollection parcelInter = parcelIn.subCollection(inter);
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
		collection.addAll(parcelIn);
		for (Object tet : parcelInter.toArray()) {
			SimpleFeature feat = (SimpleFeature) tet;
			collection.remove(feat);
		}
		return collection;
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName) throws IOException {
		return exportSFC(toExport, fileName, toExport.getSchema());
	}

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft)
			throws IOException {

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileName.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);
		Transaction transaction = new DefaultTransaction("create");
		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		if (featureSource instanceof SimpleFeatureStore) {
			SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
			featureStore.setTransaction(transaction);
			try {
				featureStore.addFeatures(toExport);
				transaction.commit();
			} catch (Exception problem) {
				problem.printStackTrace();
				transaction.rollback();
			} finally {
				transaction.close();
			}
		} else {
			System.out.println(typeName + " does not support read/write access");
			System.exit(1);
		}
		return fileName;
	}

	public static Geometry unionSFC(SimpleFeatureCollection collection) throws IOException {
		GeometryFactory factory = new GeometryFactory();
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0]))
				.map(sf -> (Geometry) sf.getDefaultGeometry());
		GeometryCollection geometryCollection = (GeometryCollection) factory.buildGeometry(Arrays.asList(s.toArray()));
		return geometryCollection.union();
	}

	public File getParcels() throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcelle.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Parcel file not found");
	}

	public File getBati() throws FileNotFoundException {
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("batiment.shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Building file not found");
	}

	public boolean isAlreadyBuilt(Feature feature) throws IOException {
		boolean isContent = false;
		ShapefileDataStore bati_datastore = new ShapefileDataStore(getBati().toURI().toURL());
		SimpleFeatureCollection batiFeatures = bati_datastore.getFeatureSource().getFeatures();
		SimpleFeatureIterator iterator = batiFeatures.features();
		while (iterator.hasNext()) {
			SimpleFeature batiFeature = iterator.next();
			if (feature.getDefaultGeometryProperty().getBounds()
					.contains(batiFeature.getDefaultGeometryProperty().getBounds())) {
				isContent = true;
			}
		}
		return isContent;
	}

	public File getZoning() throws FileNotFoundException {
		for (File f : zoningsFile.listFiles()) {
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}
}
