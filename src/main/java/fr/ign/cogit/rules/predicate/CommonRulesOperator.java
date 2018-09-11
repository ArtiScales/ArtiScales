package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Building;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.PrescriptionType;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.rjmcmc.generic.object.ISimPLU3DPrimitive;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;

public class CommonRulesOperator<O extends AbstractSimpleBuilding> {

	public CommonRulesOperator() {

	}

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

	// TODO a expérimenter (trouver une autre manière de le faire) (snapper aux
	// limites ou faire un buffer)
	/**
	 * Check if an alignment constraint is respected between a cuboid and the public
	 * road
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
					if (prescription.getGeom().isMultiSurface()
							&& !cuboid.toGeometry().touches(jtsCurveLimiteFrontParcel)) {
						return false;
					}

				}
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

	/**
	 * Check if the height of a cuboid is lesser than a given value
	 * 
	 * @param cuboid
	 * @param heightMax
	 * @return
	 */
	public boolean checkHeight(O cuboid, double heightMax) {
		if (cuboid.getHeight() > heightMax) {
			return false;
		}
		return true;
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

	/**
	 * Check the distance between the cuboids and the existing buildings
	 * 
	 * @param lCuboid
	 * @param widthBuffer
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkBuildingWidth(List<O> lCuboid, double widthBuffer, double distanceInterBati) {

		List<List<AbstractSimpleBuilding>> lGroupes = CuboidGroupCreation.createGroup(lCuboid, 0);

		// System.out.println("nb groupes " + lGroupes.size());
		for (List<AbstractSimpleBuilding> groupe : lGroupes) {
			// System.out.println("groupe x : " + lAb.size() + " batiments");
			if (!CuboidGroupCreation.checkWidth(groupe, widthBuffer)) {
				return false;
			}

		}

		// Calculer la distance entre groupes
		// 1 - par rapport à distanceInterBati
		// 2 - par rapport à la moitié de la hauteur du plus haut cuboid
		if (!CuboidGroupCreation.checkDistanceInterGroups(lGroupes, distanceInterBati))
			return false;

		// System.out.println("-------------------nb groupes " +
		// lGroupes.size());
		return true;
	}

	/**
	 * Check the distance between the cuboids
	 * 
	 * @param lO
	 * @param distanceInterBati
	 * @return
	 */
	public boolean checkDistanceInterCuboids(List<? extends AbstractSimpleBuilding> lO, double distanceInterBati) {

		int nbCuboid = lO.size();

		for (int i = 0; i < nbCuboid; i++) {
			AbstractSimpleBuilding cI = lO.get(i);

			for (int j = i + 1; j < nbCuboid; j++) {
				AbstractSimpleBuilding cJ = lO.get(j);

				double distance = cI.getFootprint().distance(cJ.getFootprint());

				if (distance < distanceInterBati) {
					return false;
				}

			}
		}

		return true;

	}

}
