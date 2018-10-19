package fr.ign.cogit;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.sig3d.calculation.parcelDecomposition.OBBBlockDecomposition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;

public class Test {


	public static void main(String[] args) throws Exception {
		
		DirectPosition.PRECISION = 5;
		

		String strMuliPol = "POLYGON ((930504.39 6676077.2 0.0, 930503.2 6676071.07 0.0, 930485.03 6676097.67 0.0, 930481.32 6676103.72 0.0, 930462.54 6676132.06 0.0, 930462.63 6676145.02 0.0, 930462.79 6676160.69 0.0, 930459.84 6676170.01 0.0, 930444.52 6676187.57 0.0, 930440.01 6676199.36 0.0, 930439.81 6676199.93 0.0, 930436.99 6676204.35 0.0, 930426.35 6676221.02 0.0, 930425.36 6676236.96 0.0, 930420.83 6676241.63 0.0, 930404.88 6676255.86 0.0, 930406.14 6676257.45 0.0, 930394.8 6676266.84 0.0, 930394.22 6676267.19 0.0, 930398.09 6676276.79 0.0, 930399.56 6676279.36 0.0, 930403.84 6676284.62 0.0, 930407.5 6676287.92 0.0, 930411.9 6676291.22 0.0, 930415.44 6676293.44 0.0, 930419.13 6676295.19 0.0, 930421.32 6676295.78 0.0, 930427.86 6676296.75 0.0, 930430.31 6676296.72 0.0, 930432.45 6676296.1 0.0, 930437.31 6676292.66 0.0, 930434.51 6676294.18 0.0, 930431.87 6676294.79 0.0, 930429.37 6676294.73 0.0, 930427.65 6676294.39 0.0, 930410.69 6676281.4 0.0, 930450.57 6676247.86 0.0, 930448.48 6676245.5 0.0, 930428.87 6676232.34 0.0, 930428.38 6676230.74 0.0, 930429.18 6676221.98 0.0, 930429.85 6676220.53 0.0, 930441.14 6676201.64 0.0, 930447.02 6676189.03 0.0, 930468.95 6676163.75 0.0, 930469.99 6676168.82 0.0, 930477.41 6676196.71 0.0, 930483.7 6676222.35 0.0, 930484.44 6676225.73 0.0, 930496.78 6676276.86 0.0, 930505.27 6676278.61 0.0, 930504.84 6676276.93 0.0, 930503.29 6676275.93 0.0, 930496.08 6676246.39 0.0, 930488.4 6676214.93 0.0, 930487.78 6676212.38 0.0, 930488.11 6676212.27 0.0, 930481.71 6676187.51 0.0, 930476.29 6676166.56 0.0, 930475.36 6676158.67 0.0, 930473.02 6676131.37 0.0, 930491.7 6676099.55 0.0, 930504.39 6676077.2 0.0))";

        double roadEpsilon = 0;
        double noise = 0;
        double maximalArea = 1200;
        double maximalWidth = 50;

        
		
		IPolygon pol = (IPolygon) FromGeomToSurface.convertGeom(WktGeOxygene.makeGeOxygene(strMuliPol)).get(0);
		System.out.println(pol.toString());
		
		OBBBlockDecomposition decomposition = new OBBBlockDecomposition(pol, maximalArea, maximalWidth, roadEpsilon);
		IFeatureCollection<IFeature> featColl = decomposition.decompParcel(noise);
		
	
		
		ShapefileWriter.write(featColl, "/tmp/tmp.shp");
	}

}
