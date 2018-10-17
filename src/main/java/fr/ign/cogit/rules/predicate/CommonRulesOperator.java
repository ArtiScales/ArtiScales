package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Building;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.PrescriptionType;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.rjmcmc.generic.object.ISimPLU3DPrimitive;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;
import fr.ign.parameters.Parameters;

public class CommonRulesOperator<O extends AbstractSimpleBuilding> {

	public CommonRulesOperator() {

	}
	
	/////////////////Checked zone

	/**
	 * Check if the number of cuboid is lesser than nbCuboids
	 * 
	 * @param allCuboids
	 * @param nbCuboid
	 * @return
	 */
	public boolean checkNumberOfBuildings(List<O> allCuboids, int nbCuboid) {

		return allCuboids.size() < nbCuboid;
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
		if(jtsCurveOppositeLimit == null || jtsCurveOppositeLimit.isEmpty()) {
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
				double distInterBatiCalculated = (distanceInterBati.size() == 1) ? distanceInterBati.get(0) : Math.min(distanceInterBati.get(i), distanceInterBati.get(j));

				if (distance < distInterBatiCalculated) {
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

		// On vérifie la contrainte de recul par rapport au fond de parcelle
		// Existe t il ?
		if (geom != null) {
			Geometry geomCuboid = cuboid.toGeometry();
			if (geomCuboid == null) {
				System.out.println("Geometry cuboid is null " + CommonRulesOperator.class.toString());
			}
			// On vérifie la distance (on récupère le foot
			if (geomCuboid.distance(geom) < distMax) {
				// elle n'est pas respectée, on retourne faux
				return false;

			}

		}

		return true;

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
	

	/**
	 * Check if the builtraio (with existing buildings) is lesser than a maxValue
	 * 
	 * @param lCuboid
	 * @param currentBPU
	 * @param maxValue
	 * @return
	 */
	public boolean checkBuiltRatio(List<O> lCuboid, BasicPropertyUnit currentBPU, double maxValue) {

		// On calcule la surface couverte par l'ensemble des cuboid
		int nbElem = lCuboid.size();

		Geometry geom = lCuboid.get(0).toGeometry();

		for (int i = 1; i < nbElem; i++) {

			geom = geom.union(lCuboid.get(i).toGeometry());

		}

		List<AbstractSimpleBuilding> lBatIni = new ArrayList<>();
		for (ISimPLU3DPrimitive s : lCuboid) {
			lBatIni.add((AbstractSimpleBuilding) s);
		}

		// On récupère la superficie de la basic propertyUnit
		double airePAr = 0;
		for (CadastralParcel cP : currentBPU.getCadastralParcels()) {
			airePAr = airePAr + cP.getArea();
		}

		return ((geom.getArea() / airePAr) <= maxValue);
	}
	
	
	/////////////////Checked zone
	
	
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

	

	// TODO a expérimenter (trouver une autre manière de le faire) (snapper aux
	// limites ou faire un buffer)
	public boolean checkAlignement(O cuboid, Geometry jtsCurveLimiteFrontParcel) {
		// On vérifie que le batiment est compris dans la zone d'alignement (surfacique)
		if (jtsCurveLimiteFrontParcel!=null && !cuboid.toGeometry().touches(jtsCurveLimiteFrontParcel)) {
			return false;
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
	public boolean checkAlignementPrescription(O cuboid, IFeatureCollection<Prescription> prescriptions, boolean align, Geometry jtsCurveLimiteFrontParcel) {
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
	public Double[] hauteur(Parameters p, ArtiScalesRegulation regle, Double heighSurroundingBuildings) {
		//@ART 10 : a faire complétement
		//////// Checking the height of the cuboid
		double min = p.getDouble("minheight");
		double max = p.getDouble("maxheight");
		System.out.println("TODO MAX HEIGHT CODE");
	
		switch (regle.getArt_10_top()) {

		// 1 hauteur à l'étage
		// 5 hauteur à l'égout (pour l'instant la même que le 1 vu que l'on ne prends pas en compte les toits)
		case 1:

			max = regle.getArt_10_1() * p.getDouble("heightStair");
			break;
		// hauteur en metre
		case 2:
		case 3:
		case 4:
		case 5:
			max = Double.valueOf(regle.getArt_10_1());
			break;

		// hauteur harmonisé avec les batiments des alentours (+/- 10 %)
		case 6:
		case 7:
		case 8:
			// si il y a des batiments
			if (heighSurroundingBuildings != null) {
				min = heighSurroundingBuildings * 0.9;
				max = heighSurroundingBuildings * 1.1;
			}
			// si pas de batiments aux alentours, on se rabat sur différentes options
			else {
				if (regle.getArt_10_top() == 6) {
					max = regle.getArt_10_1();
				}
				if (regle.getArt_10_top() == 7) {
					max = Double.valueOf(regle.getArt_10_1());
				}
			}
		case 20 : 
		default :
			System.err.println("Cas de hauteur non géré");
			
			
		}
		Double[] result = { min, max };
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
				hIni = 2;
				slope = 6;
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

			}

			if (jtsCurveLimiteFondParcel != null
					&& !cuboid.prospectJTS(jtsCurveLimiteFondParcel, slope, hIni)) {
				return false;
			}


		return true;
	}

}
