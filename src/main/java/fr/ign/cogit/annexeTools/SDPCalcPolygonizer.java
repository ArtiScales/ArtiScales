package fr.ign.cogit.annexeTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.feature.SchemaException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.precision.GeometryPrecisionReducer;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.convert.transform.Extrusion2DObject;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.loader.LoaderCuboid;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;
import fr.ign.cogit.simplu3d.util.JTS;

/**
 * Computes the "Surface de plancher" from a collection of cuboids, essentially the ground surface x nb of floors. It does it by building a partition from the intersections of the
 * cuboids, associating the correct height to each part, and finally summing them.
 * 
 * @author imran
 *
 */
public class SDPCalcPolygonizer {
  private double floorHeight = 3;

  public SDPCalcPolygonizer() {
  }

  public SDPCalcPolygonizer(double floorHeight) {
    this.floorHeight = floorHeight;
  }

  /**
   * 
   * structure to combine a surface and its associated height
   *
   */
  public class GeomHeightPair {
    public double height;
    public Geometry geom;

    public GeomHeightPair(Geometry g, double h) {
      this.height = h;
      this.geom = g;
    }

    public double sdp() {
      double epsilon = 0.01;
      // if height is x.99 we want it to be x+1
      if (height - ((int) (height)) > (1 - epsilon)) {
        height = (int) (height) + 1;
      }
      return geom.getArea() * (Math.floor(height / floorHeight));
    }

    /**
     * Calculation with the last stair as an attic the attic is set if the minimumStairsForAttic number of stairs is reached the attic makes atticRatio% of the total area of a
     * storey.
     */
    public double sdp(int minimumStairsForAttic, double atticRatio) {
      double epsilon = 0.01;
      // if height is x.99 we want it to be x+1
      if (height - ((int) (height)) > (1 - epsilon)) {
        height = (int) (height) + 1;
      }
      int storey = (int) (Math.floor(height / floorHeight));
      if (minimumStairsForAttic >= storey) {
        return geom.getArea() * ((Math.floor(height / floorHeight) - 1)) + geom.getArea() * atticRatio;
      } else {
        return geom.getArea() * (Math.floor(height / floorHeight));
      }
    }

    public double surface() {
      return geom.getArea();
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    // TODO Auto-generated method stub
    return super.clone();
  }

  public double process(String shape) {
    return process(LoaderCuboid.loadFromShapeFile(shape));
  }

  public double process(List<? extends AbstractSimpleBuilding> cubes, int nbStoreyAttic, double ratioAttic) {
    double sdp = 0;
    CuboidGroupCreation<AbstractSimpleBuilding> cGC = new CuboidGroupCreation<AbstractSimpleBuilding>();
    List<List<AbstractSimpleBuilding>> lGroupes = cGC.createGroup(cubes, 0);
    // System.out.println("nb groupes formé " + lGroupes.size());
    for (List<AbstractSimpleBuilding> g : lGroupes)
      sdp += sdpGroup(g, true, nbStoreyAttic, ratioAttic);
    return sdp;
  }

  public double process(List<? extends AbstractSimpleBuilding> cubes) {
    return process(cubes, 0, 0.0);
  }

  public double processSurface(String shape) {
    return process(LoaderCuboid.loadFromShapeFile(shape));
  }

  public double processSurface(List<? extends AbstractSimpleBuilding> cubes) {
    double sdp = 0;
    CuboidGroupCreation<AbstractSimpleBuilding> cGC = new CuboidGroupCreation<AbstractSimpleBuilding>();
    List<List<AbstractSimpleBuilding>> lGroupes = cGC.createGroup(cubes, 0);
    // System.out.println("nb groupes formé " + lGroupes.size());
    for (List<AbstractSimpleBuilding> g : lGroupes) {
      sdp += sdpGroup(g, false, 0, 0.0);
    }
    return sdp;
  }

  GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(new PrecisionModel(1000));

  /**
   * If true = sdp if false = surface 2D
   * 
   * @param group
   * @param sdp_or_surface
   * @param nbStoreyAttic
   *          : a ratio of the last floor if it won't contain some countable surface de plancher
   * @param ratioAttic
   *          : ratio of the last storey that is set as an attic.
   * @return
   */
  private double sdpGroup(List<? extends AbstractSimpleBuilding> group, boolean sdp_or_surface, int nbStoreyAttic, double ratioAttic) {
    List<Geometry> features = new ArrayList<>();
    for (AbstractSimpleBuilding building : group) {
      features.add(building.toGeometry());
    }
    double sdp = 0;
    try {
      List<Polygon> polygons = FeaturePolygonizer.getPolygons(features);
      // FeaturePolygonizer.saveGeometries(polygons, new File("./tmp/polygons.shp"), "Polygon");
      for (Polygon p : polygons) {
        Point point = p.getInteriorPoint();
        List<Double> heights = new ArrayList<>();
        for (AbstractSimpleBuilding building : group) {
          if (building.toGeometry().intersects(point)) {
            heights.add(building.height);
          }
        }
        if (heights.isEmpty()) {
          // due to minor modifications of the geometries when noding
//          FeaturePolygonizer.saveGeometries(polygons, new File("./tmp/polygons.shp"), "Polygon");
//          System.out.println(p.getInteriorPoint());
//          System.out.println(group.get(0).toGeometry().relate(p));
//          System.exit(1);
        } else {
          GeomHeightPair pair = new GeomHeightPair(p, Collections.min(heights));
          if (sdp_or_surface) {
            if (nbStoreyAttic == 0 || nbStoreyAttic == 99) {
              sdp += pair.sdp();
            } else {
              sdp += pair.sdp(nbStoreyAttic, ratioAttic);
            }
          } else {
            sdp += pair.surface();
          }
        }
      }
    } catch (IOException e1) {
      e1.printStackTrace();
    } catch (SchemaException e1) {
      e1.printStackTrace();
    }
    return sdp;
  }

  private List<List<GeomHeightPair>> geometryPairByGroup = new ArrayList<>();

  public double getFloorHeight() {
    return floorHeight;
  }

  public void setFloorHeight(double fLOOR_HEIGHT) {
    floorHeight = fLOOR_HEIGHT;
  }

  public List<List<GeomHeightPair>> getGeometryPairByGroup() {
    return geometryPairByGroup;
  }

  public static void main(String[] args) {
    // The in shapefile
    String shpeIn = "/home/mbrasebin/Documents/Donnees/IAUIDF/Resultats/ResultatChoisy/results_pchoisy/24/simul_24_true_no_demo_sampler.shp";

    // The out shapefile
    String shapeOut = "/tmp/tmp/sdp.shp";

    // Instanciating the object
    SDPCalcPolygonizer sd = new SDPCalcPolygonizer();

    // Calculating sdp and generating the merge geometry
    double sdp = sd.process(shpeIn);

    System.out.println("SDP :" + sdp);
    // Getting and adding the merged geometry to the collection
    IFeatureCollection<IFeature> featColl = new FT_FeatureCollection<>();

    List<List<GeomHeightPair>> llGeomPair = sd.getGeometryPairByGroup();

    int count = 0;

    for (List<GeomHeightPair> geomPairs : llGeomPair) {
      for (GeomHeightPair g : geomPairs) {

        IGeometry jtsGeom = JTS.fromJTS(g.geom);

        if (jtsGeom == null || jtsGeom.coord().isEmpty()) {
          continue;
        }

        IMultiSurface<IOrientableSurface> os = FromGeomToSurface.convertMSGeom(jtsGeom);

        for (IOrientableSurface osTemp : os) {
          if (osTemp.area() < 0.01) {
            continue;
          }
          IGeometry extruded = Extrusion2DObject.convertFromGeometry(osTemp, 0, g.height);

          IMultiSurface<IOrientableSurface> finalOs = FromGeomToSurface.convertMSGeom(extruded);

          IFeature feat = new DefaultFeature(finalOs);

          AttributeManager.addAttribute(feat, "ID", (count++), "Integer");
          AttributeManager.addAttribute(feat, "HAUTEUR", g.height, "Double");
          featColl.add(feat);

        }

      }

    }

    // Export the result
    ShapefileWriter.write(featColl, shapeOut);

  }
}
