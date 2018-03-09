package fr.ign.cogit.MUPCityAnalyses;

import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.thema.common.JTS;
import org.thema.common.swing.TaskMonitor;
import org.thema.data.feature.DefaultFeature;
import org.thema.data.feature.DefaultFeatureCoverage;
import org.thema.data.feature.Feature;
import org.thema.fracgis.estimation.Estimation;
import org.thema.fracgis.estimation.EstimationFactory;
import org.thema.fracgis.method.raster.mono.CorrelationRasterMethod;
import org.thema.fracgis.sampling.DefaultSampling;
import org.thema.fracgis.sampling.Sampling.Sequence;
import org.thema.process.Rasterizer;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.SelecMUPOutput;

public class FractalCorrelation {

	public static void main(String[] args) throws Exception {
		int resolution = 4;
		File fileOut = new File("/home/mcolomb/tmp/rasterGood.tif");
		File batiFile = new File("/home/mcolomb/donnee/couplage/donneeGeographiques/batiment.shp");

		File rootFile = new File("/media/mcolomb/Data_2/resultExplo/dimFract/tmp/dimFract");
		for (File f : rootFile.listFiles()) {
			if (f.toString().contains("_Moy_ahpx_seed_42")) {
				for (File ff : f.listFiles()) {
					if (ff.toString().endsWith("_Moy_ahpx_seed_42-eval_anal-20.0.tif")) {
						String name = ff.getName().replaceAll("_Moy_ahpx_seed_42-eval_anal-20.0.tif", "");
						getCorrFracDim(batiFile, ff, fileOut, resolution, name);
						System.out.println("calculé pour " + name);
					}
				}
			}
		}
	}

	public static HashMap<String, Double> getCorrFracDim(File batiFile, File mupFile, File fileOut, int resolution, String name) throws IOException {
		HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String, Double>>();
		return results.put(name, calculFracCor(mergeBuildMUPResultRast(batiFile, mupFile, fileOut, resolution)));
	}

	public static GridCoverage2D mergeBuildMUPResultRast(File batiFile, File MUPFile, File fileOut, int resolution) throws IOException {

		GridCoverage2D coverage = SelecMUPOutput.importRaster(MUPFile);
		CoordinateReferenceSystem sourceCRS = coverage.getCoordinateReferenceSystem();
		ReferencedEnvelope gridBounds = new ReferencedEnvelope(coverage.getEnvelope2D().getMinX(), coverage.getEnvelope2D().getMaxX(), coverage.getEnvelope2D().getMinY(),
				coverage.getEnvelope2D().getMaxY(), sourceCRS);

		float[][] imagePixelData = new float[(int) Math.floor(gridBounds.getWidth() / resolution)][(int) Math.floor(gridBounds.getHeight() / resolution)];
		double Xmin = gridBounds.getMinX();
		double Ymin = gridBounds.getMinY();

		File rasterBatiFile = rasterize(batiFile, new File("/home/mcolomb/tmp/rasterBati.tif"));
		GridCoverage2D rasterBati = SelecMUPOutput.importRaster(rasterBatiFile);

		for (int i = 0; i < imagePixelData.length; ++i) {
			for (int j = 0; j < imagePixelData[0].length; ++j) {
				DirectPosition2D pt = new DirectPosition2D(Xmin + (2 * i + 1) * resolution / 2, Ymin + (2 * j + 1) * resolution / 2);

				float[] val = (float[]) coverage.evaluate(pt);
				byte[] bat = (byte[]) rasterBati.evaluate(pt);

				if (val[0] > 0 || bat[0] > 0) {
					imagePixelData[i][j] = 1;
				} else {
					imagePixelData[i][j] = 0;
				}
			}
		}

		// transfo to put into a new rasterFile (yeah, too complicated)
		float[][] imgpix2 = new float[imagePixelData[0].length][imagePixelData.length];
		float[][] imgpix3 = new float[imagePixelData[0].length][imagePixelData.length];

		for (int i = 0; i < imgpix2.length; ++i) {
			for (int j = 0; j < imgpix2[0].length; ++j) {
				imgpix2[i][j] = imagePixelData[imgpix2[0].length - 1 - j][i];
			}
		}

		for (int i = 0; i < imgpix3.length; ++i) {
			for (int j = 0; j < imgpix3[0].length; ++j) {
				imgpix3[i][j] = imgpix2[imgpix3.length - 1 - i][imgpix3[0].length - 1 - j];
			}
		}

		GridCoverage2D toTestRaster = new GridCoverageFactory().create("bati", imgpix3, gridBounds);
		writeGeotiff(fileOut, toTestRaster);
		return toTestRaster;
	}

	public static HashMap<String, Double> calculFracCor(GridCoverage2D toTestRaster) {

		DefaultSampling dS = new DefaultSampling(22, 3000, 1.5, Sequence.GEOM);
		CorrelationRasterMethod correlation = new CorrelationRasterMethod("test", dS, toTestRaster.getRenderedImage(), JTS.rectToEnv(toTestRaster.getEnvelope2D()));

		correlation.execute(new TaskMonitor.EmptyMonitor(), true);

		Estimation estim = new EstimationFactory(correlation).getDefaultEstimation();

		HashMap<String, Double> values = new HashMap<String, Double>();
		values.put("dimension de corrélation ", estim.getDimension());
		values.put("R2", estim.getR2());
		values.put("Pvalue", null);
		System.out.println("dimension de corrélation " + estim.getDimension());
		System.out.println("R2 " + estim.getR2());
		System.out.println("Pvalue " + estim.getParamInfo());

		return values;
	}

	public static File rasterize(File batiFile, File fileOut) throws MalformedURLException, IOException {
		HashSet<Feature> batiCol = new HashSet<>();
		if (!fileOut.exists()) {
			ShapefileDataStore batiDS = new ShapefileDataStore((batiFile).toURI().toURL());
			SimpleFeatureCollection bati = batiDS.getFeatureSource().getFeatures();
			CoordinateReferenceSystem sourceCRS = bati.getSchema().getCoordinateReferenceSystem();
			// rasterisation avec les outils de théma
			// create a thema collection
			int h = 0;
			SimpleFeatureIterator batiIt = bati.features();
			try {
				while (batiIt.hasNext()) {
					Feature f = new DefaultFeature((Object) h, (Geometry) batiIt.next().getDefaultGeometry());
					h = h + 1;
					batiCol.add(f);
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				batiIt.close();
			}

			DefaultFeatureCoverage featCov = new DefaultFeatureCoverage(batiCol);
			Rasterizer rast = new Rasterizer(featCov, 4);
			WritableRaster wRaster = rast.rasterize(null);
			ReferencedEnvelope envBati = new ReferencedEnvelope(featCov.getEnvelope().getMinX(), featCov.getEnvelope().getMaxX(), featCov.getEnvelope().getMinY(),
					featCov.getEnvelope().getMaxY(), sourceCRS);
			GridCoverage2D rasterBati = new GridCoverageFactory().create("bati", wRaster, envBati);
			writeGeotiff(fileOut, rasterBati);
			batiDS.dispose();
		}
		return fileOut;
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