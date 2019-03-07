package fr.ign.cogit.map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
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
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;

public class MapRenderer {
  private Rectangle imageBounds;
  StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
  FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

  public MapRenderer(int imageWidth, int imageHeight) {
    this.imageBounds = new Rectangle(0, 0, imageWidth, imageHeight);
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
    Stroke inputStroke = this.styleFactory.stroke(color, null, this.filterFactory.literal(exteriorWidth), null, ConstantExpression.constant("round"), null, null);
    Symbolizer symbolizer = this.styleFactory.createLineSymbolizer(inputStroke, "the_geom");
    inputrule.symbolizers().add(symbolizer);
    FeatureTypeStyle inputfeatureTypeStyle = styleFactory.createFeatureTypeStyle();
    inputfeatureTypeStyle.rules().add(inputrule);
    Style style = this.styleFactory.createStyle();
    style.featureTypeStyles().add(inputfeatureTypeStyle);
    return style;
  }

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

  public static void main(String[] args) {
    MapRenderer renderer = new MapRenderer(2000,1000);
    File parent = new File("/home/julien/data/ArtiScales/donneeGeographiques");
    renderer.render(
        new File(parent, "parcelle.shp"), 
        new File(parent, "batimentPro.shp"), 
        new File(parent, "routeSys.shp"), 
        new File("/tmp/render.png"));
  }
}
