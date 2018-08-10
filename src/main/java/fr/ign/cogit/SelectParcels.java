package fr.ign.cogit;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.SortByImpl;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

import fr.ign.cogit.GTFunctions.Rasters;
import fr.ign.cogit.GTFunctions.Vectors;
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
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.parameters.Parameters;
import fr.ign.random.Random;

public class SelectParcels {

	// public static void main(String[] args) throws Exception {
	// //run(new File("/home/mcolomb/donnee/couplage"), new File("/home/mcolomb/donnee/couplage/output/N5_St_Moy_ahpx_seed_42-eval_anal-20.0"), "25495", true, true);
	// File fileParcelle = new File("/home/mcolomb/doc_de_travail/PAU/PAU.shp");
	// SimpleFeatureCollection parcelU = new ShapefileDataStore(fileParcelle.toURI().toURL()).getFeatureSource().getFeatures();
	// SelectParcels salut = new SelectParcels(fileParcelle.getParentFile(),fileParcelle,"",false,true);
	// Vectors.exportSFC(salut.generateSplitedParcels(parcelU), new File("/home/mcolomb/doc_de_travail/PAU/parcelSplit.shp"));
	//
	// }

	File rootFile;
	File geoFile;
	File parcelFile;
	File spatialConfiguration;
	File zoningFile;
	String zipCode;
	String action;
	boolean splitParcel;
	Parameters p = null;

	public SelectParcels(File rootfile, File geoFile,File pluFile, File spatialconfiguration, String zipcode, boolean splitparcel) throws Exception {
		this(rootfile, geoFile,pluFile, spatialconfiguration, zipcode, splitparcel, null);
	}

	public SelectParcels(File rootfile, File geofile,File pluFile, File spatialconfiguration, String zipcode, boolean splitparcel, Parameters pa) throws Exception {
		// objet contenant les paramètres
		p = pa;
		// where everything's happends
		rootFile = rootfile;
		// where the geographic data are stored
		geoFile =geofile;
		// Liste des sorties de MupCity
		spatialConfiguration = spatialconfiguration;
		// Code postal de la commune étudié
		zipCode = zipcode;
		// Paramètre si l'on découpe les parcelles ou non
		splitParcel = splitparcel;
		parcelFile = GetFromGeom.getParcels(geoFile);
		zoningFile = GetFromGeom.getZoning(pluFile, zipCode);
	}

	// public static File run(File rootfile, File testFile, String zipcode, boolean notbuilt, boolean splitparcel) throws Exception {
	// SelectParcels sp = new SelectParcels(rootfile, testFile, zipcode, notbuilt, splitparcel);
	// return sp.runBrownfield();
	// }

	private File makeDirSelection(String action) {
		File fileSelection = new File("");

		// name of selection folders

		fileSelection = new File(spatialConfiguration, zipCode + "/" + action);
		fileSelection.mkdirs();
		return fileSelection;
	}

	/**
	 * Fill the already urbanised land (recognized with the U label from the field TypeZone) and constructed parcels TODO verifier que c'est bien les bons fichiers pour choper les
	 * PLUs
	 * 
	 * @return
	 * @throws Exception
	 */
	public File runBrownfieldConstructed() throws Exception {
		File simuFile = makeDirSelection("Ubuilt");
		File newParcelSelection = new File(simuFile + "/parcelSelected.shp");
		if (!newParcelSelection.exists()) {
			SimpleFeatureCollection parcelU = parcelBuilt(GetFromGeom.selecParcelZonePLU("U", zipCode, parcelFile, zoningFile));
			if (splitParcel) {
				parcelU = generateSplitedParcels(parcelU);
			}
			SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(parcelU);
			SimpleFeatureCollection collectOut = putEvalInParcel(parcelInCell);
			Vectors.exportSFC(collectOut, newParcelSelection);
		}
		return newParcelSelection;
	}

	/**
	 * Fill the already urbanised land (recognized with the U label from the field TypeZone TODO verifier que c'est bien les bons fichiers pour choper les PLUs
	 * 
	 * @return
	 * @throws Exception
	 */
	public File runBrownfield() throws Exception {
		File simuFile = makeDirSelection("UnotBuilt");
		SimpleFeatureCollection parcelU = parcelNoBuilt(GetFromGeom.selecParcelZonePLU("U", zipCode, parcelFile, zoningFile));
		if (splitParcel) {
			parcelU = generateSplitedParcels(parcelU);
		}
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(parcelU);
		SimpleFeatureCollection collectOut = putEvalInParcel(parcelInCell);
		File newParcelSelection = new File(simuFile + "/parcelSelected.shp");
		Vectors.exportSFC(collectOut, newParcelSelection);
		return newParcelSelection;
	}

	public File runGreenfieldSelected() throws Exception {
		File simuFile = makeDirSelection("AUnotBuilt");
		SimpleFeatureCollection parcelAU = GetFromGeom.selecParcelZonePLU("AU", zipCode, parcelFile, zoningFile);
		SimpleFeatureCollection spiltedParcels = generateSplitedParcels(parcelAU);
		SimpleFeatureCollection parcelInCell = selecMultipleParcelInCell(spiltedParcels);
		SimpleFeatureCollection collectOut = putEvalInParcel(parcelInCell);
		File newParcelSelection = new File(simuFile + "/parcelSelected-splited.shp");
		Vectors.exportSFC(collectOut, newParcelSelection);
		return newParcelSelection;
	}

	// public File runGreenfield() throws Exception {
	// // typeZone = "AU";
	// SimpleFeatureCollection parcelAU = GetFromGeom.selecParcelZonePLU("AU",zipCode,zoningsFile,zoningsFile);
	// SimpleFeatureCollection spiltedParcels = generateSplitedParcels(parcelAU);
	// if (notBuilt) {
	// spiltedParcels = parcelNoBuilt(spiltedParcels);
	// }
	// SimpleFeatureCollection collectOut = putEvalRasterInParcel(spiltedParcels);
	// File newParcelSelection = new File(selecFiles + "/parcelSelected-splited.shp");
	// Vectors.exportSFC(collectOut, newParcelSelection);
	// return newParcelSelection;
	// }

	public File runAll() throws Exception {
		File simuFile = makeDirSelection("ALLnotBuilt");
		SimpleFeatureCollection parcelAU = GetFromGeom.selecParcelZonePLU("AU", zipCode, parcelFile, zoningFile);
		
		return null;
	}

	public SimpleFeatureCollection selecMultipleParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {

		// import of the MUP-City outputs
		ShapefileDataStore shpDSCells = new ShapefileDataStore((new File(spatialConfiguration, spatialConfiguration.getName() + "-vectorized.shp")).toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();
		
		// 
		Geometry cellsUnion = Vectors.unionSFC(cellsCollection);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(cellsUnion));
		
		SimpleFeatureCollection parcelSelected = parcelIn.subCollection(inter);

		System.out.println("parcelSelected with cells: " + parcelSelected.size());
		shpDSCells.dispose();
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
		SimpleFeatureIterator parcelIt = parcelIn.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				Object[] attr = { 0, 0 };
				if (((Geometry) feat.getDefaultGeometry()).getArea() > maximalArea) {
					attr[0] = 1;
				}
				attr[1] = feat.getAttribute("NUMERO");
				sfBuilder.add(feat.getDefaultGeometry());
				SimpleFeature feature = sfBuilder.buildFeature(String.valueOf(i), attr);
				toSplit.add(feature);
				i = i + 1;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise);

	}

	/**
	 * largely inspired from the simPLU. ParcelSplitting class but rewrote to work with geotools SimpleFeatureCollection objects
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
		Vectors.exportSFC(toSplit, shpIn);
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
		// splitedSFC = selecParcelZonePLU(typeZone, splitedSFC, SSD);

		SSD.dispose();
		// return
		// GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut);
		return splitedSFC;
	}

	/**
	 * 
	 * @param parcelIn
	 * @return
	 * @throws ParseException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws IOException
	 */
	public SimpleFeatureCollection putEvalInParcel(SimpleFeatureCollection parcelIn) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {
		// TODO Prends des évaluations nulles pour les parcelles ou il n'y a pas
		// de cellules : aller chercher le raster d'évaluation et le prendre yo
		// (encore pas mal de dev pour le début de la semaine prochaine mon
		// coco)
		ShapefileDataStore shpDSCells = new ShapefileDataStore((new File(spatialConfiguration, spatialConfiguration.getName() + "-vectorized.shp")).toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();

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
		SimpleFeatureIterator parcelIt = parcelIn.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(feat.getDefaultGeometry()));
				SimpleFeatureCollection onlyCells = cellsCollection.subCollection(inter);
				Double bestEval = 0.0;
				// put the best cell evaluation into the parcel
				SimpleFeatureIterator onlyCellIt = onlyCells.features();
				try {
					while (onlyCellIt.hasNext()) {

						SimpleFeature featu = onlyCellIt.next();
						if ((Double) featu.getAttribute("eval") > bestEval) {
							bestEval = (Double) featu.getAttribute("eval");
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					onlyCellIt.close();
				}
				Object[] attr = { bestEval, feat.getAttribute("num") };
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
		shpDSCells.dispose();
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
		GridCoverage2D rasterEvalGrid = Rasters
				.importRaster(new File(rootFile, "depotConfigSpat/" + spatialConfiguration.getName().substring(0, spatialConfiguration.getName().length() - 14) + "eval-20.0.tif"));
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
		SimpleFeatureIterator parcelIt = parcelIn.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				com.vividsolutions.jts.geom.Point yo = ((Geometry) feat.getDefaultGeometry()).getCentroid();
				DirectPosition2D pt = new DirectPosition2D(yo.getX(), yo.getY());
				double[] yooo = (double[]) rasterEvalGrid.evaluate(pt);
				Object[] attr = { yooo[0] };
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore((new File(spatialConfiguration, spatialConfiguration.getName() + "-vectorized.shp")).toURI().toURL());
		SimpleFeatureCollection cellsCollection = shpDSCells.getFeatureSource().getFeatures();

		SimpleFeatureIterator cellIt = cellsCollection.features();
		try {
			while (cellIt.hasNext()) {
				SimpleFeature feat = cellIt.next();
				Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(feat.getDefaultGeometry()));
				SimpleFeatureCollection parcelMultipleSelection = parcelIn.subCollection(inter);
				if (!parcelMultipleSelection.isEmpty()) {
					SimpleFeature bestFeature = null;
					SimpleFeatureIterator multipleSelec = parcelMultipleSelection.features();
					try {
						while (multipleSelec.hasNext()) {
							SimpleFeature featParc = multipleSelec.next();
							System.out.println(featParc.getAttribute("eval"));
						}
					} catch (Exception problem) {
						problem.printStackTrace();
					} finally {
						multipleSelec.close();
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cellIt.close();
		}
		shpDSCells.dispose();
		return null;
	}

	/**
	 * Return a collection of non-constructed parcels.
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws IOException
	 */
	public SimpleFeatureCollection parcelNoBuilt(SimpleFeatureCollection parcelIn) throws IOException {
		// getBatiFiles
		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		Geometry batiUnion = Vectors.unionSFC(batiCollection);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(batiUnion));

		SimpleFeatureCollection parcelInter = parcelIn.subCollection(inter);
		DefaultFeatureCollection collection = new DefaultFeatureCollection();
		collection.addAll(parcelIn);
		SimpleFeatureIterator parcelInterIt = parcelInter.features();
		try {
			while (parcelInterIt.hasNext()) {
				SimpleFeature feat = parcelInterIt.next();
				collection.remove(feat);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelInterIt.close();
		}
		shpDSBati.dispose();
		return collection;
	}

	/**
	 * Return a collection of constructed parcels.
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws IOException
	 */
	public SimpleFeatureCollection parcelBuilt(SimpleFeatureCollection parcelIn) throws IOException {
		// getBatiFiles
		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		Geometry batiUnion = Vectors.unionSFC(batiCollection);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter inter = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(batiUnion));

		shpDSBati.dispose();
		return parcelIn.subCollection(inter);
	}

	public boolean isAlreadyBuilt(Feature feature) throws IOException {
		boolean isContent = false;
		ShapefileDataStore bati_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiFeatures = bati_datastore.getFeatureSource().getFeatures();
		SimpleFeatureIterator iterator = batiFeatures.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature batiFeature = iterator.next();
				if (feature.getDefaultGeometryProperty().getBounds().contains(batiFeature.getDefaultGeometryProperty().getBounds())) {
					isContent = true;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		bati_datastore.dispose();
		return isContent;
	}

}
