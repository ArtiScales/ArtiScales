package fr.ign.cogit.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;

import fields.ArtiScalesParcelFields;
import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.parcelFunction.MarkParcelAttributeFromPosition;
import fr.ign.cogit.parcelFunction.ParcelAttribute;
import fr.ign.cogit.parcelFunction.ParcelCollection;
import fr.ign.cogit.parcelFunction.ParcelGetter;
import fr.ign.cogit.parcelFunction.ParcelState;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import goal.ConsolidationDivision;
import goal.Densification;
import goal.ZoneDivision;

public class ApplyParcelManager {
	public static boolean DEBUG = false;
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
			File mupOutput, File rootFile, File geoFile, File regulFile, SimpluParametersJSON p, String typeOfRecomp, boolean dontTouchUZones)
			throws Exception {

		boolean goOn = false;

		splitZone = ParcelAttribute.normalizeNameFrenchBigZone(splitZone);

		List<String> parcelToNotAdd = new ArrayList<String>();
		File paramFile = new File(rootFile, "paramFolder");
		File locationBuildingType = new File(paramFile, "locationBuildingType");
		File profileBuildingType = new File(paramFile, "profileBuildingType");
		File zoningFile = FromGeom.getZoning(regulFile);

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
					SimpleFeatureCollection typoedParcel = ParcelGetter.getParcelByTypo(stringParam.split("-")[0], parcelCollection, FromGeom.getZoning(regulFile));
					SimpleFeatureCollection bigZonedParcel = ParcelGetter.getParcelByZoningType(stringParam.split("-")[1], typoedParcel, zoningFile);
					if (bigZonedParcel.size() > 0) {
						parcelToNotAdd = ParcelCollection.dontAddParcel(parcelToNotAdd, bigZonedParcel);
						System.out.println(bigZonedParcel.size() + " elements : we cut the parcels with " + type + " parameters");
						result = ParcelCollection.addAllParcels(result, runParcelRecomp(splitZone, bigZonedParcel, tmpFile, mupOutput, pBuildingType,
								dontTouchUZones, geoFile, regulFile, typeOfRecomp));
						// THAT'S AN UGLY PATCH, BUT HAS TO TAKE CARE OF GEOM ERRORS TODO find somathing nices

						// if (bigZoned.size() > 10) {
						// break;
						// } else {
						// System.out.println("too small collec, we try to cut em with other parameters");
						// goOn = true;
						// }
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
						SimpleFeatureCollection typoedParcel = ParcelGetter.getParcelByTypo(stringParam, parcelCollection, FromGeom.getZoning(regulFile));
						if (typoedParcel.size() > 0) {
							Collec.exportSFC(typoedParcel, new File("/tmp/salut-" + stringParam + ".shp"));
							parcelToNotAdd = ParcelCollection.dontAddParcel(parcelToNotAdd, typoedParcel);
							System.out.println(typoedParcel.size() + " elements :we cut the parcels with " + type + " parameters");
							def = ParcelCollection.addAllParcels(def, runParcelRecomp(splitZone, typoedParcel, tmpFile, mupOutput, pBuildingType,
									dontTouchUZones, geoFile, regulFile, typeOfRecomp));
							Collec.exportSFC(def, new File("/tmp/" + stringParam + ".shp"));

							// if (typoed.size() > 10) {
							// break;
							// } else {
							// System.out.println("too small collec, we try to cut em with other parameters");
							// }
						}
					} else {
						if (splitZone.equals(stringParam)) {
							SimpleFeatureCollection bigZoned = ParcelGetter.getParcelByZoningType(stringParam, parcelCollection, zoningFile);
							if (bigZoned.size() > 0) {
								parcelToNotAdd = ParcelCollection.dontAddParcel(parcelToNotAdd, bigZoned);
								System.out.println("we cut the parcels with " + type + " parameters");
								def = ParcelCollection.addAllParcels(def, runParcelRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType,
										dontTouchUZones, geoFile, regulFile, typeOfRecomp));
							}
						}
					}
				}
			}
			result = ParcelCollection.addAllParcels(result, def);
		}
		SimpleFeatureCollection realResult = ParcelCollection.completeParcelMissing(parcelCollection, result.collection(), parcelToNotAdd);
		return realResult;
	}

	public static SimpleFeatureCollection runParcelRecomp(String splitZone, SimpleFeatureCollection bigZonedParcel, File tmpFolder, File mupOutput,
			SimpluParametersJSON pBuildingType, boolean dontTouchUZones, File geoFile, File regulFile, String typeOfRecomp) throws Exception {
		double maximalWidth = pBuildingType.getDouble("maximalWidthSplitParcel");
		double maximalArea = pBuildingType.getDouble("maximalAreaSplitParcel");
		double minimalArea = pBuildingType.getDouble("minimalAreaSplitParcel");
		double lenRoad = pBuildingType.getDouble("lenRoad");
		double lenDriveway = pBuildingType.getDouble("lenDriveway");
		int decompositionLevelWithoutStreet = pBuildingType.getInteger("decompositionLevelWithoutRoad");
		boolean allOrCell = pBuildingType.getBoolean("allZone");
		File zoningFile = FromGeom.getZoning(regulFile);
		File buildingFile = FromGeom.getBuild(geoFile);
		File predicateFile = FromGeom.getPredicate(regulFile);

		switch (typeOfRecomp) {
		case "densification":
			// return ParcelDensification.parcelDensification(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, geoFile, regulFile);

			ShapefileDataStore shpDSIlot = new ShapefileDataStore(FromGeom.getIlots(geoFile).toURI().toURL());
			SimpleFeatureCollection ilot = shpDSIlot.getFeatureSource().getFeatures();

			SimpleFeatureCollection parcMUPMarked = MarkParcelAttributeFromPosition.markParcelIntersectPolygonIntersection(bigZonedParcel, mupOutput);
			SimpleFeatureCollection toDensify = MarkParcelAttributeFromPosition.markParcelIntersectZoningType(parcMUPMarked, splitZone, zoningFile);
			SimpleFeatureCollection salut = Densification.densification(toDensify, ilot, tmpFolder, buildingFile, maximalArea,
					minimalArea, maximalWidth, lenDriveway, ParcelState.isArt3AllowsIsolatedParcel(parcMUPMarked.features().next(), predicateFile));
			SimpleFeatureCollection densifyedParcel = ArtiScalesParcelFields.fixParcelAttributes(salut, tmpFolder, buildingFile, mupOutput,
					zoningFile, allOrCell);
			if (DEBUG)
				Collec.exportSFC(densifyedParcel, new File("/tmp/ParcelConsolidRecomp.shp"));
			shpDSIlot.dispose();
			return densifyedParcel;
		case "partRecomp":
			SimpleFeatureCollection testmp = MarkParcelAttributeFromPosition.markParcelIntersectPolygonIntersection(bigZonedParcel, mupOutput);
			SimpleFeatureCollection test = MarkParcelAttributeFromPosition.markParcelIntersectZoningType(testmp, splitZone, zoningFile);
			SimpleFeatureCollection cuted = ConsolidationDivision.consolidationDivision(test, tmpFolder, maximalArea, minimalArea, maximalWidth,
					lenRoad, decompositionLevelWithoutStreet);
			SimpleFeatureCollection consolidRecompParcel = ArtiScalesParcelFields.fixParcelAttributes(cuted, tmpFolder, buildingFile,
					mupOutput, zoningFile, allOrCell);
			if (DEBUG)
				Collec.exportSFC(consolidRecompParcel, new File("/tmp/ParcelConsolidRecomp.shp"));
			return consolidRecompParcel;
		// return ParcelDensification.parcelPartRecomp(splitZone, bigZoned, tmpFile, mupOutput, pBuildingType, dontTouchUZones, geoFile, regulFile);
		case "totRecomp":
			ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
			SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
			SimpleFeatureCollection zone = ZoneDivision.createZoneToCut(splitZone, featuresZones, bigZonedParcel);
			// If no zones, we won't bother
			if (zone.isEmpty()) {
				System.out.println("parcelGenZone : no zones to be cut");
				System.exit(1);
			}
			SimpleFeatureCollection parcelCuted = ZoneDivision.zoneDivision(zone, bigZonedParcel, tmpFolder, zoningFile, maximalArea,
					minimalArea, maximalWidth, lenRoad, decompositionLevelWithoutStreet);
			SimpleFeatureCollection totRecompParcel = ArtiScalesParcelFields.fixParcelAttributes(parcelCuted, tmpFolder, buildingFile,
					mupOutput, zoningFile, allOrCell);
			if (DEBUG)
				Collec.exportSFC(totRecompParcel, new File("/tmp/parcelTotZone.shp"));
			shpDSZone.dispose();
			return totRecompParcel;
		}
		throw new FileNotFoundException("I didn't get the Recomp order");
	}

	// /**
	// * overload to allow a filter that unselect the features that musn't be cuted
	// *
	// * @param parcelIn
	// * @param filterFile
	// * @param tmpFile
	// * @param p
	// * @return
	// * @throws Exception
	// */
	// public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn, File filterFile, File tmpFile,
	// SimpluParametersJSON p) throws Exception {
	//
	// ShapefileDataStore morphoSDS = new ShapefileDataStore(filterFile.toURI().toURL());
	// SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
	// Geometry morphoUnion = Vectors.unionSFC(morphoSFC);
	// FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
	// PropertyName pName = ff.property(parcelIn.getSchema().getGeometryDescriptor().getLocalName());
	// Filter filter = ff.intersects(pName, ff.literal(morphoUnion));
	//
	// morphoSDS.dispose();
	// return generateSplitedParcels(parcelIn.subCollection(filter), tmpFile, p);
	// }
	//
	// /**
	// * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	// *
	// * @param parcelIn
	// * : Parcels collection of simple features
	// * @return
	// * @overload
	// * @throws Exception
	// */
	// public static SimpleFeatureCollection generateSplitedParcels(SimpleFeature parcelIn, File tmpFile, SimpluParametersJSON p) throws Exception {
	//
	// // splitting method option
	//
	//
	//
	// return generateSplitParcels(parcelIn, tmpFile, maximalArea, maximalWidth, 0, null, p.getDouble("lenRoad"), decompositionLevelWithoutRoad,
	// false);
	// }
	//
	// /**
	// * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	// *
	// * @param parcelsIn
	// * : Parcels collection of simple features
	// * @return
	// * @overload
	// * @throws Exception
	// */
	// public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelsIn, File tmpFile, SimpluParametersJSON p)
	// throws Exception {
	//
	// // splitting method option
	//
	// double roadEpsilon = 0.5;
	// double maximalArea = p.getDouble("maximalAreaSplitParcel");
	// double maximalWidth = p.getDouble("maximalWidthSplitParcel");
	//
	// // Exterior from the UrbanBlock if necessary or null
	// IMultiCurve<IOrientableCurve> extBlock = null;
	// // Roads are created for this number of decomposition level
	// int decompositionLevelWithoutRoad = 2;
	// // Road width
	// double roadWidth = 5.0;
	// // Boolean forceRoadaccess
	// boolean forceRoadAccess = true;
	// return generateSplitedParcels(parcelsIn, tmpFile, maximalArea, maximalWidth, roadEpsilon, extBlock, decompositionLevelWithoutRoad, roadWidth,
	// forceRoadAccess);
	//
	// }

}