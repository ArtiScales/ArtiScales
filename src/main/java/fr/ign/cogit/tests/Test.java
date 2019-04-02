package fr.ign.cogit.tests;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import fr.ign.cogit.modules.SelectParcels;

public class Test {

	public static void main(String[] args) throws Exception {
		digForBesac(new File("/home/ubuntu/boulot/these/result2903/SimPLUDepot/DDense/variantMvGrid1"));
	}

	public static void digForBesac(File fIn) throws IOException {
		for (File f : fIn.listFiles()) {
			if (f.isDirectory()) {
				digForBesac(f);
			}
			if (f.getName().equals("communities.shp")) {
				ShapefileDataStore communitiesSDS = new ShapefileDataStore(f.toURI().toURL());
				SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
				SimpleFeatureIterator it = communitiesOG.features();

				while (it.hasNext()) {
					SimpleFeature feat = it.next();
					String code = (String) feat.getAttribute("typo");
					if (code.equals("centre")) {
						File parcelF = new File(f.getParentFile().getParentFile(), "parcelle.shp");
						ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelF.toURI().toURL());
						SimpleFeatureCollection parcelOG = parcelSDS.getFeatureSource().getFeatures();
						SimpleFeatureIterator itParcel = parcelOG.features();
						int nb = 0;
						while (itParcel.hasNext()) {
							SimpleFeature featP = itParcel.next();
							String auth = (String) featP.getAttribute("DoWeSimul");
							if (auth.equals("true")) {
								nb++;
							}
						}
						itParcel.close();
						if (nb > 10) {
							System.out.println(f);
						}
					}
				}
				it.close();
				communitiesSDS.dispose();
			}
		}
	}
//		ShapefileDataStore communitiesSDS = new ShapefileDataStore((new File("/home/ubuntu/boulot/these/ArtiScales/ArtiScales/ParcelSelectionDepot/DDense/variante0/parcelGenExport.shp")).toURI().toURL());
//		SimpleFeatureCollection communitiesOG = communitiesSDS.getFeatureSource().getFeatures();
//
//		SimpleFeatureIterator it = communitiesOG.features();
//List<String> l = new ArrayList<String>();
//
//		while (it.hasNext()) {
//			SimpleFeature f =it.next();
//			String code = (String) f.getAttribute("INSEE");
//			if (code.equals("25056")) {
//				code = code + f.getAttribute("SECTION");
//			}
//			if (!l.contains(code)) {
//				l.add(code);
//			}
//		}
//		it.close();
//		communitiesSDS.dispose();
//		System.out.println(l.size());
//		
//		CSVWriter writer = new CSVWriter(new FileWriter("/media/mcolomb/Data_2/WorkSession0327/magicNumber.csv"));
//		CSVReader reader = new CSVReader(new FileReader("/media/mcolomb/Data_2/WorkSession0327/magicNumber"));
//		for (String[] l : reader.readAll()) {
//			if (l[0].startsWith("magic number wrong : ./WorkSession0327/ParcelSelectionDepot2/")) {
//				String[] s = l[0].replace("magic number wrong : ./WorkSession0327/ParcelSelectionDepot2/", "").split("/");
//				writer.writeNext(s);
//			}
//		}
//		reader.close();
//		writer.close();


	// CSVWriter writer = new CSVWriter(new FileWriter("/home/ubuntu/workspace/ArtiScales/missing.csv"));
	// for (String[] l : reader.readAll()) {
	// if (l[0].startsWith("magic number wrong : /home/ubuntu/boulot/these/result0313/ParcelSelectionDepot/")) {
	// String[] s = l[0].replace("magic number wrong : /home/ubuntu/boulot/these/result0313/ParcelSelectionDepot/", "").split("/");
	// writer.writeNext(s);
	// }
	// }
	// reader.close();
	// writer.close();

	// String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenarFakeWorldMax/").getPath();

	// SelectParcels.separateToDifferentOptimizedPack(
	// new File("/home/mcolomb/workspace/ArtiScales/ArtiScales/ParcelSelectionDepot/DDense/variante0/parcelGenExport.shp"),
	// new File("/home/mcolomb/workspace/ArtiScales/ArtiScales/ParcelSelectionDepot/DDense/variante0/"),
	// new File("/home/mcolomb/workspace/ArtiScales/ArtiScales/tmp"),
	// new File("/home/mcolomb/workspace/ArtiScales/ArtiScales/dataRegulation"),
	// new File("/home/mcolomb/workspace/ArtiScales/ArtiScales/dataGeo"));

	// SelectParcels.separateToDifferentOptimizedPack(new File(rootFile, "ParcelSelectionFile/DDense/variante0/parcelGenExport.shp"),
	// new File(rootFile,"ParcelSelectionFile/DDense/variante0/"), new File("/tmp/"), new File(rootFile,"dataGeo"), new File(rootFile,"dataRegulation"));
	// IFeatureCollection<IFeature> collec = ShapefileReader.read("/home/mcolomb/informatique/ArtiScales/dataGeo/building.shp");
	// IFeatureCollection<IFeature> result = new FT_FeatureCollection<>();
	//
	// for (IFeature feat : collec) {
	// double hmin = ((double) feat.getAttribute("Z_MIN") + (double) feat.getAttribute("Z_MAX")) / 2;
	// double hmax = hmin + (int) feat.getAttribute("HAUTEUR");
	// IGeometry extruded = Extrusion2DObject.convertFromGeometry(feat.getGeom(), hmin, hmax);
	// IMultiSurface<IOrientableSurface> finalOs = FromGeomToSurface.convertMSGeom(extruded);
	// DefaultFeature salut = new DefaultFeature(finalOs);
	// AttributeManager.addAttribute(salut, "ID", feat.getAttribute("ID"), "String");
	// AttributeManager.addAttribute(salut, "HAUTEUR", feat.getAttribute("HAUTEUR"), "Integer");
	// AttributeManager.addAttribute(salut, "NATURE", feat.getAttribute("NATURE"), "String");
	//
	// //if (res.getFeatureType().getGeometryType().equals(GeometryCollection))
	// result.add(salut);
	//
	// }
	// ShapefileWriter.write(result, "/home/mcolomb/informatique/ArtiScales/dataGeo/building3d.shp", CRS.decode("EPSG:2154"));

	// File rootFile = new File("/home/ubuntu/workspace/ArtiScales/ArtiScalesLikeTBLunch");

	// SelectParcels.separateToDifferentOptimizedPack(new File(rootFile, "ParcelSelectionFile/DDense/variante0/parcelGenExport.shp"),
	// new File(rootFile,"ParcelSelectionFile/DDense/variante0/"), new File("/tmp/"), new File(rootFile,"dataGeo"), new File(rootFile,"dataRegulation"));
	// IFeatureCollection<IFeature> collec = ShapefileReader.read("/home/mcolomb/informatique/ArtiScales/dataGeo/building.shp");
	// IFeatureCollection<IFeature> result = new FT_FeatureCollection<>();
	//
	// for (IFeature feat : collec) {
	// double hmin = ((double) feat.getAttribute("Z_MIN") + (double) feat.getAttribute("Z_MAX")) / 2;
	// double hmax = hmin + (int) feat.getAttribute("HAUTEUR");
	// IGeometry extruded = Extrusion2DObject.convertFromGeometry(feat.getGeom(), hmin, hmax);
	// IMultiSurface<IOrientableSurface> finalOs = FromGeomToSurface.convertMSGeom(extruded);
	// DefaultFeature salut = new DefaultFeature(finalOs);
	// AttributeManager.addAttribute(salut, "ID", feat.getAttribute("ID"), "String");
	// AttributeManager.addAttribute(salut, "HAUTEUR", feat.getAttribute("HAUTEUR"), "Integer");
	// AttributeManager.addAttribute(salut, "NATURE", feat.getAttribute("NATURE"), "String");
	//
	// //if (res.getFeatureType().getGeometryType().equals(GeometryCollection))
	// result.add(salut);
	//
	// }
	// ShapefileWriter.write(result, "/home/mcolomb/informatique/ArtiScales/dataGeo/building3d.shp", CRS.decode("EPSG:2154"));

	// DirectPosition.PRECISION = 5;
	// IFeatureCollection<IFeature> collec = ShapefileReader.read("/home/mcolomb/expParcelCut/AU.shp");
	//
	// for (int i = 2; i <= 3; i++) {
	//
	// int count = 0;
	// String type = "house";
	// if (i == 2) {
	// type = "dwelling";
	// }
	// List<File> toMerge = new ArrayList<File>();
	// String ou = "/tmp/expParcelCut/" + type;
	// for (IFeature feat : collec) {
	//
	// List<IOrientableSurface> surfaces = FromGeomToSurface.convertGeom(feat.getGeom());
	//
	// if (surfaces.size() != 1) {
	// System.out.println("Not simple geometry : " + feat.toString());
	// continue;
	// }
	//
	// double roadEpsilon = 0.0;
	// double noise = 0.0;
	// double maximalArea = 800.0;
	// if (i == 2) {
	// maximalArea = 2000.0;
	// }
	// double maximalWidth = 10.0;
	//
	// // Road width
	// double roadWidth = 5.0;
	// // Boolean forceRoadaccess
	// boolean forceRoadAccess = false;
	// IPolygon pol = (IPolygon) surfaces.get(0);
	//
	// File dataGeo = new File("/home/mcolomb/informatique/ArtiScales/dataGeo/");
	// String inputUrbanBlock = GetFromGeom.getIlots(dataGeo).getAbsolutePath();
	// IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
	// List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
	// IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);
	//
	// System.out.println("pour le polygone " + count++);
	//
	// OBBBlockDecomposition decomposition = new OBBBlockDecomposition(pol, maximalArea, maximalWidth,
	// roadEpsilon, iMultiCurve, roadWidth, forceRoadAccess, i);
	// IFeatureCollection<IFeature> featColl = decomposition.decompParcel(noise);
	//
	// if ((OBBBlockDecomposition.howManyIt(pol, noise, forceRoadAccess, maximalArea, maximalWidth) - i) <= 0) {
	// FlagParcelDecomposition flagDecomp = new FlagParcelDecomposition(pol,
	// ShapefileReader.read(GetFromGeom.getBuild(dataGeo).getAbsolutePath()), maximalArea,
	// maximalWidth, roadWidth, iMultiCurve);
	// featColl = flagDecomp.decompParcel(0);
	// }
	// String out = ou + "/zoneOut" + count + ".shp";
	// ShapefileWriter.write(featColl, out, CRS.decode("EPSG:2154"));
	// toMerge.add(new File(out));
	//
	// }
	// Vectors.mergeVectFiles(toMerge, new File(ou, "mergeParcels.shp"));
	// }
}
