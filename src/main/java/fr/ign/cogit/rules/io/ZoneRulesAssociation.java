package fr.ign.cogit.rules.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.util.SimuTool;

public class ZoneRulesAssociation {

	/**
	 * Associate to the regulation to the UrbanZone of an SimPLU3D-rules environment
	 * 
	 * @param env
	 * @param predicateFile
	 * @return
	 * @throws IOException
	 */
	public static boolean associate(Environnement env, File predicateFile, File zoningFile, HashMap<String, Boolean> tryToAssociateAnyway)
			throws IOException {
		// We associate regulation to UrbanZone
		System.out.println("--- SEARCHING FOR REGULATION ---");
		// Rules parameters
		ArtiScalesRegulation regle = null;
		Map<String, ArtiScalesRegulation> regles = ArtiScalesRegulation.loadRegulationSet(predicateFile.getAbsolutePath());

		if (regles == null || regles.isEmpty()) {
			System.out.println("Missing predicate file");
			System.out.println("");
			// Failed
			return false;
		}

		// For each zone we associate a regulation to the zone
		for (UrbaZone zone : env.getUrbaZones()) {
			String insee = zone.getInsee();
			String finalLibelle = zone.getLibelle() + "-" + insee;

			// if the city is at the rnu, insee code's the same
			if (SimuTool.isCommunityRuledByRNU(zoningFile, insee)) {
				System.out.println("city follows RNU or CC");
				finalLibelle = zone.getLibelle() + "-" + "7";
			}

			regle = regles.get(finalLibelle);
			if (regle != null) {
				zone.setZoneRegulation(regle);
				System.out.println("found : " + regle.getLibelle_de_dul());
			} else {
				System.out.println("Missing regulation for zone : " + zone.getLibelle());

				// If we have to construct into non constructible area, we have to seek for the rules that may be applied on that zone
				if (tryToAssociateAnyway.get("NC")) {
					System.out.println("We seek another regulation for NC zones");

					// case at RNU or Carte Communale : non constructible zones will have the same code as constructible zones
					if (finalLibelle.equals("NC-7")) {
						System.out.println("we forced RNU rules into non constructible zone");
						zone.setZoneRegulation(regles.get("ZC-7"));
					}
					// case the land is a N(atural) or a A(gricultural) zone
					search: if (finalLibelle.startsWith("N") || finalLibelle.startsWith("A")) {
						for (String code : regles.keySet()) {
							String rule = code.split("-")[0].toUpperCase();
							// if no AU zones taken, we set a non dense zone (basically Ub zone)
							if (rule.toUpperCase().contains("AU") && !rule.toUpperCase().contains("2")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced the extension program " + rule + " rules into non constructible zone");
								break search;
							}
						}
						for (String code : regles.keySet()) {
							String rule = code.split("-")[0].toUpperCase();
							// if no AU zones taken, we set a non dense zone (basically Ub zone)

							if (rule.toUpperCase().contains("UB")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced the no dense " + rule + " rules into non constructible zone");
								break;
							} else if (rule.contains("U")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced the classical " + rule + " rules into non constructible zone");
							} else {
								zone.setZoneRegulation(regles.get("ZC-7"));
								System.out.println("we put the RNU rule");
							}
						}
					}
				}

				// we do the same for the AU lands (mostly for the 2AUs)
				if (tryToAssociateAnyway.get("2AU")) {
					// if there's a rule made for un-urbanzed land, we take that (2 would means its a prediction and rules are not edicted yet)
					if (finalLibelle.contains("AU") && finalLibelle.contains("2") && !finalLibelle.toLowerCase().contains("x") && !finalLibelle.toLowerCase().contains("y")
							&& !finalLibelle.toLowerCase().contains("z")) {
						System.out.println("We seek another regulation for AU zones");
						for (String code : regles.keySet()) {
							String rule = code.split("-")[0].toUpperCase();
							// if no AU zones taken, we set a non dense zone (basically Ub zone)
							if (rule.contains("AU")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced the no dense " + rule + " rules into non constructible zone");
								break;
							} else if (rule.toUpperCase().contains("UB")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced the classical " + rule + " rules into non constructible zone");
							} else {
								zone.setZoneRegulation(regles.get("ZC-7"));
								System.out.println("we put the RNU rule");
							}
						}
					}
				}
			}
			if (zone.getZoneRegulation() == null) {
				System.out.println("No regulation (either it does not exist or the area doesn't accept residential constructions)");
				zone.setZoneRegulation(regles.get("out-0"));
			}
		}
		System.out.println("");
		return true;
	}

}
