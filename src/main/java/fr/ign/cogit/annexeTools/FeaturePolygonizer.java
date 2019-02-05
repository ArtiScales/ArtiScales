package fr.ign.cogit.annexeTools;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Function;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.LinearComponentExtracter;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

public class FeaturePolygonizer {
  private static GeometryFactory fact = new GeometryFactory();

  private static List<Geometry> getLines(List<Geometry> inputFeatures) {
    List<Geometry> linesList = new ArrayList<Geometry>();
    LinearComponentExtracter lineFilter = new LinearComponentExtracter(linesList);
    for (Geometry feature : inputFeatures)
      feature.apply(lineFilter);
    return linesList;
  }

  private static Point extractPoint(List<Geometry> lines) {
    Point point = null;
    // extract first point from first non-empty geometry
    for (Geometry geometry : lines) {
      if (!geometry.isEmpty()) {
        Coordinate p = geometry.getCoordinate();
        point = geometry.getFactory().createPoint(p);
        break;
      }
    }
    return point;
  }

  private static List<Geometry> nodeLines(List<Geometry> lines) {
    MultiLineString linesGeom = fact.createMultiLineString(lines.toArray(new LineString[lines.size()]));
    Geometry unionInput = fact.createMultiLineString(null);
    Point point = extractPoint(lines);
    if (point != null)
      unionInput = point;
    Geometry noded = linesGeom.union(unionInput);
    List<Geometry> nodedList = new ArrayList<Geometry>();
    nodedList.add(noded);
    return nodedList;
  }

  private static List<Geometry> getFeatures(File aFile, Function<SimpleFeature, Boolean> filter) throws IOException {
    ShapefileDataStore store = new ShapefileDataStore(aFile.toURI().toURL());
    ArrayList<Geometry> array = new ArrayList<Geometry>();
    FeatureReader<SimpleFeatureType, SimpleFeature> reader = store.getFeatureReader();
    while (reader.hasNext()) {
      SimpleFeature feature = reader.next();
      if (filter.apply(feature))
        array.add((Geometry) feature.getDefaultGeometry());
    }
    reader.close();
    store.dispose();
    return array;
  }

  private static void addFeatures(Polygonizer p, List<Geometry> inputFeatures) throws MalformedURLException, IOException, SchemaException {
    System.out.println(Calendar.getInstance().getTime() + " node lines");
    List<Geometry> reduced = new ArrayList<Geometry>();
    for (Geometry g : inputFeatures) {
      reduced.add(GeometryPrecisionReducer.reduce(g, new PrecisionModel(100)));
    }
//    File out = new File("./out");
//    saveGeometries(reduced, new File(out, "1_reduced.shp"), "Polygon");
    List<Geometry> lines = getLines(reduced);
//    saveGeometries(lines, new File(out, "2_lines.shp"), "LineString");
    List<Geometry> nodedLines = nodeLines(lines);
//    saveGeometries(nodedLines, new File(out, "3_noded.shp"), "LineString");
    // noding a second time to be sure
    MultiLineString mls = (MultiLineString) nodedLines.get(0);
    List<Geometry> geoms = new ArrayList<>(mls.getNumGeometries());
    for (int i = 0 ; i < mls.getNumGeometries() ; i++) geoms.add(mls.getGeometryN(i));
    nodedLines = nodeLines(geoms);
//    saveGeometries(nodedLines, new File(out, "4_noded.shp"), "LineString");
    int size = nodedLines.size();
    System.out.println(Calendar.getInstance().getTime() + " insert lines (" + size + ")");
    for (Geometry geometry : nodedLines)
      p.add(geometry);
  }

  @SuppressWarnings("unchecked")
  public static List<Polygon> getPolygons(List<Geometry> features) throws IOException, SchemaException {
    Polygonizer polygonizer = new Polygonizer();
    addFeatures(polygonizer, features);
    System.out.println(Calendar.getInstance().getTime() + " now with the real stuff");
    List<Polygon> result = new ArrayList<>();
    result.addAll(polygonizer.getPolygons());
    System.out.println(Calendar.getInstance().getTime() + " all done now");
//    for (Polygon p : result)
//      System.out.println(p);
//    System.out.println(Calendar.getInstance().getTime() + " all done now");
    return result;
  }

  public static List<Polygon> getPolygons(File[] files) throws IOException, SchemaException {
    List<Geometry> features = new ArrayList<>();
    for (File file : files) {
      System.out.println(Calendar.getInstance().getTime() + " handling " + file);
      features.addAll(getFeatures(file, f -> true));
    }
    System.out.println(Calendar.getInstance().getTime() + " adding features");
    return getPolygons(features);
  }

  public static List<Polygon> getPolygons(SimpleFeatureCollection sFC) throws IOException, SchemaException {
    List<Geometry> features = new ArrayList<>();
    SimpleFeatureIterator sFCit = sFC.features();

    try {
      while (sFCit.hasNext()) {
        features.add((Geometry) sFCit.next().getDefaultGeometry());
      }
    } catch (Exception problem) {
      problem.printStackTrace();
    } finally {
      sFCit.close();
    }
    System.out.println(Calendar.getInstance().getTime() + " adding features");
    return getPolygons(features);
  }

  public static void saveGeometries(List<? extends Geometry> geoms, File file, String geomType) throws MalformedURLException, IOException, SchemaException {
    String specs = "geom:"+ geomType +":srid=2154";
    ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
    FileDataStore dataStore = factory.createDataStore(file.toURI().toURL());
    String featureTypeName = "Object";
    SimpleFeatureType featureType = DataUtilities.createType(featureTypeName, specs);
    dataStore.createSchema(featureType);
    String typeName = dataStore.getTypeNames()[0];
    FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT);
    System.setProperty("org.geotools.referencing.forceXY", "true");
    System.out.println(Calendar.getInstance().getTime() + " write shapefile");
    for (Geometry g : geoms) {
      SimpleFeature feature = writer.next();
      feature.setAttributes(new Object[] { g });
      writer.write();
    }
    System.out.println(Calendar.getInstance().getTime() + " done");
    writer.close();
    dataStore.dispose();
  }

  public static void main(String[] args) throws MalformedURLException, IOException, SchemaException {
    // input folder for shapefiles
    // File folderData = new File("./data/pau");
    // take all shapefiles in the folder
    // File[] files = folderData.listFiles((dir, name) -> name.endsWith(".shp"));
    File[] files = { new File("./ArtiScales20190204/tmp/tmpParcel.shp"), new File("./ArtiScales20190204/dataRegulation/zoning.shp") };
    // output folder for shapefiles
    File folder = new File("./out");
    folder.mkdirs();
    File out = new File(folder, "polygon.shp");
    // build polygons
    List<Polygon> polygons = getPolygons(files);
    saveGeometries(polygons, out, "Polygon");
  }
}
