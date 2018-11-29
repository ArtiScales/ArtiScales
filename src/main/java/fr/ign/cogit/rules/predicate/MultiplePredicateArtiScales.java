package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.ParcelBoundaryType;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.mpp.configuration.AbstractBirthDeathModification;
import fr.ign.mpp.configuration.AbstractGraphConfiguration;
import fr.ign.parameters.Parameters;

public class MultiplePredicateArtiScales<O extends AbstractSimpleBuilding, C extends AbstractGraphConfiguration<O, C, M>, M extends AbstractBirthDeathModification<O, C, M>>
		extends CommonPredicateArtiScales<O, C, M> {

	// This map store the geometries of supercel relatively to the underlying
	// regulation
	Map<Geometry, ArtiScalesRegulation> mapGeomRegulation = new HashMap<>();
	CommonRulesOperator<O> cRO = new CommonRulesOperator<O>();

	/**
	 * 
	 * @param currentBPU
	 *            current bPU
	 * @param align
	 *            Alignement to prescription
	 * @param p
	 *            Parametrs
	 * @param presc
	 *            Considered prescription
	 * @param env
	 * @throws Exception
	 */
	public MultiplePredicateArtiScales(BasicPropertyUnit currentBPU, boolean align, Parameters p, IFeatureCollection<Prescription> presc, Environnement env) throws Exception {
		/*
		 * All the job is done in the abstract class
		 */
		super(currentBPU, align, p, presc, env);

		// We create the map
		// for each subparcel we add both object
		for (SubParcel sp : currentBPU.getCadastralParcels().get(0).getSubParcels()) {
			IGeometry subParcelGeometry = sp.getGeom();
			UrbaZone uZ = sp.getUrbaZone();
			mapGeomRegulation.put(AdapterFactory.toGeometry(gf, subParcelGeometry), (ArtiScalesRegulation) uZ.getZoneRegulation());

			if (!((ArtiScalesRegulation) uZ.getZoneRegulation()).getArt_5().startsWith("_")) {
				double aireMinimale = Double.valueOf(((ArtiScalesRegulation) uZ.getZoneRegulation()).getArt_5());
				// ##Rule-art-005
				if (aireMinimale != 99.0) {
					if (currentBPU.getArea() < aireMinimale) {
						canBeSimulated = false;
					}
				}
			}

			// ##Rule-art-003 road access
			double valArt3 = ((ArtiScalesRegulation) uZ.getZoneRegulation()).getArt_3();
			if (valArt3 == 1) {
				if (currentBPU.getCadastralParcels().get(0).getBoundariesByType(ParcelBoundaryType.ROAD).isEmpty()) {
					canBeSimulated = false;

				}
			}

		}

		// We clean all regulation (Removing fake values from regulation object)
		mapGeomRegulation.values().stream().forEach(x -> x.clean());

		this.p.set("maxheight", this.getMaxHeight());
		this.p.set("minheight", this.getMinHeight());
	}

	/*
	 * If necessary ....
	 * 
	 * 
	 * @Override public boolean check(C c, M m) { if (!super.check(c, m)) { return false; } //Implement some special function return true;
	 * 
	 * }
	 */

	/**
	 * Determine for a list of cuboids the distances values
	 * 
	 * @param lCuboids
	 * @return
	 */
	@Override
	protected List<Double> determineDoubleDistanceForList(List<O> lCuboids) {
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
	 * Determine the regulation for a cuboid (when subparcel is intersected)
	 */
	@Override
	protected List<ArtiScalesRegulation> getRegulationToApply(O cuboid) {

		List<ArtiScalesRegulation> lArtiScalesRegulation = new ArrayList<>();
		for (Geometry sP : this.mapGeomRegulation.keySet()) {

			if (!sP.intersects(cuboid.toGeometry())) {
				continue;
			}

			lArtiScalesRegulation.add(mapGeomRegulation.get(sP));

		}

		return lArtiScalesRegulation;
	}

	// Default maxCES value
	private double maxCES = 1;

	@Override
	/**
	 * Determine the maxCES value (a ponderated average according to the SubParcel surfaces) #art_13 #art_5
	 */
	protected double getMaxCES() {
		if (maxCES == -1) {
			//// Determine the maximalCES according to all subparcel contribution
			double totalSubParcelArea = mapGeomRegulation.keySet().stream().mapToDouble(x -> x.getArea()).sum();
			double maxBuiltArea = 0;
			double maxBuiltFreeSpace = 0;

			for (Geometry geom : mapGeomRegulation.keySet()) {

				if (mapGeomRegulation.get(geom).getArt_9() != 99) {
					maxBuiltArea = maxBuiltArea + geom.getArea() * mapGeomRegulation.get(geom).getArt_9();
				}
			
				//if rule 13 has a parcel size condition
				if (mapGeomRegulation.get(geom).getArt_13().contains(">")) {
					if (totalSubParcelArea > Double.valueOf(mapGeomRegulation.get(geom).getArt_13().split(">")[1])) {
						maxBuiltFreeSpace = maxBuiltFreeSpace + geom.getArea() * Double.valueOf(mapGeomRegulation.get(geom).getArt_13().split(">")[0]);
					}
				}
				else if (!mapGeomRegulation.get(geom).getArt_13().equals("99")) {
					maxBuiltFreeSpace = maxBuiltFreeSpace + geom.getArea() * Double.valueOf(mapGeomRegulation.get(geom).getArt_13());
				}
			}

			maxCES = Math.min(maxBuiltArea / totalSubParcelArea, 1 - (maxBuiltFreeSpace / totalSubParcelArea));
		}

		return maxCES;
	}

	@Override
	protected double getMaxHeight() {

		Double maxVal = 0.0;
		for (ArtiScalesRegulation rule : mapGeomRegulation.values()) {
			Double tmpH = cRO.hauteur(super.p, rule, heighSurroundingBuildings)[1];
			if (maxVal < tmpH) {
				maxVal = tmpH;
			}
		}
		return maxVal;
	}

	@Override
	protected double getMinHeight() {
		return mapGeomRegulation.values().stream().mapToDouble(x -> cRO.hauteur(p, x, heighSurroundingBuildings)[1]).max().getAsDouble();
	}

	@Override
	protected List<ArtiScalesRegulation> getAllRegulation() {
		return new ArrayList<>(mapGeomRegulation.values());
	}

	/**
	 * return the most restrictive rule
	 */
	@Override
	protected String getArt12Value() {
		List<String> art12 = this.mapGeomRegulation.values().stream().map(x -> x.getArt_12()).collect(Collectors.toList());
		for (String line : art12) {
			if (line.contains("l") || line.contains("m") || line.contains("x") ) {
				return line;
			}
		}

		if (art12.contains("3")) {
			return "3";
		}
		if (art12.contains("2.5")) {
			return "2.5";
		}
		
		if (art12.contains("2")) {
			return "2";
		}

		return "1";
	}

}
