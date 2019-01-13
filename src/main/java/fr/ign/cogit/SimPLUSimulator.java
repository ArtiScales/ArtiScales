package fr.ign.cogit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.MersenneTwister;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;

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
import fr.ign.cogit.rules.io.PrescriptionPreparator;
import fr.ign.cogit.rules.io.ZoneRulesAssociation;
import fr.ign.cogit.rules.predicate.CommonPredicateArtiScales;
import fr.ign.cogit.rules.predicate.MultiplePredicateArtiScales;
import fr.ign.cogit.rules.predicate.PredicateArtiScales;
import fr.ign.cogit.rules.regulation.Alignements;
import fr.ign.cogit.rules.regulation.ArtiScalesRegulation;
import fr.ign.cogit.rules.regulation.buildingType.BuildingType;
import fr.ign.cogit.rules.regulation.buildingType.MultipleRepartitionBuildingType;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.simplu3d.io.feature.AttribNames;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
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

	// parcels containing all of them and the code if we make a simulation on them
	// or not
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
		// * String folderGeo =
		// "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/donneeGeographiques/";
		// String zoningFile =
		// *
		// "/home/mbrasebin/Documents/Donnees/ArtiScales/ArtiScales/donneeGeographiques/PLU/
		// Zonage_CAGB_INSEE_25495.shp" ; String folderOut = "/tmp/tmp/";
		// *
		// * File f = Vectors.snapDatas(GetFromGeom.getRoute(new File(folderGeo)), new
		// File(zoningFile), new File(folderOut));
		// *
		// * System.out.println(f.getAbsolutePath());
		// */
		//
		// List<File> lF = new ArrayList<>();
		// // Line to change to select the right scenario
		//
		// String rootParam =
		// SimPLUSimulator.class.getClassLoader().getResource("paramSet/scenar0MKDom/").getPath();
		//
		// System.out.println(rootParam);
		//
		// lF.add(new File(rootParam + "parameterTechnic.xml"));
		// lF.add(new File(rootParam + "parameterScenario.xml"));
		//
		// Parameters p = Parameters.unmarshall(lF);
		//
		// System.out.println(p.getString("name"));
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
		// ID_PARCELLE_TO_SIMULATE.add("25381000NewSection213"); // Test for a
		// simulation with
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

		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/exScenar");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parameterTechnic.xml"));
		lF.add(new File(rootParam, "parameterScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);
		// AttribNames.setATT_CODE_PARC("CODE");
		// USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;

		File f = new File("/home/mcolomb/informatique/ArtiScales/ParcelSelectionFile/exScenar/variant0/");

		List<File> listBatiSimu = new ArrayList<File>();
		for (File ff : f.listFiles()) {
			if (ff.isDirectory()) {
				SimPLUSimulator sim = new SimPLUSimulator(ff, p);
				List<File> simued = sim.run();
				if (simued != null) {
					listBatiSimu.addAll(simued);
				}
				System.out.println("done with pack " + ff.getName());
			}
		}
		VectorFct.mergeBatis(listBatiSimu);

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
	public SimPLUSimulator(File packFile, Parameters pa) throws Exception {

		// some static parameters needed
		this.p = pa;
		this.pSaved = pa;
		this.rootFile = new File(p.getString("rootFile"));
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
	 * Run a SimPLU3D simulation on all the parcel stored in the parcelFile's SimpleFeatureCollection
	 * 
	 * @return a list of shapefile containing the simulated buildings
	 * @throws Exception
	 */
	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);

		///////////
		// asses repartition to pacels
		///////////
		// know if there's only one or multiple zones in the parcel pack
		List<String> zones = new ArrayList<String>();
		IFeatureCollection<CadastralParcel> parcels = env.getCadastralParcels();

		for (CadastralParcel parcel : parcels) {
			String tmp = GetFromGeom.affectToZoneAndTypo(p, parcel, true);
			if (!zones.contains(tmp)) {
				zones.add(tmp);
			}
		}

		// loading the type of housing to build
		System.out.println(parcelsFile);
		RepartitionBuildingType housingUnit = new RepartitionBuildingType(p, parcelsFile);
		boolean multipleRepartitionBuildingType = false;
		if (zones.size() > 1) {
			System.out.println("multiple zones in the same parcel lot : there's gon be approximations");
			System.out.println("zones are ");
			for (String s : zones) {
				System.out.println(s);
			}
			System.out.println();
			housingUnit = new MultipleRepartitionBuildingType(p, parcelsFile);
			multipleRepartitionBuildingType = true;
		} else {
			System.out.println("it's all normal");
		}

		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, p);

		boolean association = ZoneRulesAssociation.associate(env, predicateFile, GetFromGeom.rnuZip(new File(rootFile, "dataRegulation")), willWeAssociateAnyway(p));

		if (!association) {
			System.out.println("Association between rules and UrbanZone failed");
			return null;
		}

		List<File> listBatiSimu = new ArrayList<File>();
		//////////////
		// We run a simulation for each bPU with a different file for each bPU
		//////////////
		int nbBPU = env.getBpU().size();
		bpu: for (int i = 0; i < nbBPU; i++) {

			// if this parcel contains no attributes, it means that it has been put here
			// just to express its boundaries
			if (env.getBpU().get(i).getCadastralParcels().get(0).getCode() == null) {
				continue;
			}
			// if parcel has been marked as non simulable, return null
			if (!isParcelSimulable(env.getBpU().get(i).getCadastralParcels().get(0).getCode())) {
				env.getBpU().get(i).getCadastralParcels().get(0).setHasToBeSimulated(false);
				System.out.println(env.getBpU().get(i).getCadastralParcels().get(0).getCode() + " : je l'ai stopé net coz pas selec");
				continue;
			}
			String codeParcel = env.getBpU().get(i).getCadastralParcels().get(0).getCode();
			System.out.println("Parcel code : " + codeParcel);

			double eval = getParcelEval(env.getBpU().get(i).getCadastralParcels().get(0).getCode());

			// of which type should be the housing unit
			BuildingType type;
			if (multipleRepartitionBuildingType) {
				type = ((MultipleRepartitionBuildingType) housingUnit).rangeInterest(eval, codeParcel, p);
			} else {
				type = housingUnit.rangeInterest(eval);
			}

			// we get ready to change it
			boolean seekType = true;
			// boolean adjustUp = false;
			boolean adjustDown = false;

			BuildingType[] fromTo = new BuildingType[2];
			fromTo[0] = type;
			IFeatureCollection<IFeature> bati = null;
			// until we found the right type
			while (seekType) {
				System.out.println("we try to put a " + type + " housing unit");
				// we add the parameters for the building type want to simulate
				p = pSaved;
				p.add(RepartitionBuildingType.getParam(new File(this.getClass().getClassLoader().getResource("profileBuildingType").getFile()), type));

				bati = runSimulation(env, i, p, type, prescriptionUse);

				if (bati == null || bati.isEmpty()) {
					continue bpu;
				}

				// we see if the Housing Unit Type is correct
				if ((double) bati.get(0).getAttribute("SDPShon") < p.getDouble("areaMin")) {
					adjustDown = true;
					BuildingType typeTemp = housingUnit.down(type);
					// if it's not the same type, we'll continue to seek
					if (!(typeTemp == type)) {
						type = typeTemp;
						System.out.println("we'll try a " + type + "instead");
					}
					// if it's blocked, we'll go for this type
					else {
						seekType = false;
					}
					// // I'm not sure this case can occur, but..
					// } else if ((double) bati.get(0).getAttribute("SDPShon") > p.getDouble("areaMax")) {
					// adjustUp = true;
					// TypeHousingUnit typeTemp = housingUnit.down(type);
					// // if it's not the same type, we'll continue to seek
					// if (!(typeTemp == type)) {
					// type = typeTemp;
					// System.out.println("we'll try a " + type + "instead");
					// }
					// // if it's blocked, we'll go for this type
					// else {
					// seekType = false;
					// }
				} else {
					seekType = false;
					// if there's a come and go situation, we break it down like a cop
					// if (adjustDown == true && adjustUp == true) {
					if (adjustDown == true) {
						System.out.println("we break down the fuzz between housing unit like a cop/judge");
						break;
						// } else if (adjustUp) {
						// fromTo[1] = type;
						// housingUnit.adjustDistributionUp(eval, fromTo[1], fromTo[0]);
					} else if (adjustDown) {
						fromTo[1] = type;
						housingUnit.adjustDistributionDown(eval, fromTo[1], fromTo[0]);
					}
				}
			}
			// saving the output
			File folderOut = SimuTool.createScenarVariantFolders(simuFile, rootFile, "SimPLUDepot");
			folderOut.mkdirs();
			File output = new File(folderOut, "out-parcel_" + bati.get(0).getAttribute("CODE") + ".shp");
			System.out.println("Output in : " + output);
			ShapefileWriter.write(bati, output.toString(), CRS.decode("EPSG:2154"));

			if (!output.exists()) {
				output = null;
			}

			System.out.println("");
			if (output != null) {
				listBatiSimu.add(output);
			}
		}

		// No results
		if (listBatiSimu.isEmpty()) {
			System.out.println("&&&&&&&&&&&&&& Aucun bâtiment n'a été simulé &&&&&&&&&&&&&&");
			return null;
		}

		return listBatiSimu;
	}

	/**
	 * small method to know if we need to perform the simulation on zones that are not open to the urbanization.
	 * 
	 * @param p2
	 *            : paramterer file, containing the answer to our question
	 * @return boolean : true if we do
	 */
	private HashMap<String, Boolean> willWeAssociateAnyway(Parameters p2) {
		HashMap<String, Boolean> result = new HashMap<String, Boolean>();
		if (p.getBoolean("2AU")) {
			result.put("2AU", true);
		} else {
			result.put("2AU", false);
		}

		if (p.getBoolean("NC")) {
			result.put("NC", true);
		} else {
			result.put("NC", false);
		}
		return result;
	}

	/**
	 * for a given parcel, seek if the parcel general file has said that it could be simulated
	 * 
	 * @param codeParcel
	 * @return
	 * @throws IOException
	 */
	public boolean isParcelSimulable(String codeParcel) throws IOException {
		boolean result = true;
		ShapefileDataStore sds = new ShapefileDataStore(parcelsFile.toURI().toURL());
		SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if (feat.getAttribute("CODE") != null) {
					if (feat.getAttribute("CODE").equals(codeParcel)) {
						if (feat.getAttribute("DoWeSimul").equals("false")) {
							result = false;
						}
						break;
					}
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
	 * for a given parcel, seek if the parcel general file has said that it could be simulated
	 * 
	 * @param codeParcel
	 * @return
	 * @throws IOException
	 */
	public double getParcelEval(String codeParcel) throws IOException {
		double result = 0.0;
		ShapefileDataStore sds = new ShapefileDataStore(parcelsFile.toURI().toURL());
		SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if (feat.getAttribute("CODE") != null) {
					if (feat.getAttribute("CODE").equals(codeParcel)) {
						result = Double.valueOf((String) feat.getAttribute("eval"));
						break;
					}
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
	public IFeatureCollection<IFeature> runSimulation(Environnement env, int i, Parameters p, BuildingType type, IFeatureCollection<Prescription> prescriptionUse)
			throws Exception {

		BasicPropertyUnit bPU = env.getBpU().get(i);

		// List ID Parcelle to Simulate is not empty
		if (!ID_PARCELLE_TO_SIMULATE.isEmpty()) {
			// We check if the code is in the list
			if (!ID_PARCELLE_TO_SIMULATE.contains(bPU.getCadastralParcels().get(0).getCode())) {
				return null;
			}
		}

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
			// #Art71 case 3
			case ART713:
				cc = graphConfigurationWithAlignements(alignementsGeometries, pred, env, i, bPU, alignementsGeometries.getSideWithBuilding());
				break;
			case ART6:
				cc = graphConfigurationWithAlignements(alignementsGeometries, pred, env, i, bPU, alignementsGeometries.getRoadGeom());
				break;
			case NONE:
				System.err.println(this.getClass().getName() + " : Normally not possible case");
				return null;
			default:
				System.err.println(this.getClass().getName() + " : Normally not possible case" + alignementsGeometries.getType());
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

		SDPCalc surfGen = new SDPCalc(p.getDouble("heightStorey"));
		// Getting cuboid into list (we have to redo it because the cuboids are
		// dissapearing during this procces)
		List<Cuboid> cubes = cc.getGraph().vertexSet().stream().map(x -> x.getValue()).collect(Collectors.toList());
		double surfacePlancherTotal = surfGen.process(cubes);

		if (RepartitionBuildingType.hasAttic(type)) {
			surfacePlancherTotal = surfGen.process(cubes, p.getInteger("nbStoreysAttic"), p.getDouble("ratioAttic"));
		}
		// cubes = cc.getGraph().vertexSet().stream().map(x ->
		// x.getValue()).collect(Collectors.toList());
		double surfaceAuSol = surfGen.processSurface(cubes);

		// get multiple zone regulation infos infos
		List<String> typeZones = new ArrayList<>();
		List<String> libelles = new ArrayList<>();
		String libellesFinal = "";
		String typeZonesFinal = "";

		// if multiple parts of a parcel has been simulated, put a long name containing
		// them all
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
			AttributeManager.addAttribute(feat, "BUILDTYPE", type, "String");
			iFeatC.add(feat);
		}
		return iFeatC;
	}

	private GraphConfiguration<Cuboid> graphConfigurationWithAlignements(Alignements alignementsGeometries,
			CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i, BasicPropertyUnit bPU, IGeometry[] geoms)
			throws Exception {

		GraphConfiguration<Cuboid> cc = null;

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
				// We keep the configuration with the best energy
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
		// We sort the subparcel to get the biggests
		sP.sort(new Comparator<SubParcel>() {
			@Override
			public int compare(SubParcel o1, SubParcel o2) {
				return Double.compare(o1.getArea(), o2.getArea());
			}
		});
		SubParcel sPBiggest = sP.get(sP.size() - 1);

		if (sPBiggest.getUrbaZone() == null) {
			System.out.println("Regulation is nulll for : " + bPU.getCadastralParcels().get(0).getCode());
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

}
