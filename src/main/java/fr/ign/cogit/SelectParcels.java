package fr.ign.cogit;

import java.io.File;
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
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.outputs.XmlGen;
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
			Parameters p = SimuTool.getParamFile(lP, scenar.get(0).getName().split("-")[0]);
			List<String> listeAction = selectionType(p);
			for (File varianteSpatialConf : scenar) {
				spatialConf = varianteSpatialConf;
				// TODO est ce que ça pose un problème sur la grille?
				// parcelFile = GetFromGeom.getParcels(geoFile, new File("/tmp/"));
				parcelFile = new File("/home/mcolomb/tmp/parcTmp.shp");
				ShapefileDataStore shpDSparcel = new ShapefileDataStore((parcelFile).toURI().toURL());
				SimpleFeatureCollection parcelCollection = shpDSparcel.getFeatureSource().getFeatures();

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
						parcelCollection = random(parcelCollection);
						break;
					case "JustZoning":
						parcelCollection = runZoningAllowed(parcelCollection);
						break;
					}
				}

				Vectors.exportSFC(parcelCollection, new File("/tmp/parcelExport.shp"));
				// TODO faire le découpage des parcelle selon le num insee ou une méthode de Mickael
				separateToDifferentPack(parcelCollection);
				listScenar.add(varianteSpatialConf);
				shpDSparcel.dispose();
			}
			selectionFile.add(listScenar);
		}
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
				if (isParcelInZone(parcel, "U")) {
					if (isParcelBuilt(parcel)) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", 1);
							parcel.setAttribute("eval", getEvalInParcel(parcel));
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
				if (isParcelInZone(parcel, "U")) {
					if (!isParcelBuilt(parcel)) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", 1);
							parcel.setAttribute("eval", getEvalInParcel(parcel));
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
				if (isParcelInZone(parcel, "U")) {
					if (!isParcelBuilt(parcel)) {
						if (isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", 1);
							parcel.setAttribute("eval", getEvalInParcel(parcel));
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
				if (isParcelInZone(parcel, "N") || isParcelInZone(parcel, "A") || isParcelInZone(parcel, "NC")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", 1);
						parcel.setAttribute("eval", getEvalInParcel(parcel));
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
					parcel.setAttribute("DoWeSimul", 1);
					parcel.setAttribute("eval", getEvalInParcel(parcel));
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

	public SimpleFeatureCollection random(SimpleFeatureCollection parcelSFC) throws Exception {
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
				if (isParcelInZone(parcel, "U") || isParcelInZone(parcel, "AU")) {
					if (isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", 1);
						parcel.setAttribute("eval", getEvalInParcel(parcel));
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

	public boolean isParcelInZone(SimpleFeature parcelIn, String typeZone) throws Exception {

		// import of the MUP-City outputs (cache to be a lil more fast)
		SimpleFeatureCollection zoneCollection = null;
		if (!(parcelUFilled && parcelAUFilled)) {
			zoneCollection = GetFromGeom.selecParcelZoning(typeZone, parcelFile, zoningFile);
			if (typeZone.equals("U")) {
				parcelU = zoneCollection;
				parcelUFilled = true;
			} else if (typeZone.equals("AU")) {
				parcelAU = zoneCollection;
				parcelAUFilled = true;
			}
		} else {
			if (typeZone.equals("U")) {
				zoneCollection = parcelU;
			} else if (typeZone.equals("AU")) {
				zoneCollection = parcelAU;
			} else {
				System.out.println("problem");
			}
		}

		if (zoneCollection.contains(parcelIn)) {

			return true;

		} else {
			return false;
		}

	}

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

		if (((Geometry) parcelIn.getDefaultGeometryProperty()).contains(batiUnion)) {
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

	public void separateToDifferentPack(SimpleFeatureCollection parcelCollection) throws IOException {
		// TODO faire une séparation de tous les objets géographiques (parcelles, batiments, routes) pour une certaine échelle (tous le même nombre de parcelle?)

		File toWriteFile = new File(rootFile, "ParcelManagerOut");

		ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBati(geoFile).toURI().toURL());
		SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();

		ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
		SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();

		ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();

		ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();

		ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();

		ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
		SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();

		zoning_datastore.dispose();
		build_datastore.dispose();
		road_datastore.dispose();
		prescPonct_datastore.dispose();
		prescLin_datastore.dispose();
		prescSurf_datastore.dispose();

	}

}
