package fr.ign.cogit.modules;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.artiscales.parcelFunction.ParcelGetter;
import fr.ign.artiscales.parcelFunction.ParcelState;
import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.geoToolsFunctions.vectors.Geom;
import fr.ign.cogit.geoToolsFunctions.vectors.Shp;
import fr.ign.cogit.parameter.ProfileUrbanFabric;
import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.ApplyParcelManager;
import fr.ign.cogit.util.DataPreparator;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.SimuTool;

public class SelectParcels {

	File rootFile, geoFile, regulFile, parcelFile, zoningFile, outFolder, spatialConfigurationMUP;

	String action;
	SimpluParametersJSON p;

	// result parameters
	int nbParcels;
	float moyEval;

	public static void main(String[] args) throws Exception {
		// aggregateParcelsFromZips(new File("/home/ubuntu/workspace/ArtiScales/result2903/"),
		// new File("/home/ubuntu/workspace/ArtiScales/result2903/tmp/"), "ParcelManager");

		List<File> lF = new ArrayList<>();
		lF.add(new File("/home/ubuntu/boulot/these/result2903/rattrapage/paramFolder/paramSet/CDense/parameterTechnic.json"));
		lF.add(new File("/home/ubuntu/boulot/these/result2903/rattrapage/paramFolder/paramSet/CDense/parameterScenario.json"));

		SimpluParametersJSON p = new SimpluParametersJSON(lF);

		File rootFile = new File("/home/ubuntu/boulot/these/result2903/rattrapage/");
		File fileOut = new File("/home/ubuntu/boulot/these/result2903/rattrapage/depotParcel");
		File varianteSpatialConf = new File(
				"/home/ubuntu/boulot/these/result2903/rattrapage/MupCityDepot/CDense/base/CDense--N6_St_Moy_ahpE_seed_42-evalAnal-20.0.shp");
		SelectParcels selecPar = new SelectParcels(rootFile, fileOut, varianteSpatialConf, p);
		// selecPar.run(new File("/home/ubuntu/boulot/these/result2903/rattrapage/parcelMissing.shp"));

		selecPar.separateToDifferentOptimizedPack(new File("/home/ubuntu/boulot/these/result2903/rattrapage/depotParcel/parcelGenExport.shp"),
				new File("/home/ubuntu/boulot/these/result2903/rattrapage/depotParcel/tmpFile/"));

	}

	public SelectParcels(File rootfile, File outfile, File spatialconfiguration, SimpluParametersJSON par) throws Exception {
		// objet contenant les paramètres
		p = par;
		// where everything's happends
		rootFile = rootfile;
		// where the geographic data are stored
		geoFile = new File(rootFile, "dataGeo");

		// where the regulation data are stored
		regulFile = new File(rootFile, "dataRegulation");
		outFolder = outfile;

		// Liste des sorties de MupCity
		spatialConfigurationMUP = spatialconfiguration;
		// Paramètre si l'on découpe les parcelles ou non
		zoningFile = FromGeom.getZoning(new File(rootFile, "dataRegulation"));
	}

	public File run() throws Exception {
		return run(null);
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public File run(File specificFile) throws Exception {

		File parcGen = new File(outFolder, "parcelGenExport.shp");

		// if we simul on one city (debug) or the whole area
		List<String> listZip = SimuTool.getIntrestingCommunities(p, geoFile, regulFile, outFolder, specificFile);
		// we loop on every cities
		for (String zip : listZip) {
			if (specificFile != null && specificFile.exists()) {
				selectAndDecompParcels(zip, true, parcGen, specificFile);
			} else {
				selectAndDecompParcels(zip, true, parcGen);
			}
		}

		////////////////
		////// Packing the parcels for SimPLU3D distribution
		////////////////
		// optimized packages
		File tmpFile = new File(outFolder, "tmpFile");
		tmpFile.mkdirs();
		if (p.getString("package").equals("ilot")) {
			separateToDifferentOptimizedPack(parcGen, outFolder, tmpFile, regulFile, geoFile);
		}
		// city (better for a continuous urbanisation)
		else if (p.getString("package").equals("communities")) {
			separateToDifferentCitiesPack(parcGen, outFolder, regulFile, geoFile);
		}

		// SimuTool.deleteDirectoryStream(tmpFile.toPath());
		return outFolder;
	}

	public void setGeoFile(File newGeoFile) {
		geoFile = newGeoFile;
	}

	public void selectAndDecompParcels(String zip, Boolean mergeParcels, File mergeFile) throws Exception {
		selectAndDecompParcels(zip, mergeParcels, mergeFile, null);
	}

	public void selectAndDecompParcels(String zip, Boolean mergeParcels, File mergeFile, File listSpecificParcel) throws Exception {

		File tmpFile = new File(mergeFile.getParentFile(), "tmpFile");
		tmpFile.mkdirs();
		ProfileUrbanFabric profileUrbanFabric = new ProfileUrbanFabric();
		List<String> listeAction = selectionType(p);

		// if (zip.equals("25056")) {continue;}
		System.out.println();
		System.out.println("for the " + zip + " city");
		System.out.println();
		if (listSpecificParcel != null && listSpecificParcel.exists()) {
			parcelFile = ParcelGetter.getParcels(FromGeom.getBuild(geoFile), FromGeom.getZoning(regulFile), FromGeom.getParcel(geoFile), tmpFile, zip,
					listSpecificParcel, p.getBoolean("preCutParcels"));
		} else {
			parcelFile = ParcelGetter.getParcels(FromGeom.getBuild(geoFile), FromGeom.getZoning(regulFile), FromGeom.getParcel(geoFile), tmpFile, zip,
					p.getBoolean("preCutParcels"));
		}
		ShapefileDataStore shpDSparcel = new ShapefileDataStore((parcelFile).toURI().toURL());
		SimpleFeatureCollection parcelCollection = DataUtilities.collection(shpDSparcel.getFeatureSource().getFeatures());
		shpDSparcel.dispose();

		/////////////
		// first selection regarding on the scenarios
		/////////////

		for (String action : listeAction) {
			System.out.println("---=+Pour le remplissage " + action + "+=---");
			switch (action) {
			case "Ubuilt":
				parcelCollection = runBrownfieldConstructed(parcelCollection);
				break;
			case "UnotBuilt":
				parcelCollection = runBrownfieldUnconstructed(parcelCollection);
				break;
			case "AU":
				parcelCollection = runGreenfieldSelected(parcelCollection);
				break;
			case "NC":
				parcelCollection = runNaturalLand(parcelCollection, p, false);
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
		////////////////
		// Split parcel processes
		////////////////
		Collec.exportSFC(parcelCollection, new File(tmpFile, "parcelBeforeSplit"));

		// some very few cases are still crashing, so we get the parcels back when it does

		if (!p.getString("splitDensification").equals("false") && !p.getString("splitDensification").equals("")) {
			if (!p.getBoolean("Ubuilt")) {
				System.out.println("Scenar error. We cannot densify if the U build parcels haven't been selected");
			} else {
				String splitZone = p.getString("splitDensification");
				if (!splitZone.contains("-")) {
					System.out.println();
					System.out.println("///// We start the densification process\\\\\\");
					parcelCollection = ApplyParcelManager.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, outFolder,
							spatialConfigurationMUP, rootFile, geoFile, regulFile, p, profileUrbanFabric, "densification", true);
					Collec.exportSFC(parcelCollection, new File(tmpFile, "afterDensification"));
				} else {
					System.err.println("splitParcel : complex section non implemented yet");
				}
			}
		}

		if (!p.getString("splitTotRecomp").equals("false") && !p.getString("splitTotRecomp").equals("")) {
			String splitZone = p.getString("splitTotRecomp");
			if (!splitZone.contains("-")) {
				System.out.println();
				System.out.println("///// We start the splitTotRecomp process\\\\\\");
				parcelCollection = ApplyParcelManager.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, outFolder,
						spatialConfigurationMUP, rootFile, geoFile, regulFile, p, profileUrbanFabric, "totRecomp", true);
				Collec.exportSFC(parcelCollection, new File(tmpFile, "afterSplitTotRecomp"));
			} else {
				System.err.println("splitParcel : complex section non implemented yet");
			}
		}
		if (!p.getString("splitPartRecomp").equals("false") && !p.getString("splitPartRecomp").equals("")) {
			String splitZone = p.getString("splitPartRecomp");
			if (!splitZone.contains("-")) {
				System.out.println();
				System.out.println("///// We start the splitPartRecomp process\\\\\\");
				parcelCollection = ApplyParcelManager.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, outFolder,
						spatialConfigurationMUP, rootFile, geoFile, regulFile, p, profileUrbanFabric, "partRecomp", true);
				Collec.exportSFC(parcelCollection, new File(tmpFile, "aftersplitPartRecomp"));
			} else {
				System.err.println("splitParcel : complex section non implemented yet");
			}
		}

		// if there's been a bug and a parcel is missing
		ShapefileDataStore shpDSparcel2 = new ShapefileDataStore(FromGeom.getParcel(geoFile).toURI().toURL());
		SimpleFeatureCollection parcelOriginal = shpDSparcel2.getFeatureSource().getFeatures();

		// TODO look if that works well
		// parcelCollection = ParcelCollection.completeParcelMissingWithOriginal(parcelCollection, parcelOriginal);
		shpDSparcel2.dispose();

		// if used in the normal case, we append the newly generated file onto parcelGenExport
		if (mergeParcels) {
			File parcelSelectedFile = Collec.exportSFC(parcelCollection, new File(mergeFile.getParentFile(), "parcelPartExport.shp"));
			List<File> lFile = new ArrayList<File>();
			if (mergeFile.exists()) {
				lFile.add(mergeFile);
			}
			lFile.add(parcelSelectedFile);
			Shp.mergeVectFiles(lFile, mergeFile);
		}
		// else, if distributed calculation, we create a shapeFile for each zip
		else {
			File zipFolder = new File(outFolder, zip);
			zipFolder.mkdirs();
			System.out.println("Export into " + zipFolder + " ? " + zipFolder.exists());
			System.out.println("Writing " + new File(zipFolder, "parcelOut-" + zip));
			Collec.exportSFC(parcelCollection, new File(zipFolder, "parcelOut-" + zip));
		}
		shpDSparcel.dispose();
	}

	/**
	 * Know which selection method to use determined by the param file
	 * 
	 * @return a list with all the different selections
	 *
	 */
	private static List<String> selectionType(SimpluParameters p) {
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
			if (p.getBoolean("AU")) {
				routine.add("AU");
			}
			if (p.getBoolean("NC")) {
				routine.add("NC");
			}
		}
		return routine;
	}

	/**
	 * calculate the average evaluation of the parcels
	 * 
	 * @param parc
	 * @return
	 */
	@SuppressWarnings("unused")
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
	 * Fill the already urbanised land (recognized with the U label from the field TypeZone) and constructed parcels
	 * 
	 * @return
	 * @throws Exception
	 */
	public SimpleFeatureCollection runBrownfieldConstructed(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if ((boolean) parcel.getAttribute("IsBuild")) {
						if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if (!(boolean) parcel.getAttribute("IsBuild")) {
						if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("AU")) {
					if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runNaturalLand(SimpleFeatureCollection parcelSFC, SimpluParameters p, boolean flagONormal) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("NC")) {
					if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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
	 * @return a (shape)file containing the selection of parcel to urbanise
	 * @throws Exception
	 */
	public SimpleFeatureCollection runAll(SimpleFeatureCollection parcelSFC) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();

				if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
					parcel.setAttribute("DoWeSimul", "true");
					parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConfigurationMUP.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U") || (boolean) parcel.getAttribute("AU")) {
					if (Collec.isFeatIntersectsSFC(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelState.getEvalInParcel(parcel, spatialConfigurationMUP));
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

	// public File selecOneParcelInCell(SimpleFeatureCollection parcelIn) throws
	// IOException {
	// // mettre le recouvrement des cellules dans un attribut et favoriser
	// // selon le plus gros pourcentage?
	//
	// FilterFactory2 ff =
	// CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
	// String geometryParcelPropertyName =
	// parcelIn.getSchema().getGeometryDescriptor().getLocalName();
	//
	// ShapefileDataStore shpDSCells = new
	// ShapefileDataStore(spatialConf.toURI().toURL());
	// SimpleFeatureCollection cellsCollection =
	// shpDSCells.getFeatureSource().getFeatures();
	//
	// SimpleFeatureIterator cellIt = cellsCollection.features();
	// try {
	// while (cellIt.hasNext()) {
	// SimpleFeature feat = cellIt.next();
	// Filter inter = ff.intersects(ff.property(geometryParcelPropertyName),
	// ff.literal(feat.getDefaultGeometry()));
	// SimpleFeatureCollection parcelMultipleSelection =
	// parcelIn.subCollection(inter);
	// if (!parcelMultipleSelection.isEmpty()) {
	// SimpleFeature bestFeature = null;
	// SimpleFeatureIterator multipleSelec = parcelMultipleSelection.features();
	// try {
	// while (multipleSelec.hasNext()) {
	// SimpleFeature featParc = multipleSelec.next();
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// multipleSelec.close();
	// }
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// cellIt.close();
	// }
	// shpDSCells.dispose();
	// return null;
	// }

	public static void separateToDifferentCitiesPack(File parcelCollection, File fileOut, File regulFile, File geoFile) throws Exception {

		ShapefileDataStore sdsParc = new ShapefileDataStore(parcelCollection.toURI().toURL());
		SimpleFeatureCollection parcelCollec = sdsParc.getFeatureSource().getFeatures();
		SimpleFeatureIterator parcel = parcelCollec.features();

		List<String> cities = new ArrayList<>();

		try {
			while (parcel.hasNext()) {
				SimpleFeature city = parcel.next();
				if (!cities.contains(city.getAttribute("INSEE"))) {
					cities.add((String) city.getAttribute("INSEE"));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcel.close();
		}

		for (String city : cities) {
			new File(fileOut, city).mkdirs();
		}

		CSVReader predicate = new CSVReader(new FileReader(FromGeom.getPredicate(regulFile)));
		predicate.readNext();
		String[] rnu = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("RNU")) {
				rnu = line;
			}
		}
		String[] out = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("out")) {
				out = line;
			}
		}
		// rewind
		predicate.close();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

		for (File pack : fileOut.listFiles()) {
			if (pack.isDirectory()) {

				Filter filterCity = ff.like(ff.property("INSEE"), pack.getName());

				File parcelFile = FromGeom.getParcel(geoFile);

				Collec.exportSFC(parcelCollec.subCollection(filterCity), parcelFile);

				ShapefileDataStore parcelPackSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
				SimpleFeatureCollection parcelPackCollec = parcelPackSDS.getFeatureSource().getFeatures();

				File fBBox = new File(pack, "bbox.shp");

				Geom.exportGeom(Geom.unionSFC(parcelPackCollec), fBBox);
				parcelPackSDS.dispose();

				ShapefileDataStore parcel_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection parcelFeatures = parcel_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(parcelFeatures, fBBox), new File(pack, "parcelle.shp"));
				parcel_datastore.dispose();

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than
				// non extitant
				// createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(buildFeatures, fBBox), new File(snapPack, "batiment.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(FromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "route.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(FromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescription_pct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(FromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescription_lin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(FromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Collec.exportSFC(Collec.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescription_surf.shp"));
				prescSurf_datastore.dispose();

				ShapefileDataStore communitiesDatastore = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
				SimpleFeatureCollection communitiesFeatures = communitiesDatastore.getFeatureSource().getFeatures();
				SimpleFeatureCollection communitiesSS = Collec.snapDatas(communitiesFeatures, fBBox);
				if (!communitiesSS.isEmpty()) {
					Collec.exportSFC(communitiesSS, new File(snapPack, "communities.shp"));
				}
				communitiesDatastore.dispose();

				// selection of the right lines from the predicate file
				// CSVWriter newPredicate = new CSVWriter(new FileWriter(new
				// File(pack,
				// "snapPredicate.csv")),",","","");

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
				predicate = new CSVReader(new FileReader(FromGeom.getPredicate(regulFile)));

				newPredicate.writeNext(predicate.readNext());

				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee) {
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
					}
				}
				newPredicate.writeNext(rnu);
				newPredicate.writeNext(out);
				newPredicate.close();
			}
		}
		predicate.close();
	}

	/**
	 * get all the parcels from different pack and generate an aggregated shapefile in the same variante file
	 * 
	 * @param rootFile
	 *            File of the ArtiScales project
	 * @throws Exception
	 */
	public static void aggregateParcelsFromZips(File rootFile) throws Exception {
		aggregateParcelsFromZips(rootFile, rootFile, "ParcelSelectionDepot");
	}

	/**
	 * get all the parcels from different pack and generate an aggregated shapefile in a specific variante file of another rootFile
	 * 
	 * @param rootFile
	 *            File of the ArtiScales project
	 * 
	 * @throws Exception
	 */
	public static void aggregateParcelsFromZips(File rootFile, File whereToSave, String nameOfParcelDepot) throws Exception {
		for (File scenarFile : (new File(rootFile, nameOfParcelDepot)).listFiles()) {
			if (scenarFile.isDirectory()) {
				for (File variantFile : scenarFile.listFiles()) {
					List<File> zips = new ArrayList<File>();
					for (File zip : variantFile.listFiles()) {
						if (zip.isDirectory() && !zip.getName().equals("tmpFile")) {
							zips.add(new File(zip, "parcelOut-" + zip.getName() + ".shp"));
						}
					}
					System.out.println(new File(variantFile, "parcelGenExport.shp"));
					File save = new File(whereToSave, "ParcelSelectionDepot/" + scenarFile.getName() + "/" + variantFile.getName() + "/");
					save.mkdirs();
					Shp.mergeVectFiles(zips, new File(save, "parcelGenExport.shp"));
				}
			}
		}
	}

	public void separateToDifferentOptimizedPack(File parcelCollection, File tmpFile) throws Exception {
		separateToDifferentOptimizedPack(parcelCollection, outFolder, tmpFile, regulFile, geoFile);
	}

	public static void separateToDifferentOptimizedPack(File parcelCollection, File fileOut, File tmpFile, File regulFile, File geoFile)
			throws Exception {

		DataPreparator.createPackages(parcelCollection, tmpFile, fileOut);

		CSVReader predicate = new CSVReader(new FileReader(FromGeom.getPredicate(regulFile)));
		predicate.readNext();
		String[] rnu = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("RNU")) {
				rnu = line;
			}
		}
		String[] out = null;
		for (String[] line : predicate.readAll()) {
			if (line[0].equals("out")) {
				out = line;
			}
		}
		// rewind
		predicate.close();
		for (File folder : fileOut.listFiles()) {
			if (folder.isDirectory()) {
				for (File pack : folder.listFiles()) {
					if (pack.isDirectory()) {
						File fBBox = new File(pack, "bbox.shp");
						System.setOut(new PrintStream(System.out));

						if (!fBBox.exists())
							System.err.print("bbox of pack not generated");
						File snapPack = new File(pack, "geoSnap");
						snapPack.mkdirs();

						// by defalut, creation of empty shapefiles (better empty than
						// non extitant
						// createPackOfEmptyShp(snapPack);

						ShapefileDataStore build_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
						SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection buildSnapped = Collec.snapDatas(buildFeatures, fBBox);
						if (!buildSnapped.isEmpty())
							Collec.exportSFC(buildSnapped, new File(snapPack, "batiment.shp"));
						build_datastore.dispose();

						ShapefileDataStore road_datastore = new ShapefileDataStore(FromGeom.getRoute(geoFile).toURI().toURL());
						SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection roadSnap = Collec.snapDatas(roadFeatures, fBBox, 15);
						if (!roadSnap.isEmpty())
							Collec.exportSFC(roadSnap, new File(snapPack, "route.shp"));
						road_datastore.dispose();

						ShapefileDataStore zoning_datastore = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
						SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection zoningSnap = Collec.snapDatas(zoningFeatures, fBBox);
						if (!zoningSnap.isEmpty())
							Collec.exportSFC(Collec.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
						zoning_datastore.dispose();

						ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(FromGeom.getPrescPonct(regulFile).toURI().toURL());
						SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescPSnap = Collec.snapDatas(prescPonctFeatures, fBBox);
						if (!prescPSnap.isEmpty())
							Collec.exportSFC(prescPSnap, new File(snapPack, "prescription_pct.shp"));
						prescPonct_datastore.dispose();

						ShapefileDataStore prescLin_datastore = new ShapefileDataStore(FromGeom.getPrescLin(regulFile).toURI().toURL());
						SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescLSnap = Collec.snapDatas(prescLinFeatures, fBBox);
						if (!prescLSnap.isEmpty()) {
							Collec.exportSFC(prescLSnap, new File(snapPack, "prescription_lin.shp"));
						}
						prescLin_datastore.dispose();

						ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(FromGeom.getPrescSurf(regulFile).toURI().toURL());
						SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescSS = Collec.snapDatas(prescSurfFeatures, fBBox);
						if (!prescSS.isEmpty()) {
							Collec.exportSFC(prescSS, new File(snapPack, "prescription_surf.shp"));
						}
						prescSurf_datastore.dispose();

						ShapefileDataStore communitiesDatastore = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
						SimpleFeatureCollection communitiesFeatures = communitiesDatastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection communitiesSS = Collec.snapDatas(communitiesFeatures, fBBox);
						if (!communitiesSS.isEmpty()) {
							Collec.exportSFC(communitiesSS, new File(snapPack, "communities.shp"));
						}
						communitiesDatastore.dispose();

						// selection of the right lines from the predicate file
						// CSVWriter newPredicate = new CSVWriter(new FileWriter(new
						// File(pack,
						// "snapPredicate.csv")),",","",""); System.out.println(geom

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
						predicate = new CSVReader(new FileReader(FromGeom.getPredicate(regulFile)));

						newPredicate.writeNext(predicate.readNext());

						for (String[] line : predicate.readAll()) {
							for (String nIinsee : insee) {
								if (line[1].equals(nIinsee)) {
									newPredicate.writeNext(line);
								}
							}
						}
						newPredicate.writeNext(rnu);
						newPredicate.writeNext(out);
						newPredicate.close();
					}
				}
			}
		}
		predicate.close();
	}

	public static void splitIntoPack() throws Exception {
		// aggregateParcelsFromZips(new File("/home/ubuntu/boulot/these/result2903/"));
		File rootFile = new File("/home/ubuntu/boulot/these/result2903/");
		File regul = new File(rootFile, "dataRegulation");
		File geo = new File(rootFile, "dataGeo");
		File tmp = new File(rootFile, "tmp");
		tmp.mkdir();
		for (File scenarFile : (new File(rootFile, "ParcelSelectionDepot")).listFiles()) {
			if (scenarFile.isDirectory()) {
				String scenar = scenarFile.getName();
				if (!scenar.equals("DPeuDense")) {
					continue;
				}
				System.out.println(scenarFile);
				for (File variantFile : scenarFile.listFiles()) {
					String variant = variantFile.getName();
					System.out.println(variantFile);
					File parcel = new File(variantFile, "parcelGenExport.shp");
					File outFolder = new File(rootFile, "SimPLUDepot/" + scenar + "/" + variant);
					if (outFolder.exists()) {
						continue;
					}
					outFolder.mkdirs();
					separateToDifferentOptimizedPack(parcel, outFolder, tmp, regul, geo);
				}
			}
		}
	}
}
