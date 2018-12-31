package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.CRS;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.FlagParcelDecomposition;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.rjmcmc.kernel.RejectionVariate.constant_normalizer;

public class Test {

	public static void main(String[] args) throws Exception {

		DirectPosition.PRECISION = 5;
		IFeatureCollection<IFeature> collec = ShapefileReader.read("/home/yo/Documents/AU.shp");

		for (int i = 2; i <= 3; i++) {

			int count = 0;
			String type = "house";
			if (i == 2) {
				type = "dwelling";
			}
			List<File> toMerge = new ArrayList<File>();
			String ou = "/home/yo/expParcelCut/" + type;
			for (IFeature feat : collec) {

				List<IOrientableSurface> surfaces = FromGeomToSurface.convertGeom(feat.getGeom());

				if (surfaces.size() != 1) {
					System.out.println("Not simple geometry : " + feat.toString());
					continue;
				}

				double roadEpsilon = 0.0;
				double noise = 0.0;
				double maximalArea = 800.0;
				if (i == 2) {
					maximalArea = 2000.0;
				}
				double maximalWidth = 10.0;

				// Road width
				double roadWidth = 5.0;
				// Boolean forceRoadaccess
				boolean forceRoadAccess = false;
				IPolygon pol = (IPolygon) surfaces.get(0);

				File dataGeo = new File("/home/yo/Documents/these/ArtiScales/dataGeo/");
				String inputUrbanBlock = GetFromGeom.getIlots(dataGeo).getAbsolutePath();
				IFeatureCollection<IFeature> featC = ShapefileReader.read(inputUrbanBlock);
				List<IOrientableCurve> lOC = FromGeomToLineString.convert(featC.get(0).getGeom());
				IMultiCurve<IOrientableCurve> iMultiCurve = new GM_MultiCurve<>(lOC);

				System.out.println("pour le polygone " + count++);

				OBBBlockDecomposition decomposition = new OBBBlockDecomposition(pol, maximalArea, maximalWidth,
						roadEpsilon, iMultiCurve, roadWidth, forceRoadAccess, i);
				IFeatureCollection<IFeature> featColl = decomposition.decompParcel(noise);

				if ((decomposition.howManyIt(pol, noise, forceRoadAccess) - i) <= 0) {
					FlagParcelDecomposition flagDecomp = new FlagParcelDecomposition(pol,
							ShapefileReader.read(GetFromGeom.getBuild(dataGeo).getAbsolutePath()), maximalArea,
							maximalWidth, roadWidth, iMultiCurve);
					featColl = flagDecomp.decompParcel(0);
				}
				String out = ou + "/zoneOut" + count + ".shp";
				ShapefileWriter.write(featColl, out, CRS.decode("EPSG:2154"));
				toMerge.add(new File(out));

			}
			Vectors.mergeVectFiles(toMerge, new File(ou, "mergeParcels.shp"));
		}
	}

}
