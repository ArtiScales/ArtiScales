package fr.ign.cogit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.outputs.XmlGen;
import fr.ign.cogit.util.DataPreparator;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.SimuTool;
import fr.ign.cogit.util.VectorFct;
import fr.ign.parameters.Parameters;

public class SelectParcels {

	// public static void main(String[] args) throws Exception {
	// //run(new File("/home/mcolomb/donnee/couplage"), new
	// File("/home/mcolomb/donnee/couplage/output/N5_St_Moy_ahpx_seed_42-eval_anal-20.0"),
	// "25495", true, true);
	// File fileParcelle = new File("/home/mcolomb/doc_de_travail/PAU/PAU.shp");
	// SimpleFeatureCollection parcelU = new
	// ShapefileDataStore(fileParcelle.toURI().toURL()).getFeatureSource().getFeatures();
	// SelectParcels salut = new
	// SelectParcels(fileParcelle.getParentFile(),fileParcelle,"",false,true);
	// Vectors.exportSFC(salut.generateSplitedParcels(parcelU), new
	// File("/home/mcolomb/doc_de_travail/PAU/parcelSplit.shp"));
	//
	// }

	File rootFile;
	File tmpFile;
	File geoFile;
	File regulFile;
	File parcelFile;
	File zoningFile;
	List<List<File>> spatialConfigurations;
	File spatialConf;
	String action;
	List<Parameters> lP = new ArrayList<Parameters>();

	// cache parcel intersecting U
	SimpleFeatureCollection parcelU = null;
	boolean parcelUFilled = false;
	// cache parcel intersecting AU
	SimpleFeatureCollection parcelAU = null;
	boolean parcelAUFilled = false;
	// result parameters
	int nbParcels;
	float moyEval;

	public SelectParcels(File rootfile, List<List<File>> spatialconfigurations, List<Parameters> lp) throws Exception {
		// objet contenant les paramètres
		lP = lp;
		// where everything's happends
		rootFile = rootfile;
		// where the geographic data are stored
		geoFile = new File(rootFile, "dataGeo");

		// where the regulation data are stored
		regulFile = new File(rootFile, "dataRegul");

		// where temporary stuff are stored
		tmpFile = new File(rootFile, "tmp");
		tmpFile.mkdir();

		// Liste des sorties de MupCity
		spatialConfigurations = spatialconfigurations;
		// Paramètre si l'on découpe les parcelles ou non
		zoningFile = GetFromGeom.getZoning(new File(rootFile, "dataRegul"));

	}

	// public static File run(File rootfile, File testFile, String zipcode, boolean
	// notbuilt, boolean splitparcel) throws Exception {
	// SelectParcels sp = new SelectParcels(rootfile, testFile, zipcode, notbuilt,
	// splitparcel);
	// return sp.runBrownfield();
	// }

	public List<List<File>> run() throws Exception {

		List<List<File>> selectionFile = new ArrayList<List<File>>();

		for (List<File> scenar : spatialConfigurations) {

			List<File> listScenar = new ArrayList<File>();
			String scenarName = scenar.get(0).getName().split("-")[0];
			Parameters p = SimuTool.getParamFile(lP, scenarName);
			List<String> listeAction = selectionType(p);
			for (File varianteSpatialConf : scenar) {
				spatialConf = varianteSpatialConf;
				parcelFile = GetFromGeom.getParcels(geoFile, regulFile, tmpFile);
				ShapefileDataStore shpDSparcel = new ShapefileDataStore((parcelFile).toURI().toURL());
				SimpleFeatureCollection parcelCollection = shpDSparcel.getFeatureSource().getFeatures();
				Vectors.exportSFC(parcelCollection, new File(tmpFile, "parcelGenExport.shp"));
				// Split parcels
				if (p.getString("splitParcel").equals("true")) {
					parcelCollection = VectorFct.generateSplitedParcels(GetFromGeom.selecParcelZonePLUmergeAU(parcelCollection, zoningFile, p), p);
					parcelCollection = VectorFct.generateSplitedParcels(parcelCollection, p);
				}
				if (p.getString("splitParcel").equals("AU")) {
					parcelCollection = VectorFct.generateSplitedParcels(GetFromGeom.selecParcelZonePLUmergeAU(parcelCollection, zoningFile, p), p);
				}
				for (String action : listeAction) {
					System.out.println("---=+Pour le remplissage " + action + "+=---");
					switch (action) {
					case "Ubuilt":
						parcelCollection = runBrownfieldConstructed(parcelCollection);
						break;
					case "UnotBuilt":
						parcelCollection = runBrownfieldUnconstructed(parcelCollection);
						break;
					case "AUnotBuilt":
						parcelCollection = runGreenfieldSelected(parcelCollection);
						break;
					case "ALLnotBuilt":
						parcelCollection = runNaturalLand(parcelCollection);
						break;
					case "justEval":
						parcelCollection = runAll(parcelCollection);
						break;
					case "random":
						parcelCollection = random(parcelCollection, 10000);
						break;
					case "JustZoning":
						parcelCollection = runZoningAllowed(parcelCollection);
						break;
					}
				}

				File packFile = new File(rootFile, "ParcelSelectionFile/" + scenarName + "/" + varianteSpatialConf.getParentFile().getName() + "/");

				packFile.mkdirs();
				File parcelSelectedFile = Vectors.exportSFC(parcelCollection, new File(packFile, "parcelGenExport.shp"));
				// File parcelSelectedFile = new File("/home/mcolomb/informatique/ArtiScales/tmp/parcelExport.shp");

				separateToDifferentPack(parcelSelectedFile, packFile);
				listScenar.add(packFile);

				shpDSparcel.dispose();
			}
			selectionFile.add(listScenar);
		}
		SimuTool.deleteDirectoryStream(tmpFile.toPath());
		return selectionFile;

	}

	/**
	 * Know which selection method to use determined by the param file
	 * 
	 * @return a list with all the different selections
	 * 
	 * @return
	 */
	private static List<String> selectionType(Parameters p) {
		List<String> routine = new ArrayList<String>();
		if (p.getBoolean("JustEval")) {
			routine.add("justEval");
		} else if (p.getBoolean("JustZoning")) {
			routine.add("JustZoning");
		} else {
			if (p.getBoolean("Ubuilt")) {
				routine.add("Ubuilt");
			}
			if (p.getBoolean("UnotBuilt")) {
				routine.add("UnotBuilt");
			}
			if (p.getBoolean("AUnotBuilt")) {
				routine.add("AUnotBuilt");
			}
			if (p.getBoolean("ALLnotBuilt")) {
				routine.add("ALLnotBuilt");
			}
		}
		return routine;
	}

	/**
	 * create a folder form the type of action
	 * 
	 * @param action
	 * @return
	 * @throws IOException
	 */
	public void writeXMLResult(XmlGen xmlFile) throws IOException {

		xmlFile.addLine("nbParcels", String.valueOf(nbParcels));
		xmlFile.addLine("MoyenneEvalParcelles", String.valueOf(moyEval));

	}

	/**
	 * calculate the average evaluation of the parcels
	 * 
	 * @param parc
	 * @return
	 */
	private void moyenneEval(SimpleFeatureCollection parc) {
		float sommeEval = 0;
		int i = 0;
		SimpleFeatureIterator parcelIt = parc.features();
		try {
			while (parcelIt.hasNext()) {
				sommeEval = sommeEval + ((float) parcelIt.next().getAttribute("eval"));
				i = i + 1;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		moyEval = sommeEval / i;

	}

	/**
	 * create a folder form the type of action
	 * 
	 * @param action
	 * @return
	 */
	private void makeDirSelection(String selectionType) {
		for (List<File> scenar : spatialConfigurations) {
			for (File variante : scenar) {
				File fileSelection = new File(variante, selectionType);
				fileSelection.mkdirs();
			}
		}
	}

	/**
	 * Fill the already urbanised land (recognized with the U label from the field TypeZone) and constructed parcels
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runBrownfieldConstructed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if ((boolean) parcel.getAttribute("IsBuild")) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", getEvalInParcel(parcel));
						} else {
							parcel.setAttribute("DoWeSimul", "false");
						}
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result.collection();
	}

	/**
	 * Fill the not-constructed parcel within the urbanised land (recognized with the U label from the field TypeZone
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runBrownfieldUnconstructed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if (!(boolean) parcel.getAttribute("IsBuild")) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", getEvalInParcel(parcel));
						} else {
							parcel.setAttribute("DoWeSimul", "false");
						}
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	public SimpleFeatureCollection runGreenfieldSelected(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("AU")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", getEvalInParcel(parcel));
					} else {
						parcel.setAttribute("DoWeSimul", "false");

					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	/**
	 * Selection of only the natural lands that intersects the MUP-City's outputs. Selection could either be all merged and cut to produce a realistic parcel land or not. Selection
	 * is ordered by MUP-City's evaluation.
	 * 
	 * TODO quelle règles de SimPLU modèlise-t-on?
	 * 
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runNaturalLand(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("NC")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", getEvalInParcel(parcel));
					} else {
						parcel.setAttribute("DoWeSimul", "false");
					}

				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	/**
	 * Selection of only the natural lands that intersects the MUP-City's outputs. Selection could either be all merged and cut to produce a realistic parcel land or not. Selection
	 * is ordered by MUP-City's evaluation.
	 * 
	 * TODO quelle règles de SimPLU modèlise-t-on lorque rien n'est définit?
	 * 
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runAll(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();

				if (isParcelInCell(parcel, cellsSFS)) {
					parcel.setAttribute("DoWeSimul", "true");
					parcel.setAttribute("eval", getEvalInParcel(parcel));
				} else {
					parcel.setAttribute("DoWeSimul", "false");
				}

				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result.collection();
	}

	public SimpleFeatureCollection random(SimpleFeatureCollection parcelSFC, int nb) throws Exception {
		return null;
		// TODO develop such?
	}

	/**
	 * get all the parcel that are on a construtible zone without any orders
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runZoningAllowed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U") || (boolean) parcel.getAttribute("AU")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", getEvalInParcel(parcel));
					} else {
						parcel.setAttribute("DoWeSimul", "false");
					}
				}
				result.add(parcel);
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		shpDSCells.dispose();
		return result;
	}

	public boolean isParcelInCell(SimpleFeature parcelIn, SimpleFeatureCollection cellsCollection) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		// import of the cells of MUP-City outputs
		SimpleFeatureIterator cellsCollectionIt = cellsCollection.features();

		try {
			while (cellsCollectionIt.hasNext()) {
				SimpleFeature cell = cellsCollectionIt.next();
				if (((Geometry) cell.getDefaultGeometry()).intersects(((Geometry) parcelIn.getDefaultGeometry()))) {
					return true;
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cellsCollectionIt.close();
		}
		return false;

	}

	// public boolean isParcelInZone(SimpleFeature parcelIn, String typeZone) throws Exception {
	//
	// // import of the MUP-City outputs (cache to be a lil more fast)
	// SimpleFeatureCollection zoneCollection = null;
	// if (!(parcelUFilled && parcelAUFilled)) {
	// zoneCollection = GetFromGeom.selecParcelZoning(typeZone, parcelFile, zoningFile);
	// if (typeZone.equals("U")) {
	// parcelU = zoneCollection;
	// parcelUFilled = true;
	// } else if (typeZone.equals("AU")) {
	// parcelAU = zoneCollection;
	// parcelAUFilled = true;
	// }
	// } else {
	// if (typeZone.equals("U")) {
	// zoneCollection = parcelU;
	// } else if (typeZone.equals("AU")) {
	// zoneCollection = parcelAU;
	// } else {
	// System.out.println("problem");
	// }
	// }
	//
	// if (zoneCollection.contains(parcelIn)) {
	//
	// return true;
	//
	// } else {
	// return false;
	// }
	//
	// }

	public SimpleFeatureCollection mergeParcels(SimpleFeatureCollection parcelIn) throws NoSuchAuthorityCodeException, FactoryException, IOException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");

		sfTypeBuilder.setName("mergeAUParcels");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		sfBuilder.add(Vectors.unionSFC(parcelIn));

		DefaultFeatureCollection mergedParcels = new DefaultFeatureCollection();
		mergedParcels.add(sfBuilder.buildFeature("0"));
		return mergedParcels.collection();

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
	public Double getEvalInParcel(SimpleFeature parcel) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {

		ShapefileDataStore cellsSDS = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsCollection = cellsSDS.getFeatureSource().getFeatures();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = cellsCollection.getSchema().getGeometryDescriptor().getLocalName();
		//
		// int i = 0;
		// SimpleFeatureIterator parcelIt = parcelIn.features();
		// try {
		// while (parcelIt.hasNext()) {
		// SimpleFeature feat = parcelIt.next();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(parcel.getDefaultGeometry()));
		SimpleFeatureCollection onlyCells = cellsCollection.subCollection(inter);
		Double bestEval = Double.NEGATIVE_INFINITY;

		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			SimpleFeatureIterator onlyCellIt = onlyCells.features();
			try {
				while (onlyCellIt.hasNext()) {
					SimpleFeature multiCell = onlyCellIt.next();
					bestEval = Math.max(bestEval, (Double) multiCell.getAttribute("eval"));
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				onlyCellIt.close();
			}
		}

		// si jamais le nom est déjà généré

		cellsSDS.dispose();
		// sort collection with evaluation
		// PropertyName pN = ff.property("eval");
		// SortByImpl sbt = new SortByImpl(pN, org.opengis.filter.sort.SortOrder.DESCENDING);
		// SimpleFeatureCollection collectOut = new SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		//
		// moyenneEval(collectOut);

		return bestEval;
	}

	public File selecOneParcelInCell(SimpleFeatureCollection parcelIn) throws IOException {
		// TODO finir cette méthode : mais sert elle à quelque chose?
		// mettre le recouvrement des cellules dans un attribut et favoriser
		// selon le plus gros pourcentage?

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryParcelPropertyName = parcelIn.getSchema().getGeometryDescriptor().getLocalName();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
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
	 * Return a collection of constructed parcels.
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public boolean isParcelBuilt(SimpleFeature parcelIn) throws Exception {

		// couche de batiment
		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();

		// on snap la couche de batiment et la met dans une géométrie unique
		Geometry batiUnion = Vectors.unionSFC(Vectors.snapDatas(batiCollection, zoningFile));

		shpDSBati.dispose();

		if (((Geometry) parcelIn.getDefaultGeometry()).contains(batiUnion)) {
			return true;
		}
		return false;
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
					break;
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

	public void separateToDifferentPack(File parcelCollection, File fileOut) throws Exception {

		DataPreparator.createPackages(parcelCollection, tmpFile, fileOut);

		for (File pack : fileOut.listFiles()) {
			if (pack.isDirectory()) {
				File fBBox = new File(pack, "bbox.shp");

				if (!fBBox.exists()) {
					System.err.print("bbox of pack not generated");
				}

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than non extitant
				createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "building.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox), new File(snapPack, "road.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zoning.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescPonct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescLin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescSurf.shp"));
				prescSurf_datastore.dispose();

				// selection of the right lives from the predicate file
				// CSV tools
				CSVReader predicate = new CSVReader(new FileReader(new File(rootFile, "dataRegul/predicate.csv")));
				// CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")),",","","");

				CSVWriter newPredicate = new CSVWriter(new FileWriter(new File(pack, "snapPredicate.csv")), ',', '\0');

				// get insee numbers needed
				List<String> insee = new ArrayList<String>();
				ShapefileDataStore sds = new ShapefileDataStore((new File(pack, "parcelle.shp")).toURI().toURL());
				SimpleFeatureIterator itParc = sds.getFeatureSource().getFeatures().features();
				try {
					while (itParc.hasNext()) {
						String inseeTemp = (String) itParc.next().getAttribute("INSEE");
						if (!insee.contains(inseeTemp)) {
							insee.add(inseeTemp);
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itParc.close();
				}
				sds.dispose();
				newPredicate.writeNext(predicate.readNext());
				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee)
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
				}
				predicate.close();
				newPredicate.close();
			}
		}
	}

	/**
	 * create empty shapefile (better than non existent shapefile)
	 * 
	 * @param f
	 * @throws IOException
	 * @throws FactoryException
	 * @throws NoSuchAuthorityCodeException
	 */

	public static void createPackOfEmptyShp(File f) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		DefaultFeatureCollection vide = new DefaultFeatureCollection();
		String[] stuffs = { "building.shp", "road.shp", "zoning.shp", "prescPonct.shp", "prescLin.shp", "prescSurf.shp" };
		for (String object : stuffs) {
			Vectors.exportSFC(vide.collection(), new File(f, object));

		}
	}

}
