
package fr.ign.cogit.util;

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
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.annexeTools.FeaturePolygonizer;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
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
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.GeOxygeneGeoToolsTypes;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.parameters.Parameters;

public class VectorFct {

	public static void main(String[] args) throws Exception {

		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/etalIntenseRegul");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parametreTechnique.xml"));
		lF.add(new File(rootParam, "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		File tmpFile = new File("/tmp");

		ShapefileDataStore shpDSZone = new ShapefileDataStore(
				new File("/home/mcolomb/informatique/ArtiScales2/ParcelSelectionFile/intenseRegulatedSpread/variant0/parcelGenExport.shp").toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();
		SimpleFeatureIterator it = featuresZones.features();
		SimpleFeature waiting = null;
		while (it.hasNext()) {
			SimpleFeature feat = it.next();
			if (((String) feat.getAttribute("CODE")).equals("25557000AB0117")) {
				waiting = feat;
			}
		}

		// Vectors.exportSFC(generateSplitedParcels(waiting, tmpFile, p), new File("/tmp/tmp2.shp"));
		SimpleFeatureCollection salut = generateSplitedParcels(waiting, tmpFile, p);

		Vectors.exportSFC(salut, new File("/tmp/tmp2.shp"));

	}

	public static SimpleFeatureCollection generateSplitedParcelsU(SimpleFeatureCollection parcelCollection, File geoFile, Parameters p) throws Exception {
		SimpleFeatureIterator parcelIt = parcelCollection.features();

		// the little islands (ilots)
		String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
		IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
		List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
		IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

		DefaultFeatureCollection cutedAll = new DefaultFeatureCollection();
		// TODO doesn't work
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();

				// if the parcel is selected for the simulation
				if (feat.getAttribute("DoWeSimul").equals("true") && ((boolean) feat.getAttribute("U"))) {

					// if the parcel is bigger than the limit size
					if (((Geometry) feat.getDefaultGeometry()).getArea() > p.getDouble("maximalAreaSplitParcel")) {
						// we falg cut the parcel
						try {
							cutedAll.addAll(VectorFct.generateFlagSplitedParcels(feat, iMultiCurve, geoFile, p));
						}
						// how to catch the fact that it isn't reliable to the road?
						catch (Error e) {
							// how to catch the application of the rule 3?
							String art3 = "";

							if (art3.equals("1")) {
								cutedAll.add(feat);
							} else {
								// TODO make a U selection
								// cutedAll.addAll(generateFlagSplitedParcelsErrorless(feat, iMultiCurve, geoFile, p));
							}
						}
					}
				}
				// if no simulation needed, we ad the normal parcel
				else {
					cutedAll.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		Vectors.exportSFC(cutedAll, new File("/tmp/cutedParcels.shp"));
		return cutedAll;

	}

	/**
	 * Merge and recut the to urbanised (AU) zones Cut first the U parcels to keep them unsplited, then split the AU parcel and remerge them all into the original parcel file
	 * 
	 * @param parcels
	 * @param zoningFile
	 * @param p
	 *            : parametre file s
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcelsAU(SimpleFeatureCollection parcels, File tmpFile, File zoningFile, Parameters p) throws Exception {

		// parcels to save for after
		DefaultFeatureCollection savedParcels = new DefaultFeatureCollection();
		// import of the zoning file
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection featuresZones = shpDSZone.getFeatureSource().getFeatures();

		Geometry unionParcel = Vectors.unionSFC(parcels);
		String geometryParcelPropertyName = parcels.getSchema().getGeometryDescriptor().getLocalName();

		// get the AU zones from the zoning file
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		Filter filterTypeZone = ff.like(ff.property("TYPEZONE"), "AU");

		Filter filterEmprise = ff.intersects(ff.property(geometryParcelPropertyName), ff.literal(unionParcel));
		SimpleFeatureCollection zoneAU = featuresZones.subCollection(filterTypeZone).subCollection(filterEmprise);

		// If no AU zones, we won't bother
		if (zoneAU.isEmpty()) {
			System.out.println("no AU zones");
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

		// temporary shapefiles
		File fParcelsInAU = Vectors.exportSFC(parcelsInAU, new File(tmpFile, "parcelCible.shp"));
		File fZoneAU = Vectors.exportSFC(zoneAU, new File(tmpFile, "oneAU.shp"));

		// cut and separate parcel according to their spatial relation with the zonnig zones
		File[] polyFiles = { fParcelsInAU, fZoneAU };
		List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

		// parcel intersecting the U zone must not be cuted and keep their attributes
		// intermediary result
		File outU = new File(tmpFile, "polygonU.shp");
		SimpleFeatureBuilder sfBuilder = GetFromGeom.getParcelSplitSFBuilder();

		DefaultFeatureCollection write = new DefaultFeatureCollection();

		int nFeat = 0;
		// for every polygons situated in between U and AU zones, we cut the parcels regarding to the zone and copy them attributes to keep the existing U parcels
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
							sfBuilder.set("AU", feat.getAttribute("AU"));
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
		SimpleFeatureIterator it = zoneAU.features();
		int numZone = 0;

		// mark the AU zones
		try {
			while (it.hasNext()) {
				SimpleFeature zone = it.next();
				// get the insee number for that zone
				String insee = "";
				insee = (String) zone.getAttribute("INSEE");
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

		Vectors.exportSFC(write.collection(), outU);

		shpDSZone.dispose();

		double roadEpsilon = 0.5;
		double noise = 0;
		double maximalArea = 400;
		double maximalWidth = 50;
		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// get the previously cuted and reshaped shapefile
		ShapefileDataStore pSDS = new ShapefileDataStore(outU.toURI().toURL());
		SimpleFeatureCollection pSFS = pSDS.getFeatureSource().getFeatures();

		SimpleFeatureCollection splitedAUParcels = splitParcels(pSFS, maximalArea, maximalWidth, roadEpsilon, noise, null, 2, p.getDouble("largeurRouteAccess"), true, tmpFile, p);

		// Finally, put them all features in a same collec
		SimpleFeatureIterator finalIt = splitedAUParcels.features();
		int cpt = 0;
		try {
			while (finalIt.hasNext()) {
				SimpleFeature feat = finalIt.next();
				SimpleFeatureBuilder finalParcelBuilder = GetFromGeom.setSFBWithFeat(feat, savedParcels.getSchema(), geometryOutputName);
				if (feat.getAttribute("CODE") == null) {
					finalParcelBuilder = GetFromGeom.setSFBParDefaut(feat, savedParcels.getSchema(), geometryOutputName);
				}
				savedParcels.add(finalParcelBuilder.buildFeature(String.valueOf(cpt)));
				cpt++;
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			finalIt.close();
		}

		pSDS.dispose();
		SimpleFeatureCollection result = savedParcels.collection();
		Vectors.exportSFC(result, new File(tmpFile, "parcelFinal.shp"));

		return result;
	}

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

	public static SimpleFeatureCollection generateFlagSplitedParcels(SimpleFeatureCollection featColl, IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, Parameters p)
			throws NoSuchAuthorityCodeException, FactoryException, Exception {

		DefaultFeatureCollection collec = new DefaultFeatureCollection();
		SimpleFeatureIterator it = featColl.features();

		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				collec.addAll(generateFlagSplitedParcels(feat, iMultiCurve, geoFile, p));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		return collec;
	}

	public static SimpleFeatureCollection generateFlagSplitedParcels(SimpleFeature feat, IMultiCurve<IOrientableCurve> iMultiCurve, File geoFile, Parameters p) throws Exception {
		IFeature ifeat = GeOxygeneGeoToolsTypes.convert2IFeature(feat);
		IGeometry geom = ifeat.getGeom();

		// what would that be for?
		// IDirectPosition dp = new DirectPosition(0, 0, 0); // geom.centroid();
		// geom = geom.translate(-dp.getX(), -dp.getY(), 0);

		List<IOrientableSurface> surfaces = FromGeomToSurface.convertGeom(geom);

		if (surfaces.size() != 1) {
			System.out.println("Not simple geometry : " + feat.toString());
			return null;
		}

		FlagParcelDecomposition fpd = new FlagParcelDecomposition((IPolygon) surfaces.get(0), ShapefileReader.read(GetFromGeom.getBati(geoFile).getAbsolutePath()),
				p.getDouble("maximalAreaSplitParcel"), p.getDouble("maximalWidthSplitParcel"), p.getDouble("largeurRouteAccess"), iMultiCurve);
		IFeatureCollection<IFeature> decomp = fpd.decompParcel(0);
		return GeOxygeneGeoToolsTypes.convert2FeatureCollection(decomp);

	}

	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeature parcelIn, File tmpFile, Parameters p, double maximalArea, double maximalWidth, double epsilon,
			IMultiCurve<IOrientableCurve> extBlock, int decompositionLevelWithRoad, double roadWidth, boolean forceRoadAccess) throws Exception {

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
		} else {
			System.out.println("VectorFct : Other type of parcel");
		}
		Object[] attr = { numParcelValue, parcelIn.getAttribute("CODE_DEP"), parcelIn.getAttribute("CODE_COM"), parcelIn.getAttribute("COM_ABS"), parcelIn.getAttribute("SECTION"),
				parcelIn.getAttribute("NUMERO"), parcelIn.getAttribute("INSEE"), parcelIn.getAttribute("eval"), parcelIn.getAttribute("DoWeSimul"), 1 };

		sfBuilder.add(parcelIn.getDefaultGeometry());
		toSplit.add(sfBuilder.buildFeature(String.valueOf(0), attr));

		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0, extBlock, decompositionLevelWithRoad, roadWidth, true, tmpFile, p);

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

		double maximalArea = p.getDouble("maximalAreaSplitParcel");
		double maximalWidth = p.getDouble("maximalWidthSplitParcel");
		maximalArea = 1500;
		int decompositionLevelWithRoad = getDecompositionLevelWithRoad(parcelIn);

		// File geoFile = new File(p.getString("rootFile"), "dataGeo");
		// String inputUrbanBlock = GetFromGeom.getIlots(geoFile).getAbsolutePath();
		// System.out.println(inputUrbanBlock);
		// IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
		// List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
		// IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

		return generateSplitedParcels(parcelIn, tmpFile, p, maximalArea, maximalWidth, 0, null, decompositionLevelWithRoad, p.getDouble("largeurRouteAccess"), false);
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
				} else {
					System.out.println("VectorFct : Other type of parcel : " + feat.getAttribute(1));
				}
				Object[] attr = { numParcelValue, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"),
						feat.getAttribute("NUMERO"), feat.getAttribute("INSEE"), feat.getAttribute("eval"), feat.getAttribute("DoWeSimul"), 0 };

				// if(){
				// Object[] attr = { numParcelValue, feat.getAttribute("CODE_DEP"), feat.getAttribute("CODE_COM"), feat.getAttribute("COM_ABS"), feat.getAttribute("SECTION"),
				// feat.getAttribute("NUMERO"), feat.getAttribute("INSEE"), feat.getAttribute("eval"), feat.getAttribute("DoWeSimul"), 0 };
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

		return splitParcels(toSplit, maximalArea, maximalWidth, epsilon, 0, extBlock, decompositionLevelWithRoad, roadWidth, forceRoadAccess, tmpFile, p);
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

	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise,
			IMultiCurve<IOrientableCurve> extBlock, int decompositionLevelWithRoad, double roadWidth, boolean forceRoadAccess, File tmpFile, Parameters p) throws Exception {

		System.out.println("start splitting");

		String attNameToTransform = "SPLIT";

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

			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, roadEpsilon, extBlock, decompositionLevelWithRoad, roadWidth, forceRoadAccess);

			// TODO erreures récurentes sur le split
			try {
				IFeatureCollection<IFeature> featCollDecomp = obb.decompParcel(noise);
				for (IFeature featDecomp : featCollDecomp) {
					// MAJ du numéro de la parcelle
					IFeature newFeat = new DefaultFeature(featDecomp.getGeom());
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
					ifeatCollOut.add(newFeat);
				}
			} catch (NullPointerException n) {
				System.out.println("erreur sur le split pour la parcelle " + String.valueOf(feat.getAttribute("CODE")));
				IFeature featTemp = feat.cloneGeom();
				ifeatCollOut.add(featTemp);
			}
		}

		File fileOut = new File(tmpFile, "tmp_split.shp");
		ShapefileWriter.write(ifeatCollOut, fileOut.toString(), CRS.decode("EPSG:2154"));

		return GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut, CRS.decode("EPSG:2154"));
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

	public static int getDecompositionLevelWithRoad(SimpleFeature parcelIn) throws Exception {
		int decompositionLevelWithRoad = 1;

		if (((Geometry) parcelIn.getDefaultGeometry()).getArea() > 5000) {
			decompositionLevelWithRoad = 2;
		}
		return decompositionLevelWithRoad;
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
}
