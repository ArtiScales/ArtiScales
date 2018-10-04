package fr.ign.cogit.util;

import java.io.File;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.parameters.Parameters;
import fr.ign.random.Random;

public class VectorFct {

	public static void main(String[] args) throws Exception {
		mergeBatis(new File(
				"/home/yo/Documents/these/ArtiScales/output/Stability-dataAutomPhy-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_9015629222324914404-evalAnal-20.0/25495/ZoningAllowed/simu0/"));

	}
	
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn,File filterFile,  Parameters p) throws Exception {
		
		ShapefileDataStore morphoSDS = new ShapefileDataStore(filterFile.toURI().toURL());
		SimpleFeatureCollection morphoSFC = morphoSDS.getFeatureSource().getFeatures();
		Geometry morphoUnion = Vectors.unionSFC(morphoSFC);
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		PropertyName pName = ff.property(parcelIn.getSchema().getGeometryDescriptor().getLocalName());
		Filter filter = ff.intersects(pName, ff.literal(morphoUnion));
		System.out.println("number of parcels " + parcelIn.subCollection(filter).size());
		return generateSplitedParcels(parcelIn.subCollection(filter), p);
	}
	
	/**
	 * Determine if the parcels need to be splited or not, based on their area. This area is either determined by a param file, or taken as a default value of 1200 square meters
	 * 
	 * @param parcelIn
	 *            : Parcels collection of simple features
	 * @return
	 * @throws Exception
	 */
	public static SimpleFeatureCollection generateSplitedParcels(SimpleFeatureCollection parcelIn, Parameters p) throws Exception {

		// splitting method option

		double roadEpsilon = 0.5;
		double noise = 10;
		double maximalArea = 1200;
		double maximalWidth = 50;
		if (!(p == null)) {
			maximalArea = p.getDouble("maximalAreaSplitParcel");
			maximalWidth = p.getDouble("maximalWidthSplitParcel");
		}

		// putting the need of splitting into attribute
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("SPLIT", Integer.class);
		sfTypeBuilder.add("CODE", String.class);

		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

		DefaultFeatureCollection toSplit = new DefaultFeatureCollection();
		int i = 0;
		SimpleFeatureIterator parcelIt = parcelIn.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				
		
				
				String attributeValue = "";
				
				if(feat.getAttribute("CODE_DEP") != null) {
					attributeValue =  ((String) feat.getAttribute("CODE_DEP")) + (feat.getAttribute("CODE_COM").toString()) + (feat.getAttribute("COM_ABS").toString())
							+ (feat.getAttribute("SECTION").toString());
				}else if(feat.getAttribute("NUMERO") != null) {
					
					attributeValue = feat.getAttribute("NUMERO").toString();
					
				}else if (feat.getAttribute("CODE") != null) {
					
					attributeValue = feat.getAttribute("CODE").toString();
					
				}else {
					System.out.println("VectorFct : Other type of parcel : " + feat);
				}
				
				
				
				
				Object[] attr = { 0, attributeValue};
						
						
						
				if (((Geometry) feat.getDefaultGeometry()).getArea() > maximalArea) {
					attr[0] = 1;
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
		
		return splitParcels(toSplit, maximalArea, maximalWidth, roadEpsilon, noise, p);

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
	public static SimpleFeatureCollection splitParcels(SimpleFeatureCollection toSplit, double maximalArea, double maximalWidth, double roadEpsilon, double noise, Parameters p)
			throws Exception {
		// TODO un truc fait bugger la sortie dans cette classe..

		// TODO classe po bô du tout: faire une vraie conversion entre les types
		// geotools et geox (passer par des shp a été le seul moyen que j'ai
		// trouvé pour que ça fonctionne)
		String attNameToTransform = "SPLIT";

		// IFeatureCollection<?> ifeatColl =
		// GeOxygeneGeoToolsTypes.convert2IFeatureCollection(toSplit);
		File tmpFile = new File("");
		if (p != null) {
			tmpFile = new File(p.getString("rootFile"), "/temp/");

		} else {
			tmpFile = new File("/tmp/");
		}
		tmpFile.mkdir();
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
			OBBBlockDecomposition obb = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, Random.random(), roadEpsilon, noise);
			// TODO erreures récurentes sur le split
			try {
				IFeatureCollection<IFeature> featCollDecomp = obb.decompParcel();
				for (IFeature featDecomp : featCollDecomp) {
					// MAJ du numéro de la parcelle
					String newNum = feat.getAttribute("CODE") + String.valueOf(numParcelle);
					numParcelle++;
					IFeature newFeat = new DefaultFeature(featDecomp.getGeom());
					AttributeManager.addAttribute(newFeat, "CODE", newNum, "String");
					ifeatCollOut.add(newFeat);
				}
			} catch (NullPointerException n) {
				System.out.println("erreur sur le split pour la parcelle " + String.valueOf(feat.getAttribute("CODE")));
				IFeature featTemp = feat.cloneGeom();
				ifeatCollOut.add(featTemp);
			}
		}

		File fileOut = new File(tmpFile, "tmp.shp");
		ShapefileWriter.write(ifeatCollOut, fileOut.toString(), CRS.decode("EPSG:2154"));
		// nouvelle sélection en fonction de la zone pour patir à la faible
		// qualité de la sélection spatiale quand les polygones touchent les
		// zones (oui je sais, pas bô encore une fois..)

		ShapefileDataStore SSD = new ShapefileDataStore(fileOut.toURI().toURL());
		SimpleFeatureCollection splitedSFC = SSD.getFeatureSource().getFeatures();

		// splitedSFC = selecParcelZonePLU(typeZone, splitedSFC, SSD);

		// pareil, il serait peut être mieux d'échanger des shp?!
		// SSD.dispose();
		// return
		// GeOxygeneGeoToolsTypes.convert2FeatureCollection(ifeatCollOut);
		return splitedSFC;
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
