package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiCurve;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.analysis.ForbiddenZoneGenerator;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.ParcelBoundary;
import fr.ign.cogit.simplu3d.model.ParcelBoundaryType;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;
import fr.ign.mpp.configuration.AbstractBirthDeathModification;
import fr.ign.mpp.configuration.AbstractGraphConfiguration;
import fr.ign.parameters.Parameters;
import fr.ign.rjmcmc.configuration.ConfigurationModificationPredicate;

public class MultiplePredicateArtiScales<O extends AbstractSimpleBuilding, C extends AbstractGraphConfiguration<O, C, M>, M extends AbstractBirthDeathModification<O, C, M>>
		implements ConfigurationModificationPredicate<C, M> {

	BasicPropertyUnit currentBPU;

	private IFeatureCollection<Prescription> prescriptions;

	private boolean align;

	private Parameters p;

	private int nbCuboid = 0;

	Geometry forbiddenZone = null;

	Geometry jtsCurveLimiteFondParcel = null;
	Geometry jtsCurveLimiteFrontParcel = null;
	Geometry jtsCurveLimiteLatParcel = null;

	Map<Geometry, ArtiScalesRegulation> mapGeomRegulation = new HashMap<>();

	Geometry bPUGeom = null;

	private boolean canBeSimulated = true;

	public boolean isCanBeSimulated() {
		return canBeSimulated;
	}

	/**
	 * Ce constructeur initialise les géométries curveLimiteFondParcel,
	 * curveLimiteFrontParcel & curveLimiteLatParcel car elles seront utilisées pour
	 * exprimer certaines contraintes
	 * 
	 * @param bPU
	 * @throws Exception
	 */
	private MultiplePredicateArtiScales(BasicPropertyUnit bPU) throws Exception {
		super();

		this.currentBPU = bPU;

		// Pour simplifier la vérification, on extrait les différentes bordures de
		// parcelles
		IMultiCurve<IOrientableCurve> curveLimiteFondParcel = new GM_MultiCurve<>();
		IMultiCurve<IOrientableCurve> curveLimiteFrontParcel = new GM_MultiCurve<>();
		IMultiCurve<IOrientableCurve> curveLimiteLatParcel = new GM_MultiCurve<>();

		// On parcourt les parcelles du BasicPropertyUnit (un propriétaire peut
		// avoir plusieurs parcelles)
		for (CadastralParcel cP : bPU.getCadastralParcels()) {

			// On parcourt les limites séparatives
			for (ParcelBoundary sCB : cP.getBoundaries()) {

				// En fonction du type on ajoute à telle ou telle géométrie
				IGeometry geom = sCB.getGeom();

				if (geom == null || geom.isEmpty() || geom.length() < 0.01) {
					continue;
				}
				// Fond de parcel
				if (sCB.getType() == ParcelBoundaryType.BOT) {

					if (geom instanceof IOrientableCurve) {
						curveLimiteFondParcel.add((IOrientableCurve) geom);
					} else {
						System.out.println(
								"Classe SamplePredicate : quelque chose n'est pas un ICurve : " + geom.getClass());
					}
				}
				// Limite latérale
				if (sCB.getType() == ParcelBoundaryType.LAT) {

					if (geom instanceof IOrientableCurve) {
						curveLimiteLatParcel.add((IOrientableCurve) geom);
					} else {
						System.out.println(
								"Classe SamplePredicate : quelque chose n'est pas un ICurve : " + geom.getClass());
					}
				}
				// Limite front
				if (sCB.getType() == ParcelBoundaryType.ROAD) {

					if (geom instanceof IOrientableCurve) {
						curveLimiteFrontParcel.add((IOrientableCurve) geom);
					} else {
						System.out.println(
								"Classe SamplePredicate : quelque chose n'est pas un ICurve : " + geom.getClass());
					}
				}
			}
		}

		if (!curveLimiteFondParcel.isEmpty()) {
			this.jtsCurveLimiteFondParcel = AdapterFactory.toGeometry(gf, curveLimiteFondParcel);
		}

		if (!curveLimiteFrontParcel.isEmpty()) {
			this.jtsCurveLimiteFrontParcel = AdapterFactory.toGeometry(gf, curveLimiteFrontParcel);
		}

		if (!curveLimiteLatParcel.isEmpty()) {
			this.jtsCurveLimiteLatParcel = AdapterFactory.toGeometry(gf, curveLimiteLatParcel);
		}

		this.bPUGeom = AdapterFactory.toGeometry(gf, bPU.getGeom());

	}

	public MultiplePredicateArtiScales(BasicPropertyUnit currentBPU, boolean align, Parameters p,
			IFeatureCollection<Prescription> presc) throws Exception {
		this(currentBPU);
		this.currentBPU = currentBPU;
		this.align = align;
		this.p = p;
		this.prescriptions = presc;
		this.nbCuboid = p.getInteger("nbCuboid");

		for (SubParcel sp : currentBPU.getCadastralParcels().get(0).getSubParcels()) {

			mapGeomRegulation.put(AdapterFactory.toGeometry(gf, sp.getGeom()),
					(ArtiScalesRegulation) sp.getUrbaZone().getZoneRegulation());

		}

		// We clean all regulation
		mapGeomRegulation.values().stream().forEach(x -> x.clean());

		ForbiddenZoneGenerator fZG = new ForbiddenZoneGenerator();
		IGeometry geomForbidden = fZG.generateUnionGeometry(prescriptions, currentBPU);

		GeometryFactory gf = new GeometryFactory();

		// adapt geometries

		if (geomForbidden != null && !geomForbidden.isEmpty()) {
			this.forbiddenZone = AdapterFactory.toGeometry(gf, geomForbidden);
		}

		this.align = align;
		this.nbCuboid = p.getInteger("nbCuboid");

		if (geomForbidden != null && geomForbidden.contains(currentBPU.getGeom())) {
			canBeSimulated = false;
		}

		//// Determine the maximalCES according to all subparcel contribution
		double totalSubParcelArea = mapGeomRegulation.keySet().stream().mapToDouble(x -> x.getArea()).sum();
		double maxBuiltArea = 0;

		for (Geometry geom : mapGeomRegulation.keySet()) {
			maxBuiltArea = maxBuiltArea + geom.getArea() * mapGeomRegulation.get(geom).getArt_9();
		}

		maxCES = maxBuiltArea / totalSubParcelArea;
	}

	private double maxCES = 1;

	/**
	 * Cette méthode est executée à chaque fois que le système suggère une nouvelle
	 * proposition. C => contient la configuration courante (en termes de cuboids
	 * proposés) M => les modifications que l'on souhaite apporter à la
	 * configuration courante. Normalement, il n'y a jamais plus d'une naissance ou
	 * d'une mort dans M, mais là dans le code on fait comme si c'était possible,
	 * mais ça peut être simplifié
	 */
	@Override
	public boolean check(C c, M m) {

		// Il s'agit des objets de la classe Cuboid

		// NewCuboids
		List<O> lONewCuboids = m.getBirth();

		if (lONewCuboids.isEmpty()) {
			return true;
		}

		/////////// Generic regulation that concerns all subparcels indepentendely from
		/////////// its subparcel

		// All current cuboids
		List<O> lAllCuboids = listAllCuboids(c, m);

		CommonRulesOperator<O> cRO = new CommonRulesOperator<O>();

		// We check if the number of cuboids does not exceed the max
		if (!cRO.checkNumberOfBuildings(lAllCuboids, nbCuboid)) {
			return false;
		}

		// Checking only for new cuboids
		for (O cuboid : lONewCuboids) {

			if (!cRO.checkIfContainsGeometry(cuboid, bPUGeom)) {
				return false;
			}

		}

		// Checking only for new cuboids
		for (O cuboid : lONewCuboids) {

			if (!cRO.checkIfContainsGeometry(cuboid, bPUGeom)) {
				return false;
			}

			if (!cRO.checkAlignementPrescription(cuboid, prescriptions, align, jtsCurveLimiteFrontParcel)) {
				return false;
			}

			// On vérifie que le cuboid n'intersecte pas de zone issues de prescriptions
			// ATTENTION : Ons renvoie faux lorsque l'intersection a lieu
			if (cRO.checkIfIntersectsGeometry(cuboid, this.forbiddenZone)) {
				return false;
			}

		}

		/////////// Regulation that depends from the SubParcel regulation

		for (O cuboid : lONewCuboids) {

			// Determine the relevant regulation :

			for (Geometry sP : this.mapGeomRegulation.keySet()) {

				if (!sP.intersects(cuboid.toGeometry())) {
					continue;
				}

				ArtiScalesRegulation regle = mapGeomRegulation.get(sP);

				// Distance to the bottom of the parcel
				if (!cRO.checkDistanceToGeometry(cuboid, jtsCurveLimiteFondParcel, regle.getArt_73())) {
					return false;
				}

				// Distance to the front of the parcel
				if (!cRO.checkDistanceToGeometry(cuboid, jtsCurveLimiteFrontParcel, regle.getArt_6())) {
					return false;
				}

				// Distance to the front of the parcel
				if (!cRO.checkDistanceToGeometry(cuboid, jtsCurveLimiteLatParcel, regle.getArt_72())) {
					return false;
				}

				// On vérifie la contrainte de recul par rapport au prescriptions graphiques
				if (!cRO.checkDistanceBetweenCuboidandBuildings(cuboid, this.currentBPU, regle.getArt_8())) {
					return false;
				}

				// Checking the height of the cuboid
				if (cRO.checkHeight(cuboid, regle.getArt_10_m())) {
					return false;
				}

			}

		}

		// Checking the builtRatio
		if (!cRO.checkBuiltRatio(lAllCuboids, currentBPU, maxCES)) {
			return false;
		}

		// @TODO : Implement these fonction because distance depends on the subParcel
		CuboidGroupCreation<O> groupCreator = new CuboidGroupCreation<O>();
		List<List<O>> groupList = groupCreator.createGroup(lAllCuboids, 0.1);
		if (p.getBoolean("intersection")) {
			// If intersection is allowed, we check the width of the building
			if (!cRO.checkBuildingWidth(groupList, 7.5, determineDoubleDistanceForGroup(groupList)))
				return false;

		} else {

			List<Double> distances = determineDoubleDistanceForList(lAllCuboids);
			if (!cRO.checkDistanceInterCuboids(lAllCuboids, distances)) {
				return false;
			}

		}

		// On a réussi tous les tests, on renvoie vrai
		return true;

	}

	/**
	 * Determine for a list of cuboids the distances values
	 * 
	 * @param lCuboids
	 * @return
	 */
	private List<Double> determineDoubleDistanceForList(List<O> lCuboids) {
		// We assign a distance
		List<Double> d = new ArrayList<>();
		// We try to find the geometries that intersects the groups

		for (O cuboid : lCuboids) {

			double distMin = 99;

			for (Geometry geomSubParcel : this.mapGeomRegulation.keySet()) {
				// We test each cuboid until we find an intersection

				if (cuboid.toGeometry().intersects(geomSubParcel)) {
					// We update the minimal distance constraint according to the value
					distMin = Math.min(this.mapGeomRegulation.get(geomSubParcel).getArt_8(), distMin);
				}

				d.add(distMin);

			}

		}

		return d;
	}

	/**
	 * Determnig fo all groups the distance to the other cuboids
	 * 
	 * @param lCuboids
	 * @return
	 */
	private List<Double> determineDoubleDistanceForGroup(List<List<O>> lCuboids) {

		List<Double> distances = new ArrayList<>();
		// For each groups
		for (List<O> cuboidGroup : lCuboids) {
			List<Double> distMin = determineDoubleDistanceForList(cuboidGroup);
			// We add it to the list

			double distMinTemp = Collections.min(distMin);
			distances.add(distMinTemp);

		}

		return distances;
	}

	/**
	 * List all the current cuboids of the configuration
	 * 
	 * @param c
	 * @param m
	 * @return
	 */
	private List<O> listAllCuboids(C c, M m) {
		// On fait la liste de tous les objets après modification
		List<O> lCuboid = new ArrayList<>();

		// On ajoute tous les nouveaux objets
		lCuboid.addAll(m.getBirth());

		// On récupère la boîte (si elle existe) que l'on supprime lors de la
		// modification
		O cuboidDead = null;

		if (!m.getDeath().isEmpty()) {
			cuboidDead = m.getDeath().get(0);
		}

		// On parcourt les objets existants moins celui qu'on supprime
		Iterator<O> iTBat = c.iterator();
		while (iTBat.hasNext()) {

			O cuboidTemp = iTBat.next();

			// Si c'est une boîte qui est amenée à disparaître après
			// modification,
			// elle n'entre pas en jeu dans les vérifications
			if (cuboidTemp == cuboidDead) {
				continue;
			}

			lCuboid.add(cuboidTemp);

		}

		return lCuboid;
	}

	private GeometryFactory gf = new GeometryFactory();
}
