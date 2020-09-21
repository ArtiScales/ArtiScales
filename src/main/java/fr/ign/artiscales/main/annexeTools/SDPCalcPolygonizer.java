package fr.ign.artiscales.main.annexeTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.feature.SchemaException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.loader.LoaderCuboid;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;

/**
 * Computes the "Surface de plancher" from a collection of cuboids, essentially the ground surface x nb of floors. It does it by building a partition from the intersections of the
 * cuboids, associating the correct height to each part, and finally summing them.
 * 
 * @author imran
 *
 */
public class SDPCalcPolygonizer {

	private double floorHeight = 3;
	private double atticRatio = 0.0;
	private int minimumStairsForAttic = 0;

	public SDPCalcPolygonizer() {
	}

	public SDPCalcPolygonizer(double floorHeight) {
		this.floorHeight = floorHeight;
	}

	public SDPCalcPolygonizer(double floorHeight, int minimumStairsForAttic, double atticRatio) {
		this.floorHeight = floorHeight;
		this.atticRatio = atticRatio;
		this.minimumStairsForAttic = minimumStairsForAttic;
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

		/**
		 * Calculation with the last stair as an attic the attic is set if the minimumStairsForAttic number of stairs is reached the attic makes atticRatio% of the total area of a
		 * storey.
		 */
		public double sdp() {
			double epsilon = 0.01;
			// if height is x.99 we want it to be x+1
			if (height - ((int) (height)) > (1 - epsilon)) {
				height = (int) (height) + 1;
			}
			double storey = Math.floor(height / floorHeight);
			if (minimumStairsForAttic != 0 && atticRatio != 0.0 && minimumStairsForAttic >= storey) {
				return geom.getArea() * ((storey - 1)) + geom.getArea() * atticRatio;
			}
			return geom.getArea() * storey;
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

	public double process(List<? extends AbstractSimpleBuilding> cubes) {
		double sdp = 0;
		CuboidGroupCreation<AbstractSimpleBuilding> cGC = new CuboidGroupCreation<AbstractSimpleBuilding>();
		List<List<AbstractSimpleBuilding>> lGroupes = cGC.createGroup(cubes, 0);
		// System.out.println("nb groupes formé " + lGroupes.size());
		for (List<AbstractSimpleBuilding> g : lGroupes)
			sdp += sdpGroup(g, true);
		return sdp;
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
			sdp += sdpGroup(g, false);
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
	 *            : a ratio of the last floor if it won't contain some countable surface de plancher
	 * @param ratioAttic
	 *            : ratio of the last storey that is set as an attic.
	 * @return
	 */
	private double sdpGroup(List<? extends AbstractSimpleBuilding> group, boolean sdp_or_surface) {
		List<Geometry> features = new ArrayList<>();
		for (AbstractSimpleBuilding building : group) {
			features.add(building.toGeometry());
		}
		double sdp = 0;
		try {
			// polygonize the input polygons
			List<Polygon> polygons = FeaturePolygonizer.getPolygons(features);
			for (Polygon p : polygons) {
				// for each polygon created, find out which input polygons it belongs to and keep their heights
				Point point = p.getInteriorPoint();
				List<Double> heights = new ArrayList<>();
				for (AbstractSimpleBuilding building : group) {
					if (building.toGeometry().intersects(point)) {
						heights.add(building.height);
					}
				}
				if (!heights.isEmpty()) {
					GeomHeightPair pair = new GeomHeightPair(p, Collections.max(heights));
					if (sdp_or_surface) {
						sdp += pair.sdp();
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
}
