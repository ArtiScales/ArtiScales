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
import fr.ign.cogit.util.DataPreparator;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.SimuTool;
import fr.ign.cogit.util.VectorFct;
import fr.ign.parameters.Parameters;

public class SelectParcels {

	File rootFile, tmpFile, spatialConf, geoFile, regulFile, parcelFile, zoningFile;
	List<List<File>> spatialConfigurations;
	String action;
	List<Parameters> lP = new ArrayList<Parameters>();

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
		regulFile = new File(rootFile, "dataRegulation");

		// where temporary stuff are stored
		tmpFile = new File(rootFile, "tmp");
		tmpFile.mkdir();

		// Liste des sorties de MupCity
		spatialConfigurations = spatialconfigurations;
		// Paramètre si l'on découpe les parcelles ou non
		zoningFile = GetFromGeom.getZoning(new File(rootFile, "dataRegulation"));
	}

	public List<List<File>> run() throws Exception {

		List<List<File>> selectionFile = new ArrayList<List<File>>();

		for (List<File> scenar : spatialConfigurations) {
			List<File> listScenar = new ArrayList<File>();
			String scenarName = scenar.get(0).getName().split("-")[0];
			Parameters p = SimuTool.getParamFile(lP, scenarName);
			List<String> listeAction = selectionType(p);
			for (File varianteSpatialConf : scenar) {

				spatialConf = varianteSpatialConf;
				// if we simul on one city (debug) or the whole area
				List<String> listZip = SimuTool.getIntrestingCommunities(p, geoFile, regulFile, tmpFile);
				if (listZip == null) {
					listZip = GetFromGeom.allZip(geoFile);
				}
				// we loop on every cities
				for (String zip : listZip) {
					System.out.println("for the " + zip + " city");

					parcelFile = GetFromGeom.getParcels(geoFile, regulFile, tmpFile, zip);

					ShapefileDataStore shpDSparcel = new ShapefileDataStore((parcelFile).toURI().toURL());
					SimpleFeatureCollection parcelCollection = shpDSparcel.getFeatureSource().getFeatures();

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

					File ressource = new File(this.getClass().getClassLoader().getResource("").getFile());

					if (!p.getString("splitDensification").equals("false") && !p.getString("splitDensification").equals("")) {
						if (!p.getBoolean("Ubuilt")) {
							System.out.println("Scenar error. We cannot densify if the U build parcels haven't been selected");
						} else {
							String splitZone = p.getString("splitDensification");
							if (!splitZone.contains("-")) {
								System.out.println();
								System.out.println("///// We start the densification process\\\\\\");
								parcelCollection = VectorFct.parcelDensification(splitZone, parcelCollection, tmpFile, spatialConf, ressource, p);
								Vectors.exportSFC(parcelCollection, new File(tmpFile, "afterDensification"));
							} else {
								System.err.println("splitParcel : complex section non implemented yet");
							}
						}
					}

					if (!p.getString("splitMotifZone").equals("false") && !p.getString("splitMotifZone").equals("")) {
						String splitZone = p.getString("splitMotifZone");
						if (!splitZone.contains("-")) {
							System.out.println();
							System.out.println("///// We start the splitMotifZone process\\\\\\");
							parcelCollection = VectorFct.parcelGenZone(splitZone, parcelCollection, tmpFile, spatialConf, p, ressource, true);
							Vectors.exportSFC(parcelCollection, new File(tmpFile, "aftersplitMotifZone"));
						} else {
							System.err.println("splitParcel : complex section non implemented yet");
						}
					}
					if (!p.getString("splitMotif").equals("false") && !p.getString("splitMotif").equals("")) {
						String splitZone = p.getString("splitMotif");
						if (!splitZone.contains("-")) {
							System.out.println();
							System.out.println("///// We start the splitMotif process\\\\\\" + splitZone);
							parcelCollection = VectorFct.parcelGenMotif(splitZone, parcelCollection, tmpFile, spatialConf, p, ressource, true);
							Vectors.exportSFC(parcelCollection, new File(tmpFile, "aftersplitMotif"));
						} else {
							System.err.println("splitParcel : complex section non implemented yet");
						}
					}

					// //delete the tiny parcels
					// parcelCollection = Vectors.delTinyParcels(parcelCollection,
					// 10.0);

					////////////////
					////// Packing the parcels for SimPLU3D distribution
					////////////////

					File packFile = new File(rootFile, "ParcelSelectionFile/" + scenarName + "/" + varianteSpatialConf.getParentFile().getName() + "/");
					packFile.mkdirs();
					File parcelSelectedFile = Vectors.exportSFC(parcelCollection, new File(packFile, "parcelGenExport.shp"));

					// optimized packages
					if (p.getString("package").equals("ilot")) {
						separateToDifferentOptimizedPack(parcelSelectedFile, packFile);
						listScenar.add(packFile);
					}
					// city (better for a continuous urbanisation)
					else if (p.getString("package").equals("communities")) {
						separateToDifferentCitiesPack(parcelSelectedFile, packFile);
						listScenar.add(packFile);
					}

					shpDSparcel.dispose();

				}
			}
			selectionFile.add(listScenar);

		}
		// SimuTool.deleteDirectoryStream(tmpFile.toPath());
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U")) {
					if ((boolean) parcel.getAttribute("IsBuild")) {
						if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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
						if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
							parcel.setAttribute("DoWeSimul", "true");
							parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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
					if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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
	public SimpleFeatureCollection runNaturalLand(SimpleFeatureCollection parcelSFC, Parameters p, boolean flagONormal) throws Exception {
		SimpleFeatureIterator parcelIt = parcelSFC.features();

		DefaultFeatureCollection result = new DefaultFeatureCollection();

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("NC")) {
					if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();

				if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
					parcel.setAttribute("DoWeSimul", "true");
					parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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

		ShapefileDataStore shpDSCells = new ShapefileDataStore(spatialConf.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute("U") || (boolean) parcel.getAttribute("AU")) {
					if (VectorFct.isParcelInCell(parcel, cellsSFS)) {
						parcel.setAttribute("DoWeSimul", "true");
						parcel.setAttribute("eval", VectorFct.getEvalInParcel(parcel, spatialConf));
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

	public void separateToDifferentCitiesPack(File parcelCollection, File fileOut) throws Exception {

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

		// if the city is following the RNU
		List<String> rnuZip = GetFromGeom.rnuZip(regulFile);

		CSVReader predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));
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

				File parcelFile = GetFromGeom.getParcels(geoFile);

				Vectors.exportSFC(parcelCollec.subCollection(filterCity), parcelFile);

				ShapefileDataStore parcelPackSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
				SimpleFeatureCollection parcelPackCollec = parcelPackSDS.getFeatureSource().getFeatures();

				File fBBox = new File(pack, "bbox.shp");

				Vectors.exportGeom(Vectors.unionSFC(parcelPackCollec), fBBox);
				parcelPackSDS.dispose();

				ShapefileDataStore parcel_datastore = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection parcelFeatures = parcel_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(parcelFeatures, fBBox), new File(pack, "parcelle.shp"));
				parcel_datastore.dispose();

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than
				// non extitant
				createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "batiment.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "route.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescription_pct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescription_lin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescription_surf.shp"));
				prescSurf_datastore.dispose();

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
				predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));

				newPredicate.writeNext(predicate.readNext());
				for (String nIinsee : insee) {
					if (rnuZip.contains(nIinsee)) {
						newPredicate.writeNext(rnu);
						break;
					}
				}
				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee) {
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
					}
				}
				newPredicate.writeNext(out);
				newPredicate.close();
			}
		}
		predicate.close();
	}

	public void separateToDifferentOptimizedPack(File parcelCollection, File fileOut) throws Exception {

		DataPreparator.createPackages(parcelCollection, tmpFile, fileOut);
		// if the city is following the RNU
		List<String> rnuZip = GetFromGeom.rnuZip(regulFile);

		CSVReader predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));
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

		for (File pack : fileOut.listFiles()) {
			if (pack.isDirectory()) {
				File fBBox = new File(pack, "bbox.shp");

				if (!fBBox.exists()) {
					System.err.print("bbox of pack not generated");
				}

				File snapPack = new File(pack, "geoSnap");
				snapPack.mkdirs();

				// by defalut, creation of empty shapefiles (better empty than
				// non extitant
				createPackOfEmptyShp(snapPack);

				ShapefileDataStore build_datastore = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
				SimpleFeatureCollection buildFeatures = build_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(buildFeatures, fBBox), new File(snapPack, "batiment.shp"));
				build_datastore.dispose();

				ShapefileDataStore road_datastore = new ShapefileDataStore(GetFromGeom.getRoute(geoFile).toURI().toURL());
				SimpleFeatureCollection roadFeatures = road_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(roadFeatures, fBBox, 15), new File(snapPack, "route.shp"));
				road_datastore.dispose();

				ShapefileDataStore zoning_datastore = new ShapefileDataStore(GetFromGeom.getZoning(regulFile).toURI().toURL());
				SimpleFeatureCollection zoningFeatures = zoning_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(zoningFeatures, fBBox), new File(snapPack, "zone_urba.shp"));
				zoning_datastore.dispose();

				ShapefileDataStore prescPonct_datastore = new ShapefileDataStore(GetFromGeom.getPrescPonct(regulFile).toURI().toURL());
				SimpleFeatureCollection prescPonctFeatures = prescPonct_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescPonctFeatures, fBBox), new File(snapPack, "prescription_pct.shp"));
				prescPonct_datastore.dispose();

				ShapefileDataStore prescLin_datastore = new ShapefileDataStore(GetFromGeom.getPrescLin(regulFile).toURI().toURL());
				SimpleFeatureCollection prescLinFeatures = prescLin_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescLinFeatures, fBBox), new File(snapPack, "prescription_lin.shp"));
				prescLin_datastore.dispose();

				ShapefileDataStore prescSurf_datastore = new ShapefileDataStore(GetFromGeom.getPrescSurf(regulFile).toURI().toURL());
				SimpleFeatureCollection prescSurfFeatures = prescSurf_datastore.getFeatureSource().getFeatures();
				Vectors.exportSFC(Vectors.snapDatas(prescSurfFeatures, fBBox), new File(snapPack, "prescription_surf.shp"));
				prescSurf_datastore.dispose();

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
				predicate = new CSVReader(new FileReader(GetFromGeom.getPredicate(regulFile)));

				newPredicate.writeNext(predicate.readNext());
				for (String nIinsee : insee) {
					if (rnuZip.contains(nIinsee)) {
						newPredicate.writeNext(rnu);
						break;
					}
				}
				for (String[] line : predicate.readAll()) {
					for (String nIinsee : insee) {
						if (line[1].equals(nIinsee)) {
							newPredicate.writeNext(line);
						}
					}
				}
				newPredicate.writeNext(out);
				newPredicate.close();
			}
		}
		predicate.close();
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

		SimpleFeatureCollection vide = (new DefaultFeatureCollection()).collection();
		String[] stuffs = { "building.shp", "road.shp", "zoning.shp", "prescPonct.shp", "prescLin.shp", "prescSurf.shp" };
		for (String object : stuffs) {
			Vectors.exportSFC(vide, new File(f, object));

		}
	}

}
