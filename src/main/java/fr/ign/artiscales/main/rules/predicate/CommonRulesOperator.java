package fr.ign.artiscales.main.rules.predicate;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import fr.ign.artiscales.main.annexeTools.SDPCalcPolygonizer;
import fr.ign.artiscales.main.rules.regulation.ArtiScalesRegulation;
import fr.ign.artiscales.main.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Building;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.PrescriptionType;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.rjmcmc.generic.object.ISimPLU3DPrimitive;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class CommonRulesOperator<O extends AbstractSimpleBuilding> {

	public CommonRulesOperator() {

	}

	///////////////// Checked zone

	/**
	 * Check if the number of cuboid is lesser than nbCuboids
	 * 
	 * @param allCuboids
	 * @param nbCuboid
	 * @return
	 */
	public boolean checkNumberOfBuildings(List<O> allCuboids, int nbCuboid) {

		return allCuboids.size() <= nbCuboid;
	}

	/**
	 * Indicates if a geometry intersects a zone
	 * 
	 * @param cuboid
	 * @param geometry
	 * @return
	 */
	public boolean checkIfContainsGeometry(O cuboid, Geometry geometry) {
		if (geometry != null) {
			if (geometry.contains(cuboid.toGeometry())) {
				return true;
			}
		}

		return false;
	}

	public boolean checkProspectRNU(O cuboid, Geometry jtsCurveOppositeLimit) {
		if (jtsCurveOppositeLimit == null || jtsCurveOppositeLimit.isEmpty()) {
			return true;
		}
		return cuboid.prospectJTS(jtsCurveOppositeLimit, 1, 0);
	}

	/**
	 * Check the distance between the cuboids and the existing buildings
	 * 
	 * @param lCuboid
	 * @param widthBuffer
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkBuildingWidth(List<List<O>> lGroupes, double widthBuffer, double distanceInterBati) {
		List<Double> distances = new ArrayList<>();

		distances.add(distanceInterBati);

		return this.checkBuildingWidth(lGroupes, widthBuffer, distances);

	}

	/**
	 * 
	 * @param lGroupes
	 * @param numberMax
	 * @return
	 */
	public boolean numberMaxOfBuilding(List<List<O>> lGroupes, int numberMax) {

		return lGroupes.size() <= numberMax;
	}

	/**
	 * Check the distance between the cuboids and the existing buildings
	 * 
	 * @param lCuboid
	 * @param widthBuffer
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkBuildingWidth(List<List<O>> lGroupes, double widthBuffer, List<Double> distanceInterBati) {

		CuboidGroupCreation<O> cGC = new CuboidGroupCreation<O>();

		// System.out.println("nb groupes " + lGroupes.size());
		for (List<O> groupe : lGroupes) {
			// System.out.println("groupe x : " + lAb.size() + " batiments");
			if (!cGC.checkWidth(groupe, widthBuffer)) {
				return false;
			}

		}

		// Calculer la distance entre groupes
		// 1 - par rapport à distanceInterBati
		// 2 - par rapport à la moitié de la hauteur du plus haut cuboid
		if (!cGC.checkDistanceInterGroups(lGroupes, distanceInterBati))
			return false;

		// System.out.println("-------------------nb groupes " +
		// lGroupes.size());
		return true;
	}

	/**
	 * Check distance between cuboids with a same distance
	 * 
	 * @param lO
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkDistanceInterCuboids(List<? extends AbstractSimpleBuilding> lO, Double distanceInterBati) {
		List<Double> doubles = new ArrayList<>();
		doubles.add(distanceInterBati);
		return checkDistanceInterCuboids(lO, doubles);
	}

	/**
	 * Check the distance between the cuboids with differenciated distance WARNING : The size of both list have to be the same
	 * 
	 * @param lO
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkDistanceInterCuboids(List<? extends AbstractSimpleBuilding> lO, List<Double> distanceInterBati) {

		int nbCuboid = lO.size();

		for (int i = 0; i < nbCuboid; i++) {
			AbstractSimpleBuilding cI = lO.get(i);

			for (int j = i + 1; j < nbCuboid; j++) {
				AbstractSimpleBuilding cJ = lO.get(j);

				double distance = cI.getFootprint().distance(cJ.getFootprint());

				// If there is only one distance we use it or we use the max of the distance
				// constraints of the groups
				double distInterBatiCalculated = (distanceInterBati.size() == 1) ? distanceInterBati.get(0)
						: Math.min(distanceInterBati.get(i), distanceInterBati.get(j));

				if (distance < distInterBatiCalculated) {
					return false;
				}

			}
		}

		return true;

	}

	/**
	 * Check if the distance between a cuboid and a geometry is lesser or more than distMax
	 * 
	 * @param cuboid
	 * @param geom
	 * @param dist
	 * @param supOrInf
	 *            if the cubiod must be superior or inferior to the limit
	 * @return
	 */
	public boolean checkDistanceToGeometry(O cuboid, Geometry geom, double dist, boolean supOrInf) {

		if (dist == 99.0) {
			return true;
		}
		// On vérifie la contrainte de recul par rapport au fond de parcelle
		// Existe t il ?
		if (geom != null) {
			Geometry geomCuboid = cuboid.toGeometry();
			if (geomCuboid == null) {
				System.out.println("Geometry cuboid is null " + CommonRulesOperator.class.toString());
			}
			// determining if the distance in inferior or superior
			if (supOrInf) {
				// this distance must be superior
				if (geomCuboid.distance(geom) < dist) {
					// elle n'est pas respectée, on retourne faux
					return false;
				}
			} else {
				// this distance must be inferior
				if (geomCuboid.distance(geom) > dist) {
					// elle n'est pas respectée, on retourne faux
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Check if the distance between a cuboid and a geometry is lesser than distMax
	 * 
	 * @param cuboid
	 * @param geom
	 * @param distMax
	 * @return
	 */
	public boolean checkDistanceToGeometry(O cuboid, Geometry geom, double distMax) {

		return checkDistanceToGeometry(cuboid, geom, distMax, true);

	}

	/**
	 * Check the distance between a cuboid and existing buildings
	 * 
	 * @param cuboid
	 * @param bPU
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkDistanceBetweenCuboidandBuildings(O cuboid, BasicPropertyUnit bPU, double distanceInterBati) {

		if (distanceInterBati == 99.0) {
			return true;
		}
		// Distance between existig building and cuboid
		for (Building b : bPU.getBuildings()) {
			if (b.getFootprint().distance(cuboid.getFootprint()) <= distanceInterBati) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Indicates if a geometry intersects a zone
	 * 
	 * @param cuboid
	 * @param geometry
	 * @return
	 */
	public boolean checkIfIntersectsGeometry(O cuboid, Geometry geometry) {
		if (geometry != null) {
			if (cuboid.toGeometry().intersects(geometry)) {
				return true;
			}
		}

		return false;
	}

	private double assesBuiltArea(List<? extends AbstractSimpleBuilding> lCuboid) {
		// On calcule la surface couverte par l'ensemble des cuboid
		int nbElem = lCuboid.size();

		Geometry geom = lCuboid.get(0).toGeometry();

		for (int i = 1; i < nbElem; i++) {

			geom = geom.union(lCuboid.get(i).toGeometry());

		}

		return geom.getArea();
	}

	/**
	 * Check if the builtraio (with existing buildings) is lesser than a maxValue
	 * 
	 * @param lCuboid
	 * @param currentBPU
	 * @param maxValue
	 * @return
	 */
	public boolean checkBuiltRatio(List<O> lCuboid, BasicPropertyUnit currentBPU, double maxValue) {

		double builtArea = assesBuiltArea(lCuboid);

		// what is that for?
		List<AbstractSimpleBuilding> lBatIni = new ArrayList<>();
		for (ISimPLU3DPrimitive s : lCuboid) {
			lBatIni.add((AbstractSimpleBuilding) s);
		}

		// On récupère la superficie de la basic propertyUnit
		double airePar = 0;
		for (CadastralParcel cP : currentBPU.getCadastralParcels()) {
			airePar = airePar + cP.getArea();
		}
		return ((builtArea / airePar) <= maxValue);
	}

	/**
	 * Check if the total floor area (Surface de Plancher) of the configuration is lower than the limit set by the form parameters
	 * 
	 * @param lCuboid
	 *            :cuboid configuration to test
	 * @param maxSDP
	 *            ; the floor area to not go further
	 * @return true if the SDP of the configuration is not higher than the limit
	 */
	public boolean checkMaxSDP(List<O> lCuboid, SimpluParametersJSON p) {
		DirectPosition.PRECISION = 4;
		double sDP = 0.0;
		SDPCalcPolygonizer surfGen = new SDPCalcPolygonizer(p.getDouble("heightStorey") - 0.1);
		if (RepartitionBuildingType.hasAttic(p.getString("nameBuildingType"))) {
			surfGen = new SDPCalcPolygonizer(p.getDouble("heightStorey") - 0.1, p.getInteger("nbStoreysAttic"), p.getDouble("ratioAttic"));
			sDP = surfGen.process(lCuboid);
		} else {
			surfGen = new SDPCalcPolygonizer(p.getDouble("heightStorey") - 0.1);
			sDP = surfGen.process(lCuboid);
		}
		return sDP <= p.getDouble("areaMax");
	}

	/**
	 * Check if an n-m² other element fits in the parcel
	 * 
	 * @param lCuboid
	 * @param currentBPU
	 * @param eltValue
	 * @return
	 */
	public boolean checkEltFits(List<O> lCuboid, BasicPropertyUnit currentBPU, double eltValue) {

		// build area
		double builtArea = assesBuiltArea(lCuboid);

		// On récupère la superficie de la basic property Unit
		double areaPar = 0;
		for (CadastralParcel cP : currentBPU.getCadastralParcels()) {
			areaPar = areaPar + cP.getArea();
		}

		return ((builtArea + eltValue) - areaPar < 0);
	}

	///////////////// Checked zone

	/**
	 * Check if the height of a cuboid is lesser than a given number of stairs
	 * 
	 * @param cuboid
	 * @param heightMax
	 * @return
	 */
	public boolean checkHeightStairs(O cuboid, int nbStairs, double stairsHeight) {
		if (cuboid.getHeight() > nbStairs * stairsHeight) {
			return false;
		}
		return true;
	}

	// TODO methode assez imparfaite. Parfaire?
	public boolean checkAlignement(O cuboid, Geometry jtsCurveLimiteFrontParcel) {
		// On vérifie que le batiment est compris dans la zone d'alignement (surfacique)
		if (jtsCurveLimiteFrontParcel != null && !cuboid.toGeometry().touches(jtsCurveLimiteFrontParcel)) {
			return false;
		}

		return true;
	}

	public boolean checkAlignement(O cuboid, IGeometry jtsCurveLimiteParcel) {
		// TODO a thing here doesn't work
		if (jtsCurveLimiteParcel != null && jtsCurveLimiteParcel.toString() != "") {
			if (!cuboid.getGeom().touches(jtsCurveLimiteParcel)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if an alignment constraint is respected between a cuboid and the public road
	 * 
	 * @param cuboid
	 * @param prescriptions
	 * @param align
	 * @param jtsCurveLimiteFrontParcel
	 * @return
	 */
	public boolean checkAlignementPrescription(O cuboid, IFeatureCollection<Prescription> prescriptions, boolean align,
			Geometry jtsCurveLimiteFrontParcel) {
		// On vérifie que le batiment est compris dans la zone d'alignement (surfacique)

		if (prescriptions != null && align) {
			for (Prescription prescription : prescriptions) {
				if (prescription.type == PrescriptionType.FACADE_ALIGNMENT) {
					if (prescription.getGeom().isMultiSurface()) {
						checkAlignement(cuboid, jtsCurveLimiteFrontParcel);
					}
				}
			}
		}

		return true;
	}

	/**
	 * Check if the height of a cuboid is lesser than a given height in meters
	 * 
	 * @param cuboid
	 * @param heightMax
	 * @return
	 */
	public boolean checkHeightMeters(O cuboid, double heightMax) {
		if (cuboid.getHeight() > heightMax) {
			return false;
		}
		return true;
	}

	/**
	 * get authorized height
	 * 
	 * @return
	 */
	public Double[] hauteur(SimpluParametersJSON p, ArtiScalesRegulation regle, Double heighSurroundingBuildings) {
		// @ART 10 : a faire complétement
		//////// Checking the height of the cuboid
		double minPar = p.getDouble("minheight");
		double maxPar = p.getDouble("maxheight");
		double minRule = 0.0;
		double maxRule = 0.0;

		// make the value look good

		double h = 0;

		if (regle.getArt_10_1().contains("-")) {
			System.out.println("case not programmed : only one height for the building");
			h = Double.valueOf(regle.getArt_10_1().split("-")[0]);
		} else {
			h = regle.getArt_10_1().isEmpty() ? 99 : Double.valueOf(regle.getArt_10_1());
		}

		if (h == 99) {
			h = p.getDouble("maxheight");
		}

		switch (regle.getArt_10_top()) {

		// 1 hauteur à l'étage
		// 5 hauteur à l'égout (pour l'instant la même que le 1 vu que l'on ne prends
		// pas en compte les toits)
		case 1:
			maxRule = h * p.getDouble("heightStorey");
			break;
		// hauteur en metre
		case 2:
		case 3:
		case 4:
		case 5:
			maxRule = h;
			break;

		// hauteur harmonisé avec les batiments des alentours (+/- 10 %)
		case 6:
		case 7:
		case 8:
			// if we seek at surronding buildings
			if (heighSurroundingBuildings != null && heighSurroundingBuildings != 0.0) {
				System.out.println("surrounding height values : " + heighSurroundingBuildings);
				// minRule = heighSurroundingBuildings * 0.9;
				maxRule = heighSurroundingBuildings * 1.2;
			}
			// si pas de batiments aux alentours, on se rabat sur différentes options
			else if (regle.getArt_10_top() == 8) {
				maxRule = h * p.getDouble("heightStorey");
			} else if (h != 0 || h != 99) {
				maxRule = h;
			} else {
				maxRule = p.getDouble("maxheight");
			}
			break;
		default:
			System.err.println("Cas de hauteur inconnu");
			minRule = p.getDouble("minheight");
			maxRule = p.getDouble("maxheight");
		}
		Double[] result = { Math.max(minRule, minPar), Math.min(maxRule, maxPar) };
		System.out.println("Hauteur max autorisée : " + result[1]);
		return result;
	}

	public boolean checkProspectArt7(O cuboid, Geometry jtsCurveLimiteFondParcel, int art_74) {
		// ART_74 Distance minimum des constructions par rapport aux limites
		// séparatives, exprimée par rapport à la hauteur du bâtiment
		// 0 : NON
		// 1 : Retrait égal à la hauteur
		// 2 : Retrait égal à la hauteur divisé par deux
		// 3 : Retrait égal à la hauteur divisé par trois
		// 4 : Retrait égal à la hauteur divisé par quatre
		// 5 : Retrait égal à la hauteur divisé par cinq
		// 6 : Retrait égal à la hauteur divisé par deux moins trois mètres
		// 7 : Retrait égal à la hauteur moins trois mètres divisé par deux
		// 8 : retrait égal à la hauteur divisé par deux moins un mètre
		// 9 : retrait égal aux deux tiers de la hauteur
		// 10 : retrait égal aux trois quarts de la hauteur
		double slope = 0;
		double hIni = 0;
		switch (art_74) {
		case 0:
			return true;
		case 1:
			slope = 1;
			break;
		case 2:
			slope = 2;
			break;
		case 3:
			slope = 3;
			break;
		case 4:
			slope = 4;
			break;
		case 5:
			slope = 5;
			break;
		case 6:
			// 6 : Retrait égal à la hauteur divisé par deux moins trois
			// mètres
			hIni = 3;
			slope = 2;
			break;
		case 7:
			// 7 : Retrait égal à la hauteur moins trois mètres divisé par
			// deux
			hIni = 3;
			slope = 2;
			break;
		case 8:
			// 8 : retrait égal à la hauteur divisé par deux moins un mètre
			hIni = 2;
			slope = 2;
			break;
		case 9:
			slope = 3 / 2;
			break;
		case 10:
			slope = 4 / 3;
			break;
		case 99:
			return true;
		default:
			System.out.println("Cas non traité pour art_74 : " + art_74);
			return true;
		}

		if (jtsCurveLimiteFondParcel != null && !cuboid.prospectJTS(jtsCurveLimiteFondParcel, slope, hIni)) {
			return false;
		}

		return true;
	}

	public boolean checkParking(List<O> lAllCuboids, BasicPropertyUnit currentBPU, String art12, SimpluParametersJSON p) {
		if (art12.isEmpty() || art12.equals("99") || art12.equals("0")) {
			return true;
		}

		// Règle de stationnement
		//
		// 1 : un stationnement par logement .
		//
		// 2 : deux stationnements par logement
		//
		// 1l2_2 : un stationnement pour un logement par batiment 60m2, 2 pour les
		// logements à 2 et plus
		//
		// 1m60_2 : un stationnement pour un batiment dont la surface est inférieure à
		// 60m² , 2 pour les
		// logements plus grands
		//
		// 1x50 : une place par 50m² de logements

		// Parking place surface
		double surfPlace = p.getDouble("areaParkingLot");

		// Surface of a dwelling
		double surfLogement = p.getInteger("housingUnitSize");

		// Built area on the parcel
		double builtArea = assesBuiltArea(lAllCuboids);

		// Buildings height is used to assess SDP

		// We assess the SHON
		double sdp = 0.0;
		if (RepartitionBuildingType.hasAttic(p.getString("nameBuildingType"))) {
			SDPCalcPolygonizer surfGen = new SDPCalcPolygonizer(p.getDouble("heightStorey") - 0.1, p.getInteger("nbStoreysAttic"),
					p.getDouble("ratioAttic"));
			sdp = surfGen.process(lAllCuboids);
		} else {
			SDPCalcPolygonizer surfGen = new SDPCalcPolygonizer(p.getDouble("heightStorey") - 0.1);
			sdp = surfGen.process(lAllCuboids);
		}
		// Number of dwellings
		int nbDwellings = (int) Math.round((sdp / surfLogement));

		// if it's a simple house, it's gon contain only one housing unit
		if (p.getString("nameBuildingType").equals("detachedHouse") || p.getString("nameBuildingType").equals("smallHouse")) {
			nbDwellings = 1;
		}

		double multiplierParking = 1;

		if (art12.equals("1")) {
			//////// Cas 1 : 1 : un stationnement par logement
			multiplierParking = 1;
		} else if (art12.equals("3")) {
			multiplierParking = 3;
		} else if (art12.equals("0.5")) {
			multiplierParking = 0.5;
		} else if (art12.equals("2")) {
			//////// Cas 2 : 2 : deux stationnements par logement
			multiplierParking = 2;
		} else if (art12.equals("2.5")) {
			//////// Cas 2 : 2 : deux stationnements par logement
			multiplierParking = 2.5;
		} else if (art12.contains("m")) {
			double limit = Double.valueOf(art12.split("m")[1].split("_")[0]);
			if (nbDwellings < limit) {
				multiplierParking = Integer.valueOf(art12.split("m")[0]);
			} else {
				multiplierParking = Integer.valueOf(art12.split("m")[1].split("_")[1]);
			}
		} else if (art12.contains("l")) {
			double limit = Double.valueOf(art12.split("l")[1].split("_")[0]);
			if (sdp < limit) {
				multiplierParking = Integer.valueOf(art12.split("l")[0]);
			} else {
				multiplierParking = Integer.valueOf(art12.split("l")[1].split("_")[1]);
			}
		} else if (art12.contains("x")) {
			double limit = Double.valueOf(art12.split("x")[1]);
			multiplierParking = (int) Math.round(sdp / limit);
		} else {
			System.out.println("parking case unreckognized " + art12);
		}

		// Surface of parking places
		double surfaceParking = multiplierParking * nbDwellings * surfPlace;

		// We check that the total surface of the parcel is less then the sum of built
		// buildings and the surface parking
		return (currentBPU.getArea() > surfaceParking + builtArea);

	}

}
