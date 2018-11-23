package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.MersenneTwister;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.convert.FromGeomToSurface;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.geomaggr.GM_MultiSurface;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.rules.io.PrescriptionPreparator;
import fr.ign.cogit.rules.io.ZoneRulesAssociation;
import fr.ign.cogit.rules.predicate.CommonPredicateArtiScales;
import fr.ign.cogit.rules.predicate.MultiplePredicateArtiScales;
import fr.ign.cogit.rules.predicate.PredicateArtiScales;
import fr.ign.cogit.rules.regulation.Alignements;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.ParcelBoundarySide;
import fr.ign.cogit.simplu3d.model.Prescription;
import fr.ign.cogit.simplu3d.model.SubParcel;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.geometry.impl.Cuboid;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.cuboid.OptimisedBuildingsCuboidFinalDirectRejection;
import fr.ign.cogit.simplu3d.rjmcmc.cuboid.optimizer.paralellcuboid.ParallelCuboidOptimizer;
import fr.ign.cogit.simplu3d.util.merge.SDPCalc;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.SimpluParametersXML;
import fr.ign.cogit.util.SimuTool;
import fr.ign.cogit.util.VectorFct;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;
import fr.ign.parameters.Parameters;

public class SimPLUSimulator {

	// parcels containing all of them and the code if we make a simulation on them or not
	File parcelsFile;

	// one single parcel to study
	SimpleFeature singleFeat;
	boolean isSingleFeat = false;

	File rootFile;

	// Parameters from technical parameters and scenario parameters files
	Parameters p;
	// backup when p has been overwritted
	Parameters pSaved;

	// Building file
	File buildFile;
	// Road file
	File roadFile;

	// Predicate File (a csv with a key to make a join with zoning file between id
	// and libelle)
	File predicateFile;
	// PLU Zoning file
	File zoningFile;
	File codeFile;
	File simuFile;
	int compteurOutput = 0;

	// Prescription files
	File filePrescPonct;
	File filePrescLin;
	File filePrescSurf;

	public static List<String> ID_PARCELLE_TO_SIMULATE = new ArrayList<>();

	public static boolean USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;

	public static void main(String[] args) throws Exception {

		// /*
		// * String folderGeo = "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/donneeGeographiques/"; String zoningFile =
		// * "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/donneeGeographiques/PLU/ Zonage_CAGB_INSEE_25495.shp" ; String folderOut = "/tmp/tmp/";
		// *
		// * File f = Vectors.snapDatas(GetFromGeom.getRoute(new File(folderGeo)), new File(zoningFile), new File(folderOut));
		// *
		// * System.out.println(f.getAbsolutePath());
		// */
		//
		// List<File> lF = new ArrayList<>();
		// // Line to change to select the right scenario
		//
		// String rootParam = SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenar0MKDom/").getPath();
		//
		// System.out.println(rootParam);
		//
		// lF.add(new File(rootParam + "parametreTechnique.xml"));
		// lF.add(new File(rootParam + "parametreScenario.xml"));
		//
		// Parameters p = Parameters.unmarshall(lF);
		//
		// System.out.println(p.getString("nom"));
		// // Rappel de la construction du code :
		//
		// // 1/ Basically the parcels are filtered on the code with the following
		// // attributes
		// // codeDep + codeCom + comAbs + section + numero
		//
		// // 2/ Alternatively we can decided to active an attribute (Here id)
		// AttribNames.setATT_CODE_PARC("CODE");
		//
		// // ID_PARCELLE_TO_SIMULATE.add("25495000AE0102"); //Test for a simulation
		// // with 1 regulation
		//
		// USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;
		//ID_PARCELLE_TO_SIMULATE.add("25381000NewSection213"); // Test for a simulation with
		AttribNames.setATT_HAS_TO_BE_SIMULATED("DoWeSimul");
		// // 3 regulations on 3 sub
		// // parcels
		//
		// // RootFolder
		// File rootFolder = new File(p.getString("rootFile"));
		// // Selected parcels shapefile
		// File selectedParcels = new File(p.getString("selectedParcelFile"));
		//
		// SimPLUSimulator simplu = new SimPLUSimulator(rootFolder, selectedParcels, p);
		//
		// simplu.run();
		// // SimPLUSimulator.fillSelectedParcels(new File(rootFolder), geoFile,
		// // pluFile, selectedParcels, 50, "25495", p);

		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/scenar0MCIgn");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parametreTechnique.xml"));
		lF.add(new File(rootParam, "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);
		AttribNames.setATT_CODE_PARC("CODE");
		USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;
		File f = new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/intenseRegulatedSpread/variant0");

		for (File ff : f.listFiles()) {
			SimPLUSimulator sim = new SimPLUSimulator(new File("/home/mcolomb/informatique/ArtiScales/"), ff, p);
			sim.run();
		
		}
	}

	/**
	 * Constructor to make a new object to run SimPLU3D simulations.
	 * 
	 * @param rootfile
	 *            : main folder of an artiscale simulation
	 * @param geoFile
	 *            : folder for geographic data
	 * @param regulFile
	 *            : folder for PLU data
	 * @param parcels
	 *            : Folder containing the selection of parcels
	 * @param feat
	 *            : single parcel to simulate
	 * @param zipcode
	 *            : zipcode of the city that is simulated
	 * @param pa
	 *            : parameters file
	 * @param lF
	 *            : list of the initials parameters
	 * @throws Exception
	 */
	public SimPLUSimulator(File rootfile, File packFile, Parameters pa) throws Exception {

		// some static parameters needed
		this.p = pa;
		this.pSaved = pa;
		this.rootFile = rootfile;

		simuFile = packFile;
		parcelsFile = new File(packFile, "/parcelle.shp");
		zoningFile = new File(packFile, "/geoSnap/zoning.shp");
		buildFile = new File(packFile, "/geoSnap/building.shp");
		roadFile = new File(packFile, "/geoSnap/road.shp");

		predicateFile = new File(packFile, "snapPredicate.csv");

		filePrescPonct = new File(packFile, "/geoSnap/prescPonct.shp");
		filePrescLin = new File(packFile, "/geoSnap/prescLin.shp");
		filePrescSurf = new File(packFile, "/geoSnap/prescSurf.shp");

		if (!zoningFile.exists()) {
			System.err.print("error : zoning files not found");
		}

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

	public SimPLUSimulator(File rootfile, File selectedParcels, SimpleFeature feat, Parameters pa) throws Exception {
		this(rootfile, selectedParcels, pa);
		singleFeat = feat;
		isSingleFeat = true;

	}

	/*
	 * public static List<File> run(File rootFile, File geoFile, File pluFile, File parcelfiles, String zipcode, Parameters p) throws Exception { SimPLUSimulator SPLUS = new
	 * SimPLUSimulator(rootFile, geoFile, pluFile, parcelfiles, null, zipcode, p); return SPLUS.run(); }
	 */

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
		// The file that store the results
		File featFile = new File(simuFile, "tmp.shp");
		DefaultFeatureCollection tmp = new DefaultFeatureCollection();
		tmp.add(singleFeat);
		Vectors.exportSFC(tmp.collection(), featFile);

		// SimPLU3D-rules geographic loader
		Environnement env = LoaderSHP.load(simuFile, null, zoningFile, featFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);

		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, p);

		// Simulation on one parcel
		File out = runSimulation(env, numParcel, p, prescriptionUse);
		featFile.delete();
		return out;
	}

	/**
	 * Run a SimPLU3D simulation on all the parcel stored in the parcelFile's SimpleFeatureCollection
	 * 
	 * @return a list of shapefile containing the simulated buildings
	 * @throws Exception
	 */
	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);

		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, p);

		boolean association = ZoneRulesAssociation.associate(env, predicateFile, GetFromGeom.rnuZip(new File(rootFile, "dataRegul")));

		if (!association) {
			System.out.println("Association between rules and UrbanZone failed");
			return null;
		}

		List<File> listBatiSimu = new ArrayList<File>();

		// We run a simulation for each bPU with a different file for each bPU
		// @TODO : on garde 1 fichier par bPU ? Tu t'en sors après ou c'est pas trop
		// dur
		// ?
		int nbBPU = env.getBpU().size();
		for (int i = 0; i < nbBPU; i++) {
			// if parcel has been marked an non simulable, return null
			
			System.out.println("Parcel : " + env.getBpU().get(i).getCadastralParcels().get(0).hasToBeSimulated() + "  -  Code : " + env.getBpU().get(i).getCadastralParcels().get(0).getCode());
			if (! env.getBpU().get(i).getCadastralParcels().get(0).hasToBeSimulated()) {
				
			//if (!isParcelSimulable(env.getBpU().get(i).getCadastralParcels().get(0).getCode())) {
				System.out.println(env.getBpU().get(i).getCadastralParcels().get(0).getCode() + " : je l'ai stopé net coz pas selec");
				continue;
			}
			p = pSaved;
			File file = runSimulation(env, i, p, prescriptionUse);
			if (file != null) {
				listBatiSimu.add(file);
			}
		}

		// Not results
		if (listBatiSimu.isEmpty()) {
			System.out.println("&&&&&&&&&&&&&& Aucun bâtiment n'a été simulé &&&&&&&&&&&&&&");
			return null;
		}

		VectorFct.mergeBatis(listBatiSimu);

		return listBatiSimu;
	}

	public boolean isParcelSimulable(String codeParcel) throws IOException {
		boolean result = true;
		ShapefileDataStore sds = new ShapefileDataStore(parcelsFile.toURI().toURL());
		SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if (feat.getAttribute("CODE").equals(codeParcel)) {
					if (feat.getAttribute("DoWeSimul").equals("false")) {
						result = false;
					}
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		sds.dispose();
		return result;
	}

	/**
	 * Simulation for the ie bPU
	 * 
	 * @param env
	 * @param i
	 * @param p
	 * @param prescriptionUse
	 *            the prescriptions in Use prepared with PrescriptionPreparator
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "deprecation" })
	public File runSimulation(Environnement env, int i, Parameters p, IFeatureCollection<Prescription> prescriptionUse) throws Exception {

		BasicPropertyUnit bPU = env.getBpU().get(i);

		// List ID Parcelle to Simulate is not empty
		if (!ID_PARCELLE_TO_SIMULATE.isEmpty()) {
			// We check if the code is in the list
			if (!ID_PARCELLE_TO_SIMULATE.contains(bPU.getCadastralParcels().get(0).getCode())) {
				return null;
			}
		}

		System.out.println("Parcelle code : " + bPU.getCadastralParcels().get(0).getCode());

		CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = null;

		// According to the case, different predicates may be used
		// Do we consider 1 regualtion by parcel or one by subParcel ?
		if (!USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL || bPU.getCadastralParcels().get(0).getSubParcels().size() < 2) {
			// In this mode there is only one regulation for the entire BPU
			pred = preparePredicateOneRegulation(bPU, p, prescriptionUse, env);

		} else {
			pred = preparePredicateOneRegulationBySubParcel(bPU, p, prescriptionUse, env);
		}

		if (pred == null) {
			System.out.println("Predicate cannot been instanciated");
			return null;
		}

		if (!pred.isCanBeSimulated()) {
			System.out.println("Parcel is not simulable according to the predicate");
			return null;
		}
		// We compute the parcel area
		Double areaParcels = bPU.getArea(); // .getCadastralParcels().stream().mapToDouble(x -> x.getArea()).sum();

		GraphConfiguration<Cuboid> cc = null;

		Alignements alignementsGeometries = pred.getAlignements();

		if (alignementsGeometries.getHasAlignement()) {

			switch (alignementsGeometries.getType()) {
			// #Art71 case 1 or 2
			case ART7112:
				cc = article71Case12(alignementsGeometries, pred, env, i, bPU);
				break;
			// #Art71 case 1 or 3
			case ART713:
				cc = article71Case3(alignementsGeometries, pred, env, i, bPU);
				break;
			case NONE:
				System.out.println(this.getClass().getName() + " : Normally not possible case");
				return null;
			default:
				System.out.println(this.getClass().getName() + " : Normally not possible case" + alignementsGeometries.getType());
				return null;

			}

			if (cc == null) {
				return null;
			}

		} else {
			OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
			cc = oCB.process(bPU, new SimpluParametersXML(p), env, i, pred);
			if (cc == null) {
				return null;
			}
		}

		// Getting cuboid into list
		List<Cuboid> cubes = cc.getGraph().vertexSet().stream().map(x -> x.getValue()).collect(Collectors.toList());
		SDPCalc surfGen = new SDPCalc();
		double surfacePlancherTotal = surfGen.process(cubes);
		// TODO return null - fix it
		double surfaceAuSol = surfGen.processSurface(cubes);

		// get multiple zone regulation infos infos
		List<String> typeZones = new ArrayList<>();
		List<String> libelles = new ArrayList<>();
		String libellesFinal = "";
		String typeZonesFinal = "";

		// if multiple parts of a parcel has been simulated, put a long name containing them all
		try {
			for (SubParcel subParcel : bPU.getCadastralParcels().get(0).getSubParcels()) {
				String temporaryTypeZone = subParcel.getUrbaZone().getTypeZone();
				String temporarylibelle = subParcel.getUrbaZone().getLibelle();
				if (!typeZones.contains(temporaryTypeZone)) {
					typeZones.add(temporaryTypeZone);
				}
				if (!libelles.contains(temporarylibelle)) {
					libelles.add(temporarylibelle);
				}
			}
			for (String typeZoneTemp : typeZones) {
				typeZonesFinal = typeZonesFinal + typeZoneTemp + "+";
			}
			typeZonesFinal = typeZonesFinal.substring(0, typeZonesFinal.length() - 1);

			for (String libelleTemp : libelles) {
				libellesFinal = libellesFinal + libelleTemp + "+";
			}
			libellesFinal = libellesFinal.substring(0, libellesFinal.length() - 1);

		} catch (NullPointerException np) {
			libellesFinal = "NC";
			typeZonesFinal = "NC";
		}
		// Writting the output
		IFeatureCollection<IFeature> iFeatC = new FT_FeatureCollection<>();

		// For all generated boxes
		for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {

			// Output feature with generated geometry
			// IFeature feat = new
			// DefaultFeature(v.getValue().generated3DGeom());

			IFeature feat = new DefaultFeature(v.getValue().getFootprint());

			// We write some attributes

			AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
			AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
			AttributeManager.addAttribute(feat, "SurfaceBox", feat.getGeom().area(), "Double");
			AttributeManager.addAttribute(feat, "SDPShon", surfacePlancherTotal * 0.8, "Double");
			AttributeManager.addAttribute(feat, "SurfacePar", areaParcels, "Double");
			AttributeManager.addAttribute(feat, "SurfaceSol", surfaceAuSol, "Double"); // TODO doesn't work
			AttributeManager.addAttribute(feat, "CODE", bPU.getCadastralParcels().get(0).getCode(), "String");
			AttributeManager.addAttribute(feat, "LIBELLE", libellesFinal, "String");
			AttributeManager.addAttribute(feat, "TYPEZONE", typeZonesFinal, "String");
			iFeatC.add(feat);
		}

		// méthode de calcul d'aire simpliste

		// TODO sortir ça de cette méthode?
		File output = new File(SimuTool.createScenarVariantFolders(simuFile, rootFile, "SimPLUDepot"), "out-parcelle_" + bPU.getCadastralParcels().get(0).getCode() + ".shp");
		System.out.println("Output in : " + output);
		ShapefileWriter.write(iFeatC, output.toString(), CRS.decode("EPSG:2154"));

		if (!output.exists()) {
			output = null;
		}
		return output;
	}

	private GraphConfiguration<Cuboid> article71Case3(Alignements alignementsGeometries,
			CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i, BasicPropertyUnit bPU) throws Exception {

		GraphConfiguration<Cuboid> cc = null;

		IGeometry[] geoms = alignementsGeometries.getSideWithBuilding();

		if (geoms.length == 0) {
			OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
			cc = oCB.process(bPU, new SimpluParametersXML(p), env, i, pred);
		} else {

			// Instantiation of the sampler
			ParallelCuboidOptimizer oCB = new ParallelCuboidOptimizer();

			IMultiSurface<IOrientableSurface> iMSSamplinSurface = new GM_MultiSurface<>();

			for (IGeometry geom : geoms) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(p.getDouble("maxwidth") / 2)));
			}
			// Run of the optimisation on a parcel with the predicate
			cc = oCB.process(new MersenneTwister(), bPU, new SimpluParametersXML(p), env, i, pred, geoms, iMSSamplinSurface);
		}

		return cc;
	}

	private GraphConfiguration<Cuboid> article71Case12(Alignements alignementsGeometries,
			CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i, BasicPropertyUnit bPU) throws Exception {

		GraphConfiguration<Cuboid> cc = null;
		// Instantiation of the sampler
		ParallelCuboidOptimizer oCB = new ParallelCuboidOptimizer();

		IMultiSurface<IOrientableSurface> iMSSamplinSurface = new GM_MultiSurface<>();
		// art-0071 implentation (begin)
		// LEFT SIDE IS TESTED
		IGeometry[] leftAlignement = alignementsGeometries.getLeftSide();

		if (leftAlignement != null && (leftAlignement.length > 0)) {
			for (IGeometry geom : leftAlignement) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(p.getDouble("maxwidth") / 2)));
			}

			pred.setSide(ParcelBoundarySide.LEFT);

			// Run of the optimisation on a parcel with the predicate
			cc = oCB.process(new MersenneTwister(), bPU, new SimpluParametersXML(p), env, i, pred, leftAlignement, iMSSamplinSurface);
		}

		// RIGHT SIDE IS TESTED

		IGeometry[] rightAlignement = alignementsGeometries.getRightSide();
		GraphConfiguration<Cuboid> cc2 = null;
		if (rightAlignement != null && (rightAlignement.length > 0)) {

			iMSSamplinSurface = new GM_MultiSurface<>();
			oCB = new ParallelCuboidOptimizer();
			for (IGeometry geom : rightAlignement) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(p.getDouble("maxwidth") / 2)));
			}

			pred.setSide(ParcelBoundarySide.RIGHT);

			cc2 = oCB.process(new MersenneTwister(), bPU, new SimpluParametersXML(p), env, i, pred, rightAlignement, iMSSamplinSurface);
		}

		if (cc == null) {
			cc = cc2;
		}

		if (cc2 != null) {
			if (cc.getEnergy() < cc2.getEnergy()) {
				// We keep the configuratino with the best energy
				cc = cc2;
			}

		}

		return cc;
	}

	private CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulationBySubParcel(BasicPropertyUnit bPU,
			Parameters p2, IFeatureCollection<Prescription> prescriptionUse, Environnement env) throws Exception {

		MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>(
				bPU, true, p2, prescriptionUse, env);

		return pred;
	}

	private static PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulation(BasicPropertyUnit bPU, Parameters p,
			IFeatureCollection<Prescription> prescriptionUse, Environnement env) throws Exception {
		List<SubParcel> sP = bPU.getCadastralParcels().get(0).getSubParcels();
		// We sort the subparcel to get the biffests
		sP.sort(new Comparator<SubParcel>() {
			@Override
			public int compare(SubParcel o1, SubParcel o2) {
				return Double.compare(o1.getArea(), o2.getArea());
			}
		});
		SubParcel sPBiggest = sP.get(sP.size() - 1);

		if (sPBiggest.getUrbaZone() == null) {
			System.out.println("Regulation is null for : " + bPU.getCadastralParcels().get(0).getCode());
			return null;

		}

		ArtiScalesRegulation regle = (ArtiScalesRegulation) sPBiggest.getUrbaZone().getZoneRegulation();

		if (regle == null) {
			System.out.println("Regulation is null for : " + bPU.getCadastralParcels().get(0).getCode());
			return null;
		}

		System.out.println("Regulation code : " + sPBiggest.getUrbaZone().getLibelle());
		// Instantiation of the rule checker
		PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicateArtiScales<>(bPU, true, regle, p, prescriptionUse, env);

		return pred;

	}

	/**
	 * Class used to fill a parcel file containing multiple parcels with buildings simulated with SimPLU TODO verify
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

	protected static int fillSelectedParcels(File rootFile, File geoFile, File pluFile, File selectedParcels, int missingHousingUnits, String zipcode, Parameters p, List<File> lF)
			throws Exception {
		// Itérateurs sur les parcelles où l'on peut construire
		ShapefileDataStore parcelDS = new ShapefileDataStore(selectedParcels.toURI().toURL());
		SimpleFeatureIterator iterator = parcelDS.getFeatureSource().getFeatures().features();

		try {
			// Tant qu'il y a besoin de logements et qu'il y a des parcelles
			// disponibles
			while (missingHousingUnits > 0 && iterator.hasNext()) {
				SimpleFeature sinlgeParcel = iterator.next();
				// On créer un nouveau simulateur
				SimPLUSimulator simPLUsimu = new SimPLUSimulator(rootFile, selectedParcels, sinlgeParcel, p);

				// On lance la simulation
				File batiSimulatedFile = simPLUsimu.runOneSim((int) sinlgeParcel.getAttribute("CODE"));

				List<File> listSimulatedFile = new ArrayList<File>();
				listSimulatedFile.add(batiSimulatedFile);

				// On met à jour le compteur du nombre de logements avec
				// l'indicateur
				// buildingToHousehold

				BuildingToHousingUnit bTH = new BuildingToHousingUnit(listSimulatedFile, selectedParcels, p);
				System.out.println("--- missingHousingUnits  " + missingHousingUnits);

				missingHousingUnits = missingHousingUnits - bTH.runParticularSimpleEstimation();
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
