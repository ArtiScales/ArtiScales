
package fr.ign.cogit.util;

import java.io.File;
import java.io.FileReader;
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
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;

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
import fr.ign.cogit.geoxygene.util.conversion.GeOxygeneGeoToolsTypes;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.parameters.Parameters;

public class VectorFct {

	// public static void main(String[] args) throws Exception {
	//
	// File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
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
	// new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelDensification("U", featuresZones, tmpFile, new File("/home/mcolomb/informatique/ArtiScales"), new File(
	// "/home/mcolomb/informatique/ArtiScales/MupCityDepot/exScenar/variant0/exScenar-DataSys-CM20.0-S0.0-GP_915948.0_6677337.0--N6_Ba_ahpx_seed_42-evalAnal-20.0.shp"),
	// 800.0, 15.0, 5.0);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();

	// /////////////////////////
	// //////// try the parcelGenMotif method
	// /////////////////////////
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelGenMotif("NC", featuresZones, tmpFile, new File(p.getString("rootFile")), new File(
	// "/home/mcolomb/informatique/ArtiScales/MupCityDepot/exScenar/variant0/exScenar-DataSys-CM20.0-S0.0-GP_915948.0_6677337.0--N6_Ba_ahpx_seed_42-evalAnal-20.0.shp"),
	// 800.0, 7.0, 3.0, 2);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();

	// /////////////////////////
	// //////// try the parcelGenZone method
	// /////////////////////////
	//
	// ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
	//
	// // Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new
	// // File("/tmp/tmp2.shp"));
	// SimpleFeatureCollection salut = parcelGenZone("AU", featuresZones, tmpFile, new File(p.getString("rootFile")), 800.0, 7.0, 3.0, 2);
	//
	// Vectors.exportSFC(salut, new File("/tmp/parcelDensification.shp"));
	// shpDSZone.dispose();

	// /////////////////////////
	// //////// try the generateFlagSplitedParcels method
	// /////////////////////////
	//
	// File geoFile = new File(p.getString("rootFile"), "dataGeo");
	// IFeatureCollection<IFeature> featColl = ShapefileReader.read("/tmp/tmp1.shp");
	//
	// String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
	//
	// IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
	// List<IOrientableCurve> lOC = featC.select(featColl.envelope()).parallelStream().map(x -> FromGeomToLineString.convert(x.getGeom())).collect(ArrayList::new, List::addAll,
	// List::addAll);
	//
	// IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);
	//
	// // ShapefileDataStore shpDSZone = new ShapefileDataStore(
	// // new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/parcelGenExport.shp").toURI().toURL());
	// //
	// // SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
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
	// SimpleFeatureCollection salut = generateFlagSplitedParcels(featColl.get(0), iMultiCurve, geoFile, tmpFile, 2000.0, 15.0, 3.0);
	//
	// Vectors.exportSFC(salut, new File("/tmp/tmp2.shp"));
	//
	// }

	public static SimpleFeatureCollection parcelDensification(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile, File mupFile, File ressource, Parameters p)
			throws Exception {
		// TODO for the different parcel parameters (copy on the other algo)
		System.out.println("//TODO for the different parcel parameters (copy on other algo)");

		return parcelDensification(splitZone, parcelCollection, tmpFile, mupFile, p);

	}

	public static SimpleFeatureCollection parcelDensification(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile, File mupFile, Parameters p)
			throws Exception {
		return parcelDensification(splitZone, parcelCollection, tmpFile, new File(p.getString("rootFile")), mupFile, p.getDouble("maximalAreaSplitParcel"),
				p.getDouble("maximalWidthSplitParcel"), p.getDouble("lenDriveway"));
	}

	/**
	 * TODO doesn't work yet. Some weird stuff with the ParcelFlagManager + Some improvement must be done to set if the parcel must save the cut or not TODO the unsimulated parcel
	 * doesn't fall into the collection
	 * 
	 * @param splitZone
	 * @param parcelCollection
	 * @param tmpFile
	 * @param rootFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelDensification(String splitZone, SimpleFeatureCollection parcelCollection, File tmpFile, File rootFile, File mupFile,
			double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double lenDriveway) throws Exception {

		File pivotFile = new File(tmpFile, "parcelsInbfFlaged.shp");
		Vectors.exportSFC(parcelCollection, pivotFile);
		IFeatureCollection<IFeature> parcelCollec = ShapefileReader.read(pivotFile.getAbsolutePath());

		File geoFile = new File(rootFile, "dataGeo");

		// the little islands (ilots)
		String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();

		IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
		List<IOrientableCurve> lOC = featC.select(parcelCollec.envelope()).parallelStream().map(x -> FromGeomToLineString.convert(x.getGeom())).collect(ArrayList::new,
				List::addAll, List::addAll);
		IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

		IFeatureCollection<IFeature> cutedAll = new FT_FeatureCollection<>();
		for (IFeature feat : parcelCollec) {
			// if the parcel is selected for the simulation
			if (feat.getAttribute("DoWeSimul").equals("true") && ((boolean) feat.getAttribute(splitZone))) {
				// if the parcel is bigger than the limit size
				if (feat.getGeom().area() > maximalAreaSplitParcel) {
					// we falg cut the parcel
					IFeatureCollection<IFeature> tmp = VectorFct.generateFlagSplitedParcels(feat, iMultiCurve, tmpFile, rootFile, mupFile, maximalAreaSplitParcel,
							maximalWidthSplitParcel, lenDriveway);
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
	 * overload to get the wanted parameter file
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
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelGenZone(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput, Parameters p, File ressource,
			boolean allOrCell) throws Exception {

		File locationBuildingType = new File(ressource, "locationBuildingType");
		File profileBuildingType = new File(ressource, "profileBuildingType");
		// séparation entre les différentes zones
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		List<String> listZones = getLocationParamNames(locationBuildingType, p);

		// split into zones to make correct parcel recomposition
		for (String stringParam : listZones) {
			System.out.println("for line " + stringParam);
			Parameters pTemp = p;
			pTemp.add(Parameters.unmarshall(new File(locationBuildingType, stringParam)));
			// @simplification : as only one BuildingType is set per zones, we select the type that is the most represented
			BuildingType type = RepartitionBuildingType.getBiggestRepartition(pTemp);
			Parameters pAdded = p;
			pAdded.add(RepartitionBuildingType.getParam(profileBuildingType, type));

			// delete name of specials parameters
			if (stringParam.split(":").length == 2) {
				stringParam = stringParam.split(":")[1];
			}
			// del the .xml ref
			stringParam = stringParam.replace(".xml", "");

			// two specifications
			if (stringParam.split("-").length == 2) {
				SimpleFeatureCollection typoed = getParcelByTypo(stringParam.split("-")[0], parcels, new File(p.getString("rootFile")));
				SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam.split("-")[1], typoed, new File(p.getString("rootFile")));
				if (bigZoned.size() > 0) {
					System.out.println("we cut the parcels with " + type + "parameters");
					result.addAll(parcelGenZone(splitZone, bigZoned, tmpFile, mupOutput, pAdded, allOrCell));
				}
			}
			// only one specification
			else {
				if (stringParam.equals("periUrbain") || stringParam.equals("rural") || stringParam.equals("banlieue") || stringParam.equals("centre")) {
					SimpleFeatureCollection typoed = getParcelByTypo(stringParam, parcels, new File(p.getString("rootFile")));
					if (typoed.size() > 0) {
						System.out.println("we cut the parcels with " + type + "parameters");
						result.addAll(parcelGenZone(splitZone, typoed, tmpFile, mupOutput, pAdded, allOrCell));
					}
				} else {
					SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam, parcels, new File(p.getString("rootFile")));
					if (bigZoned.size() > 0) {
						System.out.println("we cut the parcels with " + type + "parameters");
						result.addAll(parcelGenZone(splitZone, bigZoned, tmpFile, mupOutput, pAdded, allOrCell));
					}
				}
			}

		}
		return result.collection();
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
	public static SimpleFeatureCollection parcelGenZone(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput, Parameters p, boolean allOrCell)
			throws Exception {

		return parcelGenZone(splitZone, parcels, tmpFile, new File(p.getString("rootFile")), mupOutput, p.getDouble("areaParcel"), p.getDouble("widParcel"), p.getDouble("lenRoad"),
				p.getInteger("decompositionLevelWithoutRoad"), allOrCell);
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
	public static SimpleFeatureCollection parcelGenZone(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File rootFile, File mupOutput, double maximalArea,
			double maximalWidth, double lenRoad, int decompositionLevelWithoutRoad, boolean allOrCell) throws Exception {

		// parcels to save for after
		DefaultFeatureCollection savedParcels = new DefaultFeatureCollection();
		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(GetFromGeom.getZoning(new File(rootFile, "dataRegulation")).toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		Geometry unionParcel = Vectors.unionSFC(parcels);
		String geometryParcelPropertyName = parcels.getSchema().getGeometryDescriptor().getLocalName();

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

		// all the AU zones
		Geometry geomAU = Vectors.unionSFC(zoneAU);
		DefaultFeatureCollection parcelsInAU = new DefaultFeatureCollection();
		SimpleFeatureIterator parcIt = parcels.features();

		// sort in two different collections, the ones that cares and the ones that doesnt
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
		// SimpleFeatureBuilder simpleSFB = GetFromGeom.getBasicSFB();
		SimpleFeatureBuilder simpleSFB = new SimpleFeatureBuilder(zoneAU.getSchema());

		DefaultFeatureCollection goOdAu = new DefaultFeatureCollection();
		SimpleFeatureIterator zoneAUIt = zoneAU.features();
		try {
			while (zoneAUIt.hasNext()) {
				SimpleFeature feat = zoneAUIt.next();
				Geometry intersection = ((Geometry) feat.getDefaultGeometry()).intersection(unionParcel);
				if (!intersection.isEmpty() && intersection.getArea() > 5.0) {

					if (intersection instanceof MultiPolygon) {
						for (int i = 0; i < intersection.getNumGeometries(); i++) {
							simpleSFB.set("the_geom", intersection.getGeometryN(i));
							simpleSFB.set("INSEE", "25569");
							goOdAu.add(simpleSFB.buildFeature(null));
							// Object[] attr = feat.getAttributes().toArray();
							// attr[0] = intersection.getGeometryN(i);
							// goOdAu.add(simpleSFB.buildFeature(null, attr));
						}
					} else if (intersection instanceof GeometryCollection) {
						for (int i = 0; i < intersection.getNumGeometries(); i++) {
							Geometry g = intersection.getGeometryN(i);
							if (g instanceof Polygon) {
								simpleSFB.set("the_geom", g);
								simpleSFB.set("INSEE", "25569");
								goOdAu.add(simpleSFB.buildFeature(null));
								// Object[] attr = feat.getAttributes().toArray();
								// attr[0] = intersection.getGeometryN(i);
								// goOdAu.add(simpleSFB.buildFeature(null, attr));
							}
						}
					} else {
						simpleSFB.set("the_geom", intersection);
						simpleSFB.set("INSEE", "25560");
						goOdAu.add(simpleSFB.buildFeature(null));
						// Object[] attr = feat.getAttributes().toArray();
						// attr[0] = intersection;
						// goOdAu.add(simpleSFB.buildFeature(null, attr));
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			zoneAUIt.close();
		}

		// temporary shapefiles that serves to do polygons
		File fParcelsInAU = Vectors.exportSFC(parcelsInAU, new File(tmpFile, "parcelCible.shp"));
		File fZoneAU = Vectors.exportSFC(goOdAu.collection(), new File(tmpFile, "oneAU.shp"));
		geomAU = Vectors.unionSFC(goOdAu.collection());
		// cut and separate parcel according to their spatial relation with the zoning
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

		// parcel intersecting the U zone must not be cuted and keep their attributes
		// intermediary result

		SimpleFeatureBuilder sfBuilder = GetFromGeom.getParcelSplitSFBuilder();

		DefaultFeatureCollection write = new DefaultFeatureCollection();

		int nFeat = 0;
		// for every polygons situated in between U and AU zones, we cut the parcels
		// regarding to the zone and copy them attributes to keep the existing U parcels
		for (Geometry poly : polygons) {
			// if the polygons are not included on the AU zone
			if (!geomAU.buffer(0.01).contains(poly)) {
				sfBuilder.add(poly);
				SimpleFeatureIterator parcelIt = parcelsInAU.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						if (((Geometry) feat.getDefaultGeometry()).buffer(0.01).contains(poly)) {
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
							// @warning the AU Parcels are mostly unbuilt, but maybe not?
							sfBuilder.set("IsBuild", feat.getAttribute("IsBuild"));
							sfBuilder.set("U", feat.getAttribute("U"));
							sfBuilder.set("AU", false);
							sfBuilder.set("NC", feat.getAttribute("NC"));
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
				write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
				nFeat++;
			}
		}
		String geometryOutputName = write.getSchema().getGeometryDescriptor().getLocalName();
		SimpleFeatureIterator it = goOdAu.features();
		int numZone = 0;

		// mark and add the AU zones to the collection
		try {
			while (it.hasNext()) {
				SimpleFeature zone = it.next();
				// get the insee number for that zone
				String insee = (String) zone.getAttribute("INSEE");
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
				Geometry intersectedGeom = ((Geometry) zone.getDefaultGeometry()).intersection(unionParcel);

				if (!intersectedGeom.isEmpty()) {
					if (intersectedGeom instanceof MultiPolygon) {
						for (int i = 0; i < intersectedGeom.getNumGeometries(); i++) {
							sfBuilder.set(geometryOutputName, intersectedGeom.getGeometryN(i));
							write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
							nFeat++;
							// ugly, but have to do it
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
						}
						continue;
					} else {
						sfBuilder.set(geometryOutputName, intersectedGeom);
					}
				} else {
					sfBuilder.set(geometryOutputName, zone.getDefaultGeometry());
				}
				write.add(sfBuilder.buildFeature(String.valueOf(nFeat)));
				nFeat++;
				numZone++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		shpDSZone.dispose();

		double roadEpsilon = 00;
		double noise = 0;

		SimpleFeatureCollection splitedAUParcels = splitParcels(write.collection(), maximalArea, maximalWidth, roadEpsilon, noise, null, lenRoad, false,
				decompositionLevelWithoutRoad, tmpFile);

		// mup output
		ShapefileDataStore mupSDS = new ShapefileDataStore(mupOutput.toURI().toURL());
		SimpleFeatureCollection mupSFC = mupSDS.getFeatureSource().getFeatures();

		// Finally, put them all features in a same collec
		SimpleFeatureIterator finalIt = splitedAUParcels.features();
		int cpt = 0;
		try {
			while (finalIt.hasNext()) {
				SimpleFeature feat = finalIt.next();
				// erase soon to be erased super thin polygons TODO one is in double and have unknown parameters : how to delete this one?
				if (((Geometry) feat.getDefaultGeometry()).getArea() > 5.0) {
					// set if the parcel is simulable or not
					if (allOrCell) {
						double eval = VectorFct.getEvalInParcel(feat, mupSFC);
						if (eval == 0.0) {
							eval = VectorFct.getCloseEvalInParcel(feat, mupSFC);
						}
						feat.setAttribute("DoWeSimul", "true");
						feat.setAttribute("eval", eval);
					} else {
						if (VectorFct.isParcelInCell(feat, mupSFC)) {
							feat.setAttribute("DoWeSimul", "true");
							feat.setAttribute("eval", VectorFct.getEvalInParcel(feat, mupSFC));
						} else {
							feat.setAttribute("DoWeSimul", "false");
						}
					}
					SimpleFeatureBuilder finalParcelBuilder = GetFromGeom.setSFBWithFeat(feat, savedParcels.getSchema(), geometryOutputName);

					if (feat.getAttribute("CODE") == null) {
						finalParcelBuilder = GetFromGeom.setSFBParDefaut(feat, savedParcels.getSchema(), geometryOutputName);
					}

					savedParcels.add(finalParcelBuilder.buildFeature(String.valueOf(cpt)));
					cpt++;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			finalIt.close();
		}
		mupSDS.dispose();
		// pSDS.dispose();
		SimpleFeatureCollection result = savedParcels.collection();
		Vectors.exportSFC(result, new File(tmpFile, "parcelFinal.shp"));

		return result;
	}

	/**
	 * overload to get the wanted parameter file TODO not tested yet
	 * 
	 * @param splitZone
	 * @param parcels
	 * @param tmpFile
	 * @param zoningFile
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection parcelGenMotif(String splitZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput, Parameters p, File ressource)
			throws Exception {

		File locationBuildingType = new File(ressource, "locationBuildingType");
		File profileBuildingType = new File(ressource, "profileBuildingType");
		// séparation entre les différentes zones
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		List<String> listZones = getLocationParamNames(locationBuildingType, p);

		// split into zones to make correct parcel recomposition
		for (String stringParam : listZones) {
			System.out.println("for line " + stringParam);
			Parameters pTemp = p;
			pTemp.add(Parameters.unmarshall(new File(locationBuildingType, stringParam)));
			// @simplification : as only one BuildingType is set per zones, we select the type that is the most represented
			BuildingType type = RepartitionBuildingType.getBiggestRepartition(pTemp);
			Parameters pAdded = p;
			pAdded.add(RepartitionBuildingType.getParam(profileBuildingType, type));

			// delete name of specials parameters
			if (stringParam.split(":").length == 2) {
				stringParam = stringParam.split(":")[1];
			}
			// del the .xml ref
			stringParam = stringParam.replace(".xml", "");

			// two specifications
			if (stringParam.split("-").length == 2) {
				SimpleFeatureCollection typoed = getParcelByTypo(stringParam.split("-")[0], parcels, new File(p.getString("rootFile")));
				SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam.split("-")[1], typoed, new File(p.getString("rootFile")));
				if (bigZoned.size() > 0) {
					System.out.println("we cut the parcels with " + type + "parameters");
					result.addAll(parcelGenMotif(splitZone, bigZoned, tmpFile, mupOutput, pAdded));
				}
			}
			// only one specification
			else {
				if (stringParam.equals("periUrbain") || stringParam.equals("rural") || stringParam.equals("banlieue") || stringParam.equals("centre")) {
					SimpleFeatureCollection typoed = getParcelByTypo(stringParam, parcels, new File(p.getString("rootFile")));
					if (typoed.size() > 0) {
						System.out.println("we cut the parcels with " + type + "parameters");
						result.addAll(parcelGenMotif(splitZone, typoed, tmpFile, mupOutput, pAdded));
					}
				} else {
					SimpleFeatureCollection bigZoned = getParcelByBigZone(stringParam, parcels, new File(p.getString("rootFile")));
					if (bigZoned.size() > 0) {
						System.out.println("we cut the parcels with " + type + "parameters");
						result.addAll(parcelGenMotif(splitZone, bigZoned, tmpFile, mupOutput, pAdded));
					}
				}
			}

		}
		return result.collection();
	}

	public static SimpleFeatureCollection parcelGenMotif(String typeZone, SimpleFeatureCollection parcels, File tmpFile, File mupOutput, Parameters p)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		return parcelGenMotif(typeZone, parcels, tmpFile, new File(p.getString("rootFile")), mupOutput, p.getDouble("areaParcel"), p.getDouble("widParcel"), p.getDouble("lenRoad"),
				p.getInteger("decompositionLevelWithoutRoad"));
	}

	public static SimpleFeatureCollection parcelGenMotif(String typeZone, SimpleFeatureCollection parcels, File tmpFile, File rootFile, File mupOutput, double maximalArea,
			double maximalWidth, double roadWidth, int decompositionLevelWithRoad) throws IOException, NoSuchAuthorityCodeException, FactoryException {

		File geoFile = new File(rootFile, "dataGeo");
		Geometry emprise = Vectors.unionSFC(parcels);

		DefaultFeatureCollection parcelResult = new DefaultFeatureCollection();
		parcelResult.addAll(parcels);

		ShapefileDataStore shpDSCells = new ShapefileDataStore(mupOutput.toURI().toURL());
		SimpleFeatureCollection cellsSFS = shpDSCells.getFeatureSource().getFeatures();
		DefaultFeatureCollection parcelToMerge = new DefaultFeatureCollection();

		// city information
		ShapefileDataStore shpDSCities = new ShapefileDataStore(GetFromGeom.getCommunities(geoFile).toURI().toURL());
		SimpleFeatureCollection citiesSFS = shpDSCities.getFeatureSource().getFeatures();

		////////////////
		// first round of selection of the intersected parcels
		////////////////

		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature parcel = parcelIt.next();
				if ((boolean) parcel.getAttribute(typeZone)) {
					if (parcel.getAttribute("DoWeSimul").equals("true")) {
						parcelToMerge.add(parcel);
						parcelResult.remove(parcel);
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		// Vectors.exportSFC(parcelToMerge.collection(), new File("/tmp/step1.shp"));
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

		SimpleFeatureBuilder sfBuilderSimple = GetFromGeom.getBasicSFB();

		Geometry multiGeom = Vectors.unionSFC(parcelToMerge);
		for (int i = 0; i < multiGeom.getNumGeometries(); i++) {
			sfBuilder.add(multiGeom.getGeometryN(i));
			sfBuilder.set("Section", i);
			mergedParcels.add(sfBuilder.buildFeature(null));
		}
		// Vectors.exportSFC(mergedParcels.collection(), new File("/tmp/step2.shp"));
		Vectors.exportSFC(mergedParcels.collection(), new File("/tmp/forSection.shp"));

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
					SimpleFeatureIterator it = splitParcels(feat, maximalArea, maximalWidth, 0, 0, null, roadWidth, false, decompositionLevelWithRoad, tmpFile, false).features();
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
		Vectors.exportSFC(cutParcels.collection(), new File("/tmp/step3.shp"));
		System.out.println("done step 3");

		////////////////
		// fourth step : selection of the parcels intersecting the cells
		////////////////

		int i = 0;
		SimpleFeatureIterator parcelFinal = cutParcels.features();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(parcelResult.getSchema());

		ShapefileDataStore sectionSDS = new ShapefileDataStore((new File("/tmp/forSection.shp")).toURI().toURL());
		SimpleFeatureCollection sectionSFC = sectionSDS.getFeatureSource().getFeatures();
		try {
			while (parcelFinal.hasNext()) {
				SimpleFeature parcel = parcelFinal.next();
				featureBuilder.add(parcel.getDefaultGeometry());

				// we get the city info
				String insee = GetFromGeom.getInseeFromParcel(citiesSFS, parcel);

				featureBuilder.set("INSEE", insee);
				featureBuilder.set("CODE_DEP", insee.substring(0, 2));
				featureBuilder.set("CODE_COM", insee.substring(2, 5));

				// that takes time but it's the best way I've found to set a correct section number (to look at the step 2 polygons)
				String sec = "Default";
				SimpleFeatureIterator sectionIt = sectionSFC.features();
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
		sectionSDS.dispose();
		shpDSCells.dispose();
		shpDSCities.dispose();
		shpDSCells.dispose();
		Vectors.exportSFC(parcelResult, new File("/tmp/step4.shp"));

		return parcelResult;
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
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn, File filterFile, File tmpFile, Parameters p) throws Exception {

		ShapefileDataStore morphoSDS = new ShapefileDataStore(filterFile.toURI().toURL());
		SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
		Geometry morphoUnion = Vectors.unionSFC(morphoSFC);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(parcelIn.getSchema().getGeometryDescriptor().getLocalName());
		Filter filter = ff.intersects(pName, ff.literal(morphoUnion));

		morphoSDS.dispose();
		return generateSplitedParcels(parcelIn.subCollection(filter), tmpFile, p);
	}

	// public static SimpleFeatureCollection generateFlagSplitedParcels(SimpleFeatureCollection featColl, IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, File tmpFile,
	// Parameters p) throws NoSuchAuthorityCodeException, FactoryException, Exception {
	//
	// DefaultFeatureCollection collec = new DefaultFeatureCollection();
	// SimpleFeatureIterator it = featColl.features();
	//
	// try {
	// while (it.hasNext()) {
	// SimpleFeature feat = it.next();
	// collec.addAll(generateFlagSplitedParcels(feat, iMultiCurve, geoFile, tmpFile, p));
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
	// public static SimpleFeatureCollection generateFlagSplitedParcels(SimpleFeature feat, IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, File tmpFile, Parameters p)
	// throws Exception {
	// return generateFlagSplitedParcels(feat, iMultiCurve, geoFile, tmpFile, p.getDouble("maximalAreaSplitParcel"), p.getDouble("maximalWidthSplitParcel"),
	// p.getDouble("lenDriveway"));
	// }
	//
	// public static SimpleFeatureCollection generateFlagSplitedParcels(SimpleFeature feat, IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, File tmpFile,
	// Double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double lenDriveway) throws Exception {
	//
	// return generateFlagSplitedParcels(GeOxygeneGeoToolsTypes.convert2IFeature(feat), iMultiCurve, geoFile, tmpFile, maximalAreaSplitParcel, maximalWidthSplitParcel,
	// lenDriveway);
	//
	// }

	public static IFeatureCollection<IFeature> generateFlagSplitedParcels(IFeature ifeat, IMultiCurve<IOrientableCurve> iMultiCurve, File tmpFile, File rootFile, File outMupFile,
			Double maximalAreaSplitParcel, Double maximalWidthSplitParcel, Double lenDriveway) throws Exception {

		IFeatureCollection<IFeature> batiLargeCollec = ShapefileReader.read(GetFromGeom.getBuild(new File(rootFile, "dataGeo")).getAbsolutePath());
		IFeatureCollection<IFeature> batiCollec = new FT_FeatureCollection<>();
		batiCollec.addAll(batiLargeCollec.select(ifeat.getGeom()));

		IGeometry geom = ifeat.getGeom();

		// what would that be for?
		IDirectPosition dp = new DirectPosition(0, 0, 0); // geom.centroid();
		geom = geom.translate(-dp.getX(), -dp.getY(), 0);

		List<IOrientableSurface> surfaces = FromGeomToSurface.convertGeom(geom);
		FlagParcelDecomposition fpd = new FlagParcelDecomposition((IPolygon) surfaces.get(0), batiCollec, maximalAreaSplitParcel, maximalWidthSplitParcel, lenDriveway,
				iMultiCurve);
		IFeatureCollection<IFeature> decomp = fpd.decompParcel(0);
		IFeatureCollection<IFeature> ifeatCollOut = new FT_FeatureCollection<>();
		int numParcelle = 0;
		for (IFeature newFeat : decomp) {
			String newCodeDep = (String) ifeat.getAttribute("CODE_DEP");
			String newCodeCom = (String) ifeat.getAttribute("CODE_COM");
			String newSection = (String) ifeat.getAttribute("SECTION") + "div";
			String newNumero = String.valueOf(numParcelle++);
			String newCode = newCodeDep + newCodeCom + "000" + newSection + newNumero;
			AttributeManager.addAttribute(newFeat, "CODE", newCode, "String");
			AttributeManager.addAttribute(newFeat, "CODE_DEP", newCodeDep, "String");
			AttributeManager.addAttribute(newFeat, "CODE_COM", newCodeCom, "String");
			AttributeManager.addAttribute(newFeat, "COM_ABS", "000", "String");
			AttributeManager.addAttribute(newFeat, "SECTION", newSection, "String");
			AttributeManager.addAttribute(newFeat, "NUMERO", newNumero, "String");
			AttributeManager.addAttribute(newFeat, "INSEE", newCodeDep + newCodeCom, "String");

			double eval = 0.0;
			boolean bati = false;
			boolean simul = false;
			boolean u = false;
			boolean au = false;
			boolean nc = false;

			// we put a small buffer because a lot of houses are just biting neighborhood parcels
			for (IFeature batiIFeat : batiCollec) {
				if (newFeat.getGeom().buffer(-1.5).intersects(batiIFeat.getGeom())) {
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
					} else if (isArt3AllowsIsolatedParcel(newFeat, rootFile)) {
						simul = true;
					}
				} else {
					simul = true;
				}
			}

			List<String> zones = GetFromGeom.parcelInBigZone(newFeat, new File(rootFile, "dataRegulation"));

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
				eval = getEvalInParcel(newFeat, outMupFile);
			}

			AttributeManager.addAttribute(newFeat, "eval", eval, "String");
			AttributeManager.addAttribute(newFeat, "DoWeSimul", simul, "String");
			AttributeManager.addAttribute(newFeat, "IsBuild", bati, "String");
			AttributeManager.addAttribute(newFeat, "U", u, "String");
			AttributeManager.addAttribute(newFeat, "AU", au, "String");
			AttributeManager.addAttribute(newFeat, "NC", nc, "String");

			ifeatCollOut.add(newFeat);
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
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeature parcelIn, File tmpFile, Parameters p) throws Exception {

		// splitting method option

		double maximalWidth = p.getDouble("maximalWidthSplitParcel");
		double maximalArea = p.getDouble("maximalAreaSplitParcel");
		int decompositionLevelWithRoad = p.getInteger("decompositionLevelWithoutRoad");
		// File geoFile = new File(p.getString("rootFile"), "dataGeo");
		// String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
		// System.out.println(inputUrbanBlock);
		// IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
		// List<IOrientableCurve> lOC =
		// FromGeomToLineString.convert(featC.get(0).getGeom());
		// IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

		return generateSplitParcels(parcelIn, tmpFile, maximalArea, maximalWidth, 0, null, p.getDouble("lenRoad"), decompositionLevelWithRoad, false);
	}

	/**
	 * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	 * 
	 * @param parcelsIn
	 *            : Parcels collection of simple features
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelsIn, File tmpFile, Parameters p) throws Exception {

		// splitting method option

		double roadEpsilon = 0.5;
		double maximalArea = p.getDouble("maximalAreaSplitParcel");
		double maximalWidth = p.getDouble("maximalWidthSplitParcel");

		// Exterior from the UrbanBlock if necessary or null
		IMultiCurve<IOrientableCurve> extBlock = null;
		// Roads are created for this number of decomposition level
		int decompositionLevelWithRoad = 2;
		// Road width
		double roadWidth = 5.0;
		// Boolean forceRoadaccess
		boolean forceRoadAccess = true;
		return generateSplitedParcels(parcelsIn, tmpFile, p, maximalArea, maximalWidth, roadEpsilon, extBlock, decompositionLevelWithRoad, roadWidth, forceRoadAccess);

	}

	public static SimpleFeatureCollection generateSplitParcels(SimpleFeature parcelIn, File tmpFile, double maximalArea, double maximalWidth, double epsilon,
			IMultiCurve<IOrientableCurve> extBlock, double roadWidth, int decompositionLevelWithRoad, boolean forceRoadAccess) throws Exception {

		// putting the need of splitting into attribute

		SimpleFeatureBuilder sfBuilder = GetFromGeom.getParcelSplitSFBuilder();

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();

		String numParcelValue = "";
		if (parcelIn.getAttribute("CODE") != null) {
			numParcelValue = parcelIn.getAttribute("CODE").toString();
		} else if (parcelIn.getAttribute("CODE_DEP") != null) {
			numParcelValue = ((String) parcelIn.getAttribute("CODE_DEP")) + (parcelIn.getAttribute("CODE_COM").toString()) + (parcelIn.getAttribute("COM_ABS").toString())
					+ (parcelIn.getAttribute("SECTION").toString());
		} else if (parcelIn.getAttribute("NUMERO") != null) {
			numParcelValue = parcelIn.getAttribute("NUMERO").toString();
		}
		Object[] attr = { numParcelValue, parcelIn.getAttribute("CODE_DEP"), parcelIn.getAttribute("CODE_COM"), parcelIn.getAttribute("COM_ABS"), parcelIn.getAttribute("SECTION"),
				parcelIn.getAttribute("NUMERO"), parcelIn.getAttribute("INSEE"), parcelIn.getAttribute("eval"), parcelIn.getAttribute("DoWeSimul"), 1 };

		sfBuilder.add(parcelIn.getDefaultGeometry());
		toSplit.add(sfBuilder.buildFeature(null, attr));

		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithRoad, tmpFile);

	}

	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelsIn, File tmpFile, Parameters p, double maximalArea, double maximalWidth,
			double epsilon, IMultiCurve<IOrientableCurve> extBlock, int decompositionLevelWithRoad, double roadWidth, boolean forceRoadAccess) throws Exception {

		///////
		// putting the need of splitting into attribute
		///////

		// create a new collection
		SimpleFeatureBuilder sfBuilder = GetFromGeom.getParcelSplitSFBuilder();
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
					numParcelValue = ((String) feat.getAttribute("CODE_DEP")) + (feat.getAttribute("CODE_COM").toString()) + (feat.getAttribute("COM_ABS").toString())
							+ (feat.getAttribute("SECTION").toString());
				} else if (feat.getAttribute("NUMERO") != null) {
					numParcelValue = feat.getAttribute("NUMERO").toString();
				}
				Object[] attr = { numParcelValue, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"),
						feat.getAttribute("NUMERO"), feat.getAttribute("INSEE"), feat.getAttribute("eval"), feat.getAttribute("DoWeSimul"), 0 };

				// if(){
				// Object[] attr = { numParcelValue, feat.getAttribute("CODE_DEP"),
				// feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"),
				// feat.getAttribute("SECTION"),
				// feat.getAttribute("NUMERO"), feat.getAttribute("INSEE"),
				// feat.getAttribute("eval"), feat.getAttribute("DoWeSimul"), 0 };
				//
				// }

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
		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0.0, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithRoad, tmpFile);
	}

	public static SimpleFeatureCollection splitParcels(SimpleFeature toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise,
			IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithRoad, File tmpFile, boolean addArg) throws Exception {
		DefaultFeatureCollection in = new DefaultFeatureCollection();
		in.add(toSplit);
		return splitParcels(in.collection(), maximalArea, maximalWidth, roadEpsilon, noise, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithRoad, tmpFile, addArg);

	}

	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise,
			IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithRoad, File tmpFile) throws Exception {

		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithRoad, tmpFile, true);
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
	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise,
			IMultiCurve<IOrientableCurve> extBlock, double roadWidth, boolean forceRoadAccess, int decompositionLevelWithRoad, File tmpFile, boolean addArg) throws Exception {

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
			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, roadEpsilon, extBlock, roadWidth, forceRoadAccess, decompositionLevelWithRoad);

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
		SimpleFeatureCollection parcelUnclean = sds.getFeatureSource().getFeatures();
		sds.dispose();
		return parcelUnclean;

	}

	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile
	 * 
	 * @param file2MergeIn
	 *            : list of files containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(List<File> file2MergeIn) throws Exception {
		File out = new File(file2MergeIn.get(0).getParentFile(), "TotBatSimuFill.shp");
		return Vectors.mergeVectFiles(file2MergeIn, out);
	}

	/**
	 * Merge all the shapefile of a folder (made for simPLU buildings) into one shapefile
	 * 
	 * @param file2MergeIn
	 *            : folder containing the shapefiles
	 * @return : file where everything is saved (here whith a building name)
	 * @throws Exception
	 */
	public static File mergeBatis(File file2MergeIn) throws Exception {
		List<File> listBatiFile = new ArrayList<File>();
		for (File f : file2MergeIn.listFiles()) {
			if (f.getName().endsWith(".shp") && f.getName().startsWith("out")) {
				listBatiFile.add(f);
			}
		}
		return mergeBatis(listBatiFile);
	}

	/**
	 * return true if there's a building on the input parcel
	 * 
	 * @return the same collection without the parcels that intersects a building
	 * @throws Exception
	 */
	public static boolean isParcelBuilt(SimpleFeature parcelIn, File geoFile) throws Exception {

		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
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
		ShapefileDataStore shpDSBati = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
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
		ShapefileDataStore bati_datastore = new ShapefileDataStore(GetFromGeom.getBuild(geoFile).toURI().toURL());
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

	public static Double getEvalInParcel(IFeature parcel, File outMup) throws NoSuchAuthorityCodeException, ParseException, FactoryException, IOException, Exception {
		if (outMup == null) {
			return 0.0;
		}

		return getEvalInParcel(GeOxygeneGeoToolsTypes.convert2SimpleFeature(parcel, CRS.decode("EPSG:2154")), outMup);
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
	public static Double getEvalInParcel(SimpleFeature parcel, File outMup) throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {

		ShapefileDataStore cellsSDS = new ShapefileDataStore(outMup.toURI().toURL());
		SimpleFeatureCollection cellsCollection = cellsSDS.getFeatureSource().getFeatures();
		Double result = getEvalInParcel(parcel, cellsCollection);
		cellsSDS.dispose();
		return result;
	}

	public static Double getEvalInParcel(SimpleFeature parcel, SimpleFeatureCollection mupSFC) {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = mupSFC.getSchema().getGeometryDescriptor().getLocalName();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(parcel.getDefaultGeometry()));
		SimpleFeatureCollection onlyCells = mupSFC.subCollection(inter);
		Double bestEval = 0.0;

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

	public static IFeatureCollection<IFeature> getParcelByCode(IFeatureCollection<IFeature> parcelles, List<String> parcelsWanted) throws IOException {
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

	private static SimpleFeatureCollection getParcelByBigZone(String zone, SimpleFeatureCollection parcelles, File rootFile) throws IOException {

		ShapefileDataStore zonesSDS = new ShapefileDataStore(GetFromGeom.getZoning(new File(rootFile, "dataRegulation")).toURI().toURL());
		SimpleFeatureCollection zonesSFCBig = zonesSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection zonesSFC = Vectors.cropSFC(zonesSFCBig, parcelles);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator it = parcelles.features();
		try {
			while (it.hasNext()) {
				SimpleFeature parcelFeat = it.next();
				Filter filter = ff.like(ff.property("BIGZONE"), zone);
				SimpleFeatureIterator itZone = zonesSFC.subCollection(filter).features();
				try {
					while (itZone.hasNext()) {
						SimpleFeature zoneFeat = itZone.next();
						Geometry zoneGeom = (Geometry) zoneFeat.getDefaultGeometry();
						Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
						if (zoneGeom.intersects(parcelGeom)) {

							result.add(parcelFeat);
							break;

							// if (zoneGeom.contains(parcelGeom)) {
							// result.add(parcelFeat);
							// break;
							// }
							// // if the intersection is less than 50% of the parcel, we let it to the other (with the hypothesis that there is only 2 features)
							// else if (parcelGeom.intersection(zoneGeom).getArea() > parcelGeom.getArea() / 2) {
							// result.add(parcelFeat);
							// break;
							// } else {
							// break; temp-In
							// }
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
		return result.collection();
	}

	public static SimpleFeatureCollection getParcelByTypo(String typo, SimpleFeatureCollection parcelles, File rootFile) throws IOException {

		ShapefileDataStore communitiesSDS = new ShapefileDataStore(GetFromGeom.getCommunities(new File(rootFile, "dataGeo")).toURI().toURL());
		SimpleFeatureCollection communitiesSFCBig = communitiesSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection communitiesSFC = Vectors.cropSFC(communitiesSFCBig, parcelles);
		Vectors.exportSFC(communitiesSFC, new File("/tmp/communi.shp"));

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator itParcel = parcelles.features();
		try {
			while (itParcel.hasNext()) {
				SimpleFeature parcelFeat = itParcel.next();
				Filter filter = ff.like(ff.property("typo"), typo);
				SimpleFeatureIterator itTypo = communitiesSFC.subCollection(filter).features();
				try {
					while (itTypo.hasNext()) {
						SimpleFeature typoFeat = itTypo.next();
						Geometry typoGeom = (Geometry) typoFeat.getDefaultGeometry();
						Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
						if (typoGeom.intersects(parcelGeom)) {
							if (typoGeom.contains(parcelGeom)) {
								result.add(parcelFeat);
								break;
							}
							// if the intersection is less than 50% of the parcel, we let it to the other (with the hypothesis that there is only 2 features)
							else if (parcelGeom.intersection(typoGeom).getArea() > parcelGeom.getArea() / 2) {
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
		Vectors.exportSFC(result.collection(), new File("/tmp/outTypo.shp"));
		communitiesSDS.dispose();
		return result.collection();
	}

	public static List<String> getLocationParamNames(File locationBuildingType, Parameters p) {
		List<String> listZones = new ArrayList<String>();
		List<String> specialScenarZone = new ArrayList<String>();

		for (File param : locationBuildingType.listFiles()) {
			String nameParam = param.getName();
			if (nameParam.equals("default.xml")) {
				continue;
			}
			// if the param repartition concerns a special scenario and it's not ours
			if (nameParam.split(":").length > 1) {
				if (nameParam.split(":")[0].equals(p.getString("code"))) {
					specialScenarZone.add(nameParam);
				} else {
					continue;
				}
			}
			listZones.add(nameParam);
		}

		// if theres a zone special for the scenario and a regular one, the regular one must be erased
		if (!specialScenarZone.isEmpty()) {
			for (String s : specialScenarZone) {
				listZones.remove((s.split(":")[1]));
			}
		}
		return listZones;
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
