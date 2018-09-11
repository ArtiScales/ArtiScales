package fr.ign.cogit.rules;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.UrbaZone;

public class ZoneRulesAssociation {

	/**
	 * Associate to the regulation to the UrbanZone of an SimPLU3D-rules environment
	 * 
	 * @param env
	 * @param predicateFile
	 * @return
	 * @throws IOException
	 */
	public static boolean associate(Environnement env, File predicateFile) throws IOException {
		// We associate regulation to UrbanZone

		// Rules parameters
		ArtiScalesRegulation regle = null;
		Map<String, ArtiScalesRegulation> regles = ArtiScalesRegulation
				.loadRegulationSet(predicateFile.getAbsolutePath());

		if (regles == null || regles.isEmpty()) {
			System.out.println("Missing predicate file");
			//Failed
			return false;
		}

		// For each zone we associate a regulation to the zone
		for (UrbaZone zone : env.getUrbaZones()) {

			regle = regles.get(zone.getLibelle());

			if (regle != null) {
				zone.setZoneRegulation(regle);
			} else {
				System.out.println("Missing regulation for zone : " + zone.getLibelle());
				System.out.println("We associate the default regulation");

				if (regles.get("999") == null) {
					System.out.println("Default regulation does not exist");
				} else {
					zone.setZoneRegulation(regles.get("999"));
				}

			}

		}

		return true;
	}

}
