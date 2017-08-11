package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.thema.mupcity.analyse.MergeRasterResultAndBati;

import com.google.common.io.Files;

public class SelecMUPOutput {
	File rootFile;

	public SelecMUPOutput(File rootfile) {
		rootFile = rootfile;
	}

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/tmp/test/"));
	}

	public static List<File> run(File rootfile) throws IOException {
		// automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile);
		return smo.run();
	}

	public List<File> run() throws IOException {
		File MupOutputFolder = new File(rootFile, "depotConfigSpat");
		File output = new File(rootFile, "output");
		output.mkdirs();
		ArrayList<File> listMupOutput = new ArrayList<File>();
		for (File rasterOutputFolder : MupOutputFolder.listFiles()) {
			if (rasterOutputFolder.getName().endsWith(".tif")) {
				File outputMup = new File(output, rasterOutputFolder.getName().replace(".tif", ""));
				outputMup.mkdirs();
				listMupOutput.add(outputMup);
				File outputMupRaster = new File(outputMup, rasterOutputFolder.getName());
				Files.copy(rasterOutputFolder, outputMupRaster);
				VectorizeMupOutput(outputMupRaster, outputMup);
			}
		}
		return listMupOutput;
	}

	public File VectorizeMupOutput(File rasterIn, File mupVector) throws IOException {
		// TODO one feature per cell. no option so the best thing may be to
		// intersect the produced vector with a grid. Findd a way to extract the
		// grid from MUP-City simulation (and put the eval value in it?)
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);
		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(false);
		GeneralParameterValue[] params = new GeneralParameterValue[] { policy, gridsize, useJaiRead };

		GridCoverage2DReader reader = new GeoTiffReader(rasterIn);
		GridCoverage2D coverage = reader.read(params);

		Collection<Number> nooData = Arrays.asList(-1, 0, 1);

		PolygonExtractionProcess coucou = new PolygonExtractionProcess();
		SimpleFeatureCollection vectorizedCells = coucou.execute(coverage, 0, true, null, nooData, null, null);
		SelectParcels.exportSFC(vectorizedCells, new File(mupVector, mupVector.getName()+ "-vectorized.shp"));
		return mupVector;

	}
}
