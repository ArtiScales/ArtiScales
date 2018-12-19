package fr.ign.cogit;

import java.io.File;
import java.util.List;

import org.geotools.referencing.CRS;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;
import fr.ign.cogit.util.GetFromGeom;

public class Test {


	public static void main(String[] args) throws Exception {
		
		DirectPosition.PRECISION = 5;
				
 		String strMuliPol = "POLYGON ((928720.68999999994412065 6693812.88999999966472387, 928743.07999999995809048 6693831.30999999959021807, 928744.2099999999627471 6693832.09999999962747097, 928808.76000000000931323 6693891.04999999981373549, 928881.61999999999534339 6693958.33000000007450581, 928945.92000000004190952 6693887.20000000018626451, 928934.19999999995343387 6693876.53000000026077032, 928896.47999999998137355 6693823.58999999985098839, 928857.52000000001862645 6693775.74000000022351742, 928818.25 6693744.78000000026077032, 928789.82999999995809048 6693713.32000000029802322, 928754.28000000002793968 6693764.34999999962747097, 928726.84999999997671694 6693745.57000000029802322, 928722.09999999997671694 6693752.37999999988824129, 928749.56000000005587935 6693771.28000000026077032, 928727.72999999998137355 6693802.74000000022351742, 928720.68999999994412065 6693812.88999999966472387))";
 		 double roadEpsilon = 0;
         double noise = 0;
        double maximalArea = 1200;
        double maximalWidth = 50;
		
		// Exterior from the UrbanBlock if necessary or null
		IMultiCurve<IOrientableCurve> imC = null;
		// Roads are created for this number of decomposition level
		int decompositionLevelWithRoad = 2;
		// Road width
		double roadWidth = 5.0;
		// Boolean forceRoadaccess
		boolean forceRoadAccess = false;

		IPolygon pol = (IPolygon) FromGeomToSurface.convertGeom(WktGeOxygene.makeGeOxygene(strMuliPol)).get(0);
		

		
			String inputUrbanBlock = GetFromGeom.getIlots(new File("/home/yo/Documents/these/ArtiScales/dataGeo/")).getAbsolutePath();
			IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
			List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
			IMultiCurve<IOrientableCurve>	iMultiCurve = new GM_MultiCurve<>(lOC);
	
		
		OBBBlockDecomposition decomposition = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, roadEpsilon, iMultiCurve, decompositionLevelWithRoad, roadWidth, forceRoadAccess);
		IFeatureCollection<IFeature> featColl = decomposition.decompParcel(noise);

		ShapefileWriter.write(featColl, "/tmp/tmp2.shp",CRS.decode("EPSG:2154"));


	}

}
