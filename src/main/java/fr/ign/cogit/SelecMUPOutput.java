package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	public SelecMUPOutput(File rootfile) {
		rootFile = rootfile;
	}

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"));
	}

	public static List<File> run(File rootfile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException {
		// automatic vectorization of the MUP-City outputs
		SelecMUPOutput smo = new SelecMUPOutput(rootfile);
		return smo.run();
	}

	public List<File> run() throws IOException, NoSuchAuthorityCodeException, FactoryException, ParseException {

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
				File vectFile = new File(outputMup, outputMup.getName() + "-vectorized.shp");
				if (!vectFile.exists()) {
					createMupOutput(importRaster(outputMupRaster), vectFile);
				}
			}
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

		// s'eut aurait été la méthode propre pour créer un nouvel attribut,
		// mais je me suis arraché trop de poils de barbe et ça ne marche
		// toujours pas
		// AttributeTypeBuilder attBuilder = new AttributeTypeBuilder();
		// attBuilder.setName("eval");
		// attBuilder.setBinding(Float.class);
		// attBuilder.setNillable(false);
		// attBuilder.defaultValue(0);
		// System.out.println(attBuilder.buildDescriptor("eval"));
		// System.out.println(attBuilder.buildDescriptor("eval").getType());
		// System.out.println(attBuilder.buildDescriptor("eval").getType().getBinding());
		// sfTypeBuilder.add(attBuilder.buildDescriptor("eval",
		// attBuilder.buildType()));
		// sfTypeBuilder.addBinding(attBuilder.buildType());

		sfTypeBuilder.add("eval", Float.class);

		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder sfBuilder = new SimpleFeatureBuilder(featureType);

		DefaultFeatureCollection victory = new DefaultFeatureCollection();

		SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, 20.0);

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

	public GridCoverage2D importRaster(File rasterIn) throws IOException {
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);
		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		gridsize.setValue(20 + "," + 20);
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);
		GeneralParameterValue[] params = new GeneralParameterValue[] { policy, gridsize, useJaiRead };
		GridCoverage2DReader reader = new GeoTiffReader(rasterIn);
		GridCoverage2D coverage = reader.read(params);
		return coverage;
	}
}
