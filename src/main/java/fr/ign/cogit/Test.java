package fr.ign.cogit;

import java.io.File;

import fr.ign.cogit.GTFunctions.Rasters;
import fr.ign.tools.OutputTools;

public class Test {

	public static void main(String[] args) throws Exception {

		OutputTools.vectorizeMupOutput(Rasters.importRaster(new File(
				"/media/mcolomb/Data_2/resultFinal/testAHP/comparaison/compAHP-Autom-CM20.0-S0.0-GP_915948.0_6677337.0/N7_Ba_YagahpT_Yag_seed_42/N7_Ba_YagahpT_Yag_seed_42-evalAnal-20.0.tif")),
				new File("/media/mcolomb/Data_2/resultFinal/testAHP/extra/vector/N7_Ba_Yag_ahpT.shp"), 20);

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

}
