package fr.ign.cogit.rules.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.util.GetFromGeom;

public class ZoneRulesAssociation {

	/**
	 * Associate to the regulation to the UrbanZone of an SimPLU3D-rules environment
	 * 
	 * @param env
	 * @param predicateFile
	 * @return
	 * @throws IOException
	 */
	public static boolean associate(Environnement env, File predicateFile, List<String> listRNU, boolean tryToAssociateAnyway) throws IOException {
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
			System.out.println("Insee " + zone.getInsee());
			String finalLibelle = zone.getLibelle() + "-" + zone.getInsee();

			// if the city is at the rnu, insee code's the same
			if (listRNU != null) {
				if (listRNU.contains(zone.getInsee())) {
					System.out.println("city follows RNU");
					finalLibelle = zone.getLibelle() + "-" + "7";
				}
			}
			regle = regles.get(finalLibelle);
			if (regle != null) {
				zone.setZoneRegulation(regle);
				System.out.println("found : " + regle.getLibelle_de_dul());
			} else {
				System.out.println("Missing regulation for zone : " + zone.getLibelle());
				System.out.println("We associate the default regulation");

				// If we have to construct into not allowed to construct area, we have to seek for the rules that may be applied on that zone
				if (tryToAssociateAnyway) {
					// case at RNU or Carte Communale : non constructible zones will have the same code as constructible zones
					if (finalLibelle.equals("NC-7")) {
						System.out.println("we forced RNU rules into non constructible zone");
						zone.setZoneRegulation(regles.get("ZC-7"));
					}
					// case the land is a N(atural) or a A(gricultural) zone
					if (finalLibelle.startsWith("N") || finalLibelle.startsWith("A")) {
						for (String code : regles.keySet()) {
							String rule = code.split("-")[0].toUpperCase();
							// if there's a rule made for un-urbanzed land, we take that (2 would means its a prediction and rules are not edicted yet)
							if (rule.contains("AU") && !rule.contains("2")) {
								zone.setZoneRegulation(regles.get(code));
								System.out.println("we forced " + rule + " rules into non constructible zone");
								break;
							}
						}
						if (zone.getZoneRegulation() == null) {
							for (String code : regles.keySet()) {
								String rule = code.split("-")[0].toUpperCase();
								// if no AU zones taken, we set a non dense zone (basically Ub zone)
								if (rule.contains("UB")) {
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
				}

				if (zone.getZoneRegulation() == null) {
					System.out.println("Default regulation does not exist");
					zone.setZoneRegulation(regles.get("out-0"));
				} 
			}
		}
		System.out.println("");
		return true;
	}

}
