package fr.ign.cogit.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.ConstantExpression;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class MapRenderer {
	private Rectangle imageBounds;
	protected File sldFile, svgFile, rootMapStyle, toMapShapeFile, outFolder;

	protected String mapName, text, legendName;

	StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
	FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

	public MapRenderer(int imageWidth, int imageHeight, String mapname, String text, File rootMapstyle, File svgfile, File tomapshp, File outfolder) {
		this.outFolder = outfolder;
		this.mapName = mapname;
		this.text = text;
		this.imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
		this.rootMapStyle = rootMapstyle;
		this.sldFile = new File(rootMapstyle, mapName + ".sld");
		this.svgFile = svgfile;
		this.toMapShapeFile = tomapshp;
		this.legendName = mapname;
	}

	private static String format(int v) {
		return String.format("%02X", Math.min(255, Math.max(0, v)));
	}

	private Style polygonStyle(int red, int green, int blue) {
		Literal color = this.filterFactory.literal("#" + format(red) + format(green) + format(blue));
		Rule inputrule = this.styleFactory.createRule();
		Fill inputFill = this.styleFactory.fill(null, color, null);
		Symbolizer symbolizer = this.styleFactory.createPolygonSymbolizer(null, inputFill, "the_geom");
		inputrule.symbolizers().add(symbolizer);
		FeatureTypeStyle inputfeatureTypeStyle = styleFactory.createFeatureTypeStyle();
		inputfeatureTypeStyle.rules().add(inputrule);
		Style style = this.styleFactory.createStyle();
		style.featureTypeStyles().add(inputfeatureTypeStyle);
		return style;
	}

	private Style lineStyle(int red, int green, int blue, double exteriorWidth) {
		Literal color = this.filterFactory.literal("#" + format(red) + format(green) + format(blue));
		Rule inputrule = this.styleFactory.createRule();
		Stroke inputStroke = this.styleFactory.stroke(color, null, this.filterFactory.literal(exteriorWidth), null,
				ConstantExpression.constant("round"), null, null);
		Symbolizer symbolizer = this.styleFactory.createLineSymbolizer(inputStroke, "the_geom");
		inputrule.symbolizers().add(symbolizer);
		FeatureTypeStyle inputfeatureTypeStyle = styleFactory.createFeatureTypeStyle();
		inputfeatureTypeStyle.rules().add(inputrule);
		Style style = this.styleFactory.createStyle();
		style.featureTypeStyles().add(inputfeatureTypeStyle);
		return style;
	}

	// private Style aplatStyle(SimpleFeatureCollection collec, double exteriorWidth) {
	// FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	// Function classify = ff.function("Quantile", ff.property("obj_2035"), ff.literal(2));
	// Classifier objectif = (Classifier) classify.evaluate(collec);
	// System.out.println("categories : "+objectif.getTitles());
	//
	// // create a partially opaque outline stroke
	// Stroke stroke = styleFactory.createStroke(
	// filterFactory.literal(Color.BLACK),
	// filterFactory.literal(1),
	// filterFactory.literal(1));
	// Rule rule = styleFactory.createRule();
	// Symbolizer symbolizer = this.styleFactory.polygonSymbolizer("objectif", "the_geom", objectif, unit, stroke, fill, displacement, offset)
	// rule.symbolizers().add(symbolizer);
	//
	//
	// // create a partial opaque fill
	// FeatureTypeStyle zob = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
	//
	//
	//// Fill fill = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
	//// filterFactory.literal(Color.CYAN),
	//// filterFactory.literal(0.5));
	//
	// /*
	// * Setting the geometryPropertyName arg to null signals that we want to
	// * draw the default geomettry of features
	// */
	//
	//
	// PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke, fill, null);
	//
	// Rule rule = styleFactory.createRule();
	// rule.symbolizers().add(sym);
	// FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle(new Rule[]{rule});
	// Style style = styleFactory.createStyle();
	// style.featureTypeStyles().add(fts);
	//
	// FeatureTypeStyle style = StyleGenerator.createFeatureTypeStyle(
	// groups,
	// propteryExpression,
	// colors,
	// "Generated FeatureTypeStyle for GreeBlue",
	// featureCollection.getSchema().getGeometryDescriptor(),
	// StyleGenerator.ELSEMODE_IGNORE,
	// 0.95,
	// null);
	//
	// Literal color = this.filterFactory.literal("#" + format(red) + format(green) + format(blue));
	// Rule inputrule = this.styleFactory.createRule();
	// Stroke inputStroke = this.styleFactory.stroke(color, null, this.filterFactory.literal(exteriorWidth), null, ConstantExpression.constant("round"), null, null);
	// Symbolizer symbolizer = this.styleFactory.createLineSymbolizer(inputStroke, "the_geom");
	// inputrule.symbolizers().add(symbolizer);
	// FeatureTypeStyle inputfeatureTypeStyle = styleFactory.createFeatureTypeStyle();
	// inputfeatureTypeStyle.rules().add(inputrule);
	// Style style = this.styleFactory.createStyle();
	// style.featureTypeStyles().add(inputfeatureTypeStyle);
	// return style;
	// }

	private Layer toLayer(File f, Style style) {
		try {
			ShapefileDataStore store = new ShapefileDataStore(f.toURI().toURL());
			SimpleFeatureCollection features = store.getFeatureSource().getFeatures();
			return new FeatureLayer(features, style);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void saveImage(final BufferedImage image, final File file) {
		try {
			ImageIO.write(image, "png", file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// private Graphics2D drawLegend(Drawer drawer, BufferedImage img, Style s) {
	// SimpleFeature feature = null;
	// Symbolizer sym = s.getDefaultSpecification();
	// if (sym instanceof LineSymbolizer) {
	// LineString line = drawer.line(new int[] { 1, 1, 10, 20, 20, 20 });
	// feature = drawer.feature(line);
	// } else if (sym instanceof PolygonSymbolizer) {
	// Polygon p = drawer.polygon(new int[] { 1, 1, 1, 18, 18, 18, 18, 1, 1, 1 });
	// feature = drawer.feature(p);
	// } else if (sym instanceof PointSymbolizer || sym instanceof TextSymbolizer) {
	// Point p = drawer.point(10, 10);
	// feature = drawer.feature(p);
	// }
	// drawer.drawDirect(img, feature, s);
	// Graphics2D gr = img.createGraphics();
	// return gr;
	// //
	// // DefaultFeatureCollection df = new DefaultFeatureCollection();
	// // df.add(feature);
	// // return new FeatureLayer(df, s);
	// }

	// public void renderLegend(File communityFile, File sLDFile, File out)
	// throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
	// MapContent map = new MapContent();
	//
	// Style style = (new SLDParser(styleFactory, sLDFile.toURI().toURL())).readXML()[0];
	// Layer communityLayer = toLayer(communityFile, style);
	//
	// BufferedImage legend = new BufferedImage(this.imageBounds.width / 2, this.imageBounds.height / 2, BufferedImage.TYPE_INT_ARGB);
	// Graphics2D gr = drawLegend(Drawer.create(), legend, style);
	//// map.addLayer(gr);
	// GTRenderer renderer = new StreamingRenderer();
	// renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
	// Map<Object, Object> rendererParams = new HashMap<Object, Object>();
	// rendererParams.put("optimizedDataLoadingEnabled", new Boolean(true));
	// rendererParams.put("forceCRS", CRS.decode("EPSG:2154"));
	// renderer.setRendererHints(rendererParams);
	// renderer.setMapContent(map);
	//
	// renderer.paint(gr, this.imageBounds, communityLayer.getBounds());
	// map.dispose();
	// saveImage(legend, out);
	// }

	// public void renderCityInfoWithLegend(File communityFile, File sLDFile, File out)
	// throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
	// MapContent map = new MapContent();
	//
	// Style style = (new SLDParser(styleFactory, sLDFile.toURI().toURL())).readXML()[0];
	// Layer communityLayer = toLayer(communityFile, style);
	//
	// if (communityLayer != null)
	// map.addLayer(communityLayer);
	//
	// BufferedImage img = new BufferedImage(this.imageBounds.width, this.imageBounds.height, BufferedImage.TYPE_INT_ARGB);
	// BufferedImage legend = new BufferedImage(this.imageBounds.width / 2, this.imageBounds.height / 2, BufferedImage.TYPE_INT_ARGB);
	// Layer grr = drawLegend(Drawer.create(), legend, style);
	// map.addLayer(grr);
	// GTRenderer renderer = new StreamingRenderer();
	// renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
	// Map<Object, Object> rendererParams = new HashMap<Object, Object>();
	// rendererParams.put("optimizedDataLoadingEnabled", new Boolean(true));
	// rendererParams.put("forceCRS", CRS.decode("EPSG:2154"));
	// renderer.setRendererHints(rendererParams);
	// renderer.setMapContent(map);
	//
	// int wid = img.getWidth() + legend.getWidth();
	// int height = Math.max(img.getHeight(), legend.getHeight());
	// // create a new buffer and draw two image into the new image
	// BufferedImage newImage = new BufferedImage(wid, height, BufferedImage.TYPE_INT_ARGB);
	// Graphics2D gr = newImage.createGraphics();
	// gr.drawImage(img, null, 0, 0);
	// gr.drawImage(legend, null, img.getWidth(), 0);
	//
	// renderer.paint(gr, this.imageBounds, communityLayer.getBounds());
	// map.dispose();
	// saveImage(newImage, out);
	// }
	//

	public void renderCityInfo() throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
		renderCityInfo(new File(outFolder, mapName + "-map.png"));
	}

	public void renderCityInfo(String name) throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
		renderCityInfo(new File(outFolder, name + "-map.png"));
	}

	public void renderCityInfo(File out) throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
		MapContent map = new MapContent();

		Style style = (new SLDParser(styleFactory, sldFile.toURI().toURL())).readXML()[0];
		Layer communityLayer = toLayer(toMapShapeFile, style);

		if (communityLayer != null) {
			map.addLayer(communityLayer);
		}
		BufferedImage img = new BufferedImage(this.imageBounds.width, this.imageBounds.height, BufferedImage.TYPE_INT_ARGB);
		GTRenderer renderer = new StreamingRenderer();
		renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		Map<Object, Object> rendererParams = new HashMap<Object, Object>();
		rendererParams.put("optimizedDataLoadingEnabled", new Boolean(true));
		rendererParams.put("forceCRS", CRS.decode("EPSG:2154"));
		renderer.setRendererHints(rendererParams);
		renderer.setMapContent(map);

		// create a new buffer and draw two image into the new image
		Graphics2D gr = img.createGraphics();
		gr.drawImage(img, null, 0, 0);

		renderer.paint(gr, this.imageBounds, communityLayer.getBounds());
		map.dispose();
		saveImage(img, out);
	}

	public static BufferedImage joinBufferedImage(BufferedImage img1, BufferedImage img2) {

		// do some calculate first
		int offset = 5;
		int wid = img1.getWidth() + img2.getWidth() + offset;
		int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
		// create a new buffer and draw two image into the new image
		BufferedImage newImage = new BufferedImage(wid, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = newImage.createGraphics();
		Color oldColor = g2.getColor();
		// fill background
		g2.setPaint(Color.WHITE);
		g2.fillRect(0, 0, wid, height);
		// draw image
		g2.setColor(oldColor);
		g2.drawImage(img1, null, 0, 0);
		g2.drawImage(img2, null, img1.getWidth() + offset, 0);
		g2.dispose();
		return newImage;
	}

	public void render(File parcels, File buildings, File roads, File out) {
		MapContent map = new MapContent();
		Layer roadLayer = toLayer(roads, lineStyle(255, 0, 0, 2.0));
		if (roadLayer != null)
			map.addLayer(roadLayer);
		Layer parcelLayer = toLayer(parcels, polygonStyle(0, 255, 0));
		if (parcelLayer != null)
			map.addLayer(parcelLayer);
		Layer buildingLayer = toLayer(buildings, polygonStyle(0, 0, 0));
		if (buildingLayer != null)
			map.addLayer(buildingLayer);
		BufferedImage image = new BufferedImage(this.imageBounds.width, this.imageBounds.height, BufferedImage.TYPE_INT_ARGB);
		GTRenderer renderer = new StreamingRenderer();
		renderer.setJava2DHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
		Map<Object, Object> rendererParams = new HashMap<Object, Object>();
		rendererParams.put("optimizedDataLoadingEnabled", new Boolean(true));
		renderer.setRendererHints(rendererParams);
		renderer.setMapContent(map);
		Graphics2D gr = image.createGraphics();
		renderer.paint(gr, this.imageBounds, parcelLayer.getBounds());
		map.dispose();
		saveImage(image, out);
	}

	public File generateSVG() throws IOException {
		return generateSVG(new File(outFolder, mapName + ".svg"), mapName);
	}

	public File generateSVG(File svgOutFile, String imgName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(svgFile));
		BufferedWriter bw = new BufferedWriter(new FileWriter(svgOutFile));
		StringBuffer sb = new StringBuffer();
		for (Object line : br.lines().toArray()) {
			if (((String) line).contains("sodipodi:absref=")) {
				String newLine = "sodipodi:absref=\"" + outFolder.getAbsolutePath() + "/" + imgName + "-map.png\"";
				if (((String) line).contains("legend")) {
					newLine = "sodipodi:absref=\"" + rootMapStyle.getAbsolutePath() + "/" + legendName + "-legend.png\"";
				}
				sb.append(newLine + "\n");
			} else if (((String) line).contains("inkscape:export-filename")) {
				String newLine = "inkscape:export-filename=\"" + "./" + mapName + ".png\"";
				sb.append(newLine + "\n");
			} else if (((String) line).contains("> </flowPara>")) {
				String newLine = ((String) line).replace("> </flowPara>", ">" + text + "</flowPara>");
				sb.append(newLine + "\n");
			} else {
				sb.append(line + "\n");
			}
		}
		br.close();
		bw.write(sb.toString(), 0, sb.length());
		bw.close();

		// BufferedImage input_image = ImageIO.read(svgFile); // read svginto input_image object
		// File outputfile = new File(outFolder, mapName + ".png");
		// ImageIO.write(input_image, "PNG", outputfile);

		return svgFile;
	}

	// public static void main(String[] args) throws MalformedURLException, IOException, NoSuchAuthorityCodeException, FactoryException {
	//
	//
	// MapRenderer renderer = new MapRenderer(1000, 1000,new File("/home/ubuntu/boulot/these/result0308/mapStyle/diffObjLgt.sld"), );
	// renderer.renderCityInfo(new File("/home/ubuntu/boulot/these/result0308/indic/bTH/DDense/variante0/commStat.shp"),
	// new File("/tmp/zob.png"));
	// (
	// new File(parent, "parcelle.shp"),
	// new File(parent, "batimentPro.shp"),
	// new File(parent, "routeSys.shp"),
	// new File("/tmp/render.png"));
	// }
}
