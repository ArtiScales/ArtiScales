package fr.ign.cogit.modules;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.MultiPolygon;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.outputs.XmlGen;
import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.DataPreparator;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;
import fr.ign.cogit.util.SimuTool;

public class SelectParcels {

	File rootFile, geoFile, regulFile, parcelFile, zoningFile, outFile, spatialConfigurationMUP;

	String action;
	SimpluParametersJSON p;

	// result parameters
	int nbParcels;
	float moyEval;

	public static void main(String[] args) throws Exception {
		aggregateParcelsFromZips(new File("./WorkSession0327"));
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
		outFile = outfile;

		// Liste des sorties de MupCity
		spatialConfigurationMUP = spatialconfiguration;
		// Paramètre si l'on découpe les parcelles ou non
		zoningFile = FromGeom.getZoning(new File(rootFile, "dataRegulation"));
	}

	public File run() throws Exception {

		File parcGen = new File(outFile, "parcelGenExport.shp");

		// if we simul on one city (debug) or the whole area
		List<String> listZip = SimuTool.getIntrestingCommunities(p, geoFile, regulFile, outFile);

		// we loop on every cities
		for (String zip : listZip) {
			selectAndDecompParcels(zip, true, parcGen);
		}

		////////////////
		////// Packing the parcels for SimPLU3D distribution
		////////////////
		// optimized packages
		File tmpFile = new File(outFile, "tmpFile");
		tmpFile.mkdirs();
		if (p.getString("package").equals("ilot")) {
			separateToDifferentOptimizedPack(parcGen, outFile, tmpFile, regulFile, geoFile);
		}
		// city (better for a continuous urbanisation)
		else if (p.getString("package").equals("communities")) {
			separateToDifferentCitiesPack(parcGen, outFile, regulFile, geoFile);
		}

		// SimuTool.deleteDirectoryStream(tmpFile.toPath());
		return outFile;
	}

	public void selectAndDecompParcels(String zip, Boolean mergeParcels, File mergeFile) throws Exception {

		File tmpFile = new File(mergeFile.getParentFile(), "tmpFile");
		tmpFile.mkdirs();

		List<String> listeAction = selectionType(p);

		// if (zip.equals("25056")) {continue;}
		System.out.println();
		System.out.println("for the " + zip + " city");
		System.out.println();
		parcelFile = ParcelFonction.getParcels(geoFile, regulFile, tmpFile, zip, p.getBoolean("preCutParcels"));

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
		Vectors.exportSFC(parcelCollection, new File(tmpFile, "parcelBeforeSplit"));

		// some very few cases are still crashing, so we get the parcels back when it does

		if (!p.getString("splitDensification").equals("false") && !p.getString("splitDensification").equals("")) {
			if (!p.getBoolean("Ubuilt")) {
				System.out.println("Scenar error. We cannot densify if the U build parcels haven't been selected");
			} else {
				String splitZone = p.getString("splitDensification");
				if (!splitZone.contains("-")) {
					System.out.println();
					System.out.println("///// We start the densification process\\\\\\");
					parcelCollection = ParcelFonction.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, spatialConfigurationMUP,
							rootFile, p, "densification", true);
					Vectors.exportSFC(parcelCollection, new File(tmpFile, "afterDensification"));
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
				parcelCollection = ParcelFonction.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, spatialConfigurationMUP, rootFile,
						p, "totRecomp", true);
				Vectors.exportSFC(parcelCollection, new File(tmpFile, "afterSplitTotRecomp"));
			} else {
				System.err.println("splitParcel : complex section non implemented yet");
			}
		}
		if (!p.getString("splitPartRecomp").equals("false") && !p.getString("splitPartRecomp").equals("")) {
			String splitZone = p.getString("splitPartRecomp");
			if (!splitZone.contains("-")) {
				System.out.println();
				System.out.println("///// We start the splitPartRecomp process\\\\\\");
				parcelCollection = ParcelFonction.setRecompositionProcesssus(splitZone, parcelCollection, tmpFile, spatialConfigurationMUP, rootFile,
						p, "partRecomp", true);
				Vectors.exportSFC(parcelCollection, new File(tmpFile, "aftersplitPartRecomp"));
			} else {
				System.err.println("splitParcel : complex section non implemented yet");
			}
		}

		// if there's been a bug and a parcel is missing
		ShapefileDataStore shpDSparcel2 = new ShapefileDataStore(FromGeom.getParcels(geoFile).toURI().toURL());
		SimpleFeatureCollection parcelOriginal = shpDSparcel2.getFeatureSource().getFeatures();
		parcelCollection = ParcelFonction.completeParcelMissingWithOriginal(parcelCollection, parcelOriginal);
		shpDSparcel2.dispose();

		// if used in the normal case, we append the newly generated file onto parcelGenExport
		if (mergeParcels) {
			File parcelSelectedFile = Vectors.exportSFC(parcelCollection, new File(mergeFile.getParentFile(), "parcelPartExport.shp"));
			List<File> lFile = new ArrayList<File>();
			if (mergeFile.exists()) {
				lFile.add(mergeFile);
			}
			lFile.add(parcelSelectedFile);
			Vectors.mergeVectFiles(lFile, mergeFile);
		}
		// else, if distributed calculation, we create a shapeFile for each zip
		else {
			File zipFolder = new File(outFile, zip);
			zipFolder.mkdirs();
			System.out.println("Export into " + zipFolder + " ? " + zipFolder.exists());
			System.out.println("Writing " + new File(zipFolder, "parcelOut-" + zip));
			Vectors.exportSFC(parcelCollection, new File(zipFolder, "parcelOut-" + zip));
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
						if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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
						if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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
					if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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
					if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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

				if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
					parcel.setAttribute("DoWeSimul", "true");
					parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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
					if (ParcelFonction.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", ParcelFonction.getEvalInParcel(parcel, spatialConfigurationMUP));
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

				File parcelFile = FromGeom.getParcels(geoFile);

				Vectors.exportSFC(parcelCollec.subCollection(filterCity), parcelFile);

				ShapefileDataStore parcelPackSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
				SimpleFeatureCollection parcelPackCollec = parcelPackSDS.getFeatureSource().getFeatures();

				File fBBox = new File(pack, "bbox.shp");

				Vectors.exportGeom(Vectors.unionSFC(parcelPackCollec), fBBox);
				parcelPackSDS.dispose();

				ShapefileDataStore parcel_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection parcelFeatures = parcel_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(parcelFeatures, fBBox), new File(pack, "parcelle.shp"));
				parcel_datastore.dispose();

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than
				// non extitant
				// createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "batiment.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(FromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "route.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(FromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescription_pct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(FromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescription_lin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(FromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescription_surf.shp"));
				prescSurf_datastore.dispose();

				ShapefileDataStore communitiesDatastore = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
				SimpleFeatureCollection communitiesFeatures = communitiesDatastore.getFeatureSource().getFeatures();
				SimpleFeatureCollection communitiesSS = Vectors.snapDatas(communitiesFeatures, fBBox);
				if (!communitiesSS.isEmpty()) {
					Vectors.exportSFC(communitiesSS, new File(snapPack, "communities.shp"));
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

	public static void aggregateParcelsFromZips(File rootFile) throws Exception {
		for (File scenarFile : (new File(rootFile, "ParcelSelectionDepot")).listFiles()) {
			if (scenarFile.isDirectory()) {
				System.out.println(scenarFile);
				for (File variantFile : scenarFile.listFiles()) {
					System.out.println(variantFile);
					List<File> zips = new ArrayList<File>();
					for (File zip : variantFile.listFiles()) {
						if (zip.isDirectory() && !zip.getName().equals("tmpFile")) {
							// if (zip.getName().equals("25410") || zip.getName().equals("25576") || zip.getName().equals("25395")
							// || zip.getName().equals("25084") || zip.getName().equals("25036") || zip.getName().equals("25258")
							// || zip.getName().equals("25056HV") || zip.getName().equals("25371") || zip.getName().equals("25594")
							// || zip.getName().equals("25058") || zip.getName().equals("25594") || zip.getName().equals("25111")) {
							// continue;
							// }
							zips.add(new File(zip, "parcelOut-" + zip.getName() + ".shp"));
						}
					}
					System.out.println(new File(variantFile, "parcelGenExport.shp"));
					Vectors.mergeVectFiles(zips, new File(variantFile, "parcelGenExport.shp"));
				}
			}
		}
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

						if (!fBBox.exists()) {
							System.err.print("bbox of pack not generated");
						}

						File snapPack = new File(pack, "geoSnap");
						snapPack.mkdirs();

						// by defalut, creation of empty shapefiles (better empty than
						// non extitant
						// createPackOfEmptyShp(snapPack);

						ShapefileDataStore build_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
						SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection buildSnapped = Vectors.snapDatas(buildFeatures, fBBox);
						if (!buildSnapped.isEmpty()) {
							Vectors.exportSFC(buildSnapped, new File(snapPack, "batiment.shp"));
						}
						build_datastore.dispose();

						ShapefileDataStore road_datastore = new ShapefileDataStore(FromGeom.getRoute(geoFile).toURI().toURL());
						SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection roadSnap = Vectors.snapDatas(roadFeatures, fBBox, 15);
						if (!roadSnap.isEmpty()) {
							Vectors.exportSFC(roadSnap, new File(snapPack, "route.shp"));
						}
						road_datastore.dispose();

						ShapefileDataStore zoning_datastore = new ShapefileDataStore(FromGeom.getZoning(regulFile).toURI().toURL());
						SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection zoningSnap = Vectors.snapDatas(zoningFeatures, fBBox);
						if (!zoningSnap.isEmpty()) {
							Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
						}
						zoning_datastore.dispose();

						ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(FromGeom.getPrescPonct(regulFile).toURI().toURL());
						SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescPSnap = Vectors.snapDatas(prescPonctFeatures, fBBox);
						if (!prescPSnap.isEmpty()) {
							Vectors.exportSFC(prescPSnap, new File(snapPack, "prescription_pct.shp"));
						}
						prescPonct_datastore.dispose();

						ShapefileDataStore prescLin_datastore = new ShapefileDataStore(FromGeom.getPrescLin(regulFile).toURI().toURL());
						SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescLSnap = Vectors.snapDatas(prescLinFeatures, fBBox);
						if (!prescLSnap.isEmpty()) {
							Vectors.exportSFC(prescLSnap, new File(snapPack, "prescription_lin.shp"));
						}
						prescLin_datastore.dispose();

						ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(FromGeom.getPrescSurf(regulFile).toURI().toURL());
						SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection prescSS = Vectors.snapDatas(prescSurfFeatures, fBBox);
						if (!prescSS.isEmpty()) {
							Vectors.exportSFC(prescSS, new File(snapPack, "prescription_surf.shp"));
						}
						prescSurf_datastore.dispose();

						ShapefileDataStore communitiesDatastore = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
						SimpleFeatureCollection communitiesFeatures = communitiesDatastore.getFeatureSource().getFeatures();
						SimpleFeatureCollection communitiesSS = Vectors.snapDatas(communitiesFeatures, fBBox);
						if (!communitiesSS.isEmpty()) {
							Vectors.exportSFC(communitiesSS, new File(snapPack, "communities.shp"));
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
}
