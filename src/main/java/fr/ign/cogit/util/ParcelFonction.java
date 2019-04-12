package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.annexeTools.FeaturePolygonizer;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.FlagParcelDecomposition;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;
import fr.ign.cogit.geoxygene.util.conversion.GeOxygeneGeoToolsTypes;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class ParcelFonction {

	// public static void main(String[] args) throws Exception {
	//
	// ShapefileDataStore parcelSDS = new ShapefileDataStore(
	// new
	// File("/home/mcolomb/informatique/ArtiScalesLikeTBLunch/ParcelSelectionFile/DDense/variante0/parcelGenExport.shp").toURI()
	// .toURL());
	// int tot = parcelSDS.getFeatureSource().getFeatures().size();
	// DefaultFeatureCollection result = new DefaultFeatureCollection();
	// SimpleFeatureIterator parcelIt =
	// parcelSDS.getFeatureSource().getFeatures().features();
	// // initialize the
	// result.add(parcelIt.next());
	// int count = 0;
	// try {
	// while (parcelIt.hasNext()) {
	// SimpleFeature feat = parcelIt.next();
	// SimpleFeatureIterator resIt = (Vectors.snapDatas(result.collection(),
	// ((Geometry) feat.getDefaultGeometry()).buffer(10))).features();
	// boolean add = true;
	// try {
	// while (resIt.hasNext()) {
	// SimpleFeature featRes = resIt.next();
	// if (featRes.getAttribute("CODE").equals(feat.getAttribute("CODE"))) {
	// add = false;
	// break;
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// resIt.close();
	// }
	// if (add) {
	// result.add(feat);
	// }
	// System.out.println(count++ + " on " + tot);
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// parcelIt.close();
	// }
	// parcelSDS.dispose();
	// Vectors.exportSFC(result,
	// new
	// File("/home/mcolomb/informatique/ArtiScalesLikeTBLunch/ParcelSelectionFile/DDense/variante0/parcelGenExportNoDouble.shp"));
	// }
	// File rootParam = new
	// File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
	// List<File> lF = new ArrayList<>();
	// lF.add(new File(rootParam, "parameterTechnic.xml"));
	// lF.add(new File(rootParam, "parameterScenario.xml"));
	//
	// Parameters p = Parameters.unmarshall(lF);
	//
	// File tmpFile = new File("/tmp/");
	//
	// /////////////////////////
	// //////// try the parcelDensification method
	// /////////////////////////
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// new
	// File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones =
	// shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelDensification("U", featuresZones,
	// tmpFile, new File("/home/mcolomb/informatique/ArtiScales"), new File(
	// "/home/mcolomb/informatique/ArtiScales/MupCityDepot/exScenar/variant0/exScenar-DataSys-CM20.0-S0.0-GP_915948.0_6677337.0--N6_Ba_ahpx_seed_42-evalAnal-20.0.shp"),
	// 800.0, 15.0, 5.0);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();
	//
	// // /////////////////////////
	// // //////// try the parcelGenMotif method
	// /////////////////////////
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// new
	// File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones =
	// shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelGenMotif("NC", featuresZones, tmpFile,
	// ), new File(
	// "/home/mcolomb/informatique/ArtiScales/MupCityDepot/exScenar/variant0/exScenar-DataSys-CM20.0-S0.0-GP_915948.0_6677337.0--N6_Ba_ahpx_seed_42-evalAnal-20.0.shp"),
	// 800.0, 7.0, 3.0, 2);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();
	//
	// /////////////////////////
	// //////// try the parcelGenZone method
	// /////////////////////////
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(new File("/tmp/toTest.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelTotRecomp("AU", featuresZones, new File("/tmp/"),
	// new File("/home/mcolomb/informatique/ArtiScalesTest/"),
	// new File("/home/mcolomb/informatique/ArtiScalesTest/MupCityDepot/DDense/variante0/DDense-yager-evalAnal.shp"), 4000, 12, 5, 2, true);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();
	// }

	// /////////////////////////
	// //////// try the generateFlagSplitedParcels method
	// /////////////////////////
	//
	// File geoFile = new File(, "dataGeo");
	// IFeatureCollection<IFeature> featColl =
	// ShapefileReader.read("/tmp/tmp1.shp");
	//
	// String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
	//
	// IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
	// List<IOrientableCurve> lOC =
	// featC.select(featColl.envelope()).parallelStream().map(x ->
	// FromGeomToLineString.convert(x.getGeom())).collect(ArrayList::new,
	// List::addAll,
	// List::addAll);
	//
	// IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);
	//
	// // ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// // new
	// File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// //
	// // SimpleFeatureCollection featuresZones =
	// shpDSZone.getFeatureSource().getFeatures();
	// // SimpleFeatureIterator it = featuresZones.features();
	// // SimpleFeature waiting = null;
	// // while (it.hasNext()) {
	// // SimpleFeature feat = it.next();
	// // if (((String) feat.getAttribute("CODE")).equals("25598000AB0446") ) {
	// // waiting = feat;
	// // }
	// // }
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = generateFlagSplitedParcels(featColl.get(0),
	// iMultiCurve, geoFile, tmpFile, 2000.0, 15.0, 3.0);
	//
	// Vectors.exportSFC(salut, new File("/tmp/tmp2.shp"));
	//
	// }

	public static String makeParcelCode(SimpleFeature feat) {
		return ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")) + ((String) feat.getAttribute("COM_ABS"))
				+ ((String) feat.getAttribute("SECTION")) + ((String) feat.getAttribute("NUMERO"));
	}

	public static File makeCommunitiesFromParcels(File communitiesFile, File parcelFile, File outFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("DEPCOM", String.class);
		sfTypeBuilder.add("NOM_COM", String.class);
		sfTypeBuilder.add("typo", String.class);
		sfTypeBuilder.add("surface", String.class);
		sfTypeBuilder.add("scot", String.class);
		sfTypeBuilder.add("log-icone", String.class);

		SimpleFeatureBuilder ft = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		ShapefileDataStore shpDSParcels = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection featuresParcels = shpDSParcels.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = featuresParcels.features();

		ShapefileDataStore shpDSCommunes = new ShapefileDataStore(communitiesFile.toURI().toURL());
		SimpleFeatureCollection featuresCommunes = shpDSCommunes.getFeatureSource().getFeatures();

		DefaultFeatureCollection dfC = new DefaultFeatureCollection();

		HashMap<String, List<Geometry>> result = new HashMap<String, List<Geometry>>();
		try {
			while (it.hasNext()) {
				SimpleFeature featAdd = it.next();
				String insee = ((String) featAdd.getAttribute("CODE_DEP")) + ((String) featAdd.getAttribute("CODE_COM"));
				List<Geometry> lG = new ArrayList<Geometry>();
				lG.add((Geometry) featAdd.getDefaultGeometry());
				if (result.containsKey(insee)) {
					List<Geometry> tmp = result.remove(insee);
					tmp.addAll(lG);
					result.put(insee, tmp);
				} else {
					result.put(insee, lG);
				}

			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		for (String insee : result.keySet()) {
			Geometry geom = Vectors.unionGeom(result.get(insee));
			SimpleFeatureIterator itCom = featuresCommunes.features();
			// ft.set("the_geom", geom.buffer(20).buffer(-20));
			ft.set("the_geom", geom);

			try {
				while (itCom.hasNext()) {
					SimpleFeature featAdd = itCom.next();
					if (insee.equals(featAdd.getAttribute("DEPCOM"))) {
						ft.set("DEPCOM", featAdd.getAttribute("DEPCOM"));
						ft.set("NOM_COM", featAdd.getAttribute("NOM_COM"));
						ft.set("typo", featAdd.getAttribute("typo"));
						ft.set("surface", featAdd.getAttribute("surface"));
						ft.set("scot", featAdd.getAttribute("scot"));
						ft.set("log-icone", featAdd.getAttribute("log-icone"));
						dfC.add(ft.buildFeature(null));
						break;
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				itCom.close();
			}
		}
		shpDSParcels.dispose();
		shpDSCommunes.dispose();
		return Vectors.exportSFC(DataUtilities.collection(dfC), outFile);

	}

	public static DefaultFeatureCollection addAllParcels(SimpleFeatureCollection parcelIn, SimpleFeatureCollection parcelAdd) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(parcelIn);
		SimpleFeatureIterator parcelAddIt = parcelAdd.features();
		try {
			while (parcelAddIt.hasNext()) {
				SimpleFeature featAdd = parcelAddIt.next();
				SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featAdd);
				result.add(fit.buildFeature(null));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelAddIt.close();
		}
		return result;
	}

	// public static SimpleFeatureCollection
	// completeParcelMissing(SimpleFeatureCollection parcelTot,
	// SimpleFeatureCollection parcelCuted)
	// throws NoSuchAuthorityCodeException, FactoryException {
	// DefaultFeatureCollection result = new DefaultFeatureCollection();
	// SimpleFeatureType schema = parcelTot.features().next().getFeatureType();
	// // result.addAll(parcelCuted);
	// SimpleFeatureIterator parcelCutedIt = parcelCuted.features();
	// try {
	// while (parcelCutedIt.hasNext()) {
	// SimpleFeature featCut = parcelCutedIt.next();
	// SimpleFeatureBuilder fit = GetFromGeom.setSFBParcelWithFeat(featCut, schema);
	// result.add(fit.buildFeature(null));
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// parcelCutedIt.close();
	// }
	//
	// SimpleFeatureIterator totIt = parcelTot.features();
	// try {
	// while (totIt.hasNext()) {
	// SimpleFeature featTot = totIt.next();
	// boolean add = true;
	// SimpleFeatureIterator cutIt = parcelCuted.features();
	// try {
	// while (cutIt.hasNext()) {
	// SimpleFeature featCut = cutIt.next();
	// if (((Geometry)
	// featTot.getDefaultGeometry()).buffer(0.1).contains(((Geometry)
	// featCut.getDefaultGeometry()))) {
	// add = false;
	// break;
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// cutIt.close();
	// }
	// if (add) {
	// SimpleFeatureBuilder fit = GetFromGeom.setSFBParcelWithFeat(featTot, schema);
	// result.add(fit.buildFeature(null));
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// totIt.close();
	// }
	//
	// return result;
	// }

	public static SimpleFeatureCollection completeParcelMissingWithOriginal(SimpleFeatureCollection parcelToComplete,
			SimpleFeatureCollection originalParcel) throws NoSuchAuthorityCodeException, FactoryException, IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(parcelToComplete);
		// List<String> codeParcelAdded = new ArrayList<String>();

		// SimpleFeatureType schema =
		// parcelToComplete.features().next().getFeatureType();

		// result.addAll(parcelCuted);

		SimpleFeatureIterator parcelToCompletetIt = parcelToComplete.features();
		try {
			while (parcelToCompletetIt.hasNext()) {
				SimpleFeature featToComplete = parcelToCompletetIt.next();
				Geometry geomToComplete = (Geometry) featToComplete.getDefaultGeometry();
				Geometry geomsOrigin = Vectors.unionSFC(Vectors.snapDatas(originalParcel, geomToComplete));
				if (!geomsOrigin.buffer(1).contains(geomToComplete)) {
					System.out.println("completeParcelMissingWithOriginal: this parcel has disapeard : " + geomToComplete);
					// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featToComplete,
					// schema);
					// result.add(fit.buildFeature(null));
					// SimpleFeatureBuilder builder =
					// FromGeom.setSFBOriginalParcelWithFeat(featToComplete, schema);
					// result.add(builder.buildFeature(null));
					// codeParcelAdded.add(ParcelFonction.makeParcelCode(featToComplete));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelToCompletetIt.close();
		}

		// SimpleFeatureIterator parcelOriginal = originalParcel.features();
		// try {
		// while (parcelOriginal.hasNext()) {
		// SimpleFeature featOriginal = parcelOriginal.next();
		// Geometry geom = (Geometry) featOriginal.getDefaultGeometry();
		// Geometry geomToComplete =
		// Vectors.unionSFC(Vectors.snapDatas(parcelToComplete, geom.buffer(10)));
		// if (!geomToComplete.contains(geom.buffer(-1))) {
		// System.out.println(geomToComplete);
		// System.out.println();
		// SimpleFeatureBuilder builder =
		// FromGeom.setSFBOriginalParcelWithFeat(featOriginal, schema);
		// result.add(builder.buildFeature(null));
		// codeParcelAdded.add(ParcelFonction.makeParcelCode(featOriginal));
		// }
		// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featOriginal,
		// schema);
		// result.add(fit.buildFeature(null));
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// parcelOriginal.close();
		// }

		return result;
	}

	public static SimpleFeatureCollection completeParcelMissing(SimpleFeatureCollection parcelTot, SimpleFeatureCollection parcelCuted,
			List<String> parcelToNotAdd) throws NoSuchAuthorityCodeException, FactoryException, IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureType schema = parcelTot.getSchema();
		// result.addAll(parcelCuted);
		SimpleFeatureIterator parcelCutedIt = parcelCuted.features();
		try {
			while (parcelCutedIt.hasNext()) {
				SimpleFeature featCut = parcelCutedIt.next();
				SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featCut, schema);
				result.add(fit.buildFeature(null));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelCutedIt.close();
		}

		SimpleFeatureIterator totIt = parcelTot.features();
		try {
			while (totIt.hasNext()) {
				SimpleFeature featTot = totIt.next();
				boolean add = true;
				for (String code : parcelToNotAdd) {
					if (featTot.getAttribute("CODE").equals(code)) {
						add = false;
						break;
					}
				}
				if (add) {
					SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featTot, schema);
					result.add(fit.buildFeature(null));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			totIt.close();
		}
		return DataUtilities.collection(result);
	}

	/**
	 * Apply the densification process with a Parameter file instead of a
	 * 
	 * @param splitZone
	 * @param parcelCollection
	 * @param tmpFile
	 * @param mupFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelDensification(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile, File mupFile,
			SimpluParametersJSON p, File rootFile) throws Exception {
		return parcelDensification(splitZone, parcelCollection, tmpFile, rootFile, mupFile, p.getDouble("areaParcel"), p.getDouble("widParcel"),
				p.getDouble("lenDriveway"));
	}

	/**
	 * Apply the densification process
	 * 
	 * @param splitZone
	 * @param parcelCollection
	 * @param tmpFile
	 * @param rootFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelDensification(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile, File rootFile,
			File mupFile, double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double lenDriveway) throws Exception {

		File pivotFile = new File(tmpFile, "parcelsInbfFlaged.shp");
		Vectors.exportSFC(parcelCollection, pivotFile);
		IFeatureCollection<IFeature> parcelCollec = ShapefileReader.read(pivotFile.getAbsolutePath());

		File geoFile = new File(rootFile, "dataGeo");

		// the little islands (ilots)
		File ilotReduced = new File(tmpFile, "ilotReduced.shp");
		Vectors.exportSFC(FromGeom.getIlots(geoFile, parcelCollection), ilotReduced);
		IFeatureCollection<IFeature> featC = ShapefileReader.read(ilotReduced.getAbsolutePath());

		List<IOrientableCurve> lOC = featC.select(parcelCollec.envelope()).parallelStream().map(x -> FromGeomToLineString.convert(x.getGeom()))
				.collect(ArrayList::new, List::addAll, List::addAll);
		IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

		IFeatureCollection<IFeature> cutedAll = new FT_FeatureCollection<>();
		for (IFeature feat : parcelCollec) {
			// if the parcel is selected for the simulation
			if (feat.getAttribute("DoWeSimul").equals("true") && ((boolean) feat.getAttribute(splitZone))) {
				// if the parcel is bigger than the limit size
				if (feat.getGeom().area() > maximalAreaSplitParcel) {
					// we falg cut the parcel
					IFeatureCollection<IFeature> tmp = generateFlagSplitedParcels(feat, iMultiCurve, tmpFile, rootFile, mupFile,
							maximalAreaSplitParcel, maximalWidthSplitParcel, lenDriveway);
					cutedAll.addAll(tmp);
				} else {
					if ((boolean) feat.getAttribute("IsBuild")) {
						AttributeManager.addAttribute(feat, "DoWeSimul", "false", "String");
						AttributeManager.addAttribute(feat, "eval", "0.0", "String");
					}
					cutedAll.add(feat);
				}
			}
			// if no simulation needed, we ad the normal parcel
			else {
				cutedAll.add(feat);
			}
		}
		// Forcing 2D geometries before saving to shapefile
		for (IFeature feat : cutedAll) {
      feat.setGeom(AdapterFactory.to2DGM_Object(feat.getGeom()));
		}

		File fileTmp = new File(tmpFile, "tmpFlagSplit.shp");
		ShapefileWriter.write(cutedAll, fileTmp.toString(), CRS.decode("EPSG:2154"));

		// TODO that's an ugly thing, i thought i could go without it, but apparently it
		// seems like my only option to get it done
		// return GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut,
		// CRS.decode("EPSG:2154"));

		ShapefileDataStore sds = new ShapefileDataStore(fileTmp.toURI().toURL());
		SimpleFeatureCollection parcelFlaged = sds.getFeatureSource().getFeatures();
		sds.dispose();

		return parcelFlaged;
	}

	/**
	 * overload to directly put a parameter file
	 * 
	 * @param splitZone
	 * @param parcels
	 * @param tmpFile
	 * @param zoningFile
	 * @param p
	 * @param allOrCell
	 *            if true, all the new parcels in the zone will be set as simulable. If false, nothing is set on those new parcels (we need to check the intersection with cells at
	 *            a different point)
	 * @return the whole parcels
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelTotRecomp(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput,
			SimpluParametersJSON p, boolean allOrCell, File rootFile) throws Exception {

		return parcelTotRecomp(splitZone, parcels, tmpFile, rootFile, mupOutput, p.getDouble("areaParcel"), p.getDouble("widParcel"),
				p.getDouble("lenRoad"), p.getInteger("decompositionLevelWithoutRoad"), allOrCell);
	}

	/**
	 * Merge and recut the to urbanised (AU) zones Cut first the U parcels to keep them unsplited, then split the AU parcel and remerge them all into the original parcel file
	 * 
	 * @param splitZone
	 * @param parcels
	 * @param tmpFile
	 * @param zoningFile
	 * @param maximalArea
	 * @param maximalWidth
	 * @param lenRoad
	 * @param decompositionLevelWithoutRoad
	 * @param allOrCell
	 *            if true, all the new parcels in the zone will be set as simulable. If false, nothing is set on those new parcels (we need to check the intersection with cells at
	 *            a different point)
	 * @return the whole parcels
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelTotRecomp(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File rootFile,
			File mupOutput, double maximalArea, double maximalWidth, double lenRoad, int decompositionLevelWithoutRoad, boolean allOrCell)
			throws Exception {

		// parcel schema for all
		SimpleFeatureType schema = parcels.getSchema();

		// parcels to save for after
		DefaultFeatureCollection savedParcels = new DefaultFeatureCollection();
		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(FromGeom.getZoning(new File(rootFile, "dataRegulation")).toURI().toURL());
		SimpleFeatureCollection featuresZones = DataUtilities.collection(shpDSZone.getFeatureSource().getFeatures());

		Geometry unionParcel = Vectors.unionSFC(parcels);
		String geometryParcelPropertyName = schema.getGeometryDescriptor().getLocalName();

		// get the AU zones from the zoning file
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filterTypeZone = ff.like(ff.property("TYPEZONE"), splitZone);

		Filter filterEmprise = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(unionParcel));
		SimpleFeatureCollection zoneAU = featuresZones.subCollection(filterTypeZone).subCollection(filterEmprise);

		// If no AU zones, we won't bother
		if (zoneAU.isEmpty()) {
			System.out.println("parcelGenZone : no " + splitZone + " zones");
			return parcels;
		}

		// get the insee number
		SimpleFeatureIterator pInsee = parcels.features();
		String insee = (String) pInsee.next().getAttribute("INSEE");
		pInsee.close();

		// all the AU zones
		Geometry geomAU = Vectors.unionSFC(zoneAU);
		DefaultFeatureCollection parcelsInAU = new DefaultFeatureCollection();
		SimpleFeatureIterator parcIt = parcels.features();

		// sort in two different collections, the ones that matters and the ones that
		// doesnt
		try {
			while (parcIt.hasNext()) {
				SimpleFeature feat = parcIt.next();
				if (((Geometry) feat.getDefaultGeometry()).intersects(geomAU)) {
					parcelsInAU.add(feat);
				} else {
					savedParcels.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcIt.close();
		}

		// delete the existing roads from the AU zones
		SimpleFeatureBuilder simpleSFB = new SimpleFeatureBuilder(zoneAU.getSchema());

		DefaultFeatureCollection goOdAu = new DefaultFeatureCollection();
		SimpleFeatureIterator zoneAUIt = zoneAU.features();
		try {
			while (zoneAUIt.hasNext()) {
				SimpleFeature feat = zoneAUIt.next();
				Geometry intersection = Vectors
						.scaledGeometryReductionIntersection(Arrays.asList(((Geometry) feat.getDefaultGeometry()), unionParcel));
				if (!intersection.isEmpty() && intersection.getArea() > 5.0) {
					if (intersection instanceof MultiPolygon) {
						for (int i = 0; i < intersection.getNumGeometries(); i++) {
							simpleSFB.set("the_geom", GeometryPrecisionReducer.reduce(intersection.getGeometryN(i), new PrecisionModel(100)));
							simpleSFB.set("INSEE", insee);
							goOdAu.add(simpleSFB.buildFeature(null));
						}
					} else if (intersection instanceof GeometryCollection) {
						for (int i = 0; i < intersection.getNumGeometries(); i++) {
							Geometry g = intersection.getGeometryN(i);
							if (g instanceof Polygon) {
								simpleSFB.set("the_geom", g.buffer(1).buffer(-1));
								simpleSFB.set("INSEE", insee);
								goOdAu.add(simpleSFB.buildFeature(null));
							}
						}
					} else {
						simpleSFB.set("the_geom", intersection.buffer(1).buffer(-1));
						simpleSFB.set("INSEE", insee);
						goOdAu.add(simpleSFB.buildFeature(null));
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			zoneAUIt.close();
		}
		SimpleFeatureCollection gOOdAU = DataUtilities.collection(goOdAu);
		if (gOOdAU.isEmpty()) {
			System.out.println("parcelGenZone : no " + splitZone + " zones");
			return parcels;
		}
		// if the zone is a leftover (this could be done as a stream. When I'll have time I'll get used to it
		SimpleFeatureIterator itGoOD = gOOdAU.features();
		double totAireGoOD = 0.0;
		try {
			while (itGoOD.hasNext()) {
				totAireGoOD += ((Geometry) itGoOD.next().getDefaultGeometry()).getArea();//FIXME check that
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itGoOD.close();
		}
		if (totAireGoOD < 15) {
			System.out.println("Tot zone is too small to be taken into consideration -- return null");
			return parcels;
		}

		// parts of parcel outside the zone must not be cut by the algorithm and keep
		// their attributes
		// temporary shapefiles that serves to do polygons
		File fParcelsInAU = Vectors.exportSFC(parcelsInAU, new File(tmpFile, "parcelCible.shp"));
		File fZoneAU = Vectors.exportSFC(gOOdAU, new File(tmpFile, "oneAU.shp"));
		geomAU = Vectors.unionSFC(gOOdAU);
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

		SimpleFeatureBuilder sfBuilder = FromGeom.getParcelSplitSFBuilder();
		DefaultFeatureCollection write = new DefaultFeatureCollection();

		geoms: for (Geometry poly : polygons) {
			// if the polygons are not included on the AU zone
			if (!geomAU.buffer(0.01).contains(poly)) {
				sfBuilder.set("the_geom", poly);
				SimpleFeatureIterator parcelIt = parcelsInAU.features();
				boolean isCode = false;
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						if (((Geometry) feat.getDefaultGeometry()).buffer(0.01).contains(poly)) {
							// set those good ol attributes
							sfBuilder.set("CODE", feat.getAttribute("CODE"));
							sfBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
							sfBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
							sfBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
							sfBuilder.set("SECTION", feat.getAttribute("SECTION"));
							sfBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
							sfBuilder.set("INSEE", feat.getAttribute("INSEE"));
							sfBuilder.set("eval", feat.getAttribute("eval"));
							sfBuilder.set("DoWeSimul", feat.getAttribute("DoWeSimul"));
							sfBuilder.set("SPLIT", 0);
							sfBuilder.set("IsBuild", feat.getAttribute("IsBuild"));
							sfBuilder.set("U", feat.getAttribute("U"));
							sfBuilder.set("AU", feat.getAttribute("AU"));
							sfBuilder.set("NC", feat.getAttribute("NC"));
							isCode = true;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
				// if some polygons are inside others but aren't selected they gon be polygonised, we ain't puttin 'em
				if (!isCode) {
					continue geoms;
				}
				write.add(sfBuilder.buildFeature(null));
			}
		}
		String geometryOutputName = "";
		try {
			geometryOutputName = write.getSchema().getGeometryDescriptor().getLocalName();
		} catch (NullPointerException e) {
			// no parts are outside the zones, so we automaticaly set the geo name attribute
			// with the most used one
			geometryOutputName = "the_geom";
		}
		SimpleFeatureIterator it = gOOdAU.features();
		int numZone = 0;

		// mark and add the AU zones to the collection
		try {
			while (it.hasNext()) {
				SimpleFeature zone = it.next();
				// get the insee number for that zone
				// String insee = (String) zone.getAttribute("INSEE");
				sfBuilder.set("CODE", insee + "000" + "New" + numZone + "Section");
				sfBuilder.set("CODE_DEP", insee.substring(0, 2));
				sfBuilder.set("CODE_COM", insee.substring(2, 5));
				sfBuilder.set("COM_ABS", "000");
				sfBuilder.set("SECTION", "New" + numZone + "Section");
				sfBuilder.set("NUMERO", "");
				sfBuilder.set("INSEE", insee);
				sfBuilder.set("eval", "0");
				sfBuilder.set("DoWeSimul", false);
				sfBuilder.set("SPLIT", 1);
				// @warning the AU Parcels are mostly unbuilt, but maybe not?
				sfBuilder.set("IsBuild", false);
				sfBuilder.set("U", false);
				sfBuilder.set("AU", true);
				sfBuilder.set("NC", false);
				// avoid multi geom bugs

				Geometry intersectedGeom = Vectors
						.scaledGeometryReductionIntersection(Arrays.asList((Geometry) zone.getDefaultGeometry(), unionParcel));

				if (!intersectedGeom.isEmpty()) {
					write = Vectors.addSimpleGeometry(sfBuilder, write, geometryOutputName, intersectedGeom);
				} else {
					System.out.println("this intersection is empty");
					write = Vectors.addSimpleGeometry(sfBuilder, write, geometryOutputName, intersectedGeom);
				}
				numZone++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		shpDSZone.dispose();
		SimpleFeatureCollection toSplit = Vectors.delTinyParcels(DataUtilities.collection(write), 5.0);
		double roadEpsilon = 00;
		double noise = 0;
		// Sometimes it bugs (like on Sector NV in Besançon)
		SimpleFeatureCollection splitedAUParcels = splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise, null, lenRoad, false,
				decompositionLevelWithoutRoad, tmpFile);

		// mup output
		ShapefileDataStore mupSDS = new ShapefileDataStore(mupOutput.toURI().toURL());
		SimpleFeatureCollection mupSFC = DataUtilities.collection(mupSDS.getFeatureSource().getFeatures());

		// Finally, put them all features in a same collec
		SimpleFeatureIterator finalIt = splitedAUParcels.features();
		try {
			while (finalIt.hasNext()) {
				SimpleFeature feat = finalIt.next();
				// erase soon to be erased super thin polygons TODO one is in double and have
				// unknown parameters : how to delete this one?
				if (((Geometry) feat.getDefaultGeometry()).getArea() > 5.0) {
					// set if the parcel is simulable or not
					if (allOrCell) {
						// must be contained into the AU zones
						if (geomAU.buffer(1).contains((Geometry) feat.getDefaultGeometry())) {
							double eval = getEvalInParcel(feat, mupSFC);
							if (eval == 0.0) {
								eval = getCloseEvalInParcel(feat, mupSFC);
							}
							feat.setAttribute("DoWeSimul", "true");
							feat.setAttribute("eval", eval);
						} else {
							feat.setAttribute("DoWeSimul", "false");
							feat.setAttribute("eval", "0.0");

						}
					} else {
						if (isParcelInCell(feat, mupSFC)) {
							feat.setAttribute("DoWeSimul", "true");
							feat.setAttribute("eval", getEvalInParcel(feat, mupSFC));
						} else {
							feat.setAttribute("DoWeSimul", "false");
							feat.setAttribute("eval", "0.0");
						}
					}

					SimpleFeatureBuilder finalParcelBuilder = FromGeom.setSFBParcelWithFeat(feat, schema);

					if (feat.getAttribute("CODE") == null) {
						finalParcelBuilder = FromGeom.setSFBParDefaut(feat, schema, geometryOutputName);
					}
					savedParcels.add(finalParcelBuilder.buildFeature(null));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			finalIt.close();
		}
		mupSDS.dispose();
		SimpleFeatureCollection result = Vectors.delTinyParcels(DataUtilities.collection(savedParcels), 5.0);

		Vectors.exportSFC(result, new File(tmpFile, "parcelFinal.shp"));

		return result;

	}

	public static String normalizeNameBigZone(String nameZone) throws Exception {
		switch (nameZone) {
		case "U":
		case "ZC":
		case "C":
			return "U";
		case "AU":
			return "AU";
		case "N":
		case "A":
		case "NC":
		case "ZNC":
			return "NC";
		}
		throw new Exception("unknown big zone name");
	}

	/**
	 * select the location for the wanted parcel recomposition method
	 * 
	 * @param splitZone
	 * @param parcelCollection
	 * @param tmpFile
	 * @param zoningFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection setRecompositionProcesssus(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile,
			File mupOutput, File rootFile, SimpluParametersJSON p, String typeOfRecomp, boolean dontTouchUZones) throws Exception {

		boolean goOn = false;

		splitZone = normalizeNameBigZone(splitZone);

		List<String> parcelToNotAdd = new ArrayList<String>();
		File paramFile = new File(rootFile, "paramFolder");
		File locationBuildingType = new File(paramFile, "locationBuildingType");
		File profileBuildingType = new File(paramFile, "profileBuildingType");
		// séparation entre les différentes zones
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		List<String> listZones = SimuTool.getLocationParamNames(locationBuildingType, p);

		List<String> listZonesOneSector = new ArrayList<String>();
		List<String> listZonesTwoSector = new ArrayList<String>();
		for (String stringParam : listZones) {
			if (stringParam.split("-").length == 2) {
				listZonesTwoSector.add(stringParam);
			} else {
				listZonesOneSector.add(stringParam);
			}
		}
		// split into zones to make correct parcel recomposition
		for (String stringParam : listZonesTwoSector) {
			if (stringParam.endsWith(".json")) {
				System.out.println("for line " + stringParam);
				SimpluParametersJSON pLoc = new SimpluParametersJSON(p);
				pLoc.add(new SimpluParametersJSON(new File(locationBuildingType, stringParam)));
				// @simplification : as only one BuildingType is set per zones, we select the
				// type that is the most represented
				BuildingType type = RepartitionBuildingType.getBiggestRepartition(pLoc);
				SimpluParametersJSON pBuildingType = new SimpluParametersJSON(p);
				pBuildingType.add(RepartitionBuildingType.getParamBuildingType(profileBuildingType, type));
				stringParam = SimuTool.cleanSectorName(stringParam);
				// two specifications
				if (stringParam.split("-").length == 2 && splitZone.equals(stringParam.split("-")[1])) {
					SimpleFeatureCollection typoed = getParcelByTypo(stringParam.split("-")[0], parcelCollection, rootFile);
					SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam.split("-")[1], typoed, rootFile);
					if (bigZoned.size() > 0) {
						parcelToNotAdd = dontAddParcel(parcelToNotAdd, bigZoned);
						System.out.println("we cut the parcels with " + type + " parameters");
						result = addAllParcels(result,
								runParcelRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, dontTouchUZones, rootFile, typeOfRecomp));
						// THAT'S AN UGLY PATCH, BUT HAS TO TAKE CARE OF GEOM ERRORS TODO find somathing nices
						if (bigZoned.size() > 2) {
							break;
						} else {
							System.out.println("too small collec, we try to cut em with other parameters");
							goOn = true;
						}
					}
				}
			}
		}
		if (result.isEmpty() || goOn) {
			SimpleFeatureCollection def = new DefaultFeatureCollection();
			// only one specification
			for (String stringParam : listZonesOneSector) {
				if (stringParam.endsWith(".json")) {
					System.out.println("one sector attribute");
					System.out.println("for line " + stringParam);
					SimpluParametersJSON pLoc = new SimpluParametersJSON(p);
					pLoc.add(new SimpluParametersJSON(new File(locationBuildingType, stringParam)));
					// @simplification : as only one BuildingType is set per zones, we select the
					// type that is the most represented
					BuildingType type = RepartitionBuildingType.getBiggestRepartition(pLoc);
					SimpluParametersJSON pBuildingType = new SimpluParametersJSON(p);
					pBuildingType.add(RepartitionBuildingType.getParamBuildingType(profileBuildingType, type));

					stringParam = SimuTool.cleanSectorName(stringParam);

					if (stringParam.equals("periUrbain") || stringParam.equals("rural") || stringParam.equals("banlieue")
							|| stringParam.equals("centre")) {
						SimpleFeatureCollection typoed = getParcelByTypo(stringParam, parcelCollection, rootFile);
						if (typoed.size() > 0) {
							parcelToNotAdd = dontAddParcel(parcelToNotAdd, typoed);
							System.out.println("we cut the parcels with " + type + " parameters");
							def = runParcelRecomp(splitZone, typoed, tmpFile, mupOutput, pBuildingType, dontTouchUZones, rootFile, typeOfRecomp);
							if (typoed.size() > 2) {
								break;
							} else {
								System.out.println("too small collec, we try to cut em with other parameters");
							}
						}
					} else {
						if (splitZone.equals(stringParam)) {
							SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam, parcelCollection, rootFile);
							if (bigZoned.size() > 0) {
								parcelToNotAdd = dontAddParcel(parcelToNotAdd, bigZoned);
								System.out.println("we cut the parcels with " + type + " parameters");
								def = runParcelRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, dontTouchUZones, rootFile,
										typeOfRecomp);
							}
						}
					}
				}
			}
			result = addAllParcels(result, def);
		}
		SimpleFeatureCollection realResult = completeParcelMissing(parcelCollection, DataUtilities.collection(result), parcelToNotAdd);
		return realResult;
	}

	public static SimpleFeatureCollection runParcelRecomp(String splitZone, SimpleFeatureCollection bigZoned, File tmpFile, File mupOutput,
			SimpluParametersJSON pBuildingType, boolean dontTouchUZones, File rootFile, String typeOfRecomp) throws Exception {
		switch (typeOfRecomp) {
		case "densification":
			return parcelDensification(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, rootFile);
		case "partRecomp":
			return parcelPartRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, dontTouchUZones, rootFile);
		case "totRecomp":
			return parcelTotRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, pBuildingType.getBoolean("allZone"), rootFile);
		}
		throw new FileNotFoundException("I didn't get the Recomp order");

	}

	private static List<String> dontAddParcel(List<String> parcelToNotAdd, SimpleFeatureCollection bigZoned) {

		SimpleFeatureIterator feat = bigZoned.features();
		try {
			while (feat.hasNext()) {
				parcelToNotAdd.add((String) feat.next().getAttribute("CODE"));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			feat.close();
		}
		return parcelToNotAdd;
	}

	public static SimpleFeatureCollection parcelPartRecomp(String typeZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput,
			SimpluParametersJSON p, boolean dontTouchUZones, File rootFile) throws Exception {
		return parcelPartRecomp(typeZone, parcels, tmpFile, rootFile, mupOutput, p.getDouble("areaParcel"), p.getDouble("widParcel"),
				p.getDouble("lenRoad"), p.getInteger("decompositionLevelWithoutRoad"), dontTouchUZones);
	}

	public static SimpleFeatureCollection parcelPartRecomp(String typeZone, SimpleFeatureCollection parcels, File tmpFile, File rootFile,
			File mupOutput, double maximalArea, double maximalWidth, double roadWidth, int decompositionLevelWithoutRoad, boolean dontTouchUZones)
			throws Exception {
		File geoFile = new File(rootFile, "dataGeo");
		Geometry emprise = Vectors.unionSFC(parcels);

		DefaultFeatureCollection parcelResult = new DefaultFeatureCollection();
		parcelResult.addAll(parcels);

		ShapefileDataStore shpDSCells = new ShapefileDataStore(mupOutput.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();
		DefaultFeatureCollection parcelToMerge = new DefaultFeatureCollection();

		// city information
		ShapefileDataStore shpDSCities = new ShapefileDataStore(FromGeom.getCommunities(geoFile).toURI().toURL());
		SimpleFeatureCollection citiesSFS = shpDSCities.getFeatureSource().getFeatures();

		////////////////
		// first step : round of selection of the intersected parcels
		////////////////
		// dontTouchUZones is very ugly but i was very tired of it
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if (dontTouchUZones) {
					if (((boolean) parcel.getAttribute(typeZone)) && !((boolean) parcel.getAttribute("U"))) {
						if (parcel.getAttribute("DoWeSimul").equals("true")) {
							parcelToMerge.add(parcel);
							parcelResult.remove(parcel);
						}
					}
				} else {
					if ((boolean) parcel.getAttribute(typeZone)) {
						if (parcel.getAttribute("DoWeSimul").equals("true")) {
							parcelToMerge.add(parcel);
							parcelResult.remove(parcel);
						}
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		Vectors.exportSFC(DataUtilities.collection(parcelToMerge), new File(tmpFile, "step1.shp"));
		System.out.println("done step 1");

		////////////////
		// second step : merge of the parcel that touches themselves by lil island
		////////////////

		DefaultFeatureCollection mergedParcels = new DefaultFeatureCollection();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");

		sfTypeBuilder.setName("toSplit");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.add("SPLIT", Integer.class);
		sfTypeBuilder.add("Section", Integer.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		SimpleFeatureBuilder sfBuilderSimple = FromGeom.getBasicSFB();

		Geometry multiGeom = Vectors.unionSFC(parcelToMerge);
		for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
			sfBuilder.add(multiGeom.getGeometryN(i));
			sfBuilder.set("Section", i);
			mergedParcels.add(sfBuilder.buildFeature(null));
		}

		SimpleFeatureCollection forSection = DataUtilities.collection(mergedParcels);

		Vectors.exportSFC(forSection, new File(tmpFile, "step2.shp"));
		System.out.println("done step 2");

		////////////////
		// third step : cuting of the parcels
		////////////////

		SimpleFeatureIterator bigParcelIt = mergedParcels.features();
		DefaultFeatureCollection cutParcels = new DefaultFeatureCollection();
		try {
			while (bigParcelIt.hasNext()) {
				SimpleFeature feat = bigParcelIt.next();
				// if the parcel is bigger than the limit size
				if (((Geometry) feat.getDefaultGeometry()).getArea() > maximalArea) {
					// we cut the parcel
					feat.setAttribute("SPLIT", 1);
					SimpleFeatureIterator it = splitParcels(feat, maximalArea, maximalWidth, 0, 0, null, roadWidth, false,
							decompositionLevelWithoutRoad, tmpFile, false).features();
					while (it.hasNext()) {
						SimpleFeature f = it.next();
						cutParcels.add(sfBuilderSimple.buildFeature(null, new Object[] { f.getDefaultGeometry() }));
					}
					it.close();
				} else {
					cutParcels.add(sfBuilderSimple.buildFeature(null, new Object[] { feat.getDefaultGeometry() }));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			bigParcelIt.close();
		}
		Vectors.exportSFC(DataUtilities.collection(cutParcels), new File(tmpFile, "step3.shp"));
		System.out.println("done step 3");

		////////////////
		// fourth step : selection of the parcels intersecting the cells
		////////////////

		int i = 0;
		SimpleFeatureIterator parcelFinal = cutParcels.features();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(parcelResult.getSchema());

		try {
			while (parcelFinal.hasNext()) {
				SimpleFeature parcel = parcelFinal.next();
				featureBuilder.add(parcel.getDefaultGeometry());

				// we get the city info
				String insee = FromGeom.getInseeFromParcel(citiesSFS, parcel);

				featureBuilder.set("INSEE", insee);
				featureBuilder.set("CODE_DEP", insee.substring(0, 2));
				featureBuilder.set("CODE_COM", insee.substring(2, 5));

				// that takes time but it's the best way I've found to set a correct section
				// number (to look at the step 2 polygons)
				String sec = "Default";
				SimpleFeatureIterator sectionIt = forSection.features();
				try {
					while (sectionIt.hasNext()) {
						SimpleFeature feat = sectionIt.next();
						if (((Geometry) feat.getDefaultGeometry()).intersects((Geometry) parcel.getDefaultGeometry())) {
							sec = String.valueOf(feat.getAttribute("Section"));
							break;
						}
					}

				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					sectionIt.close();
				}

				String section = "newSection" + sec + "Natural";

				featureBuilder.set("SECTION", section);
				featureBuilder.set("NUMERO", i);

				featureBuilder.set("CODE", insee + "000" + section + i);
				featureBuilder.set("COM_ABS", "000");

				featureBuilder.set("IsBuild", isParcelBuilt(parcel, emprise, geoFile));

				featureBuilder.set("U", false);
				featureBuilder.set("AU", false);
				featureBuilder.set("NC", true);

				if (isParcelInCell(parcel, cellsSFS)) {
					featureBuilder.set("DoWeSimul", "true");
					featureBuilder.set("eval", getEvalInParcel(parcel, mupOutput));
				} else {
					featureBuilder.set("DoWeSimul", "false");
					featureBuilder.set("eval", 0);
				}

				parcelResult.add(featureBuilder.buildFeature(String.valueOf(i++)));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelFinal.close();
		}
		shpDSCells.dispose();
		shpDSCities.dispose();
		shpDSCells.dispose();

		SimpleFeatureCollection result = Vectors.delTinyParcels(parcelResult, 10.0);

		Vectors.exportSFC(result, new File(tmpFile, "step4.shp"));

		return result;
	}

	/**
	 * overlaod to allow a filter that unselect the features that musn't be cuted
	 * 
	 * @param parcelIn
	 * @param filterFile
	 * @param tmpFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn, File filterFile, File tmpFile,
			SimpluParametersJSON p) throws Exception {

		ShapefileDataStore morphoSDS = new ShapefileDataStore(filterFile.toURI().toURL());
		SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
		Geometry morphoUnion = Vectors.unionSFC(morphoSFC);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(parcelIn.getSchema().getGeometryDescriptor().getLocalName());
		Filter filter = ff.intersects(pName, ff.literal(morphoUnion));

		morphoSDS.dispose();
		return generateSplitedParcels(parcelIn.subCollection(filter), tmpFile, p);
	}

	// public static SimpleFeatureCollection
	// generateFlagSplitedParcels(SimpleFeatureCollection featColl,
	// IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, File tmpFile,
	// Parameters p) throws NoSuchAuthorityCodeException, FactoryException,
	// Exception {
	//
	// DefaultFeatureCollection collec = new DefaultFeatureCollection();
	// SimpleFeatureIterator it = featColl.features();
	//
	// try {
	// while (it.hasNext()) {
	// SimpleFeature feat = it.next();
	// collec.addAll(generateFlagSplitedParcels(feat, iMultiCurve, geoFile, tmpFile,
	// p));
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// it.close();
	// }
	//
	// return collec;
	// }
	//
	// public static SimpleFeatureCollection
	// generateFlagSplitedParcels(SimpleFeature feat, IMultiCurve<IOrientableCurve>
	// iMultiCurve, File geoFile, File tmpFile, Parameters p)
	// throws Exception {
	// return generateFlagSplitedParcels(feat, iMultiCurve, geoFile, tmpFile,
	// p.getDouble("maximalAreaSplitParcel"),
	// p.getDouble("maximalWidthSplitParcel"),
	// p.getDouble("lenDriveway"));
	// }
	//
	// public static SimpleFeatureCollection
	// generateFlagSplitedParcels(SimpleFeature feat, IMultiCurve<IOrientableCurve>
	// iMultiCurve, File geoFile, File tmpFile,
	// Double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double
	// lenDriveway) throws Exception {
	//
	// return
	// generateFlagSplitedParcels(GeOxygeneGeoToolsTypes.convert2IFeature(feat),
	// iMultiCurve, geoFile, tmpFile, maximalAreaSplitParcel,
	// maximalWidthSplitParcel,
	// lenDriveway);
	//
	// }

	public static IFeatureCollection<IFeature> generateFlagSplitedParcels(IFeature ifeat, IMultiCurve<IOrientableCurve> iMultiCurve, File tmpFile,
			File rootFile, File outMupFile, Double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double lenDriveway) throws Exception {
		DirectPosition.PRECISION = 3;
		IFeatureCollection<IFeature> batiLargeCollec = ShapefileReader.read(FromGeom.getBuild(new File(rootFile, "dataGeo")).getAbsolutePath());
		IFeatureCollection<IFeature> batiCollec = new FT_FeatureCollection<>();
		batiCollec.addAll(batiLargeCollec.select(ifeat.getGeom()));

		IGeometry geom = ifeat.getGeom();

		// what would that be for?
		IDirectPosition dp = new DirectPosition(0, 0, 0); // geom.centroid();
		geom = geom.translate(-dp.getX(), -dp.getY(), 0);

		List<IOrientableSurface> surfaces = FromGeomToSurface.convertGeom(geom);
		FlagParcelDecomposition fpd = new FlagParcelDecomposition((IPolygon) surfaces.get(0), batiCollec, maximalAreaSplitParcel,
				maximalWidthSplitParcel, lenDriveway, iMultiCurve);
		IFeatureCollection<IFeature> decomp = fpd.decompParcel(0);
		IFeatureCollection<IFeature> ifeatCollOut = new FT_FeatureCollection<>();
		long numParcelle = Math.round(Math.random() * 10000);

		// may we need to normal cut it?
		if (decomp.size() == 1 && isArt3AllowsIsolatedParcel(decomp.get(0), rootFile)) {
			System.out.println("normal decomp instead of flagg decomp allowed");
			File superTemp = Vectors
					.exportSFC(
							splitParcels(GeOxygeneGeoToolsTypes.convert2SimpleFeature(ifeat, CRS.decode("EPSG:2154"),true), maximalAreaSplitParcel,
									maximalWidthSplitParcel, 0, 0, iMultiCurve, 0, false, 5, tmpFile, false),
							new File(tmpFile, "normalCutedParcel.shp"));
			decomp = ShapefileReader.read(superTemp.getAbsolutePath());
		}

		for (IFeature newParcel : decomp) {
			// impeach irregularities
			newParcel.setGeom(newParcel.getGeom().buffer(0.5).buffer(-0.5));

			String newCodeDep = (String) ifeat.getAttribute("CODE_DEP");
			String newCodeCom = (String) ifeat.getAttribute("CODE_COM");
			String newSection = (String) ifeat.getAttribute("SECTION") + "div";
			String newNumero = String.valueOf(numParcelle++);
			String newCode = newCodeDep + newCodeCom + "000" + newSection + newNumero;
			AttributeManager.addAttribute(newParcel, "CODE", newCode, "String");
			AttributeManager.addAttribute(newParcel, "CODE_DEP", newCodeDep, "String");
			AttributeManager.addAttribute(newParcel, "CODE_COM", newCodeCom, "String");
			AttributeManager.addAttribute(newParcel, "COM_ABS", "000", "String");
			AttributeManager.addAttribute(newParcel, "SECTION", newSection, "String");
			AttributeManager.addAttribute(newParcel, "NUMERO", newNumero, "String");
			AttributeManager.addAttribute(newParcel, "INSEE", newCodeDep + newCodeCom, "String");

			double eval = 0.0;
			boolean bati = false;
			boolean simul = false;
			boolean u = false;
			boolean au = false;
			boolean nc = false;

			// we put a small buffer because a lot of houses are just biting neighborhood
			// parcels
			for (IFeature batiIFeat : batiCollec) {
				if (newParcel.getGeom().buffer(-1.5).intersects(batiIFeat.getGeom())) {
					bati = true;
				}
			}

			// we decide here if we want to simul that parcel
			if (!bati) {
				// if the parcels hasn't been decomposed
				if (decomp.size() == 1) {
					// has access to road, we put it whole to simul
					if (fpd.hasRoadAccess((IPolygon) surfaces.get(0))) {
						simul = true;
					}
					// doesn't has to be connected to the road to be urbanized
					else if (isArt3AllowsIsolatedParcel(newParcel, rootFile)) {
						simul = true;
					}
				} else {
					if (ParcelFonction.isParcelInCell(newParcel, outMupFile)) {
						simul = true;
					}
				}
			}

			List<String> zones = FromGeom.parcelInBigZone(newParcel, FromGeom.getZoning(new File(rootFile, "dataRegulation")));

			if (zones.contains("U")) {
				u = true;
			}
			if (zones.contains("AU")) {
				au = true;
			}
			if (zones.contains("NC")) {
				nc = true;
			}

			if (simul) {
				eval = getEvalInParcel(newParcel, outMupFile);
			}

			AttributeManager.addAttribute(newParcel, "eval", eval, "String");
			AttributeManager.addAttribute(newParcel, "DoWeSimul", simul, "String");
			AttributeManager.addAttribute(newParcel, "IsBuild", bati, "String");
			AttributeManager.addAttribute(newParcel, "U", u, "String");
			AttributeManager.addAttribute(newParcel, "AU", au, "String");
			AttributeManager.addAttribute(newParcel, "NC", nc, "String");

			ifeatCollOut.add(newParcel);
		}
		return decomp;

	}

	/**
	 * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	 * 
	 * @param parcelIn
	 *            : Parcels collection of simple features
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeature parcelIn, File tmpFile, SimpluParametersJSON p) throws Exception {

		// splitting method option

		double maximalWidth = p.getDouble("maximalWidthSplitParcel");
		double maximalArea = p.getDouble("maximalAreaSplitParcel");
		int decompositionLevelWithoutRoad = p.getInteger("decompositionLevelWithoutRoad");

		return generateSplitParcels(parcelIn, tmpFile, maximalArea, maximalWidth, 0, null, p.getDouble("lenRoad"), decompositionLevelWithoutRoad,
				false);
	}

	/**
	 * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	 * 
	 * @param parcelsIn
	 *            : Parcels collection of simple features
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelsIn, File tmpFile, SimpluParametersJSON p)
			throws Exception {

		// splitting method option

		double roadEpsilon = 0.5;
		double maximalArea = p.getDouble("maximalAreaSplitParcel");
		double maximalWidth = p.getDouble("maximalWidthSplitParcel");

		// Exterior from the UrbanBlock if necessary or null
		IMultiCurve<IOrientableCurve> extBlock = null;
		// Roads are created for this number of decomposition level
		int decompositionLevelWithoutRoad = 2;
		// Road width
		double roadWidth = 5.0;
		// Boolean forceRoadaccess
		boolean forceRoadAccess = true;
		return generateSplitedParcels(parcelsIn, tmpFile, maximalArea, maximalWidth, roadEpsilon, extBlock, decompositionLevelWithoutRoad, roadWidth,
				forceRoadAccess);

	}

	public static SimpleFeatureCollection generateSplitParcels(SimpleFeature parcelIn, File tmpFile, double maximalArea, double maximalWidth,
			double epsilon, IMultiCurve<IOrientableCurve> extBlock, double roadWidth, int decompositionLevelWithoutRoad, boolean forceRoadAccess)
			throws Exception {

		// putting the need of splitting into attribute

		SimpleFeatureBuilder sfBuilder = FromGeom.getParcelSplitSFBuilder();

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();

		String numParcelValue = "";
		if (parcelIn.getAttribute("CODE") != null) {
			numParcelValue = parcelIn.getAttribute("CODE").toString();
		} else if (parcelIn.getAttribute("CODE_DEP") != null) {
			numParcelValue = ((String) parcelIn.getAttribute("CODE_DEP")) + (parcelIn.getAttribute("CODE_COM").toString())
					+ (parcelIn.getAttribute("COM_ABS").toString()) + (parcelIn.getAttribute("SECTION").toString());
		} else if (parcelIn.getAttribute("NUMERO") != null) {
			numParcelValue = parcelIn.getAttribute("NUMERO").toString();
		}
		Object[] attr = { numParcelValue, parcelIn.getAttribute("CODE_DEP"), parcelIn.getAttribute("CODE_COM"), parcelIn.getAttribute("COM_ABS"),
				parcelIn.getAttribute("SECTION"), parcelIn.getAttribute("NUMERO"), parcelIn.getAttribute("INSEE"), parcelIn.getAttribute("eval"),
				parcelIn.getAttribute("DoWeSimul"), 1 };

		sfBuilder.add(parcelIn.getDefaultGeometry());
		toSplit.add(sfBuilder.buildFeature(null, attr));

		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithoutRoad,
				tmpFile);

	}

	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelsIn, File tmpFile, double maximalArea,
			double maximalWidth, double epsilon, IMultiCurve<IOrientableCurve> extBlock, int decompositionLevelWithoutRoad, double roadWidth,
			boolean forceRoadAccess) throws Exception {

		///////
		// putting the need of splitting into attribute
		///////

		// create a new collection
		SimpleFeatureBuilder sfBuilder = FromGeom.getParcelSplitSFBuilder();
		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();

		// iterate to get all the concerned parcels
		int i = 0;
		SimpleFeatureIterator parcelIt = parcelsIn.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				String numParcelValue = "";
				if (feat.getAttribute("CODE") != null) {
					numParcelValue = feat.getAttribute("CODE").toString();
				} else if (feat.getAttribute("CODE_DEP") != null) {
					numParcelValue = ((String) feat.getAttribute("CODE_DEP")) + (feat.getAttribute("CODE_COM").toString())
							+ (feat.getAttribute("COM_ABS").toString()) + (feat.getAttribute("SECTION").toString());
				} else if (feat.getAttribute("NUMERO") != null) {
					numParcelValue = feat.getAttribute("NUMERO").toString();
				}
				Object[] attr = { numParcelValue, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"),
						feat.getAttribute("SECTION"), feat.getAttribute("NUMERO"), feat.getAttribute("INSEE"), feat.getAttribute("eval"),
						feat.getAttribute("DoWeSimul"), 0 };

				if (((Geometry) feat.getDefaultGeometry()).getArea() > maximalArea) {
					attr[9] = 1;
				}
				sfBuilder.add(feat.getDefaultGeometry());
				toSplit.add(sfBuilder.buildFeature(String.valueOf(i), attr));
				i = i + 1;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0.0, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithoutRoad,
				tmpFile);
	}

	public static SimpleFeatureCollection splitParcels(SimpleFeature toSplit, double maximalArea, double maximalWidth, double roadEpsilon,
			double noise, IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithoutRoad,
			File tmpFile, boolean addArg) throws Exception {
		DefaultFeatureCollection in = new DefaultFeatureCollection();
		in.add(toSplit);
		return splitParcels(DataUtilities.collection(in), maximalArea, maximalWidth, roadEpsilon, noise, extBlock, roadWidth, forceRoadAccess,
				decompositionLevelWithoutRoad, tmpFile, addArg);

	}

	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon,
			double noise, IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithoutRoad,
			File tmpFile) throws Exception {

		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise, extBlock, roadWidth, forceRoadAccess,
				decompositionLevelWithoutRoad, tmpFile, true);
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
	 * @thro)ws Exception
	 */
	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon,
			double noise, IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithoutRoad,
			File tmpFile, boolean addArg) throws Exception {

		String attNameToTransform = "SPLIT";
		// TODO po belle conversion
		File shpIn = new File(tmpFile, "temp-In.shp");
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

			int numParcelle = 1;
			int decompositionLevelWithRoad = OBBBlockDecomposition.howManyIt(pol, noise, forceRoadAccess, maximalArea, maximalWidth)
					- decompositionLevelWithoutRoad;
			if (decompositionLevelWithRoad < 0) {
				decompositionLevelWithRoad = 0;
			}
			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, roadEpsilon, extBlock, roadWidth, forceRoadAccess,
					decompositionLevelWithRoad);

			try {
				IFeatureCollection<IFeature> featCollDecomp = obb.decompParcel(noise);
				for (IFeature featDecomp : featCollDecomp) {
					// MAJ du numéro de la parcelle
					IFeature newFeat = new DefaultFeature(featDecomp.getGeom());
					if (addArg) {
						String newCodeDep = (String) feat.getAttribute("CODE_DEP");
						String newCodeCom = (String) feat.getAttribute("CODE_COM");
						String newSection = (String) feat.getAttribute("SECTION");
						String newNumero = String.valueOf(numParcelle++);
						String newCode = newCodeDep + newCodeCom + "000" + newSection + newNumero;
						AttributeManager.addAttribute(newFeat, "CODE", newCode, "String");
						AttributeManager.addAttribute(newFeat, "CODE_DEP", newCodeDep, "String");
						AttributeManager.addAttribute(newFeat, "CODE_COM", newCodeCom, "String");
						AttributeManager.addAttribute(newFeat, "COM_ABS", "000", "String");
						AttributeManager.addAttribute(newFeat, "SECTION", newSection, "String");
						AttributeManager.addAttribute(newFeat, "NUMERO", newNumero, "String");
						AttributeManager.addAttribute(newFeat, "INSEE", newCodeDep + newCodeCom, "String");
						AttributeManager.addAttribute(newFeat, "eval", "0", "String");
						AttributeManager.addAttribute(newFeat, "DoWeSimul", false, "String");
						AttributeManager.addAttribute(newFeat, "IsBuild", feat.getAttribute("IsBuild"), "String");
						AttributeManager.addAttribute(newFeat, "U", feat.getAttribute("U"), "String");
						AttributeManager.addAttribute(newFeat, "AU", feat.getAttribute("AU"), "String");
						AttributeManager.addAttribute(newFeat, "NC", feat.getAttribute("NC"), "String");
					}
					ifeatCollOut.add(newFeat);
				}
			} catch (NullPointerException n) {
				System.out.println("erreur sur le split pour la parcelle " + String.valueOf(feat.getAttribute("CODE")));
				IFeature featTemp = feat.cloneGeom();
				ifeatCollOut.add(featTemp);
			}
		}
		if (ifeatColl.isEmpty()) {
			System.out.println("nothing cuted ");
			return toSplit;
		}
		File fileOut = new File(tmpFile, "tmp_split.shp");
		ShapefileWriter.write(ifeatCollOut, fileOut.toString(), CRS.decode("EPSG:2154"));

		// TODO that's an ugly thing, i thought i could go without it, but apparently it
		// seems like my only option to get it done
		// return GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut,
		// CRS.decode("EPSG:2154"));

		ShapefileDataStore sds = new ShapefileDataStore(fileOut.toURI().toURL());
		SimpleFeatureCollection parcelOut = DataUtilities.collection(sds.getFeatureSource().getFeatures());
		sds.dispose();
		return parcelOut;
	}

	/**
	 * return true if there's a building on the input parcel
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public static boolean isParcelBuilt(SimpleFeature parcelIn, File geoFile) throws Exception {

		ShapefileDataStore shpDSBati = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		batiCollection = Vectors.snapDatas(batiCollection, (Geometry) parcelIn.getDefaultGeometry());
		Geometry emprise = Vectors.unionSFC(batiCollection);

		return isParcelBuilt(parcelIn, emprise, geoFile);
	}

	/**
	 * return true if there's a building on the input parcel
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public static boolean isParcelBuilt(SimpleFeature parcelIn, Geometry emprise, File geoFile) throws Exception {

		// couche de batiment
		ShapefileDataStore shpDSBati = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
		SimpleFeatureCollection batiCollection = shpDSBati.getFeatureSource().getFeatures();
		// on snap la couche de batiment et la met dans une géométrie unique
		Geometry batiUnion = Vectors.unionSFC(Vectors.snapDatas(batiCollection, emprise));
		shpDSBati.dispose();

		if (((Geometry) parcelIn.getDefaultGeometry()).contains(batiUnion)) {
			return true;
		}
		return false;
	}

	public static boolean isAlreadyBuilt(Feature feature, File geoFile) throws IOException {
		boolean isContent = false;
		ShapefileDataStore bati_datastore = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());
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

	public static Double getEvalInParcel(IFeature parcel, File outMup)
			throws NoSuchAuthorityCodeException, ParseException, FactoryException, IOException, Exception {
		if (outMup == null) {
			return 0.0;
		}

		return getEvalInParcel(GeOxygeneGeoToolsTypes.convert2SimpleFeature(parcel, CRS.decode("EPSG:2154"),true), outMup);
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
	public static Double getEvalInParcel(SimpleFeature parcel, File outMup)
			throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {

		ShapefileDataStore cellsSDS = new ShapefileDataStore(outMup.toURI().toURL());
		SimpleFeatureCollection cellsCollection = DataUtilities.collection(cellsSDS.getFeatureSource().getFeatures());
		Double result = getEvalInParcel(parcel, cellsCollection);
		cellsSDS.dispose();
		return result;
	}

	public static Double getEvalInParcel(SimpleFeature parcel, SimpleFeatureCollection mupSFC) {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = mupSFC.getSchema().getGeometryDescriptor().getLocalName();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(parcel.getDefaultGeometry()));
		SimpleFeatureCollection onlyCells = DataUtilities.collection(mupSFC.subCollection(inter));
		double bestEval = 0.0;
		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			SimpleFeatureIterator onlyCellIt = onlyCells.features();
			try {
				while (onlyCellIt.hasNext()) {
					SimpleFeature multiCell = onlyCellIt.next();
					if (multiCell != null) {
					  Double eval = (Double) multiCell.getAttribute("eval");
					  if (eval != null) {
					    bestEval = Math.max(bestEval, eval.doubleValue());
					  } else {
					    System.out.println("Null eval for " + multiCell);
					  }
					}
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				onlyCellIt.close();
			}
		}

		// si jamais le nom est déjà généré

		// sort collection with evaluation
		// PropertyName pN = ff.property("eval");
		// SortByImpl sbt = new SortByImpl(pN,
		// org.opengis.filter.sort.SortOrder.DESCENDING);
		// SimpleFeatureCollection collectOut = new
		// SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		//
		// moyenneEval(collectOut);

		return bestEval;
	}

	/**
	 * If we want an evaluation for a parcel that is not intersected by a MUP-City cell, we will increasly seek for a cell around The seeking is made 5 meters by 5 meters and the
	 * first cell found is chosen The evaluation of this cell is then sent
	 * 
	 * @param parcel
	 * @param mupSFC
	 * @return
	 */
	public static Double getCloseEvalInParcel(SimpleFeature parcel, SimpleFeatureCollection mupSFC) {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = mupSFC.getSchema().getGeometryDescriptor().getLocalName();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(((Geometry) parcel.getDefaultGeometry()).buffer(100.0)));
		SimpleFeatureCollection onlyCells = mupSFC.subCollection(inter);
		Double bestEval = 0.0;

		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			double distBuffer = 0.0;
			// we randomly decide that the cell cannot be further than 100 meters
			while (distBuffer < 100) {
				Geometry geometryUp = ((Geometry) parcel.getDefaultGeometry()).buffer(distBuffer);
				SimpleFeatureIterator onlyCellIt = onlyCells.features();
				try {
					while (onlyCellIt.hasNext()) {
						SimpleFeature cell = onlyCellIt.next();
						if (geometryUp.intersects((Geometry) cell.getDefaultGeometry())) {
							return ((Double) cell.getAttribute("eval"));
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					onlyCellIt.close();
				}
				distBuffer = distBuffer + 5;
			}

		}
		return bestEval;
	}
	
	public static boolean isParcelInCell(IFeature parcelIn, File cellsCollection) throws NoSuchAuthorityCodeException, FactoryException, Exception {
		ShapefileDataStore shpDSCells = new ShapefileDataStore(cellsCollection.toURI().toURL());
		SimpleFeatureCollection cellsSFS = DataUtilities.collection(shpDSCells.getFeatureSource().getFeatures());
		shpDSCells.dispose();
		return isParcelInCell(GeOxygeneGeoToolsTypes.convert2SimpleFeature(parcelIn, CRS.decode("EPSG:2154"),true),cellsSFS);
	}

	public static boolean isParcelInCell(SimpleFeature parcelIn, SimpleFeatureCollection cellsCollection) throws Exception {

		cellsCollection = Vectors.snapDatas(cellsCollection, (Geometry) parcelIn.getDefaultGeometry());

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

	/**
	 * method that compares two set of parcels and export only the ones that are in common Useless for not but will be used to determine the cleaned parcels
	 * 
	 * @param parcelOG
	 * @param parcelToSort
	 * @param parcelOut
	 * @throws IOException
	 */
	public static void diffParcel(File parcelOG, File parcelToSort, File parcelOut) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(parcelToSort.toURI().toURL());
		SimpleFeatureCollection parcelUnclean = sds.getFeatureSource().getFeatures();

		ShapefileDataStore sdsclean = new ShapefileDataStore(parcelOG.toURI().toURL());
		SimpleFeatureCollection parcelClean = sdsclean.getFeatureSource().getFeatures();
		SimpleFeatureIterator itClean = parcelClean.features();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(parcelUnclean.getSchema().getGeometryDescriptor().getLocalName());

		DefaultFeatureCollection result = new DefaultFeatureCollection();
		int i = 0;
		try {
			while (itClean.hasNext()) {
				SimpleFeature clean = itClean.next();

				Filter filter = ff.bbox(pName, clean.getBounds());

				SimpleFeatureIterator itUnclean = parcelUnclean.subCollection(filter).features();
				try {
					while (itUnclean.hasNext()) {
						SimpleFeature unclean = itUnclean.next();
						if (clean.getDefaultGeometry().equals(unclean.getDefaultGeometry())) {
							result.add(unclean);
							break;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itUnclean.close();
				}
				System.out.println(i + " on " + parcelClean.size());
				i++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itClean.close();
		}

		Vectors.exportSFC(result, parcelOut);
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to run on every cities contained into the
	 * parcel file, simulate a single community and automatically cut all parcels regarding to the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param zip
	 *            : Community code that must be simulated.
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File geoFile, File regulFile, File currentFile, boolean preCutParcels) throws Exception {
		return getParcels(geoFile, regulFile, currentFile, new ArrayList<String>());
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to simulate a single community and
	 * automatically cut all parcels regarding to the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param zip
	 *            : Community code that must be simulated.
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File geoFile, File regulFile, File tmpFile, String zip, boolean preCutParcels) throws Exception {
		List<String> lZip = new ArrayList<String>();
		lZip.add(zip);
		return getParcels(geoFile, regulFile, tmpFile, lZip, preCutParcels);
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to automatically cut all parcels regarding to
	 * the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param listZip
	 *            : List of all the communities codes that must be simulated. If empty, we run it on every cities contained into the parcel file
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File geoFile, File regulFile, File tmpFile, List<String> listZip) throws Exception {
		return getParcels(geoFile, regulFile, tmpFile, listZip, true);
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param listZip
	 *            : List of all the communities codes that must be simulated. If empty, we work on every cities contained into the parcel file
	 * @param preCutParcels
	 *            : if cut all parcels regarding to the zoning file
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File geoFile, File regulFile, File tmpFile, List<String> listZip, boolean preCutParcels) throws Exception {

		DirectPosition.PRECISION = 3;

		File result = new File("");
		for (File f : geoFile.listFiles()) {
			if (f.toString().contains("parcel.shp")) {
				result = f;
			}
		}

		ShapefileDataStore parcelSDS = new ShapefileDataStore(result.toURI().toURL());
		SimpleFeatureCollection parcels = parcelSDS.getFeatureSource().getFeatures();

		ShapefileDataStore shpDSBati = new ShapefileDataStore(FromGeom.getBuild(geoFile).toURI().toURL());

		// if we decided to work on a set of cities
		if (!listZip.isEmpty()) {
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			DefaultFeatureCollection df = new DefaultFeatureCollection();
			for (String zip : listZip) {

				// could have been nicer, but Filters are some pain in the socks and doesn't
				// work in I factor the code
				if (zip.length() > 5) {
					// those zip are containing the Section value too
					String codep = zip.substring(0, 2);
					String cocom = zip.substring(2, 5);
					String section = zip.substring(5);

					Filter filterDep = ff.like(ff.property("CODE_DEP"), codep);
					Filter filterCom = ff.like(ff.property("CODE_COM"), cocom);
					Filter filterSection = ff.like(ff.property("SECTION"), section);
					df.addAll(parcels.subCollection(filterDep).subCollection(filterCom).subCollection(filterSection));
				} else {
					String codep = zip.substring(0, 2);
					String cocom = zip.substring(2, 5);
					Filter filterDep = ff.like(ff.property("CODE_DEP"), codep);
					Filter filterCom = ff.like(ff.property("CODE_COM"), cocom);
					df.addAll(parcels.subCollection(filterDep).subCollection(filterCom));
				}
			}
			parcels = DataUtilities.collection(df);
		}

		// if we cut all the parcel regarding to the zoning code
		if (preCutParcels) {
			File tmpParcel = Vectors.exportSFC(parcels, new File(tmpFile, "tmpParcel.shp"));
			File[] polyFiles = { tmpParcel, FromGeom.getZoning(regulFile) };
			List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

			// register to precise every parcel that are in the output
			List<String> codeParcelsTot = new ArrayList<String>();

			// auto parcel feature builder
			SimpleFeatureBuilder sfSimpleBuilder = FromGeom.getSimpleParcelSFBuilder();

			DefaultFeatureCollection write = new DefaultFeatureCollection();

			// for every made up polygons out of zoning and parcels
			for (Geometry poly : polygons) {
				// for every parcels around the polygon

				SimpleFeatureCollection snaped = Vectors.snapDatas(parcels, poly.getBoundary());
				SimpleFeatureIterator parcelIt = snaped.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						// if the polygon part was between that parcel, we add its attribute
						if (((Geometry) feat.getDefaultGeometry()).buffer(1).contains(poly)) {
							sfSimpleBuilder.set("the_geom", GeometryPrecisionReducer.reduce(poly, new PrecisionModel(100)));
							String code = ParcelFonction.makeParcelCode(feat);
							sfSimpleBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
							sfSimpleBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
							sfSimpleBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
							sfSimpleBuilder.set("SECTION", feat.getAttribute("SECTION"));
							String num = (String) feat.getAttribute("NUMERO");
							// if a part has already been added

							if (codeParcelsTot.contains(code)) {
								while (true) {
									num = num + "bis";
									code = code + "bis";
									sfSimpleBuilder.set("NUMERO", num);
									if (!codeParcelsTot.contains(code)) {
										codeParcelsTot.add(code);
										break;
									}
								}
							} else {
								sfSimpleBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
								codeParcelsTot.add(code);
							}
							sfSimpleBuilder.set("CODE", code);
							write.add(sfSimpleBuilder.buildFeature(null));

							// this could be nicer but it doesn't work
							// for (int i = 0; i < codeParcelsTot.size(); i++) {
							// if (codeParcelsTot.get(i).substring(0, 13).equals(code)) {
							// num = num + "bis";
							// code = code + "bis";
							// }
							// }
							// sfSimpleBuilder.set("NUMERO", num);
							// sfSimpleBuilder.set("CODE", code);
							// codeParcelsTot.add(code);
							// write.add(sfSimpleBuilder.buildFeature(null));

						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
			}
			parcels = DataUtilities.collection(write);
		}
		// under the carpet
		ReferencedEnvelope carpet = parcels.getBounds();
		Coordinate[] coord = { new Coordinate(carpet.getMaxX(), carpet.getMaxY()), new Coordinate(carpet.getMaxX(), carpet.getMinY()),
				new Coordinate(carpet.getMinX(), carpet.getMinY()), new Coordinate(carpet.getMinX(), carpet.getMaxY()),
				new Coordinate(carpet.getMaxX(), carpet.getMaxY()) };

		GeometryFactory gf = new GeometryFactory();
		Polygon bbox = gf.createPolygon(coord);
		SimpleFeatureCollection batiSFC = Vectors.snapDatas(shpDSBati.getFeatureSource().getFeatures(), bbox);

		// SimpleFeatureCollection batiSFC =
		// Vectors.snapDatas(shpDSBati.getFeatureSource().getFeatures(),
		// Vectors.unionSFC(parcels));

		SimpleFeatureBuilder sfBuilder = FromGeom.getParcelSFBuilder();

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();

		// int tot = parcels.size();
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			parc: while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				Geometry geom = (Geometry) feat.getDefaultGeometry();
				if (geom.getArea() > 5.0) {
					// put the best cell evaluation into the parcel
					String INSEE = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));
					// say if the parcel intersects a particular zoning type
					boolean u = false;
					boolean au = false;
					boolean nc = false;

					for (String s : FromGeom.parcelInBigZone(feat, FromGeom.getZoning(regulFile))) {
						if (s.equals("AU")) {
							au = true;
						} else if (s.equals("U")) {
							u = true;
						} else if (s.equals("NC")) {
							nc = true;
						} else {
							// if the parcel is outside of the zoning file, we don't keep it
							continue parc;
						}
					}

					Object[] attr = { ParcelFonction.makeParcelCode(feat), feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"),
							feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"), feat.getAttribute("NUMERO"), INSEE, 0, "false",
							FromGeom.isBuilt(feat, batiSFC), u, au, nc };

					sfBuilder.add(geom);

					SimpleFeature feature = sfBuilder.buildFeature(null, attr);
					newParcel.add(feature);
					// System.out.println(i+" on "+tot);
				}
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		parcelSDS.dispose();
		shpDSBati.dispose();

		return Vectors.exportSFC(DataUtilities.collection(newParcel), new File(tmpFile, "parcelProcessed.shp"));
	}

	public static List<String> getCodeParcels(SimpleFeatureCollection parcels) {
		List<String> result = new ArrayList<String>();
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				String code = ((String) feat.getAttribute("CODE"));
				if (code != null && !code.isEmpty()) {
					result.add(code);
				} else {
					result.add(makeParcelCode(feat));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		return result;
	}

	public static IFeatureCollection<IFeature> getParcelByCode(IFeatureCollection<IFeature> parcelles, List<String> parcelsWanted)
			throws IOException {
		IFeatureCollection<IFeature> result = new FT_FeatureCollection<>();
		for (IFeature parcelle : parcelles) {
			for (String s : parcelsWanted) {
				if (s.equals((String) parcelle.getAttribute("CODE"))) {
					result.add(parcelle);
				}
			}
		}
		return result;
	}
	
	
	public static SimpleFeatureCollection getParcelByZip(SimpleFeatureCollection parcelIn , String val) throws IOException {
		SimpleFeatureIterator it = parcelIn.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				String insee = ((String)feat.getAttribute("CODE_DEP")).concat(((String)feat.getAttribute("CODE_COM")));
				if (insee.equals(val)) {
					result.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return result.collection();
	}
	

	private static SimpleFeatureCollection getParcelByBigZone(String zone, SimpleFeatureCollection parcelles, File rootFile) throws IOException {
		ShapefileDataStore zonesSDS = new ShapefileDataStore(FromGeom.getZoning(new File(rootFile, "dataRegulation")).toURI().toURL());
		SimpleFeatureCollection zonesSFCBig = DataUtilities.collection(zonesSDS.getFeatureSource().getFeatures());
		SimpleFeatureCollection zonesSFC = Vectors.cropSFC(zonesSFCBig, parcelles);
		List<String> listZones = new ArrayList<>();
		switch (zone) {
		case "U":
			listZones.add("U");
			listZones.add("ZC");
			break;
		case "AU":
			listZones.add("AU");
			break;
		case "NC":
			listZones.add("A");
			listZones.add("N");
			listZones.add("NC");
			break;
		}

		DefaultFeatureCollection zoneSelected = new DefaultFeatureCollection();
		SimpleFeatureIterator itZonez = zonesSFC.features();
		try {
			while (itZonez.hasNext()) {
				SimpleFeature zones = itZonez.next();
				if (listZones.contains(zones.getAttribute("TYPEZONE"))) {
					zoneSelected.add(zones);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itZonez.close();
		}

		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator it = parcelles.features();
		try {
			while (it.hasNext()) {
				SimpleFeature parcelFeat = it.next();
				SimpleFeatureIterator itZone = zoneSelected.features();
				try {
					while (itZone.hasNext()) {
						SimpleFeature zoneFeat = itZone.next();
						Geometry zoneGeom = (Geometry) zoneFeat.getDefaultGeometry();
						Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
						// if (zoneGeom.intersects(parcelGeom)) {
						//
						// result.add(parcelFeat);
						// break;

						if (zoneGeom.contains(parcelGeom)) {
							result.add(parcelFeat);
						}
						// if the intersection is less than 50% of the parcel, we let it to the other
						// (with the hypothesis that there is only 2 features)
						else if (Vectors.scaledGeometryReductionIntersection(Arrays.asList(parcelGeom, zoneGeom)).getArea() > parcelGeom.getArea()
								/ 2) {
							result.add(parcelFeat);
						}
					}

				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itZone.close();
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		zonesSDS.dispose();
		return DataUtilities.collection(result);
	}

	public static SimpleFeatureCollection getParcelByTypo(String typo, SimpleFeatureCollection parcelles, File rootFile) throws IOException {

		ShapefileDataStore communitiesSDS = new ShapefileDataStore(FromGeom.getCommunities(new File(rootFile, "dataGeo")).toURI().toURL());
		SimpleFeatureCollection communitiesSFCBig = communitiesSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection communitiesSFC = Vectors.cropSFC(communitiesSFCBig, parcelles);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator itParcel = parcelles.features();
		try {
			while (itParcel.hasNext()) {
				SimpleFeature parcelFeat = itParcel.next();
				Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
				// if tiny parcel, we don't care
				if (parcelGeom.getArea() < 5.0) {
					continue;
				}
				Filter filter = ff.like(ff.property("typo"), typo);
				SimpleFeatureIterator itTypo = communitiesSFC.subCollection(filter).features();
				try {
					while (itTypo.hasNext()) {
						SimpleFeature typoFeat = itTypo.next();
						Geometry typoGeom = (Geometry) typoFeat.getDefaultGeometry();

						if (typoGeom.intersects(parcelGeom)) {
							if (typoGeom.contains(parcelGeom)) {
								result.add(parcelFeat);
								break;
							}
							// if the intersection is less than 50% of the parcel, we let it to the other
							// (with the hypothesis that there is only 2 features)
							// else if (parcelGeom.intersection(typoGeom).getArea() > parcelGeom.getArea() /
							// 2) {

							else if (Vectors.scaledGeometryReductionIntersection(Arrays.asList(typoGeom, parcelGeom))
									.getArea() > (parcelGeom.getArea() / 2)) {
								result.add(parcelFeat);
								break;
							} else {
								break;
							}
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itTypo.close();
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
		communitiesSDS.dispose();
		return DataUtilities.collection(result);
	}

	/**
	 * return false if the parcel mandatory needs a contact with the road to be urbanized. return true otherwise TODO haven't done it for the zones because I only found communities
	 * that set the same rule regardless of the zone, but that could be done
	 * 
	 * @param feat
	 *            : the parcel
	 * @param rootFile
	 *            : the rootFile of ArtiScales's project
	 * @return
	 * @throws IOException
	 */
	public static boolean isArt3AllowsIsolatedParcel(IFeature feat, File rootFile) throws IOException {
		// get Insee Number
		String insee = ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));

		int nInsee = 0;
		int nArt3 = 0;
		// get rule file
		CSVReader rule = new CSVReader(new FileReader(new File(rootFile, "dataRegulation/predicate.csv")));

		// seek for attribute numbers
		String[] firstLine = rule.readNext();
		for (int i = 0; i < firstLine.length; i++) {
			String s = firstLine[i];
			if (s.equals("insee")) {
				nInsee = i;
			} else if (s.equals("art_3")) {
				nArt3 = i;
			}
		}

		for (String[] line : rule.readAll()) {
			if (insee.equals(line[nInsee])) {
				if (line[nArt3].equals("1")) {
					rule.close();
					return false;
				} else {
					rule.close();
					return true;
				}
			}
		}

		rule.close();
		return true;
	}
}