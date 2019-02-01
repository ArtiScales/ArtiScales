package fr.ign.cogit.rules.predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.ParcelBoundaryType;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.AbstractSimpleBuilding;
import fr.ign.cogit.util.SimuTool;
import fr.ign.mpp.configuration.AbstractBirthDeathModification;
import fr.ign.mpp.configuration.AbstractGraphConfiguration;
import fr.ign.parameters.Parameters;
import fr.ign.simulatedannealing.SimulatedAnnealing;

public class PredicateArtiScales<O extends AbstractSimpleBuilding, C extends AbstractGraphConfiguration<O, C, M>, M extends AbstractBirthDeathModification<O, C, M>>
		extends CommonPredicateArtiScales<O, C, M> {

	// There is only one regulation
	ArtiScalesRegulation regles;
	Parameters p;
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
	 * @throws Exception
	 */
	public PredicateArtiScales(BasicPropertyUnit currentBPU, boolean align, ArtiScalesRegulation regle, Parameters pA, IFeatureCollection<Prescription> presc, Environnement env)
			throws Exception {
		/*
		 * All the job is done in the abstract class
		 */
		super(currentBPU, align, pA, presc, env);
		// Removing fake values from regulation object
		regle.clean();
		// The rules
		this.regles = regle;
		this.p = pA;

		double maxH = this.getMaxHeight();
		double minH = this.getMinHeight();

		if (minH > maxH) {
			System.out.println("problem : the maximal height(" + maxH + " )is inf to the minimal one " + minH + " (certainly because of the reglementation)");
			if (maxH - minH < 3.5) {
				System.out.println("diff is small enough, we force the min from the building type");
				this.p.set("maxheight", minH);
				this.p.set("minheight", minH);
			} else {
				System.out.println("diff is too big. we let the regulation rule");
				this.p.set("maxheight", maxH);
				this.p.set("minheight", maxH);
			}

		} else {
			this.p.set("maxheight", maxH);
			this.p.set("minheight", minH);
		}

		if (!(regle.getArt_5().contains("_"))) {
			double aireMinimale = Double.valueOf(regle.getArt_5());

			// ##Rule-art-005
			if (aireMinimale != 99.0) {

				if (currentBPU.getArea() < aireMinimale) {
					canBeSimulated = false;
					denial = SimuTool.increm(denial, "art5");
				}
			}
		}
		// has acces to road?
		double valArt3 = regle.getArt_3();
		if (valArt3 == 1) {
			if (currentBPU.getCadastralParcels().get(0).getBoundariesByType(ParcelBoundaryType.ROAD).isEmpty()) {
				System.out.println("no access to road");
				canBeSimulated = false;
				denial = SimuTool.increm(denial, "art3");
			}
		}
	}

	/*
	 * If necessary ....
	 * 
	 * 
	 * @Override public boolean check(C c, M m) { if (!super.check(c, m)) { return false; }
	 * 
	 * //Implement some special function
	 * 
	 * 
	 * }
	 */

	@Override
	// There is only one regulation in this case
	protected List<ArtiScalesRegulation> getRegulationToApply(O cuboid) {

		List<ArtiScalesRegulation> lArtiScalesRegulation = new ArrayList<>();
		lArtiScalesRegulation.add(regles);

		return lArtiScalesRegulation;
	}

	@Override
	// The minimal distance between cuboids is always the same
	protected List<Double> determineDoubleDistanceForList(List<O> lCuboids) {
		List<Double> lD = new ArrayList<>();
		lD.add(regles.getArt_8());
		return lD;
	}

	@Override
	// The max CES is directly stored in the regulation object
	// #art_13 #art_9
	protected double getMaxCES() {
		// if there's a condition
		double coeff13 = 0;
		if (regles.getArt_13().contains(">")) {
			if (currentBPU.getArea() > Double.valueOf(regles.getArt_13().split(">")[1])) {
				coeff13 = Double.valueOf(regles.getArt_13().split(">")[0]);
			}
		} else {
			coeff13 = Double.valueOf(regles.getArt_13());
		}

		return Math.min(regles.getArt_9(), 1 - coeff13);
	}

	@Override
	protected double getMaxHeight() {
		return cRO.hauteur(p, regles, heighSurroundingBuildings)[1];
	}

	@Override
	protected double getMinHeight() {
		return cRO.hauteur(p, regles, heighSurroundingBuildings)[0];
	}

	@Override
	protected List<ArtiScalesRegulation> getAllRegulation() {
		List<ArtiScalesRegulation> regulations = new ArrayList<>();
		regulations.add(this.regles);
		return regulations;
	}

	@Override
	protected String getArt12Value() {
		return this.regles.getArt_12();
	}

}
