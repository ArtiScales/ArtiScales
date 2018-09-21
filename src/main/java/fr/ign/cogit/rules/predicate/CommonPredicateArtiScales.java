package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.simplu3d.util.CuboidGroupCreation;
import fr.ign.mpp.configuration.AbstractBirthDeathModification;
import fr.ign.mpp.configuration.AbstractGraphConfiguration;
import fr.ign.parameters.Parameters;
import fr.ign.rjmcmc.configuration.ConfigurationModificationPredicate;

/**
 * This abstract class contains generics methods to have only one implementation
 * of regulation if we considere a parcel with one regulation or several
 * subparcels in a parcel with their own regulations
 *
 * @param <O>
 * @param <C>
 * @param <M>
 */
public abstract class CommonPredicateArtiScales<O extends AbstractSimpleBuilding, C extends AbstractGraphConfiguration<O, C, M>, M extends AbstractBirthDeathModification<O, C, M>>
		implements ConfigurationModificationPredicate<C, M> {

	//Indicate if we can simulate on a parcel
	protected boolean canBeSimulated = true;
	//Indicate if alignement option is activated
	protected boolean align = false;
	//Maxmimal number of cuboids
	protected int nbCuboid = 0;
	//Technical and scenario parameters
	private Parameters p;
	//Selected prescriptinons
	protected IFeatureCollection<Prescription> prescriptions;
	//Is intersection between cuboids allowed ?
	protected boolean intersection = false;

	//The geometry factory for jts operations
	protected GeometryFactory gf = new GeometryFactory();

	//The property unit on which we make the simulation and its JTS geometry
	protected BasicPropertyUnit currentBPU;
	protected Geometry bPUGeom = null;
	
	// Cached geometries according to the different limits categories
	protected Geometry jtsCurveLimiteFondParcel = null;
	protected Geometry jtsCurveLimiteFrontParcel = null;
	protected Geometry jtsCurveLimiteLatParcel = null;

	//A geometry that store a forbidden zone (where buildings cannot be built)
	//According to the selected prescriptions
	protected Geometry forbiddenZone = null;




	/**
	 * The default constructor with a considered BasicPropertyUnit, technical and scenario parameters and a set of selected prescriptions
	 * @param currentBPU
	 * @param align
	 * @param pA
	 * @param presc
	 * @throws Exception
	 */
	protected CommonPredicateArtiScales(BasicPropertyUnit currentBPU, boolean align, Parameters pA,
			IFeatureCollection<Prescription> presc) throws Exception {

		//Set the different initial values
		p = pA;
		intersection = p.getBoolean("intersection");
		this.prescriptions = presc;
		this.align = align;
		this.nbCuboid = p.getInteger("nbCuboid");
		this.currentBPU = currentBPU;
		//This prepare the geoemtries
		this.preapreCachedGeoemtries(currentBPU);

	}

	/**
	 * Ce constructeur initialise les géométries curveLimiteFondParcel,
	 * curveLimiteFrontParcel & curveLimiteLatParcel car elles seront utilisées pour
	 * exprimer certaines contraintes
	 * 
	 * @param bPU
	 * @throws Exception
	 */
	protected void preapreCachedGeoemtries(BasicPropertyUnit bPU) throws Exception {
	
	

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
		
		// Prepare the forbidden geometry from prescription
		ForbiddenZoneGenerator fZG = new ForbiddenZoneGenerator();
		IGeometry geomForbidden = fZG.generateUnionGeometry(prescriptions, currentBPU);

		if (geomForbidden != null && !geomForbidden.isEmpty()) {
			this.forbiddenZone = AdapterFactory.toGeometry(gf, geomForbidden);
		}

		// If the forbidden geometry overlaps the parcel we cannot proceed to the
		// simulation
		if (geomForbidden != null && geomForbidden.contains(currentBPU.getGeom())) {
			canBeSimulated = false;
		}

	}



	/**
	 * This method is executed every time the simulator suggest a new proposition
	 * C => current configuration composed of cuboids
	 * M => modification the simulator tries to applied
	 * 
	 * Normally there is a maximum of 1 birth and/or 1 death
	 */
	@Override
	public boolean check(C c, M m) {


		// NewCuboids
		List<O> lONewCuboids = m.getBirth();

		//No new cuboids, we do not have to checked
		//Warning only works with certain rules
		if (lONewCuboids.isEmpty()) {
			return true;
		}
		
		// All current cuboids
		List<O> lAllCuboids = listAllCuboids(c, m);

		//The operators to check the rules
		CommonRulesOperator<O> cRO = new CommonRulesOperator<O>();
		
		/////////// General constraints that is always checked

		// We check if the number of cuboids does not exceed the max
		if (!cRO.checkNumberOfBuildings(lAllCuboids, nbCuboid)) {
			return false;
		}

		// Checking only for new cuboids
		for (O cuboid : lONewCuboids) {
			//Does the cuboid lays inside the basic property unit
			if (!cRO.checkIfContainsGeometry(cuboid, bPUGeom)) {
				return false;
			}
			
			//Checking prescriptions alignement
			if (!cRO.checkAlignementPrescription(cuboid, prescriptions, align, jtsCurveLimiteFrontParcel)) {
				return false;
			}

			// We check if the cuboids intersects tne forbiddent zone
			// ATTENTION : Ons renvoie faux lorsque l'intersection a lieu
			if (cRO.checkIfIntersectsGeometry(cuboid, this.forbiddenZone)) {
				return false;
			}
		}

		/////////// Regulation that depends from the SubParcel regulation

		for (O cuboid : lONewCuboids) {

			// Determine the relevant regulations to apply :
			List<ArtiScalesRegulation> lRegles = this.getRegulationToApply(cuboid);
			
			for (ArtiScalesRegulation regle : lRegles) {

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

				// We check the constrain distance according to existing buildings
				if (!cRO.checkDistanceBetweenCuboidandBuildings(cuboid, this.currentBPU, regle.getArt_8())) {
					return false;
				}

				// Checking the height of the cuboid
				if (cRO.checkHeight(cuboid, regle.getArt_10_m())) {
					return false;
				}

			}
			
			/////////// Groups or whole configuration constraints
			
			
			//Getting the maxCES according to the implementation
			
			double maxCES = this.getMaxCES();
			// Checking the builtRatio
			if (!cRO.checkBuiltRatio(lAllCuboids, currentBPU, maxCES)) {
				return false;
			}

			//Width and distance between buildings constraints
			
			CuboidGroupCreation<O> groupCreator = new CuboidGroupCreation<O>();

			List<List<O>> groupList = groupCreator.createGroup(lAllCuboids, 0.1);
			if (intersection) {
				// If intersection is allowed, we check the width of the building
				if (!cRO.checkBuildingWidth(groupList, 7.5, determineDoubleDistanceForGroup(groupList)))
					return false;

			} else {

				List<Double> distances = determineDoubleDistanceForList(lAllCuboids);
				if (!cRO.checkDistanceInterCuboids(lAllCuboids, distances)) {
					return false;
				}

			}

		}

		return true;
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
	protected List<O> listAllCuboids(C c, M m) {
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

	// Determine for a cuboid the list of constraints that has to be ckecked
	protected abstract List<ArtiScalesRegulation> getRegulationToApply(O cuboid);

	//Determine the maximal CES
	protected abstract double getMaxCES();
	
	//Determine the recoils applied to a list of cuboids
	protected abstract List<Double> determineDoubleDistanceForList(List<O> lCuboids);
	
	
	public boolean isCanBeSimulated() {
		return canBeSimulated;
	}

}
