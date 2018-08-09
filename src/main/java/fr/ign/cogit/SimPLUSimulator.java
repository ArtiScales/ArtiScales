package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.Indicators.BuildingToHousehold;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.iauidf.predicate.PredicateIAUIDF;
import fr.ign.cogit.simplu3d.experiments.iauidf.regulation.Regulation;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.Cuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.loader.LoaderCuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.cuboid.OptimisedBuildingsCuboidFinalDirectRejection;
import fr.ign.cogit.simplu3d.rjmcmc.generic.predicate.SamplePredicate;
import fr.ign.cogit.simplu3d.util.SDPCalc;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.VectorFct;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;
import fr.ign.parameters.Parameters;

public class SimPLUSimulator {

	String zipCode;
	// parcel
	File parcelsFile;
	
	//one single parcel to study (exported as temp)
	File featFile;
	
	File buildFile;
	File roadFile;
	File paramFile = new File("donnee/couplage/pluZoning/codes/");
	File codeFile;
	File zoningFile;
	File simuFile;
	int compteurOutput = 0;

	IFeatureCollection<IFeature> iFeatGenC = new FT_FeatureCollection<>();

	File filePrescPonct;
	File filePrescLin;
	File filePrescSurf;
	File rootFile;

	Parameters p;
	
	

	public static void main(String[] args) throws Exception {
		
		//Method  to only test the SimPLU3D simulation
		String rootFolder = "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/";
		String selectedParcels = rootFolder+ "donneeGeographiques/parcelle.shp";
		
		String paramFile = "/home/mbrasebin/Documents/Code/ArtiScales/ArtiScales/src/main/resources/paramSet/param0.xml";
		
	    Parameters p = Parameters.unmarshall(new File(paramFile));


		int missingHousingUnits = SimPLUSimulator.fillSelectedParcels(new File(selectedParcels), 50, new File(rootFolder), "25084", p, "2");

	}

	public SimPLUSimulator(File rootfile, File selectedParcels, SimpleFeature feat, String zipcode, Parameters pa) throws Exception {

		p = pa;
		rootFile = rootfile;
		zipCode = zipcode;
		zoningFile = GetFromGeom.getZoning(rootFile, zipcode);
		parcelsFile = selectedParcels;

		
		simuFile = new File(parcelsFile.getParentFile(), "simu0");
		simuFile.mkdir();

		//export as temp the single parcel to study
		DefaultFeatureCollection tempShp = new DefaultFeatureCollection();
		tempShp.add(feat);
		featFile = new File(simuFile, "tmp.shp");
		Vectors.exportSFC(tempShp, featFile);
		
		if (!(new File(simuFile.getParentFile(), "/snap/route.shp")).exists()) {
			System.out.println("in snapDatas" + GetFromGeom.getBati(new File(rootfile, "donneeGeographiques")));
			File snapFile = new File(simuFile.getParentFile(), "/snap/");
			snapFile.mkdir();
			buildFile = Vectors.snapDatas(GetFromGeom.getBati(new File(rootfile, "donneeGeographiques")), zoningFile, new File(simuFile.getParentFile(), "/snap/batiment.shp"));
			roadFile = Vectors.snapDatas(GetFromGeom.getRoute(new File(rootfile, "donneeGeographiques")), zoningFile, new File(simuFile.getParentFile(), "/snap/route.shp"));
		} else {
			buildFile = new File(simuFile.getParentFile(), "/snap/batiment.shp");
			roadFile = new File(simuFile.getParentFile(), "/snap/route.shp");
		}

		codeFile = new File(rootFile, "donneeGeographiques/PLU/codes/DOC_URBA.shp");
		filePrescPonct = new File(rootFile, "donneeGeographiques/PLU/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(rootFile, "donneeGeographiques/PLU/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(rootFile, "donneeGeographiques/PLU/codes/PRESCRIPTION_SURF.shp");

		
	}


	public static List<File> run(File rootFile, File parcelfiles, String zipcode, Parameters p) throws Exception {
		SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelfiles, null, zipcode, p);
		return SPLUS.run();
	}

	/**
	 * Run one SimPLU simulation and calculate the number of housing units which can fit into the simulated building
	 * 
	 * @param f
	 *            main folder
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public int runOneSim() throws Exception {
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, featFile , roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);
		File yoy = runSimulation(env, 0, p);
		BuildingToHousehold building = new BuildingToHousehold(yoy, p.getInteger("HousingUnitSize"));
		return building.simpleEstimate(yoy);
	}

	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		List<File> listBatiSimu = new ArrayList<File>();

		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, new File(parcelsFile, "parcelSelected.shp"), roadFile, buildFile, filePrescPonct, filePrescLin,
				filePrescSurf, null);
		for (int i = 0; i < env.getBpU().size(); i++) {
			runSimulation(env, i, p);
		}
		buildingInOneShp();
		listBatiSimu.add(simuFile);
		return listBatiSimu;
	}

	public File runSimulation(Environnement env, int i, Parameters p) throws Exception {
		HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>> catalog = new HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>>();
		BasicPropertyUnit bPU = env.getBpU().get(i);

		// Instantiation of the sampler
		OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
		String typez = new String();

		// Rules parameters

		Regulation regle = null;
		Map<Integer, List<Regulation>> regles = Regulation.loadRegulationSet(codeFile.getParent() + "/predicate.csv");
		for (UrbaZone zone : env.getUrbaZones()) {
			if (zone.getGeom().contains(bPU.getGeom())) {
				typez = zone.getLibelle();
			}
		}

		for (int imu : regles.keySet()) {
			for (Regulation reg : regles.get(imu)) {
				if (reg.getLibelle_de_dul().equals(typez) && Integer.valueOf(zipCode) == reg.getInsee()) {
					regle = reg;
					System.out.println("j'ai bien retrouvé la ligne. son type est " + typez);
				}
			}
		}

		if (regle == null) {
			System.out.println("iz null");
			regle = regles.get(999).get(0);
		}

		double distReculVoirie = regle.getArt_6();
		if (distReculVoirie == 77) {
			distReculVoirie = 0;

		}
		double distReculFond = regle.getArt_73();
		// regle.getArt_74()) devrait prendre le minimum de la valeur fixe et du rapport à la hauteur du batiment à coté ::à développer yo
		double distReculLat = regle.getArt_72();

		double distanceInterBati = regle.getArt_8();
		if (regle.getArt_8() == 99) {
			distanceInterBati = 0;
		}

		double maximalCES = regle.getArt_9();
		if (regle.getArt_8() == 99) {
			maximalCES = 0;
		}

		// définition de la hauteur. Si elle est exprimé en nombre d'étage, on comptera 3m pour le premier étage et 2.5m pour les étages supérieurs. Je ne sais pas comment on
		// utilise ce paramètre car il n'est pas en argument dans le predicate.
		// TODO utiliser cette hauteur
		double maximalhauteur = regle.getArt_10_m();
		IFeatureCollection<Prescription> presc = null;
		// Instantiation of the rule checker
		// PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicatePLUCities<>(bPU, distReculVoirie, distReculFond, distReculLat,
		// distanceInterBati, maximalCES, maximalhauteur);
		PredicateIAUIDF<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicateIAUIDF(bPU, regle, regle);
		Double areaParcels = 0.0;
		for (CadastralParcel yo : bPU.getCadastralParcels()) {
			areaParcels = areaParcels + yo.getArea();
		}

		// Run of the optimisation on a parcel with the predicate
		GraphConfiguration<Cuboid> cc = oCB.process(bPU, p, env, 1, pred);

		// Witting the output
		IFeatureCollection<IFeature> iFeatC = new FT_FeatureCollection<>();
		IFeatureCollection<IFeature> iFeatCtemp = new FT_FeatureCollection<>();

		// For all generated boxes
		for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {

			// Output feature with generated geometry
			IFeature feat = new DefaultFeature(v.getValue().generated3DGeom());

			// IFeature feat = new DefaultFeature(v.getValue().getFootprint());

			// We write some attributes

			AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
			AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
			AttributeManager.addAttribute(feat, "SurfaceBox", v.getValue().getArea(), "Double");
			AttributeManager.addAttribute(feat, "areaParcel", areaParcels, "Double");
			AttributeManager.addAttribute(feat, "num", i, "Integer");
			iFeatCtemp.add(feat);
		}
		// TODO mettre la bonne aire des cuboides mergés
		List<Cuboid> cubes = LoaderCuboid.loadFromCollection(iFeatCtemp);
		SDPCalc surfGen = new SDPCalc();
		double formTot = surfGen.process(cubes);

		for (IFeature feat : iFeatCtemp) {
			AttributeManager.addAttribute(feat, "SurfaceTot", formTot, "Double");
			iFeatC.add(feat);
			iFeatGenC.add(feat);
		}
		// A shapefile is written as output
		// WARNING : 'out' parameter from configuration file have to be
		// change

		File output = new File(simuFile, "out-parcelle_" + i + ".shp");
		while (output.exists()) {
			output = new File(simuFile, "out-parcelle_" + compteurOutput + ".shp");
			compteurOutput = compteurOutput + 1;
		}
		output.getParentFile().mkdirs();
		// TODO merge of the iFeatC objects

		ShapefileWriter.write(iFeatC, output.toString());

		return output;
	}

	public void buildingInOneShp() {
		File shpGen = (new File(simuFile, "TotBatiSimu/TotBatiSimu.shp"));
		shpGen.getParentFile().mkdirs();
		ShapefileWriter.write(iFeatGenC, shpGen.toString());
	}

	public File putNewBatiInBati() throws IOException {
		// TODO when a new building is created with the fill method, add it to
		// the building file
		ShapefileDataStore builsSnapSDS = new ShapefileDataStore(buildFile.toURI().toURL());
		SimpleFeatureCollection newBuild = builsSnapSDS.getFeatureSource().getFeatures();
		return buildFile;
	}

	protected static int fillSelectedParcels(File selectedParcels, int missingHousingUnits, File rootFile, String zipcode, Parameters p, String action) throws Exception {
		// Itérateurs sur les parcelles où l'on peut construire
		ShapefileDataStore parcelDS = new ShapefileDataStore(selectedParcels.toURI().toURL());
		SimpleFeatureIterator iterator = parcelDS.getFeatureSource().getFeatures().features();

		File simuFile = selectedParcels.getParentFile();

		try {
			// Tant qu'il y a de logements
			while (missingHousingUnits > 0 && iterator.hasNext()) {
				// On créer un nouveau simulateur
				SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, selectedParcels, iterator.next(), zipcode, p);
				int fill = simPLUsimu.runOneSim();
				// On met à jour le compteur du nombre de logements
				missingHousingUnits = missingHousingUnits - fill;
				System.out.println("missing housing units : " + missingHousingUnits);
				if (!iterator.hasNext()) {
					System.out.println(" MISSING ROOM FOR NEW HOUSING in the greyfield : " + missingHousingUnits + " HOUSING UNITS MISSING");
				}
			}
		} finally {
			iterator.close();
		}

		// On convertit les surface bâties en nombre de logements
		BuildingToHousehold bhtU = new BuildingToHousehold(simuFile, p.getInteger("HousingUnitSize"));
		bhtU.run();
		// On fusionne les sorties des simulations
		VectorFct.mergeBatis(simuFile);
		parcelDS.dispose();
		return missingHousingUnits;
	}

}
