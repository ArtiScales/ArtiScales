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
	public static boolean associate(Environnement env, File predicateFile, List<String> listRNU) throws IOException {
		// We associate regulation to UrbanZone

		// Rules parameters
		ArtiScalesRegulation regle = null;
		Map<String, ArtiScalesRegulation> regles = ArtiScalesRegulation.loadRegulationSet(predicateFile.getAbsolutePath());

		if (regles == null || regles.isEmpty()) {
			System.out.println("Missing predicate file");
			// Failed
			return false;
		}

		// For each zone we associate a regulation to the zone
		for (UrbaZone zone : env.getUrbaZones()) {
System.out.println("NSEEEEEE "+zone.getInsee());
			String finalLibelle = zone.getLibelle() + "-" + zone.getInsee();
System.out.println("finalLibelle  df : "+finalLibelle);
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

				if (regles.get("out-0") == null) {
					System.out.println("Default regulation does not exist");
				} else {
					zone.setZoneRegulation(regles.get("out-0"));
				}
			}
		}
		return true;
	}

}
