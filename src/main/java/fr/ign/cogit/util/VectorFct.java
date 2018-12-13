
package fr.ign.cogit.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.parameters.Parameters;

public class VectorFct {

	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException, IOException {
	}
	
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

	public static File generateSplitedParcels(SimpleFeatureCollection parcelIn, File filterFile, File tmpFile, Parameters p) throws Exception {

		ShapefileDataStore morphoSDS = new ShapefileDataStore(filterFile.toURI().toURL());
		SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
		Geometry morphoUnion = Vectors.unionSFC(morphoSFC);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(parcelIn.getSchema().getGeometryDescriptor().getLocalName());
		Filter filter = ff.intersects(pName, ff.literal(morphoUnion));

		morphoSDS.dispose();
		return generateSplitedParcels(parcelIn.subCollection(filter), tmpFile, p);
	}

	/**
	 * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	 * 
	 * @param parcelIn
	 *            : Parcels collection of simple features
	 * @return
	 * @throws Exception
	 */
	public static File generateSplitedParcels(SimpleFeatureCollection parcelIn, File tmpFile, Parameters p) throws Exception {

		// splitting method option

		double roadEpsilon = 0.5;
		double noise = 0;
		double maximalArea = 1200;
		double maximalWidth = 50;
		
		// Exterior from the UrbanBlock if necessary or null
		IMultiCurve<IOrientableCurve> extBlock = null;
		// Roads are created for this number of decomposition level
		int decompositionLevelWithRoad = 2;
		// Road width
		double roadWidth = 5.0;
		// Boolean forceRoadaccess
		boolean forceRoadAccess = false;
		
		
		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// putting the need of splitting into attribute

		SimpleFeatureBuilder sfBuilder = GetFromGeom.getParcelSFBuilder();

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
		int i = 0;
		SimpleFeatureIterator parcelIt = parcelIn.features();
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

		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise, extBlock, decompositionLevelWithRoad, roadWidth, forceRoadAccess,tmpFile, p);
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
	public static File splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise, IMultiCurve<IOrientableCurve> extBlock, int decompositionLevelWithRoad, double roadWidth,
			boolean forceRoadAccess, File tmpFile, Parameters p)
			throws Exception {
		// TODO un truc fait bugger la sortie dans cette classe..

		// TODO classe po bô du tout: faire une vraie conversion entre les types
		// geotools et geox (passer par des shp a été le seul moyen que j'ai
		// trouvé pour que ça fonctionne)
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
					String newNumero = String.valueOf(numParcelle);
					AttributeManager.addAttribute(newFeat, "CODE_DEP", newCodeDep, "String");
					AttributeManager.addAttribute(newFeat, "CODE_COM", newCodeCom, "String");
					AttributeManager.addAttribute(newFeat, "SECTION", newSection, "String");
					AttributeManager.addAttribute(newFeat, "COM_ABS", "000", "String");
					AttributeManager.addAttribute(newFeat, "NUMERO", newNumero, "String");
					String newCode = newCodeDep + newCodeCom + "000" + newSection + newNumero;
					AttributeManager.addAttribute(newFeat, "INSEE", newCodeDep + newCodeCom, "String");
					AttributeManager.addAttribute(newFeat, "CODE", newCode, "String");
					AttributeManager.addAttribute(newFeat, "eval", "0", "String");
					AttributeManager.addAttribute(newFeat, "DoWeSimul", false, "String");
					AttributeManager.addAttribute(newFeat, "IsBuild", feat.getAttribute("IsBuild"), "String");
					AttributeManager.addAttribute(newFeat, "U", feat.getAttribute("U"), "String");
					AttributeManager.addAttribute(newFeat, "AU", feat.getAttribute("AU"), "String");
					AttributeManager.addAttribute(newFeat, "NC", feat.getAttribute("NC"), "String");
					ifeatCollOut.add(newFeat);
					numParcelle++;
				}
			} catch (NullPointerException n) {
				System.out.println("erreur sur le split pour la parcelle " + String.valueOf(feat.getAttribute("CODE")));
				IFeature featTemp = feat.cloneGeom();
				ifeatCollOut.add(featTemp);
			}
		}

		File fileOut = new File(tmpFile, "tmp_split.shp");
		ShapefileWriter.write(ifeatCollOut, fileOut.toString(), CRS.decode("EPSG:2154"));

		return fileOut;
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
}
