package fr.ign.cogit;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.indicators.BuildingToHousehold;
import fr.ign.cogit.rules.ArtiScalesRegulation;
import fr.ign.cogit.rules.PredicatePLUCities;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.model.UrbaZone;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.Cuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.loader.LoaderCuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.cuboid.OptimisedBuildingsCuboidFinalDirectRejection;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.mix.MultipleBuildingsCuboid;
import fr.ign.cogit.simplu3d.util.SDPCalc;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.VectorFct;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;
import fr.ign.parameters.Parameters;
import fr.ign.rjmcmc.configuration.ConfigurationModificationPredicate;

public class SimPLUSimulator {

	String zipCode;
	// parcels containing all of em
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

	private static List<String> ID_PARCELLE_TO_SIMULATE = new ArrayList<>();
	
	public static boolean USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;

	public static void main(String[] args) throws Exception {

		List<File> lF = new ArrayList<>();
		// Line to change to select the right scenario
		String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenar0MKDom/").getPath();
		lF.add(new File(rootParam + "parametreTechnique.xml"));
		lF.add(new File(rootParam + "parametreScenario.xml"));
		Parameters p = Parameters.unmarshall(lF);

		// Rappel de la construction du code :
		// codeDep + codeCom + comAbs + section + numero
		ID_PARCELLE_TO_SIMULATE.add("25495000AK0005");
		//ID_PARCELLE_TO_SIMULATE.add("25495000AE0036");

		
		// RootFolder
		File rootFolder = new File(p.getString("rootFile"));
		// Selected parcels shapefile
		File selectedParcels = new File(p.getString("selectedParcelFile"));
		// GeographicData folder
		File geoFile = new File(p.getString("geoFile"));
		// PLU Folder
		File pluFile = new File(p.getString("pluFile"));

		SimPLUSimulator simplu = new SimPLUSimulator(rootFolder, geoFile, pluFile, selectedParcels,
				p.getString("listZipCode"), p);

		simplu.run();
		// SimPLUSimulator.fillSelectedParcels(new File(rootFolder), geoFile,
		// pluFile, selectedParcels, 50, "25495", p);

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
	public SimPLUSimulator(File rootfile, File geoFile, File pluFile, File selectedParcels, String zipcode,
			Parameters pa) throws Exception {

		// some static parameters needed
		p = pa;
		rootFile = rootfile;
		zipCode = zipcode;
		zoningFile = GetFromGeom.getZoning(pluFile, zipcode);
		parcelsFile = selectedParcels;
		predicateFile = new File(p.getString("pluPredicate"));
		simuFile = new File(parcelsFile.getParentFile(), "simu");
		simuFile.mkdir();

		// snap datas for lighter geographic files (do not do if it already
		// exists)
		if (!(new File(simuFile.getParentFile(), "/snap/route.shp")).exists()) {
			System.out.println("in snapDatas" + GetFromGeom.getBati(new File(rootfile, "donneeGeographiques")));
			File snapFile = new File(simuFile.getParentFile(), "/snap/");
			snapFile.mkdir();

			buildFile = Vectors.snapDatas(GetFromGeom.getBati(new File(rootfile, "donneeGeographiques")), zoningFile,
					new File(simuFile.getParentFile(), "/snap/batiment.shp"));
			roadFile = Vectors.snapDatas(GetFromGeom.getRoute(new File(rootfile, "donneeGeographiques")), zoningFile,
					new File(simuFile.getParentFile(), "/snap/route.shp"));
			filePrescPonct = Vectors.snapDatas(new File(pluFile, "prescPonctRegroupe.shp"), zoningFile,
					new File(simuFile.getParentFile(), "/snap/prescPonctRegroupe.shp"));
			filePrescLin = Vectors.snapDatas(new File(pluFile, "prescLinRegroupe.shp"), zoningFile,
					new File(simuFile.getParentFile(), "/snap/prescLinRegroupe.shp"));
			filePrescSurf = Vectors.snapDatas(new File(pluFile, "prescSurfRegroupe.shp"), zoningFile,
					new File(simuFile.getParentFile(), "/snap/prescSurfRegroupe.shp"));

		} else {
			buildFile = new File(simuFile.getParentFile(), "/snap/batiment.shp");
			roadFile = new File(simuFile.getParentFile(), "/snap/route.shp");
			filePrescPonct = new File(simuFile.getParentFile(), "/snap/prescPonctRegroupe.shp");
			filePrescLin = new File(simuFile.getParentFile(), "/snap/prescLinRegroupe.shp");
			filePrescSurf = new File(simuFile.getParentFile(), "/snap/prescSurfRegroupe.shp");
		}
		codeFile = new File(pluFile, "/codes/DOC_URBA.shp");
	}

	/**
	 * Constructor to make a new object to run SimPLU3D simulations. Concerns a
	 * single parcel (mainly for the filling method)
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

	public SimPLUSimulator(File rootfile, File geoFile, File pluFile, File selectedParcels, SimpleFeature feat,
			String zipcode, Parameters pa) throws Exception {
		this(rootfile, geoFile, pluFile, selectedParcels, zipcode, pa);
		singleFeat = feat;
		isSingleFeat = true;

	}

	public static List<File> run(File rootFile, File geoFile, File pluFile, File parcelfiles, String zipcode,
			Parameters p) throws Exception {
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

		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, featFile, roadFile, buildFile,
				filePrescPonct, filePrescLin, filePrescSurf, null);
		File yoy = runSimulation(env, numParcel, p);
		featFile.delete();
		return yoy;
	}

	/**
	 * run a SimPLU3D simulation on all the parcel stored in the parcelFile's
	 * SimpleFeatureCollection
	 * 
	 * @return a list of shapefile containing the simulated buildings
	 * @throws Exception
	 */
	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		System.out.println(filePrescLin);
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile,
				filePrescPonct, filePrescLin, filePrescSurf, null);
		//We associate regulation to UrbanZone
		
		// Rules parameters
		ArtiScalesRegulation regle = null;
		Map<String, ArtiScalesRegulation> regles = ArtiScalesRegulation.loadRegulationSet(predicateFile.getAbsolutePath());

		if (regles == null || regles.isEmpty()) {
			System.out.println("Missing predicate file");
			return null;
		}
		
		
		//For each zone we associate a regulation to the zone
		for (UrbaZone zone : env.getUrbaZones()) {
			
		 regle = regles.get(zone.getLibelle());
		 
		 if(regle != null){
			 zone.setZoneRegulation(regle); 
		 }else{
			 System.out.println("Missing regulation for zone : " + zone.getLibelle());
			 System.out.println("We associate the default regulation");
			 
			 if(regles.get("999") == null){
				 System.out.println("Default regulation does not exist");
			 }else {
				 zone.setZoneRegulation(regles.get("999")); 
			 }
			 
			
		 }
		
		}

		List<File> listBatiSimu = new ArrayList<File>();

		int nbBPU = env.getBpU().size();
		for (int i = 0; i < nbBPU; i++) {

			File file = runSimulation(env, i, p);
			if (file != null) {
				listBatiSimu.add(file);
			}

		}
		// Si la simu n'est pas trop inspirée
		if (listBatiSimu.isEmpty()) {
			System.out.println(
					"&&&&&&&&&&&&&& Aucun bâtiment n'a été simulé pour la commune " + zipCode + " &&&&&&&&&&&&&&");
			System.exit(1);
		}

		return listBatiSimu;
	}

	public File runSimulation(Environnement env, int i, Parameters p) throws Exception {
		// HashMap<String, SamplePredicate<Cuboid, GraphConfiguration<Cuboid>,
		// BirthDeathModification<Cuboid>>> catalog = new HashMap<String,
		// SamplePredicate<Cuboid,
		// GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>>();
		BasicPropertyUnit bPU = env.getBpU().get(i);

		// List ID Parcelle to Simulate is not empty
		if (!ID_PARCELLE_TO_SIMULATE.isEmpty()) {
			// We check if the code is in the list
			if (!ID_PARCELLE_TO_SIMULATE.contains(bPU.getCadastralParcels().get(0).getCode())) {
				return null;
			}
		}

		System.out.println("Parcelle code : " + bPU.getCadastralParcels().get(0).getCode());

		// si on lance une simulation avec une seule parcelle décrite dans
		// l'environnement, son numéro sera 0 mais le numéro de la parcelle sera
		// conservé
		if (isSingleFeat) {
			bPU = env.getBpU().get(0);
		}

		// Instantiation of the sampler
		OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
	



		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = new FT_FeatureCollection<>();

		for (Prescription prescription : prescriptions) {
			switch (prescription.getType()) {
			case ESPACE_BOISE:
				if (p.getBoolean("ESPACE_BOISE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case NUISSANCES_RISQUE:
				if (p.getBoolean("NUISSANCES_RISQUE")) {
					// si pas toutes les nuissances sont exclues
					if (p.getBoolean("NUISSANCES_RISQUE-MAX")) {
						// si le libelle ne contiens pas ces keywords, ce n'est
						// somme toute pas très grave
						if (prescription.getLabel().contains("grave") || prescription.getLabel().contains("fort")
								|| prescription.getLabel().contains("Maximal")
								|| prescription.getLabel().contains("rouge")) {
							prescriptionUse.add(prescription);
						}
					} else {
						prescriptionUse.add(prescription);
					}
				}
				break;
			case EMPLACEMENT_RESERVE:
				if (!p.getBoolean("EMPLACEMENT_RESERVE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case ELEMENT_PAYSAGE:
				if (!p.getBoolean("ELEMENT_PAYSAGE")) {
					prescriptionUse.add(prescription);
				}
				break;
			case RECOIL:
				if (!p.getBoolean("RECOIL")) {
					prescriptionUse.add(prescription);
				}
				break;
			case TVB:
				if (!p.getBoolean("TVB")) {
					prescriptionUse.add(prescription);
				}
				break;
			default:
				System.out.println(SimPLUSimulator.class.toString() + "  :  Other case " + prescription.getLabel()
						+ " Code : " + prescription.getType());
				break;
			}
		}

		ConfigurationModificationPredicate<GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = null;
		
		
		
		if(! USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL|| bPU.getCadastralParcels().get(0).getSubParcels().size() < 2) {
			//In this mod there is only one regulation for the entire BPU
			 pred = preparePredicateOneRegulation(bPU, p, prescriptionUse);
			 
				if (!((PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>)pred).isCanBeSimulated()) {
					System.out.println("Parcel is overlapped by graphical prescriptions");
					return null;
				}
		}else {
			
		}



	

		Double areaParcels = 0.0;
		for (CadastralParcel yo : bPU.getCadastralParcels()) {
			areaParcels = areaParcels + yo.getArea();
		}

		// Run of the optimisation on a parcel with the predicate
		GraphConfiguration<Cuboid> cc = oCB.process(bPU, p, env, 1, pred);

		IFeatureCollection<IFeature> iFeat3D = new FT_FeatureCollection<>();
		for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {
			IFeature feat = new DefaultFeature(v.getValue().generated3DGeom());
			iFeat3D.add(feat);
		}
		List<Cuboid> cubes = LoaderCuboid.loadFromCollection(iFeat3D);
		SDPCalc surfGen = new SDPCalc();
		double formTot = surfGen.process(cubes);

		// Witting the output
		IFeatureCollection<IFeature> iFeatC = new FT_FeatureCollection<>();

		// For all generated boxes
		for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {

			// Output feature with generated geometry
			// IFeature feat = new
			// DefaultFeature(v.getValue().generated3DGeom());

			IFeature feat = new DefaultFeature(v.getValue().getFootprint());

			// We write some attributes

			AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width),
					"Double");
			AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
			AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
			AttributeManager.addAttribute(feat, "SurfaceBox", feat.getGeom().area(), "Double");
			AttributeManager.addAttribute(feat, "SDPShon", formTot * 0.8, "Double");
			AttributeManager.addAttribute(feat, "areaParcel", areaParcels, "Double");
			AttributeManager.addAttribute(feat, "num", i, "Integer");
			iFeatC.add(feat);
		}

		// TODO Prendre la shon (calcul dans
		// simplu3d.experiments.openmole.diversity ? non, c'est la shob et pas
		// la shon !! je suis ingénieur en génie civil que diable. Je ne peux
		// pas me permettre de ne pas prendre en compte un des seuls trucs que
		// je peux sortir de mes quatre ans d'étude pour cette these..!)

		// méthode de calcul d'air simpliste

		File output = new File(simuFile, "out-parcelle_" + i + ".shp");

		// while (output.exists()) {
		// output = new File(simuFile, "out-parcelle_" + compteurOutput +
		// ".shp");
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
	
	private static PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulation(BasicPropertyUnit bPU, Parameters p, IFeatureCollection<Prescription>  prescriptionUse) throws Exception
	{
		List<SubParcel> sP =  bPU.getCadastralParcels().get(0).getSubParcels();
		//We sort the parcel
		sP.sort(new Comparator<SubParcel>() {
			@Override
			public int compare(SubParcel o1, SubParcel o2) {
				return Double.compare(o1.getArea(), o2.getArea());
			}
		});
		SubParcel sPBiggest = sP.get(sP.size() -1);
	
		ArtiScalesRegulation regle =(ArtiScalesRegulation) sPBiggest.getUrbaZone().getZoneRegulation();
		
	

		// Instantiation of the rule checker

		PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicatePLUCities<>(bPU,true, regle,p, prescriptionUse);
		
		
		return pred;
	}

	/**
	 * Class used to fill a parcel file containing multiple parcels with
	 * buildings simulated with SimPLU
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
	protected static int fillSelectedParcels(File rootFile, File geoFile, File pluFile, File selectedParcels,
			int missingHousingUnits, String zipcode, Parameters p) throws Exception {
		// Itérateurs sur les parcelles où l'on peut construire
		ShapefileDataStore parcelDS = new ShapefileDataStore(selectedParcels.toURI().toURL());
		SimpleFeatureIterator iterator = parcelDS.getFeatureSource().getFeatures().features();

		try {
			// Tant qu'il y a besoin de logements et qu'il y a des parcelles
			// disponibles
			while (missingHousingUnits > 0 && iterator.hasNext()) {
				SimpleFeature sinlgeParcel = iterator.next();
				// On créer un nouveau simulateur
				SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, geoFile, pluFile, selectedParcels,
						sinlgeParcel, zipcode, p);
				// On lance la simulation
				File batiSimulatedFile = simPLUsimu.runOneSim((int) sinlgeParcel.getAttribute("num"));

				// On met à jour le compteur du nombre de logements avec
				// l'indicateur
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
