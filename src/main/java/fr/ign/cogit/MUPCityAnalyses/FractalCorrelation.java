package fr.ign.cogit.MUPCityAnalyses;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.thema.common.JTS;
import org.thema.common.swing.TaskMonitor;
import org.thema.data.IOImage;
import org.thema.fracgis.estimation.Estimation;
import org.thema.fracgis.estimation.EstimationFactory;
import org.thema.fracgis.method.raster.mono.CorrelationRasterMethod;
import org.thema.fracgis.sampling.DefaultSampling;
import org.thema.fracgis.sampling.Sampling.Sequence;

import fr.ign.cogit.SelecMUPOutput;
import fr.ign.cogit.SelectParcels;

public class FractalCorrelation {
//	public static double fracgisCorrelationVect(File shapefile) {
//
////		File f = new File("mypointshape.shp");
//////		new DefaultSampling(min, max, 1.5, Sequence.GEOM);
////		CorrelationMethod correlation = new CorrelationMethod(f.getName(), new DefaultSampling(), new DefaultFeatureCoverage(DefaultFeature.loadFeatures(f, true)));
////		correlation.execute(new TaskMonitor.EmptyMonitor(), true);
////		Estimation estim = new EstimationFactory(correlation).getDefaultEstimation();
////		double dim = estim.getDimension();
////	}
////
////	public static double fracgisBoundingBox(File shapefile) {
////
//	}
	public static void main(String[] args) throws Exception {
		File rootFile = new File("");
		int resolution = 4;
		
		//rasterisation du bati 
		SimpleFeatureCollection bati = (new ShapefileDataStore((new File("/home/mcolomb/donnee/couplage/donneeGeographiques/batiment.shp")).toURI().toURL())).getFeatureSource().getFeatures();
		
		File configFile = new File(rootFile, "/media/mcolomb/Data_2/resultExplo/testOct/exOct/N5_Ba_Moy_ahpx_seed_42/N5_Ba_Moy_ahpx_seed_42-eval_anal-20.0.tif");
		GridCoverage2D coverage = SelecMUPOutput.importRaster(configFile);
		
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		ReferencedEnvelope gridBounds = new ReferencedEnvelope(coverage.getEnvelope2D().getMinX(),
				coverage.getEnvelope2D().getMaxX(), coverage.getEnvelope2D().getMinY(),
				coverage.getEnvelope2D().getMaxY(), sourceCRS);
		float[][] imagePixelData = new float[((int) gridBounds.getHeight()/3)][((int)gridBounds.getWidth()/3)];
	//	float[][] imagePixelDataStable = new float[1467][1467];

		
		for (double i = gridBounds.getMinX(); i <  (gridBounds.getMaxX()-resolution); i = i + resolution){
			for (double j = gridBounds.getMinY(); j <  (gridBounds.getMaxY()-resolution); j = j+resolution){
			DirectPosition2D pt = new DirectPosition2D(i,j);
			float[] yo = (float[]) coverage.evaluate(pt);
			if (yo[0] > 0) {
					if (bati.contains(pt)){
						imagePixelData[(int)i][(int)j]=1;
					System.out.println("yo");	
					}
					else{
						imagePixelData[(int)i][(int)j]=0;
					}
				}
			}
		}
		
		GridCoverage2D toTestRaster = new GridCoverageFactory().create("test", imagePixelData, gridBounds);
	
		DefaultSampling dS = new DefaultSampling(22, 3000, 1.5, Sequence.GEOM);
		CorrelationRasterMethod correlation = new CorrelationRasterMethod("test", dS, toTestRaster.getRenderedImage(), JTS.rectToEnv(toTestRaster.getEnvelope2D()));

		correlation.execute(new TaskMonitor.EmptyMonitor(), true);

		Estimation estim = new EstimationFactory(correlation).getDefaultEstimation();

		System.out.println("dimension de corrélation " + estim.getDimension());
		System.out.println("R2 " + estim.getR2());
		System.out.println("Pvalue " + estim.getParamInfo());
	}
}