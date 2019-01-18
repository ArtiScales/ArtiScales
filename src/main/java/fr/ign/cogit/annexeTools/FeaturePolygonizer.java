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

  private static void addFeatures(Polygonizer p, List<Geometry> inputFeatures) {
    System.out.println(Calendar.getInstance().getTime() + " node lines");
    List<Geometry> reduced = new ArrayList<Geometry>();
    for (Geometry g : inputFeatures) {
    	reduced.add(GeometryPrecisionReducer.reduce(g,new PrecisionModel(100)));
    }
    List<Geometry> lines = getLines(reduced);
    List<Geometry> nodedLines = nodeLines(lines);
    int size = nodedLines.size();
    System.out.println(Calendar.getInstance().getTime() + " insert lines (" + size + ")");
    for (Geometry geometry : nodedLines) p.add(geometry);
  }
  
  @SuppressWarnings("unchecked")
  public static List<Polygon> getPolygons(List<Geometry> features) throws IOException {
    Polygonizer polygonizer = new Polygonizer();
    addFeatures(polygonizer, features);
    System.out.println(Calendar.getInstance().getTime() + " now with the real stuff");
    List<Polygon> result = new ArrayList<>();
    result.addAll(polygonizer.getPolygons());
    return result;    
  }

  public static List<Polygon> getPolygons(File[] files) throws IOException {
    List<Geometry> features = new ArrayList<>();
    for (File file : files) {
      System.out.println(Calendar.getInstance().getTime() + " handling " + file);
      features.addAll(getFeatures(file, f -> true));
    }
    System.out.println(Calendar.getInstance().getTime() + " adding features");
    return getPolygons(features);
  }

  public static List<Polygon> getPolygons(SimpleFeatureCollection sFC) throws IOException {
	    List<Geometry> features = new ArrayList<>();
	    SimpleFeatureIterator sFCit = sFC.features();
		
	    try {
			while (sFCit.hasNext()) {
				features.add((Geometry)sFCit.next().getDefaultGeometry());
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			sFCit.close();
		}
	    System.out.println(Calendar.getInstance().getTime() + " adding features");
	    return getPolygons(features);
	  }
  
	public static void main(String[] args) throws MalformedURLException, IOException, SchemaException {
    // input folder for shapefiles
    File folderData = new File("./data/pau");
    // output folder for shapefiles
    File folder = new File("./out");
    folder.mkdirs();
    // take all shapefiles in the folder
    File[] files = folderData.listFiles((dir, name) -> name.endsWith(".shp"));
    // build polygons
    List<Polygon> polygons = getPolygons(files);
    String specs = "geom:Polygon:srid=2154";
    File out = new File(folder, "polygon.shp");
    ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
    FileDataStore dataStore = factory.createDataStore(out.toURI().toURL());
    String featureTypeName = "Object";
    SimpleFeatureType featureType = DataUtilities.createType(featureTypeName, specs);
    dataStore.createSchema(featureType);
    String typeName = dataStore.getTypeNames()[0];
    FeatureWriter<SimpleFeatureType, SimpleFeature> writer = dataStore.getFeatureWriterAppend(typeName, Transaction.AUTO_COMMIT);
    System.setProperty("org.geotools.referencing.forceXY", "true");
    System.out.println(Calendar.getInstance().getTime() + " write shapefile");
    for (Polygon p : polygons) {
      SimpleFeature feature = writer.next();
      feature.setAttributes(new Object[] { p });
      writer.write();
    }
    System.out.println(Calendar.getInstance().getTime() + " done");
    writer.close();
    dataStore.dispose();
  }
}
