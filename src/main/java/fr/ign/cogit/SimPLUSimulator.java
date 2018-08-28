package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.Indicators.BuildingToHousehold;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.PLUCities.PredicatePLUCities;
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

	// one single parcel to study
	SimpleFeature singleFeat;
	boolean isSingleFeat = false;

	Parameters p;

	File buildFile;
	File roadFile;
	File codeFile;
	File predicateFile;
	File zoningFile;
	File simuFile;
	int compteurOutput = 0;

	// IFeatureCollection<IFeature> iFeatGenC = new FT_FeatureCollection<>();

	File filePrescPonct;
	File filePrescLin;
	File filePrescSurf;
	File rootFile;

	public static void main(String[] args) throws Exception {

		// Method to only test the SimPLU3D simulation
		File rootFolder = new File("/home/mcolomb/informatique/ArtiScales/");
		File selectedParcels = new File(
				"/home/mcolomb/informatique/ArtiScales/output/Stability-dataAutomPhy-CM20.0-S0.0-GP_915948.0_6677337.0--N6_St_Moy_ahpx_seed_9015629222324914404-evalAnal-20.0/25495/ZoningAllowed/parcelSelected.shp");
		File pluFile = new File(rootFolder, "/donneeGeographiques/PLU");
		File geoFile = new File(rootFolder, "/donneeGeographiques");
		Parameters p = Parameters.unmarshall(new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0/parametreTechnique.xml"),
				new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0/parametreScenario.xml"));
		System.out.println(p.getString(""));
		SimPLUSimulator simplu = new SimPLUSimulator(rootFolder, geoFile, pluFile, selectedParcels, p.getString("listZipCode"), p);
		simplu.run();
		// SimPLUSimulator.fillSelectedParcels(new File(rootFolder), geoFile, pluFile, selectedParcels, 50, "25495", p);

	}

	/**
	 * Constructor to make a new object to run SimPLU3D simulations.
	 * 
	 * @param rootfile
	 *            : main folder of an artiscale simulation
	 * @param geoFile
	 *            : folder for geographic data
	 * @param pluFile
	 *            : folder for PLU data
	 * @param selectedParcels
	 *            : Folder containing the selection of parcels
	 * @param feat
	 *            : single parcel to simulate
	 * @param zipcode
	 *            : zipcode of the city that is simulated
	 * @param pa
	 *            : parameters file
	 * @throws Exception
	 */
	public SimPLUSimulator(File rootfile, File geoFile, File pluFile, File selectedParcels, String zipcode, Parameters pa) throws Exception {

		// some static parameters needed
		p = pa;
		rootFile = rootfile;
		zipCode = zipcode;
		zoningFile = GetFromGeom.getZoning(pluFile, zipcode);
		parcelsFile = selectedParcels;
		predicateFile = new File(p.getString("pluPredicate"));
		simuFile = new File(parcelsFile.getParentFile(), "simu0");
		simuFile.mkdir();

		// snap datas for lighter geographic files (do not do if it already exists)
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

		codeFile = new File(pluFile, "/codes/DOC_URBA.shp");
		// TODO snap ça quand il y aura quelque chose dans ces couches
		filePrescPonct = new File(pluFile, "/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(pluFile, "/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(pluFile, "/codes/PRESCRIPTION_SURF.shp");
	}

	/**
	 * Constructor to make a new object to run SimPLU3D simulations. Concerns a single parcel (mainly for the filling method)
	 * 
	 * @param rootfile
	 *            : main folder of an artiscale simulation
	 * @param geoFile
	 *            : folder for geographic data
	 * @param pluFile
	 *            : folder for PLU data
	 * @param selectedParcels
	 *            : Folder containing the selection of parcels
	 * @param feat
	 *            : single parcel to simulate
	 * @param zipcode
	 *            : zipcode of the city that is simulated
	 * @param pa
	 *            : parameters file
	 * @throws Exception
	 */
	public SimPLUSimulator(File rootfile, File geoFile, File pluFile, File selectedParcels, SimpleFeature feat, String zipcode, Parameters pa) throws Exception {

		p = pa;
		rootFile = rootfile;
		zipCode = zipcode;
		zoningFile = GetFromGeom.getZoning(pluFile, zipcode);
		parcelsFile = selectedParcels;

		simuFile = new File(parcelsFile.getParentFile(), "simu0");
		simuFile.mkdir();

		singleFeat = feat;

		// snap datas for lighter geographic files (do not do if it already exists)
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

		codeFile = new File(pluFile, "/codes/DOC_URBA.shp");
		filePrescPonct = new File(pluFile, "/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(pluFile, "/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(pluFile, "/codes/PRESCRIPTION_SURF.shp");
	}

	public static List<File> run(File rootFile, File geoFile, File pluFile, File parcelfiles, String zipcode, Parameters p) throws Exception {
		SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, geoFile, pluFile, parcelfiles, null, zipcode, p);
		return SPLUS.run();
	}

	/**
	 * Run a SimPLU simulation on a single parcel
	 * 
	 * @param f
	 *            main folder
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public File runOneSim(int numParcel) throws Exception {
		File featFile = new File(simuFile, "tmp.shp");
		DefaultFeatureCollection tmp = new DefaultFeatureCollection();
		tmp.add(singleFeat);
		Vectors.exportSFC(tmp.collection(), featFile);
		isSingleFeat = true;

		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, featFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);
		File yoy = runSimulation(env, numParcel, p);
		featFile.delete();
		return yoy;
	}

	/**
	 * run a SimPLU3D simulation on all the parcel stored in the parcelFile's SimpleFeatureCollection
	 * 
	 * @return a list of shapefile containing the simulated buildings
	 * @throws Exception
	 */
	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);
		List<File> listBatiSimu = new ArrayList<File>();

		for (int i = 0; i < env.getBpU().size(); i++) {
			File file = runSimulation(env, i, p);
			if (file != null) {
				listBatiSimu.add(file);
			}
		}
		// Si la simu n'est pas trop inspirée
		if (listBatiSimu.isEmpty()) {
			System.out.println("&&&&&&&&&&&&&& Aucun bâtiments n'ont été simulés pour la commune " + zipCode + " &&&&&&&&&&&&&&");
			System.exit(1);
		}

		return listBatiSimu;
	}

	public File runSimulation(Environnement env, int i, Parameters p) throws Exception {
		// HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>> catalog = new HashMap<String, SamplePredicate<Cuboid,
		// GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>>();
		BasicPropertyUnit bPU = env.getBpU().get(i);

		// si on lance une simulation avec une seule parcelle décrite dans
		// l'environnement, son numéro sera 0 mais le numéro de la parcelle sera
		// conservé
		if (isSingleFeat) {
			bPU = env.getBpU().get(0);
		}

		// Instantiation of the sampler
		OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
		String typez = new String();

		// Rules parameters
		Regulation regle = null;
		Map<Integer, List<Regulation>> regles = Regulation.loadRegulationSet(predicateFile.getAbsolutePath());
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
		// regle.getArt_74()) devrait prendre le minimum de la valeur fixe et du rapport
		// à la hauteur du batiment à coté
		double distReculLat = regle.getArt_72();

		double distanceInterBati = regle.getArt_8();
		if (regle.getArt_8() == 99) {
			distanceInterBati = 0;
		}

		double maximalCES = regle.getArt_9();
		if (regle.getArt_8() == 99) {
			maximalCES = 0;
		}

		double maximalhauteur = regle.getArt_10_m();
		IFeatureCollection<Prescription> presc = null;
		// Instantiation of the rule checker
		PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicatePLUCities<>(bPU, distReculVoirie, distReculFond, distReculLat,
				distanceInterBati, maximalCES, maximalhauteur, p.getInteger("nbCuboid"), false);

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
			// IFeature feat = new DefaultFeature(v.getValue().generated3DGeom());

			IFeature feat = new DefaultFeature(v.getValue().getFootprint());

			// We write some attributes

			AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
			AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
			AttributeManager.addAttribute(feat, "SurfaceBox", feat.getGeom().area(), "Double");
			AttributeManager.addAttribute(feat, "areaParcel", areaParcels, "Double");
			AttributeManager.addAttribute(feat, "num", i, "Integer");
			iFeatCtemp.add(feat);
		}
		// TODO réparer ça. Je ne comprends pas pourquoi toutes les hauteur des cuboïdes sont nulles, ce qui fait que la surface qui suit est nulle aussi. Pour l'instant, j'utilise
		// donc une suface comprennant les intersections
		List<Cuboid> cubes = LoaderCuboid.loadFromCollection(iFeatCtemp);
		if (!cubes.isEmpty()) {
			System.out.println("èèèèèèèèèèèèèèè__hauteuuur du cube " + cubes.get(0).height);
		}
		SDPCalc surfGen = new SDPCalc();
		double formTot = surfGen.process(cubes);

		System.out.println("---------------------SUFRACE DE BLOC " + formTot);

		// TODO Prendre la shon (calcul dans simplu3d.experiments.openmole.diversity ? non, c'est la shob et pas la shon !! je suis ingénieur en génie civil que diable. Je ne peux
		// pas me permettre de ne pas prendre en compte un des seuls trucs que je peux sortir de mes quatre ans d'étude pour cette these..!)

		// méthode de calcul d'air simpliste
		double aireTemp = 0;
		for (IFeature feat : iFeatCtemp) {
			aireTemp = aireTemp + ((double) feat.getAttribute("SurfaceBox"));
		}
		for (IFeature feat : iFeatCtemp) {
			AttributeManager.addAttribute(feat, "SurfaceTot", aireTemp, "Double");
			iFeatC.add(feat);
			// iFeatGenC.add(feat);
		}
		// A shapefile is written as output
		// WARNING : 'out' parameter from configuration file have to be
		// change

		File output = new File(simuFile, "out-parcelle_" + i + ".shp");

		// while (output.exists()) {
		// output = new File(simuFile, "out-parcelle_" + compteurOutput + ".shp");
		// compteurOutput = compteurOutput + 1;
		// }
		output.getParentFile().mkdirs();
		// TODO merge of the iFeatC objects

		ShapefileWriter.write(iFeatC, output.toString(), CRS.decode("EPSG:2154"));

		if (!output.exists()) {
			output = null;
		}

		return output;
	}

	/**
	 * Class used to fill a parcel file containing multiple parcels with buildings simulated with SimPLU
	 * 
	 * @param rootFile
	 *            : main file of the ArtiScales's simulation
	 * @param geoFile
	 *            : file containing geographical informations
	 * @param pluFile
	 *            : file containnin
	 * @param selectedParcels
	 * @param missingHousingUnits
	 * @param zipcode
	 * @param p
	 * @return
	 * @throws Exception
	 */
	protected static int fillSelectedParcels(File rootFile, File geoFile, File pluFile, File selectedParcels, int missingHousingUnits, String zipcode, Parameters p)
			throws Exception {
		// Itérateurs sur les parcelles où l'on peut construire
		ShapefileDataStore parcelDS = new ShapefileDataStore(selectedParcels.toURI().toURL());
		SimpleFeatureIterator iterator = parcelDS.getFeatureSource().getFeatures().features();

		try {
			// Tant qu'il y a besoin de logements et qu'il y a des parcelles disponibles
			while (missingHousingUnits > 0 && iterator.hasNext()) {
				SimpleFeature sinlgeParcel = iterator.next();
				// On créer un nouveau simulateur
				SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, geoFile, pluFile, selectedParcels, sinlgeParcel, zipcode, p);
				// On lance la simulation
				File batiSimulatedFile = simPLUsimu.runOneSim((int) sinlgeParcel.getAttribute("num"));

				// On met à jour le compteur du nombre de logements avec l'indicateur
				// buildingToHousehold

				BuildingToHousehold bTH = new BuildingToHousehold(batiSimulatedFile, p);
				System.out.println("--- missingHousingUnits  " + missingHousingUnits);

				missingHousingUnits = missingHousingUnits - bTH.run();
				if (!iterator.hasNext()) {
					System.out.println(" STILL MISSING : " + missingHousingUnits + " HOUSING UNITS");
				}
			}
		} finally {
			iterator.close();
		}
		// On fusionne les sorties des simulations (c'est plus pratique)
		VectorFct.mergeBatis(new File(selectedParcels.getParentFile(), "simu0"));

		parcelDS.dispose();
		return missingHousingUnits;
	}

}
