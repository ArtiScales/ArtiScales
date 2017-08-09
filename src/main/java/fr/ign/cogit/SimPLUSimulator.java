package fr.ign.cogit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.demo.DemoEnvironmentProvider;
import fr.ign.cogit.simplu3d.exec.BasicSimulator;
import fr.ign.cogit.simplu3d.experiments.iauidf.regulation.Regulation;
import fr.ign.cogit.simplu3d.experiments.mupcity.util.Simulator;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.Cuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.cuboid.OptimisedBuildingsCuboidFinalDirectRejection;
import fr.ign.cogit.simplu3d.rjmcmc.generic.predicate.SamplePredicate;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;
import fr.ign.parameters.Parameters;

public class SimPLUSimulator {
	
	public static String zipCode ;
	public static String scenarName;
	public static File folderName ;
	public static File shpIn;
	
	
	public static void run(File paramFile) throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		String scenar = zipCode;
		Parameters p = Parameters.unmarshall(paramFile);
		int nInsee = Integer.valueOf(zipCode);
		// Load default environment (data are in resource directory)

		File depo = new File(shpIn.toString());

		Environnement env = LoaderSHP.loadNoDTM(depo);
		// Select a parcel on which generation is proceeded

		// definition de différents profils de réglementation

		HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>> catalog = new HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>>();
		System.out.println("total de parcelles : " + env.getBpU().size());
		for (int i = 0; i < env.getBpU().size(); i++) {
			BasicPropertyUnit bPU = env.getBpU().get(i);
			// Instantiation of the sampler
			OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
			String typez = new String();

			// Rules parameters

			Regulation regle = null;
			Map<Integer, List<Regulation>> regles = null;

			regles = Regulation.loadRegulationSet("/home/mcolomb/amenagement/PLUs/RegRight.csv");

			for (UrbaZone zone : env.getUrbaZones()) {
				if (zone.getGeom().contains(bPU.getGeom())) {
					typez = zone.getTypeZone();
					System.out.println("got it " + typez);
				}
			}

			for (int imu : regles.keySet()) {
				for (Regulation youhou : regles.get(imu)) {
					if (youhou.getLibelle_de_dul().equals(typez) && nInsee == youhou.getInsee()) {
						regle = youhou;
						System.out.println("j'ai bien retrouvé la ligne. son type est " + typez);
					}
				}
			}

			if (regle == null) {
				System.out.println("iz null");
				regle = regles.get(999).get(0);
			}

			System.out.println("for ex : " + regle.getArt_6());
			double distReculVoirie = regle.getArt_6();
			double distReculFond = regle.getArt_73(); //,regle.getArt_74()) devrait prendre le minimum de la valeur fixe et du rapport à la hauteur du batiment à coté ::à développer yo
			double distReculLat = regle.getArt_72();
			double distanceInterBati = regle.getArt_8();
			if (regle.getArt_8() == 99) {
				distanceInterBati = 0;
			}

			double maximalCES = regle.getArt_9();
			if (regle.getArt_8() == 99) {
				maximalCES = 0;
			}

			//définition de la hauteur. Si elle est exprimé en nombre d'étage, on comptera
			//3m pour le premier étage et 2.5m pour les étages supérieurs. Je ne sais pas comment
			//on utilise ce paramètre car il n'est pas en argument dans le predicate. 
			//TODO utiliser cette hauteur 
			double maximalhauteur = regle.getArt_10_m();

			// Instantiation of the rule checker
			SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new SamplePredicate<>(bPU, distReculVoirie, distReculFond, distReculLat, distanceInterBati, maximalCES);
			//PredicateDensification<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicateIAUIDF();

			// Run of the optimisation on a parcel with the predicate
			GraphConfiguration<Cuboid> cc = oCB.process(bPU, p, env, 1, pred);

			// Witting the output
			IFeatureCollection<IFeature> iFeatC = new FT_FeatureCollection<>();
			// For all generated boxes
			for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {

				// Output feature with generated geometry
				IFeature feat = new DefaultFeature(v.getValue().generated3DGeom());

				// We write some attributes
				AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width), "Double");
				AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
				AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
				AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
				AttributeManager.addAttribute(feat, "Surface", v.getValue().getArea(), "Double");
				iFeatC.add(feat);
			}

			// A shapefile is written as output
			// WARNING : 'out' parameter from configuration file have to be change

			ShapefileWriter.write(iFeatC, p.get("result").toString() + "out-scenar" + scenar + "-parcelle_" + i + ".shp");

			System.out.println("That's all folks");
		}
	}
	
}
