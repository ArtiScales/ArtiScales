package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class SelecMUPOutput {
	File rootFile;
	double sizeCell;
	File rasterOutputFolder;

	public SelecMUPOutput(File rootfile, File rasteroutputfolder) {
		rootFile = rootfile;
		rasterOutputFolder = rasteroutputfolder;
	}

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"),
				new File("/home/mcolomb/donnee/couplage/depotConfigSpat/N5_Ba_Moy_ahpx_seed42-eval_anal-20.0.tif"));
	}

	public static List<File> run(File rootfile, File rasteroutputfolder)
			throws Exception {
		// automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile, rasteroutputfolder);
		return smo.run();
	}

	public List<File> run() throws Exception {
		File output = new File(rootFile, "output");
		output.mkdirs();
		
		ArrayList<File> listMupOutput = new ArrayList<File>();
		// get the cells size
		String rasterOutputString = rasterOutputFolder.getName().replace(".tif", "");
		Pattern ech = Pattern.compile("-");
		
		String[] list = ech.split(rasterOutputString);
		sizeCell = Double.parseDouble(list[2]);
		
		File outputMup = new File(output, rasterOutputString);
		outputMup.mkdirs();
		
		
		listMupOutput.add(outputMup);
		File outputMupRaster = new File(outputMup, rasterOutputFolder.getName());

		Files.copy(rasterOutputFolder, outputMupRaster);
		File vectFile = new File(outputMup, outputMup.getName() + "-vectorized.shp");
		if (!vectFile.exists()) {
			createMupOutput(importRaster(outputMupRaster), vectFile);
		}

		return listMupOutput;
	}

	public SimpleFeatureSource createMupOutput(GridCoverage2D coverage, File destFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException {

		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		ReferencedEnvelope gridBounds = new ReferencedEnvelope(coverage.getEnvelope2D().getMinX(),
				coverage.getEnvelope2D().getMaxX(), coverage.getEnvelope2D().getMinY(),
				coverage.getEnvelope2D().getMaxY(), sourceCRS);

		WKTReader wktReader = new WKTReader();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();

		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");

		sfTypeBuilder.add("eval", Float.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection victory = new DefaultFeatureCollection();

		SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, sizeCell);

		int i = 0;
		for (Object object : grid.getFeatures().toArray()) {

			SimpleFeature feat = (SimpleFeature) object;

			DirectPosition2D coord = new DirectPosition2D(
					(feat.getBounds().getMaxX() - feat.getBounds().getHeight() / 2),
					(feat.getBounds().getMaxY() - feat.getBounds().getHeight() / 2));
			float[] yo = (float[]) coverage.evaluate(coord);
			if (yo[0] > 0) {
				i = i + 1;
				Object[] attr = { yo[0] };
				sfBuilder.add(wktReader.read(feat.getDefaultGeometry().toString()));
				SimpleFeature feature = sfBuilder.buildFeature("id" + i, attr);
				victory.add(feature);
			}
		}
		SelectParcels.exportSFC(victory.collection(), destFile);
		return grid;
	}

	public static GridCoverage2D importRaster(File rasterIn) throws IOException {
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);
		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);
		GeneralParameterValue[] params = new GeneralParameterValue[] { policy, gridsize, useJaiRead };
		GridCoverage2DReader reader = new GeoTiffReader(rasterIn);
		GridCoverage2D coverage = reader.read(params);
		return coverage;
	}
}
