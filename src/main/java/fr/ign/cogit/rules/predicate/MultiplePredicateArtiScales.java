package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.HashMap;
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
import fr.ign.cogit.simplu3d.model.Building;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.ParcelBoundary;
import fr.ign.cogit.simplu3d.model.ParcelBoundaryType;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.PrescriptionType;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
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
	private boolean singleBuild = false;
	private int nbCuboid = 0;
	Geometry forbiddenZone = null;

	Geometry jtsCurveLimiteFondParcel = null;
	Geometry jtsCurveLimiteFrontParcel = null;
	Geometry jtsCurveLimiteLatParcel = null;

	private GeometryFactory gf = new GeometryFactory();

	Map<Geometry, ArtiScalesRegulation> mapGeomRegulation = new HashMap<>();

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
	}

	public MultiplePredicateArtiScales(BasicPropertyUnit currentBPU, boolean align, Parameters p,
			IFeatureCollection<Prescription> presc) throws Exception {
		this(currentBPU);
		this.currentBPU = currentBPU;
		this.align = align;
		this.p = p;
		this.prescriptions = presc;

		for (SubParcel sp : currentBPU.getCadastralParcels().get(0).getSubParcels()) {

			mapGeomRegulation.put(AdapterFactory.toGeometry(gf, sp.getGeom()),
					(ArtiScalesRegulation) sp.getUrbaZone().getZoneRegulation());

		}

		ForbiddenZoneGenerator fZG = new ForbiddenZoneGenerator();
		IGeometry geomForbidden = fZG.generateUnionGeometry(prescriptions, currentBPU);

		GeometryFactory gf = new GeometryFactory();

		// adapt geometries

		if (geomForbidden != null && !geomForbidden.isEmpty()) {
			this.forbiddenZone = AdapterFactory.toGeometry(gf, geomForbidden);
		}

		this.align = align;
		this.nbCuboid = p.getInteger("nbCuboid");
		this.singleBuild = p.getBoolean("intersection");

		if (geomForbidden != null && geomForbidden.contains(currentBPU.getGeom())) {
			canBeSimulated = false;
		}
	}

	@Override
	public boolean check(C c, M m) {

		// Il s'agit des objets de la classe Cuboid
		List<O> lO = m.getBirth();

		// On vérifie les règles sur tous les pavés droits, dès qu'il y en a un qui ne
		// respecte pas une règle, on rejette
		// On traite les contraintes qui ne concernent que les nouveux bâtiments

		/////////////// Contraintes concernant les bPU dans leur ensemble :

		int nbAdded = m.getBirth().size() - m.getDeath().size();
		if (c.size() + nbAdded > 1 && singleBuild) {
			return false;
		}

		// System.out.println("taille présumé de notre collec "+c.size());

		if (c.size() + nbAdded > nbCuboid) {
			return false;
		}

		/*
		 * 
		 * // On vérifie la contrainte de recul par rapport au prescriptions graphiques
		 * 
		 * if ((!MultipleBuildingsCuboid.ALLOW_INTERSECTING_CUBOID) &&
		 * (!checkDistanceInterBuildings(c, m, distanceInterBati))) { return false; }
		 * 
		 * if (MultipleBuildingsCuboid.ALLOW_INTERSECTING_CUBOID) { if
		 * (!testWidthBuilding(c, m, 7.5, distanceInterBati)) { return false; } }
		 */

		for (O cuboid : lO) {

			if (!this.currentBPU.getGeom().contains(cuboid.getGeom())) {
				return false;
			}

			// On vérifie que le batiment est compris dans la zone d'alignement (surfacique)

			if (prescriptions != null && align == true) {
				for (Prescription prescription : prescriptions) {
					if (prescription.type == PrescriptionType.FACADE_ALIGNMENT) {
						if (prescription.getGeom().isMultiSurface()
								&& !cuboid.toGeometry().touches(jtsCurveLimiteFrontParcel)) {
							return false;
						}

					}
				}
			}

			List<ArtiScalesRegulation> lR = this.getAssociatedRegulation(cuboid);

			for (ArtiScalesRegulation r : lR) {
				checkRegulationBySubParcel(cuboid, r);
			}

		}

		// Pour produire des boîtes séparées et vérifier que la distance inter
		// bâtiment est respectée

		/*
		 * try { if (!checkDistanceInterBuildings(c, m)) { return false; } } catch
		 * (Exception e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 * 
		 * 
		 * 
		 * // Pour vérifier que le CES (surface bâti) est respecté if
		 * (!respectMaximalBuiltArea(c, m)) { return false; }
		 */

		return false;
	}

	private boolean checkRegulationBySubParcel(O cuboid, ArtiScalesRegulation regle) {
		double distReculVoirie = regle.getArt_6();
		if (distReculVoirie == 77) {
			distReculVoirie = 0;

		}
		double distReculFond = regle.getArt_73();
		// regle.getArt_74()) devrait prendre le minimum de la valeur fixe et du
		// rapport
		// à la hauteur du batiment à coté
		double distReculLat = regle.getArt_72();

		double distanceInterBati = regle.getArt_8();
		if (distanceInterBati == 88.0 || distanceInterBati == 99.0) {
			distanceInterBati = 50; // quelle valeur faut il mettre ??
		}

		if (jtsCurveLimiteFondParcel != null) {
			Geometry geom = cuboid.toGeometry();
			if (geom == null) {
				System.out.println("Nullll");
			}
			// On vérifie la distance (on récupère le foot
			if (this.jtsCurveLimiteFondParcel.distance(geom) < distReculFond) {
				// elle n'est pas respectée, on retourne faux
				return false;

			}

		}
		// On vérifie la contrainte de recul par rapport au front de parcelle (voirie).
		// Existe t il ?
		if (this.jtsCurveLimiteFrontParcel != null) {
			// On vérifie la distance
			if (this.jtsCurveLimiteFrontParcel.distance(cuboid.toGeometry()) < distReculVoirie) {
				// elle n'est pas respectée, on retourne faux
				return false;
			}
		}

		// On vérifie la contrainte de recul par rapport aux bordures de la parcelle
		// Existe t il ?
		if (jtsCurveLimiteLatParcel != null) {
			// On vérifie la distance
			if (this.jtsCurveLimiteLatParcel.distance(cuboid.toGeometry()) < distReculLat) {
				// elle n'est pas respectée, on retourne faux
				return false;

			}

		}

		// Distance between existig building and cuboid
		for (Building b : currentBPU.getBuildings()) {

			if (b.getFootprint().distance(cuboid.getFootprint()) <= distanceInterBati) {
				return false;
			}

		}

		if (forbiddenZone != null) {
			if (cuboid.toGeometry().intersects(forbiddenZone)) {
				return false;
			}
		}

		double maximalHauteur = regle.getArt_10_m();
		// Autres règles :

		// Pour la hauteur => c'est plutôt dans le fichier de configuration.
		// sinon on peut la mesurer comme ça : cuboid.height(1, 2) => mais
		// c'est
		// plus performant dans le fichier de configuration

		// Pour les bandes de constructibilité : on imaginera qu'il y a un
		// polygone ou un multisurface mp

		// IMultiSurface<IOrientableSurface> mS = null; // à définir
		// mS.contains(cuboid.footprint);

		if (cuboid.getHeight() > maximalHauteur) {
			return false;
		}

		return true;

	}

	private List<ArtiScalesRegulation> getAssociatedRegulation(O cuboid) {
		List<ArtiScalesRegulation> lRegulation = new ArrayList<>();
		for (Geometry g : this.mapGeomRegulation.keySet()) {
			if (g.intersects(cuboid.toGeometry())) {
				lRegulation.add(this.mapGeomRegulation.get(g));
			}
		}
		return lRegulation;
	}

}
