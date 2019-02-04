package fr.ign.cogit.annexeTools.fakeWorld.geo;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPositionList;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.sig3d.convert.transform.Extrusion2DObject;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPositionList;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_LineString;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Polygon;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;

public class FakeZone {

	//
	public IFeatureCollection<IFeature> parcels;
	public IFeatureCollection<IFeature> buildings;
	public IFeatureCollection<IFeature> urbaZones;
	public IFeatureCollection<IFeature> roads;

	private FakeZone() {
		parcels = new FT_FeatureCollection<>();
		buildings = new FT_FeatureCollection<>();
		urbaZones = new FT_FeatureCollection<>();
		roads = new FT_FeatureCollection<>();
	}

	public FakeZone(int shift, String libelle) {
		this(shift, libelle, 0, 0);
	}

	public FakeZone(int shift, String libelle, double dx, double dy) {
		this();
		double xOrigin = dx + shift * 300;
		double yOrigin = dy;
		double zDefault = 0;

		// ptRoad2 <---- 300 ----> ptRoad3
		// ^
		// |
		// |
		// 5
		// |
		// |
		// pt6 <---- 200 ----> pt7 <-- 100 --> pt8
		// ^
		// |
		// |
		// 100
		// |
		// |
		// v
		// pt3 <---- 200 ----> pt4 <-- 100 --> pt5
		// ^
		// |
		// |
		// 200
		// |
		// |
		// v
		// pt0 <---- 200 ----> pt1 <-- 100 --> pt2

		// ^
		// |
		// |
		// 10
		// |
		// |
		// ptRoad0 <---- 300 ----> ptRoad1

		// Parcels points
		IDirectPosition dp0 = new DirectPosition(xOrigin, yOrigin, zDefault);
		IDirectPosition dp1 = new DirectPosition(xOrigin + 200, yOrigin, zDefault);
		IDirectPosition dp2 = new DirectPosition(xOrigin + 300, yOrigin, zDefault);
		IDirectPosition dp3 = new DirectPosition(xOrigin, yOrigin + 200, zDefault);
		IDirectPosition dp4 = new DirectPosition(xOrigin + 200, yOrigin + 200, zDefault);
		IDirectPosition dp5 = new DirectPosition(xOrigin + 300, yOrigin + 200, zDefault);
		IDirectPosition dp6 = new DirectPosition(xOrigin, yOrigin + 300, zDefault);
		IDirectPosition dp7 = new DirectPosition(xOrigin + 200, yOrigin + 300, zDefault);
		IDirectPosition dp8 = new DirectPosition(xOrigin + 300, yOrigin + 300, zDefault);

		// The parcel geometries and urba
		IDirectPositionList dplFaceParcel0 = new DirectPositionList(dp0, dp1, dp4, dp3, dp0);
		IDirectPositionList dplFaceParcel1 = new DirectPositionList(dp1, dp2, dp5, dp4, dp1);
		IDirectPositionList dplFaceParcel2 = new DirectPositionList(dp3, dp4, dp7, dp6, dp3);
		IDirectPositionList dplFaceParcel3 = new DirectPositionList(dp4, dp5, dp8, dp7, dp4);

		// Parcel features
		IFeature parcel0 = new DefaultFeature(new GM_Polygon(new GM_LineString(dplFaceParcel0)));
		IFeature parcel1 = new DefaultFeature(new GM_Polygon(new GM_LineString(dplFaceParcel1)));
		IFeature parcel2 = new DefaultFeature(new GM_Polygon(new GM_LineString(dplFaceParcel2)));
		IFeature parcel3 = new DefaultFeature(new GM_Polygon(new GM_LineString(dplFaceParcel3)));

		AttributeManager.addAttribute(parcel1, "CODE", shift + "000" + 1, "String");
		AttributeManager.addAttribute(parcel2, "CODE", shift + "000" + 2, "String");
		AttributeManager.addAttribute(parcel3, "CODE", shift + "000" + 3, "String");
		AttributeManager.addAttribute(parcel0, "CODE", shift + "000" + 0, "String");
		AttributeManager.addAttribute(parcel0, "CODE", shift + "000" + 0, "String");

		AttributeManager.addAttribute(parcel1, "DoWeSimul", "true", "String");
		AttributeManager.addAttribute(parcel2, "DoWeSimul", "true", "String");
		AttributeManager.addAttribute(parcel3, "DoWeSimul", "true", "String");
		AttributeManager.addAttribute(parcel0, "DoWeSimul", "true", "String");
		AttributeManager.addAttribute(parcel0, "DoWeSimul", "true", "String");

		AttributeManager.addAttribute(parcel1, "INSEE", "42400", "String");
		AttributeManager.addAttribute(parcel2, "INSEE", "42400", "String");
		AttributeManager.addAttribute(parcel3, "INSEE", "42400", "String");
		AttributeManager.addAttribute(parcel0, "INSEE", "42400", "String");
		AttributeManager.addAttribute(parcel0, "INSEE", "42400", "String");

		// The building geometry (inside the lower left parcel)

		IDirectPosition dpBuilding1 = new DirectPosition(xOrigin + 50, yOrigin + 50, zDefault);
		IDirectPosition dpBuilding2 = new DirectPosition(xOrigin + 150, yOrigin + 50, zDefault);
		IDirectPosition dpBuilding3 = new DirectPosition(xOrigin + 150, yOrigin + 100, zDefault);
		IDirectPosition dpBuilding4 = new DirectPosition(xOrigin + 50, yOrigin + 100, zDefault);

		IDirectPositionList dplFaceBuilding = new DirectPositionList(dpBuilding1, dpBuilding2, dpBuilding3, dpBuilding4, dpBuilding1);

		IGeometry geom = Extrusion2DObject.convertFromGeometry(new GM_Polygon(new GM_LineString(dplFaceBuilding)), 0, 15);
		IFeature building = new DefaultFeature(FromGeomToSurface.convertMSGeom(geom));

		// The building geometry (inside the upper right parcel)
		IDirectPosition dpBuilding5 = new DirectPosition(xOrigin + 200, yOrigin + 250, zDefault);
		IDirectPosition dpBuilding6 = new DirectPosition(xOrigin + 250, yOrigin + 250, zDefault);
		IDirectPosition dpBuilding7 = new DirectPosition(xOrigin + 250, yOrigin + 275, zDefault);
		IDirectPosition dpBuilding8 = new DirectPosition(xOrigin + 200, yOrigin + 275, zDefault);

		IDirectPositionList dplFaceBuilding2 = new DirectPositionList(dpBuilding5, dpBuilding6, dpBuilding7, dpBuilding8, dpBuilding5);

		IGeometry geom2 = Extrusion2DObject.convertFromGeometry(new GM_Polygon(new GM_LineString(dplFaceBuilding2)), 0, 10);
		IFeature building2 = new DefaultFeature(FromGeomToSurface.convertMSGeom(geom2));

		// The road geometry (upper and downer the parcel)
		IDirectPosition dpRoad0 = new DirectPosition(xOrigin, yOrigin - 10, zDefault);
		IDirectPosition dpRoad1 = new DirectPosition(xOrigin + 300, yOrigin - 10, zDefault);
		IDirectPosition dpRoad2 = new DirectPosition(xOrigin, yOrigin + 305, zDefault);
		IDirectPosition dpRoad3 = new DirectPosition(xOrigin + 300, yOrigin + 305, zDefault);

		IFeature road1 = new DefaultFeature(new GM_LineString(new DirectPositionList(dpRoad0, dpRoad1)));
		IFeature road2 = new DefaultFeature(new GM_LineString(new DirectPositionList(dpRoad2, dpRoad3)));

		// URba zone that shares coorindates with roads

		IDirectPositionList dplFaceUrbaZone = new DirectPositionList(dpRoad0, dpRoad1, dpRoad3, dpRoad2, dpRoad0);
		IFeature urbaZone = new DefaultFeature(new GM_Polygon(new GM_LineString(dplFaceUrbaZone)));
		AttributeManager.addAttribute(urbaZone, "LIBELLE", libelle, "String");
		AttributeManager.addAttribute(urbaZone, "INSEE", "42400", "String");

		AttributeManager.addAttribute(road1, "LARGEUR", 5, "Double");
		AttributeManager.addAttribute(road2, "LARGEUR", 2.5, "Double");
		AttributeManager.addAttribute(road1, "NOM", "Route numéro" + shift + "000" + 1, "String");
		AttributeManager.addAttribute(road2, "NOM", "Route numéro" + shift + "000" + 2, "String");

		this.parcels.add(parcel0);
		this.parcels.add(parcel1);
		this.parcels.add(parcel2);
		this.parcels.add(parcel3);

		this.buildings.add(building);
		this.buildings.add(building2);

		this.urbaZones.add(urbaZone);

		this.roads.add(road1);
		this.roads.add(road2);
	}

	public IFeatureCollection<IFeature> getParcels() {
		return parcels;
	}

	public IFeatureCollection<IFeature> getBuildings() {
		return buildings;
	}

	public IFeatureCollection<IFeature> getUrbaZones() {
		return urbaZones;
	}

	public IFeatureCollection<IFeature> getRoads() {
		return roads;
	}

}
