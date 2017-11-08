package fr.ign.cogit.MUPCityAnalyses;

import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.thema.common.JTS;
import org.thema.common.swing.TaskMonitor;
import org.thema.data.feature.DefaultFeatureCoverage;
import org.thema.data.feature.FeatureCoverage;
import org.thema.fracgis.estimation.Estimation;
import org.thema.fracgis.estimation.EstimationFactory;
import org.thema.fracgis.method.raster.mono.CorrelationRasterMethod;
import org.thema.fracgis.sampling.DefaultSampling;
import org.thema.fracgis.sampling.Sampling.Sequence;
import org.thema.mupcity.analyse.RasterMerge;
import org.thema.process.Rasterizer;

import fr.ign.cogit.MainTask;
import fr.ign.cogit.SelecMUPOutput;

public class FractalCorrelation {
	// public static double fracgisCorrelationVect(File shapefile) {
	//
	//// File f = new File("mypointshape.shp");
	////// new DefaultSampling(min, max, 1.5, Sequence.GEOM);
	//// CorrelationMethod correlation = new CorrelationMethod(f.getName(), new DefaultSampling(), new DefaultFeatureCoverage(DefaultFeature.loadFeatures(f, true)));
	//// correlation.execute(new TaskMonitor.EmptyMonitor(), true);
	//// Estimation estim = new EstimationFactory(correlation).getDefaultEstimation();
	//// double dim = estim.getDimension();
	//// }
	////
	//// public static double fracgisBoundingBox(File shapefile) {
	////
	// }
	public static void main(String[] args) throws Exception {
		File rootFile = new File("");
		int resolution = 4;

//		SimpleFeatureCollection bati = (new ShapefileDataStore((new File("/home/mcolomb/donnee/couplage/donneeGeographiques/batiment.shp")).toURI().toURL())).getFeatureSource()
//				.getFeatures();
//		 // rasterisation du bati
//		 List<Feature> yo = new ArrayList<Feature>();
//		 for (Object obj : bati.toArray()){
//		 Feature feat = (Feature) obj;
//		 yo.add(feat);
//		 }
//		
//		 DefaultFeatureCoverage featCov = new DefaultFeatureCoverage(yo);
//		 Rasterizer rast = new Rasterizer(featCov,4);
//		 WritableRaster wRaster = rast.rasterize(null);
		File rasterFile = new File(MainTask.class.getClassLoader().getResource("N5_Ba_Moy_ahpx_seed_42-eval_anal-20.0.tif").getPath());

		// File rasterFile = new File(rootFile, "/media/mcolomb/Data_2/resultExplo/testOct/exOct/N5_Ba_Moy_ahpx_seed_42/N5_Ba_Moy_ahpx_seed_42-eval_anal-20.0.tif");
		GridCoverage2D coverage = SelecMUPOutput.importRaster(rasterFile);

		System.out.println("minx : " + coverage.getEnvelope2D().getMinX() + "maxx : " + coverage.getEnvelope2D().getMaxX() + "miny : " + coverage.getEnvelope2D().getMinY()
				+ "maxY : " + coverage.getEnvelope2D().getMaxY());
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		ReferencedEnvelope gridBounds = new ReferencedEnvelope(coverage.getEnvelope2D().getMinX(), coverage.getEnvelope2D().getMaxX(), coverage.getEnvelope2D().getMinY(),
				coverage.getEnvelope2D().getMaxY(), sourceCRS);
		float[][] imagePixelData = new float[((int) gridBounds.getWidth() / resolution) + 1][((int) gridBounds.getHeight() / resolution) + 1];
		double Xmin = gridBounds.getMinX();
		double Xmax = gridBounds.getMaxX();
		double Ymin = gridBounds.getMinY();
		double Ymax = gridBounds.getMaxY();


		for (double i = Xmin + resolution; i < Xmax - resolution; i = i + resolution) {
			for (double j = Ymin + resolution; j < Ymax - resolution; j = j + resolution) {
				DirectPosition2D pt = new DirectPosition2D(i, j);
				float[] val = (float[]) coverage.evaluate(pt);
				if (val[0] > 0) {
					imagePixelData[(int) ((i - Xmin) / resolution)][(int) ((j - Ymin) / resolution)] = 1;
				} else {
					imagePixelData[(int) ((i - Xmin) / resolution)][(int) ((j - Ymin) / resolution)] = 0;
				}
				// for (Object obj : bati.toArray()) {
				// Feature feat = (Feature) obj;
				// if (feat.getDefaultGeometryProperty().getBounds().contains(pt)) {
				// imagePixelData[(int) ((i - Xmin) / resolution)][(int) ((i - Xmin) / resolution)] = 1;
				// }
				// }
			}
			// System.out.println("resume = " + com + " et " + teur);
		}

		System.out.println("résumons, la matrice fait " + imagePixelData.length + " par " + imagePixelData[0].length + " et la grille de couverture fait " + gridBounds.getWidth()
				+ " par " + gridBounds.getHeight()+ " ce qui donne des cellules de  " +  gridBounds.getWidth()/imagePixelData.length  + " and "
				+ gridBounds.getHeight()/ imagePixelData[0].length + " mètres");
		GridCoverage2D toTestRaster = new GridCoverageFactory().create("test", imagePixelData, gridBounds);

		writeGeotiff(new File("/home/mcolomb/tmp/rasterBin.tif"), toTestRaster);
		// DefaultSampling dS = new DefaultSampling(22, 3000, 1.5, Sequence.GEOM);
		// CorrelationRasterMethod correlation = new CorrelationRasterMethod("test", dS, toTestRaster.getRenderedImage(), JTS.rectToEnv(toTestRaster.getEnvelope2D()));
		//
		// correlation.execute(new TaskMonitor.EmptyMonitor(), true);
		//
		// Estimation estim = new EstimationFactory(correlation).getDefaultEstimation();
		//
		// System.out.println("dimension de corrélation " + estim.getDimension());
		// System.out.println("R2 " + estim.getR2());
		// System.out.println("Pvalue " + estim.getParamInfo());
	}

	public static void writeGeotiff(File fileName, GridCoverage2D coverage) {
		try {
			GeoTiffWriteParams wp = new GeoTiffWriteParams();
			wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
			wp.setCompressionType("LZW");
			ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
			params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
			GeoTiffWriter writer = new GeoTiffWriter(fileName);
			writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
		} catch (Exception e) {

			e.printStackTrace();
		}

	}
}