package fr.ign.cogit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.geotools.coverage.grid.GridCoverage2D;
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
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.SortByImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
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
import fr.ign.cogit.geoxygene.api.feature.type.GF_AttributeType;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.parameters.Parameters;
import fr.ign.random.Random;

public class SelectParcels {

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"), new File("/home/mcolomb/donnee/couplage/output/N5_St_Moy_ahpx_seed_42-eval_anal-20.0"), "25495", true, true);
	}

	File rootFile;
	File spatialConfiguration;
	File zoningsFile;
	File geoFile;
	File selecFiles;
	String zipCode;
	String typeZone;
	boolean notBuilt;
	boolean splitParcel;
	Parameters p = null;

	public SelectParcels(File rootfile, File spatialconfiguration, String zipcode, boolean notbuilt, boolean splitparcel) throws Exception {
		this(rootfile, spatialconfiguration, zipcode, notbuilt, splitparcel, null);
	}

	public SelectParcels(File rootfile, File spatialconfiguration, String zipcode, boolean notbuilt, boolean splitparcel, Parameters pa) throws Exception {
		p = pa;
		rootFile = rootfile;
		spatialConfiguration = spatialconfiguration;
		zipCode = zipcode;
		notBuilt = notbuilt;
		splitParcel = splitparcel;
		zoningsFile = new File(rootFile, "pluZoning");
		geoFile = new File(rootFile, "donneeGeographiques");

		File zipFiles = new File(spatialconfiguration, zipcode);

		// name of selection folders
		String nBuilt = "built";
		if (notbuilt) {
			nBuilt = "notBuilt";
		}
		String nSplit = "notSplit";
		if (splitParcel) {
			nSplit = "Split";
		}
		selecFiles = new File(zipFiles, nBuilt + "-" + nSplit);
		selecFiles.mkdirs();
	}

	public static File run(File rootfile, File testFile, String zipcode, boolean notbuilt, boolean splitparcel) throws Exception {
		SelectParcels sp = new SelectParcels(rootfile, testFile, zipcode, notbuilt, splitparcel);
		return sp.runBrownfield();
	}

	// fill the already urbanised lands
	public File runBrownfield() throws Exception {
		typeZone = "U";
		SimpleFeatureCollection parcelU = selecParcelZonePLU();
		if (splitParcel) {
			parcelU = generateSplitedParcels(parcelU);
		}
		if (notBuilt) {
			parcelU = parcelNoBuilt(parcelU);
		}
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(parcelU);
		SimpleFeatureCollection collectOut = putEvalInParcel(parcelInCell);
		File newParcelSelection = new File(selecFiles + "/parcelSelected.shp");
		exportSFC(collectOut, newParcelSelection);
		return newParcelSelection;
	}

	public File runGreenfieldSelected() throws Exception {
		typeZone = "AU";
		SimpleFeatureCollection parcelAU = selecParcelZonePLU();
		SimpleFeatureCollection spiltedParcels = generateSplitedParcels(parcelAU);
		if (notBuilt) {
			spiltedParcels = parcelNoBuilt(spiltedParcels);
		}
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(spiltedParcels);
		SimpleFeatureCollection collectOut = putEvalInParcel(parcelInCell);
		File newParcelSelection = new File(selecFiles + "/parcelSelected-splited.shp");
		exportSFC(collectOut, newParcelSelection);
		return newParcelSelection;
	}

	public File runGreenfield() throws Exception {
		typeZone = "AU";
		SimpleFeatureCollection parcelAU = selecParcelZonePLU();
		SimpleFeatureCollection spiltedParcels = generateSplitedParcels(parcelAU);
		if (notBuilt) {
			spiltedParcels = parcelNoBuilt(spiltedParcels);
		}
		SimpleFeatureCollection collectOut = putEvalRasterInParcel(spiltedParcels);
		File newParcelSelection = new File(selecFiles + "/parcelSelected-splited.shp");
		exportSFC(collectOut, newParcelSelection);
		return newParcelSelection;
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

	public SimpleFeatureCollection selecParcelZonePLU() throws Exception {
		// import of the parcel file
		ShapefileDataStore shpDSParcel = new ShapefileDataStore(getParcels().toURI().toURL());
		SimpleFeatureCollection parcelCollection = shpDSParcel.getFeatureSource().getFeatures();
		return selecParcelZonePLU(typeZone, parcelCollection, shpDSParcel);
	}

	public SimpleFeatureCollection selecParcelZonePLU(String typeZone, SimpleFeatureCollection parcelCollection, ShapefileDataStore shpDSParcel)
			throws IOException, CQLException, NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(getZoning().toURI().toURL());
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

		Geometry union = unionSFC(featureZoneSelected);

		String geometryParcelPropertyName = shpDSParcel.getSchema().getGeometryDescriptor().getLocalName();
		// TODO opérateur géométrique pas terrible, mais rattrapé par le
		// découpage de SimPLU
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(union));
		SimpleFeatureCollection parcelSelected = parcelCollection.subCollection(inter);

		System.out.println("parcelSelected : " + parcelSelected.size());
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

		System.out.println("parcelSelected with cells: " + parcelSelected.size());
		return parcelSelected;
	}

	public SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn) throws Exception {

		// splitting method option

		double roadEpsilon = 0.5;
		double noise = 10;
		double maximalArea = 1000;
		double maximalWidth = 50;
		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// putting the need of splitting into attribute
		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("SPLIT", Integer.class);
		sfTypeBuilder.add("num", Integer.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
		int i = 0;

		for (Object obj : parcelIn.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			Object[] attr = { 0, 0 };
			if (((Geometry) feat.getDefaultGeometry()).getArea() > maximalArea) {
				attr[0] = 1;
			}
			attr[1] = feat.getAttribute("NUMERO");
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			toSplit.add(feature);
			i = i + 1;
		}
		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise);

	}

	/**
	 * largely inspired from the simPLU.ParcelSplitting class but rewrote to work with geotools SimpleFeatureCollection objects
	 * 
	 * @param toSplit
	 * @param maximalArea
	 * @param maximalWidth
	 * @param roadEpsilon
	 * @param noise
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise) throws Exception {
		// TODO classe po bô du tout: faire une vraie conversion entre les types
		// geotools et geox (passer par des shp a été le seul moyen que j'ai
		// trouvé pour que ça fonctionne)
		String attNameToTransform = "SPLIT";

		// IFeatureCollection<?> ifeatColl =
		// GeOxygeneGeoToolsTypes.convert2IFeatureCollection(toSplit);

		File shpIn = new File(rootFile, "temp-In.shp");
		exportSFC(toSplit, shpIn);
		IFeatureCollection<?> ifeatColl = ShapefileReader.read(shpIn.toString());
		IFeatureCollection<IFeature> ifeatCollOut = new FT_FeatureCollection<IFeature>();
		for (IFeature feat : ifeatColl) {
			Object o = feat.getAttribute(attNameToTransform);
			if (o == null) {
				ifeatCollOut.add(feat);
				continue;
			}
			if (Integer.parseInt(o.toString()) != 1) {
				ifeatCollOut.add(feat);
				continue;
			}
			IPolygon pol = (IPolygon) FromGeomToSurface.convertGeom(feat.getGeom()).get(0);
			int num = (int) feat.getAttribute("num");
			int numParcelle = 1;
			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, Random.random(), roadEpsilon, noise);
			// TODO erreures récurentes sur le split
			try {
				IFeatureCollection<IFeature> featCollDecomp = obb.decompParcel();
				// recup du GF_AttributeType des numeros
				GF_AttributeType attribute = null;
				for (GF_AttributeType att : ifeatColl.getFeatureType().getFeatureAttributes()) {
					if (att.toString().contains("num")) {
						attribute = att;
					}
				}
				for (IFeature featDecomp : featCollDecomp) {
					// MAJ du numéro de la parcelle
					int newNum = Integer.parseInt(String.valueOf(num) + "000" + String.valueOf(numParcelle));
					numParcelle = numParcelle + 1;
					IFeature newFeat = new DefaultFeature(featDecomp.getGeom());
					AttributeManager.addAttribute(newFeat, "num", newNum, "Integer");
					ifeatCollOut.add(newFeat);
					System.out.println("num" + newFeat.getAttribute(attribute));
				}
			} catch (NullPointerException n) {
				System.out.println("erreur sur le split pour la parcelle " + num);
				IFeature featTemp = feat.cloneGeom();
				ifeatCollOut.add(featTemp);
			}
		}
		for (IFeature ft : ifeatCollOut) {
			System.out.println(ft.getAttribute("num"));
		}

		File fileOut = new File(rootFile, "tmp.shp");
		ShapefileWriter.write(ifeatCollOut, fileOut.toString());

		// nouvelle sélection en fonction de la zone pour patir à la faible
		// qualité de la sélection spatiale quand les polygones touchent les
		// zones (oui je sais, pas bô encore une fois..)

		ShapefileDataStore SSD = new ShapefileDataStore(fileOut.toURI().toURL());
		SimpleFeatureCollection splitedSFC = SSD.getFeatureSource().getFeatures();
		splitedSFC = selecParcelZonePLU(typeZone, splitedSFC, SSD);

		// return
		// GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut);
		return splitedSFC;
	}

	public SimpleFeatureCollection putEvalInParcel(SimpleFeatureCollection parcelIn) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {
		// TODO Prends des évaluations nulles pour les parcelles ou il n'y a pas
		// de cellules : aller chercher le raster d'évaluation et le prendre yo
		// (encore pas mal de dev pour le début de la semaine prochaine mon
		// coco)
		SimpleFeatureCollection cellsCollection = getCellSFC();
		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("eval", Float.class);
		sfTypeBuilder.add("num", Integer.class);

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
			Object[] attr = { bestEval, feat.getAttribute("num") };
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));

			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			newParcel.add(feature);
			i = i + 1;
		}

		// sort collection with evaluation
		PropertyName pN = ff.property("eval");
		SortByImpl sbt = new SortByImpl(pN, org.opengis.filter.sort.SortOrder.DESCENDING);
		SimpleFeatureCollection collectOut = new SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		return collectOut;
	}

	public SimpleFeatureCollection putEvalRasterInParcel(SimpleFeatureCollection parcelIn) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {
		// TODO Prends des évaluations nulles pour les parcelles ou il n'y a pas
		// de cellules : aller chercher le raster d'évaluation et le prendre yo
		// (encore pas mal de dev pour le début de la semaine prochaine mon
		// coco)
		System.out
				.println("est ce qu c'est bon?? : depotConfigSpat/" + spatialConfiguration.getName().substring(0, spatialConfiguration.getName().length() - 14) + "eval-20.0.tif");
		GridCoverage2D rasterEvalGrid = SelecMUPOutput
				.importRaster(new File(rootFile, "depotConfigSpat/" + spatialConfiguration.getName().substring(0, spatialConfiguration.getName().length() - 14) + "eval-20.0.tif"));
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

		int i = 0;
		for (Object obj : parcelIn.toArray()) {
			SimpleFeature feat = (SimpleFeature) obj;
			com.vividsolutions.jts.geom.Point yo = ((Geometry) feat.getDefaultGeometry()).getCentroid();
			DirectPosition2D pt = new DirectPosition2D(yo.getX(), yo.getY());
			double[] yooo = (double[]) rasterEvalGrid.evaluate(pt);
			Object[] attr = { yooo[0] };
			sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
			SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
			newParcel.add(feature);
			i = i + 1;
		}

		// sort collection with evaluation
		PropertyName pN = ff.property("eval");
		SortByImpl sbt = new SortByImpl(pN, org.opengis.filter.sort.SortOrder.DESCENDING);
		SimpleFeatureCollection collectOut = new SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		return collectOut;
	}

	public File selecOneParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {
		// TODO finir cette méthode : mais sert elle à quelque chose?
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
			Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(feat.getDefaultGeometry()));
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

	/**
	 * get the MUP-City vectorized output
	 * 
	 * @return the MUP-City vectorized output
	 * @throws IOException
	 */
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

	public static File exportSFC(SimpleFeatureCollection toExport, File fileName, SimpleFeatureType ft) throws IOException {

		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

		Map<String, Serializable> params = new HashMap<>();
		params.put("url", fileName.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);

		ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
		newDataStore.createSchema(ft);

		Transaction transaction = new DefaultTransaction("create");

		String typeName = newDataStore.getTypeNames()[0];
		SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

		SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
		System.out.println("SHAPE:" + SHAPE_TYPE);

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
		Stream<Geometry> s = Arrays.stream(collection.toArray(new SimpleFeature[0])).map(sf -> (Geometry) sf.getDefaultGeometry());
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
			if (feature.getDefaultGeometryProperty().getBounds().contains(batiFeature.getDefaultGeometryProperty().getBounds())) {
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
