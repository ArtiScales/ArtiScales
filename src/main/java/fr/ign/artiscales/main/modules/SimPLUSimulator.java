package fr.ign.artiscales.main.modules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.MersenneTwister;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.artiscales.main.annexeTools.SDPCalcPolygonizer;
import fr.ign.artiscales.main.indicators.BuildingToHousingUnit;
import fr.ign.artiscales.main.rules.io.PrescriptionPreparator;
import fr.ign.artiscales.main.rules.io.ZoneRulesAssociation;
import fr.ign.artiscales.main.rules.predicate.CommonPredicateArtiScales;
import fr.ign.artiscales.main.rules.predicate.MultiplePredicateArtiScales;
import fr.ign.artiscales.main.rules.predicate.PredicateArtiScales;
import fr.ign.artiscales.main.rules.regulation.Alignements;
import fr.ign.artiscales.main.rules.regulation.ArtiScalesRegulation;
import fr.ign.artiscales.main.rules.regulation.buildingType.BuildingType;
import fr.ign.artiscales.main.rules.regulation.buildingType.MultipleRepartitionBuildingType;
import fr.ign.artiscales.main.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.artiscales.main.util.FromGeom;
import fr.ign.artiscales.main.util.SimuTool;
import fr.ign.artiscales.main.util.TransformXMLToJSON;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Shp;
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
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;

public class SimPLUSimulator {

	// parcels containing all of them and the code if we make a simulation on them
	// or not

	// one single parcel to study
	SimpleFeature singleFeat;

	// Parameters from technical parameters and scenario parameters files
	SimpluParametersJSON p;

	File parcelsFile, paramFile, folderOut, buildFile, roadFile, communitiesFile, simuFile, codeFile, zoningFile, predicateFile, filePrescPonct,
			filePrescLin, filePrescSurf;

	int compteurOutput = 0;

	// only for the one to one parcel type of simulation
	private File geoFile, regulationFile, tmpFile;

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
		// // Selected parcels shapefile
		// File selectedParcels = new File(p.getString("selectedParcelFile"));
		//
		// SimPLUSimulator simplu = new SimPLUSimulator(rootFolder, selectedParcels, p);
		//
		// simplu.run();
		// // SimPLUSimulator.fillSelectedParcels(new File(rootFolder), geoFile,
		// // pluFile, selectedParcels, 50, "25495", p);

		// //rattrapage des Cartes Communales
		// String[] scenars = { "DDense", "CPeuDense", "DPeuDense", "CDense" };
		// File rootFile = new File("/home/ubuntu/boulot/these/result2903/rattrapage");
		// for (String scenar : scenars) {
		// for (File f : new File(rootFile, "/cc/" + scenar).listFiles()) {
		// File paramFolder = new File(rootFile, "paramFolder");
		// System.out.println(paramFolder);
		// TransformXMLToJSON.convert(paramFolder);
		// List<File> lF = new ArrayList<>();
		// lF.add(new File(paramFolder, "paramSet/" + scenar + "/parameterTechnic.json"));
		// lF.add(new File(paramFolder, "paramSet/" + scenar + "/parameterScenario.json"));
		// SimpluParametersJSON p = new SimpluParametersJSON(lF);
		// // AttribNames.setATT_CODE_PARC("CODE");
		// // USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;
		// File fOut = new File(rootFile + "/cc/" + scenar + "/result/");
		// fOut.mkdirs();
		// System.out.println("start pack " + f);
		// SimPLUSimulator sim = new SimPLUSimulator(paramFolder, f, p, fOut);
		// sim.run();
		// System.out.println("done with pack " + f.getName());
		// }
		// }
		String scenar = "DPeuDense";
		String variant = "variantSizeCell16";
		File rootFile = new File("/home/ubuntu/boulot/these/result2903/");

		File paramFolder = new File(rootFile, "paramFolder");
		TransformXMLToJSON.convert(paramFolder);
		List<File> lF = new ArrayList<>();
		lF.add(new File(paramFolder, "paramSet/" + scenar + "/parameterTechnic.json"));
		lF.add(new File(paramFolder, "paramSet/" + scenar + "/parameterScenario.json"));
		SimpluParametersJSON p = new SimpluParametersJSON(lF);
		// AttribNames.setATT_CODE_PARC("CODE");
		// USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;
		// for (File variante : (new File(rootFile, "/cc/CDense/variantes/").listFiles())) {
		// File variante = new File(rootFile, "/cc/CDense/variantes/variantMvData1");
		// File out = new File(fOut, variante.getName());
		// fOut.mkdirs();
		File packagerFile = new File(rootFile + "/Packager/" + scenar + "/" + variant);
//		for (File superPack : packagerFile.listFiles()) {
		File superPack = new File(packagerFile,"2");	
		for (File pack : superPack.listFiles()) {
				File fOut = new File(rootFile + "/SimPLUDepot/" + scenar + "/" + variant + "/" + superPack + "/" + pack);
				System.out.println("start pack " + superPack);
				SimPLUSimulator sim = new SimPLUSimulator(paramFolder, pack, p, fOut);
				sim.run();
				System.out.println("done with pack " + pack.getName());
			}
			System.out.println("youpi");
			System.out.println("done with superpack " + superPack);
//		}
		// File f = new File("./" + nameMainFolder + "/ParcelSelectionDepot/DDense/variante0/");
		// File fOut = new File("." + nameMainFolder + "/ArtiScalesTest/SimPLUDepot/DDense/variante0/");
		// List<File> listBatiSimu = new ArrayList<File>();
		// for (File superPack : f.listFiles()) {
		// if (superPack.isDirectory()) {
		// for (File pack : superPack.listFiles()) {
		// if (pack.isDirectory()) {
		// System.out.println("start pack " + pack);
		// SimPLUSimulator sim = new SimPLUSimulator(paramFolder, pack, p, fOut);
		// List<File> simued = sim.run();
		// if (simued != null) {
		// listBatiSimu.addAll(simued);
		// }
		// System.out.println("done with pack " + pack.getName());
		// }
		// }
		// }
		// }
		// FromGeom.mergeBatis(listBatiSimu);

		// File rootFile = new File("/media/mcolomb/Data_2/root20190221/ParcelSelectionDepot/DDense/variante0/0/201");
		//
		// File paramFolder = new File("/media/mcolomb/Data_2/root20190221/paramFolder");
		//
		// List<File> lF = new ArrayList<>();
		//
		// lF.add(new File(paramFolder, "paramSet/DDense/parameterTechnic.json"));
		// lF.add(new File(paramFolder, "paramSet/DDense/parameterScenario.json"));
		//
		// SimpluParametersJSON p = new SimpluParametersJSON(lF);
		//
		// SimPLUSimulator sim = new SimPLUSimulator(paramFolder, rootFile, p, new File("media/mcolomb/Data_2/root20190221/out"));
		// sim.run();

	}

	/**
	 * Constructor to make a new object to run SimPLU3D simulations.
	 * 
	 * @param rootfile
	 *            : folder where profileBuildingType and locationBuildingType directories are
	 * @param packFile
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
	public SimPLUSimulator(File paramFile, File packFile, SimpluParametersJSON pa, File fileOut) throws Exception {

		// some static parameters needed
		this.p = pa;
		System.out.println("Param file = " + paramFile);
		System.out.println("Simu file = " + packFile);
		this.paramFile = paramFile;
		this.simuFile = packFile;
		this.folderOut = fileOut;
		folderOut.mkdirs();
		this.parcelsFile = new File(packFile, "parcelle.shp");
		File geoSnap = new File(packFile, "geoSnap");
		this.zoningFile = new File(geoSnap, "zone_urba.shp");
		this.buildFile = new File(geoSnap, "batiment.shp");
		this.roadFile = new File(geoSnap, "route.shp");
		this.communitiesFile = new File(geoSnap, "communities.shp");
		this.predicateFile = new File(packFile, "snapPredicate.csv");
		this.filePrescPonct = new File(geoSnap, "prescription_ponct.shp");
		this.filePrescLin = new File(geoSnap, "prescription_lin.shp");
		this.filePrescSurf = new File(geoSnap, "prescription_surf.shp");

		if (!this.zoningFile.exists()) {
			System.err.print("error : zoning files not found");
			System.out.println(geoSnap);
			System.out.println(geoSnap.exists());
			System.out.println(this.zoningFile);
		}
	}

	public SimPLUSimulator(File paramFile, File mainFile, File geoFile, File regulationFile, File tmpFile, File parcelfile, SimpluParametersJSON pa,
			File fileOut) throws Exception {

		// some static parameters needed
		this.p = pa;

		this.paramFile = paramFile;
		this.simuFile = mainFile;
		this.folderOut = fileOut;
		folderOut.mkdirs();
		this.parcelsFile = parcelfile;

		this.geoFile = geoFile;
		this.regulationFile = regulationFile;
		this.tmpFile = tmpFile;
		this.predicateFile = new File(regulationFile, "predicate.csv");

	}

	/**
	 * Run overload if the BuildingType has already been decided
	 * 
	 * @param type
	 *            : The chosen BuildingType
	 * @param par
	 *            : the scenario&technic parameters
	 * @return a list of simulated building shapefile
	 * @throws Exception
	 */
	public List<File> run(BuildingType type, SimpluParametersJSON par) throws Exception {
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin,
				filePrescSurf, null);

		///////////
		// asses repartition to pacels
		///////////
		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, par);

		boolean association = ZoneRulesAssociation.associate(env, predicateFile, zoningFile, willWeAssociateAnyway(par));

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

			CadastralParcel CadParc = env.getBpU().get(i).getCadastralParcels().get(0);
			String codeParcel = CadParc.getCode();

			// if this parcel contains no attributes, it means that it has been put here
			// just to express its boundaries
			if (codeParcel == null) {
				continue;
			}
			// if parcel has been marked as non simulable, return null
			if (!isParcelSimulable(codeParcel)) {
				CadParc.setHasToBeSimulated(false);
				System.out.println(codeParcel + " : je l'ai stopé net coz pas selec");
				continue;
			}
			System.out.println("Parcel code : " + codeParcel + "(pack " + simuFile.getName() + ")");

			IFeatureCollection<IFeature> building = null;
			SimpluParametersJSON pTemp = new SimpluParametersJSON((SimpluParametersJSON) par);

			pTemp.add(RepartitionBuildingType.getParamBuildingType(new File(paramFile, "profileBuildingType"), type));

			System.out.println("nombre de boites autorisées : " + pTemp.getString("nbCuboid"));

			building = runSimulation(env, i, pTemp, type, prescriptionUse);

			// if it's null, we skip to another parcel
			if (building == null) {
				continue bpu;
			}

			File output = new File(folderOut, "out-parcel_" + codeParcel + ".shp");
			System.out.println("Output in : " + output);
			ShapefileWriter.write(building, output.toString(), CRS.decode("EPSG:2154"));

			if (!output.exists()) {
				output = null;
			}

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

	public int run(int obj, File parcelFile) throws Exception {
		List<SimpleFeature> sortedList = new LinkedList<SimpleFeature>();
		ShapefileDataStore sds = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureIterator it = sds.getFeatureSource().getFeatures().features();
		Hashtable<SimpleFeature, Double> repart = new Hashtable<SimpleFeature, Double>();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				repart.put(feat, Double.valueOf((String) feat.getAttribute("eval")));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}

		List<Entry<SimpleFeature, Double>> entryList = new ArrayList<Entry<SimpleFeature, Double>>(repart.entrySet());
		Collections.sort(entryList, new Comparator<Entry<SimpleFeature, Double>>() {
			@Override
			public int compare(Entry<SimpleFeature, Double> obj1, Entry<SimpleFeature, Double> obj2) {
				return obj2.getValue().compareTo(obj1.getValue());
			}
		});

		for (Entry<SimpleFeature, Double> s : entryList) {
			sortedList.add(s.getKey());
		}
		DefaultFeatureCollection d = new DefaultFeatureCollection();
		for (SimpleFeature s : sortedList) {
			d.add(s);
		}
		// Vectors.exportSFC(d.collection(), new File("/tmp/lookout.shp"));

		while (obj > 0 && sortedList.size() > 0) {
			SimpleFeature toSimul = sortedList.remove(0);
			System.out.println("bella chiao " + toSimul.getAttribute("CODE") + " of eval " + toSimul.getAttribute("eval"));
			obj = obj - run(toSimul);
			System.out.println("obj are " + obj);
		}
		sds.dispose();
		return obj;
	}

	public int run(SimpleFeature parcel) throws Exception {
		DefaultFeatureCollection parcelColl = new DefaultFeatureCollection();
		parcelColl.add(parcel);
		File parcelTemp = Collec.exportSFC(parcelColl, new File(folderOut, "parcelTemp.shp"));
		// parcels aside not taken into account : thats untrue (but it's just for an example)
		File emprise = Geom.exportGeom(((Geometry) parcel.getDefaultGeometry()).buffer(30), new File(tmpFile, "emprise"));

		this.buildFile = Shp.snapDatas(new File(geoFile, "building.shp"), emprise, tmpFile);
		this.roadFile = Shp.snapDatas(new File(geoFile, "road.shp"), emprise, tmpFile);
		this.communitiesFile = Shp.snapDatas(new File(geoFile, "communities.shp"), emprise, tmpFile);
		this.parcelsFile = parcelTemp;

		this.zoningFile = Shp.snapDatas(new File(regulationFile, "zoning.shp"), emprise, tmpFile);
		this.filePrescPonct = Shp.snapDatas(new File(regulationFile, "prescPonct.shp"), emprise, tmpFile);
		this.filePrescLin = Shp.snapDatas(new File(regulationFile, "prescLin.shp"), emprise, tmpFile);
		this.filePrescSurf = Shp.snapDatas(new File(regulationFile, "prescSurf.shp"), emprise, tmpFile);
		Environnement env;
		try {
			env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelTemp, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);
		} catch (Exception e) {
			System.out.println("error catched : " + e);
			return 0;
		}
		List<File> batiSimuled = run(env);
		int nbBuilt = 0;
		if (batiSimuled != null && !batiSimuled.isEmpty()) {
			File bati = batiSimuled.get(0);
			ShapefileDataStore sds = new ShapefileDataStore(bati.toURI().toURL());
			SimpleFeatureCollection sfc = DataUtilities.collection(sds.getFeatureSource().getFeatures());
			sds.dispose();
			BuildingToHousingUnit bTH = new BuildingToHousingUnit(folderOut, paramFile, p);
			nbBuilt = bTH.simpleDistributionEstimate(sfc);
		}
		System.out.println("returned : " + nbBuilt);
		return nbBuilt;
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
		// SimuTool.setEnvEnglishName();

		if (!zoningFile.exists()) {
			System.out.println("Zoning File not found: " + zoningFile);
			System.out.println("&&&&&&&&&&&&&& Aucun bâtiment n'a été simulé &&&&&&&&&&&&&&");
			return null;
		}

		// FileWriter importantInfo = new FileWriter(new File(folderOut, "importantInfo"), true);
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin,
				filePrescSurf, null);
		return run(env);
	}
/**
 * simplifies the stop conditions for small simulations 
 * @param parIn
 * @param type
 * @return
 */
	public SimpluParametersJSON simplifyStopCondition(SimpluParametersJSON parIn, BuildingType type){
		
		switch (type) {
		case DETACHEDHOUSE:
			parIn.set("absolute_nb_iter", 1100000);
			parIn.set("relative_nb_iter", 42000);
			System.out.println("simplifyStopCondition for detached house");
			break;
		case SMALLHOUSE:
			parIn.set("absolute_nb_iter", 800000);
			parIn.set("relative_nb_iter", 32000);
			System.out.println("simplifyStopCondition for small house");
			break;
		case SMALLBLOCKFLAT:
			parIn.set("absolute_nb_iter", 2250000);
			parIn.set("relative_nb_iter", 66666);
			System.out.println("simplifyStopCondition for smallBlock");
		default:
			break;
		}
		return parIn;
	}
	
	/**
	 * method employed for every simPLU Simu
	 * 
	 * @param env
	 * @return
	 * @throws Exception
	 */
	public List<File> run(Environnement env) throws Exception {
		SimpluParametersJSON pUsed = new SimpluParametersJSON(p);
		FileWriter importantInfo = new FileWriter(new File(folderOut, "importantInfo"), true);
		///////////
		// asses repartition to pacels
		///////////
		// know if there's only one or multiple zones in the parcel pack
		List<String> sectors = new ArrayList<String>();
		IFeatureCollection<CadastralParcel> parcels = env.getCadastralParcels();
		for (CadastralParcel parcel : parcels) {
			String tmp = FromGeom.affectZoneAndTypoToLocation(pUsed.getString("useRepartition"), pUsed.getString("scenarioPMSP3D"), parcel,
					zoningFile, communitiesFile, true);
			if (tmp == null) {
				break;
			}
			if (!sectors.contains(tmp)) {
				sectors.add(tmp);
			}
		}

		// of no sectors, we use the default file
		if (sectors.isEmpty()) {
			sectors.add("default.json");
		}

		// loading the type of housing to build

		RepartitionBuildingType housingUnit = new RepartitionBuildingType(pUsed, paramFile, zoningFile, communitiesFile, parcelsFile);
		if (sectors.size() > 1) {
			System.out.println("multiple zones in the same parcel lot : there's gon be approximations");
			System.out.println("zones are ");
			for (String s : sectors) {
				System.out.println(s);
			}
			System.out.println();
			housingUnit = new MultipleRepartitionBuildingType(pUsed, paramFile, zoningFile, communitiesFile, parcelsFile);
		} else {
			System.out.println("it's all normal : one sector");
		}

		// Prescription setting
		IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
		IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, pUsed);

		boolean association = ZoneRulesAssociation.associate(env, predicateFile, zoningFile, willWeAssociateAnyway(pUsed));

		if (!association) {
			System.out.println("Association between rules and UrbanZone failed");
			importantInfo.close();
			return null;
		}

		List<File> listBatiSimu = new ArrayList<File>();
		//////////////
		// We run a simulation for each bPU with a different file for each bPU
		//////////////
		int nbBPU = env.getBpU().size();
		bpu: for (int i = 0; i < nbBPU; i++) {
			pUsed = new SimpluParametersJSON(p);
			CadastralParcel cadParc = env.getBpU().get(i).getCadastralParcels().get(0);
			String codeParcel = cadParc.getCode();
			File output = new File(folderOut, "out-parcel_" + codeParcel + ".shp");
			if (output.exists()) {
				System.out.println(codeParcel + "already exist");
				continue;
			}
			importantInfo.append(codeParcel + "\n");
			// if this parcel contains no attributes, it means that it has been put here
			// just to express its boundaries
			if (codeParcel == null) {
				continue;
			}
			// if parcel has been marked as non simulable, return null
			if (!isParcelSimulable(codeParcel)) {
				cadParc.setHasToBeSimulated(false);
				System.out.println(codeParcel + " not selected : simulation stoped");
				importantInfo.append(codeParcel + "pas sélectionnée \n \n");
				continue;
			}
			System.out.println("Parcel code : " + codeParcel + "(pack " + simuFile.getName() + ")");

			double eval = getParcelEval(codeParcel);

			// of which type should be the housing unit
			BuildingType type;
			if (housingUnit instanceof MultipleRepartitionBuildingType) {
				type = ((MultipleRepartitionBuildingType) housingUnit).rangeInterest(eval, codeParcel, pUsed);
			} else {
				type = housingUnit.rangeInterest(eval);
			}

			// we get ready to change it
			boolean seekType = true;
			boolean adjustDown = false;

			BuildingType[] fromTo = new BuildingType[2];
			fromTo[0] = type;
			IFeatureCollection<IFeature> building = null;
			// until we found the right type
			while (seekType) {
				System.out.println("we try to put a " + type + " housing unit");
				importantInfo.append("simulation d'un " + type + " \n");
				// we add the parameters for the building type want to simulate
				SimpluParametersJSON pWithBuildingType = new SimpluParametersJSON(pUsed);
				pWithBuildingType.add(RepartitionBuildingType.getParamBuildingType(new File(paramFile, "profileBuildingType"), type));
				pWithBuildingType = simplifyStopCondition(pWithBuildingType,type);
				
				building = runSimulation(env, i, pWithBuildingType, type, prescriptionUse, importantInfo);
				
				// if it's null, we skip to another parcel
				if (building == null) {
					continue bpu;
				}
				// if the size of floor is inferior to the minimum we set, we downsize to see if a smaller type fits
				if ((double) building.get(0).getAttribute("SDPShon") < pWithBuildingType.getDouble("areaMin")) {
					System.out.println("SDP is too small ( " + (double) building.get(0).getAttribute("SDPShon") + " for a min of "
							+ pWithBuildingType.getDouble("areaMin") + ")");
					importantInfo.append("SDP is too small ( " + (double) building.get(0).getAttribute("SDPShon") + " for a min of "
							+ pWithBuildingType.getDouble("areaMin") + ") \n");
					// File output = new File(folderOut, "temp-parcel_" + codeParcel + "-" + type + ".shp");
					// System.out.println("Output in : " + output);
					// ShapefileWriter.write(building, output.toString(), CRS.decode("EPSG:2154"));

					adjustDown = true;
					BuildingType typeTemp = housingUnit.down(type);
					// if it's not the same type, we'll continue to seek
					if (!(typeTemp == type)) {
						type = typeTemp;
						System.out.println("we'll try a " + type + "instead");
					}
					// if it's blocked, we'll go for this type
					else {
						System.out.println("anyway, we'll go for this " + type + " type");
						importantInfo.append("anyway, we'll go for this " + type + " type \n");
						seekType = false;
					}
				} else {
					seekType = false;
					if (adjustDown) {
						fromTo[1] = type;
						housingUnit.adjustDistributionDown(eval, fromTo[1], fromTo[0]);
					} else {
						System.out.println("first hit, we set the " + type + " building type");
						break;
					}
				}
			}

			System.out.println("Output in : " + output);
			ShapefileWriter.write(building, output.toString(), CRS.decode("EPSG:2154"));

			if (!output.exists()) {
				output = null;
			}

			if (output != null) {
				listBatiSimu.add(output);
			}
		}

		// No results
		if (listBatiSimu.isEmpty()) {
			System.out.println("&&&&&&&&&&&&&& Aucun bâtiment n'a été simulé &&&&&&&&&&&&&&");
			return null;
		}
		importantInfo.close();
		return listBatiSimu;
	}

	/**
	 * small method to know if we need to perform the simulation on zones that are not open to the urbanization.
	 * 
	 * @param p2
	 *            : paramterer file, containing the answer to our question
	 * @return boolean : true if we do
	 */
	private HashMap<String, Boolean> willWeAssociateAnyway(SimpluParametersJSON p2) {
		HashMap<String, Boolean> result = new HashMap<String, Boolean>();
		if (p2.getBoolean("2AU")) {
			result.put("2AU", true);
		} else {
			result.put("2AU", false);
		}

		if (p2.getBoolean("NC")) {
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
	 * for a given parcel, get its interest to be urbanized
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

	public IFeatureCollection<IFeature> runSimulation(Environnement env, int i, SimpluParametersJSON par, BuildingType type,
			IFeatureCollection<Prescription> prescriptionUse) throws Exception {
		FileWriter fw = new FileWriter(new File(folderOut, "important"), true);
		IFeatureCollection<IFeature> result = runSimulation(env, i, par, type, prescriptionUse, fw);
		fw.close();
		return result;
	}

	/**
	 * Simulation for the ie bPU
	 * 
	 * @param env
	 * @param i
	 * @param par
	 * @param prescriptionUse
	 *            the prescriptions in Use prepared with PrescriptionPreparator
	 * @return if null, we pass to another parcel. if an empty collection, we downsize the type
	 * 
	 * @throws Exception
	 */
	public IFeatureCollection<IFeature> runSimulation(Environnement env, int i, SimpluParametersJSON par, BuildingType type,
			IFeatureCollection<Prescription> prescriptionUse, FileWriter writer) throws Exception {

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
			pred = preparePredicateOneRegulation(bPU, par, prescriptionUse, env);

		} else {
			pred = preparePredicateOneRegulationBySubParcel(bPU, par, prescriptionUse, env);
		}

		if (pred == null) {
			System.out.println("Predicate cannot been instanciated");
			return null;
		}

		if (!pred.isCanBeSimulated()) {
			System.out.println("Parcel is not simulable according to the predicate");
			writer.append("no simu possible for many reasons \n");
			return null;
		}
		// if (!pred.isOutsized()) {
		// System.out.println("Building type is too big");
		// return new FT_FeatureCollection<IFeature>();
		// }

		// We compute the parcel area
		Double areaParcels = bPU.getArea(); // .getCadastralParcels().stream().mapToDouble(x -> x.getArea()).sum();

		GraphConfiguration<Cuboid> cc = null;

		Alignements alignementsGeometries = pred.getAlignements();

		if (alignementsGeometries.getHasAlignement()) {

			switch (alignementsGeometries.getType()) {
			// #Art71 case 1 or 2
			case ART7112:
				// TODO fix that defaite
				try {
					cc = article71Case12(alignementsGeometries, pred, env, i, bPU, par);
					writer.append("ART7112 used \n");
				} catch (Exception e) {
					writer.write("ART7112 not used \n");
					System.out.println("cuboid from ART7112 failed");
					writer.append("ART7112 not used \n");
					OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
					cc = oCB.process(bPU, par, env, i, pred);
				}
				break;
			// #Art71 case 3
			case ART713:
				cc = graphConfigurationWithAlignements(alignementsGeometries, pred, env, i, bPU, alignementsGeometries.getSideWithBuilding(), par);
				break;
			case ART6:
				cc = graphConfigurationWithAlignements(alignementsGeometries, pred, env, i, bPU, alignementsGeometries.getRoadGeom(), par);
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
			cc = oCB.process(bPU, par, env, i, pred);
			if (cc == null) {
				return null;
			}
		}
		System.out.println(pred.getDenial());
		writer.append("denial reasons : " + pred.getDenial() + " \n \n");
		// writer.write("stoped because of : "+ "" + " \n \n ");
		// the -0.1 is set to avoid uncounting storeys when its very close to make one storey (which is very frequent)
		double surfacePlancherTotal = 0.0;
		double surfaceAuSol = 0.0;
		SDPCalcPolygonizer surfGen = new SDPCalcPolygonizer(par.getDouble("heightStorey") - 0.1);
		if (RepartitionBuildingType.hasAttic(type)) {
			surfGen = new SDPCalcPolygonizer(par.getDouble("heightStorey") - 0.1, par.getInteger("nbStoreysAttic"), par.getDouble("ratioAttic"));
		}

		List<Cuboid> cubes = cc.getGraph().vertexSet().stream().map(x -> x.getValue()).collect(Collectors.toList());
		surfacePlancherTotal = surfGen.process(cubes) * 0.8;
		if (RepartitionBuildingType.hasCommonParts(type)) {
			surfacePlancherTotal = surfacePlancherTotal * 0.9;
		}
		surfaceAuSol = surfGen.processSurface(cubes);

		// Getting cuboid into list (we have to redo it because the cuboids are
		// dissapearing during this procces)

		// get multiple zone regulation infos infos
		String libelles = SimuTool.getLibInfo(bPU, "LIBELLE");
		String typeZones = SimuTool.getLibInfo(bPU, "TYPEZONE");

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
			AttributeManager.addAttribute(feat, "SDPShon", surfacePlancherTotal, "Double");
			AttributeManager.addAttribute(feat, "SurfacePar", areaParcels, "Double");
			AttributeManager.addAttribute(feat, "SurfaceSol", surfaceAuSol, "Double");
			AttributeManager.addAttribute(feat, "CODE", bPU.getCadastralParcels().get(0).getCode(), "String");
			AttributeManager.addAttribute(feat, "LIBELLE", libelles, "String");
			AttributeManager.addAttribute(feat, "TYPEZONE", typeZones, "String");
			AttributeManager.addAttribute(feat, "BUILDTYPE", type, "String");
			iFeatC.add(feat);
		}

		if (iFeatC.isEmpty()) {
			return null;
		}
		return iFeatC;
	}

	private GraphConfiguration<Cuboid> graphConfigurationWithAlignements(Alignements alignementsGeometries,
			CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i,
			BasicPropertyUnit bPU, IGeometry[] geoms, SimpluParametersJSON par) throws Exception {

		GraphConfiguration<Cuboid> cc = null;

		if (geoms.length == 0) {
			OptimisedBuildingsCuboidFinalDirectRejection oCB = new OptimisedBuildingsCuboidFinalDirectRejection();
			cc = oCB.process(bPU, par, env, i, pred);
		} else {

			// Instantiation of the sampler
			ParallelCuboidOptimizer oCB = new ParallelCuboidOptimizer();

			IMultiSurface<IOrientableSurface> iMSSamplinSurface = new GM_MultiSurface<>();

			for (IGeometry geom : geoms) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(par.getDouble("maxwidth") / 2)));
			}
			// Run of the optimisation on a parcel with the predicate
			cc = oCB.process(new MersenneTwister(), bPU, par, env, i, pred, geoms, iMSSamplinSurface);
		}

		return cc;
	}

	private GraphConfiguration<Cuboid> article71Case12(Alignements alignementsGeometries,
			CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i,
			BasicPropertyUnit bPU, SimpluParametersJSON par) throws Exception {

		GraphConfiguration<Cuboid> cc = null;
		// Instantiation of the sampler
		ParallelCuboidOptimizer oCB = new ParallelCuboidOptimizer();

		IMultiSurface<IOrientableSurface> iMSSamplinSurface = new GM_MultiSurface<>();
		// art-0071 implentation (begin)
		// LEFT SIDE IS TESTED
		IGeometry[] leftAlignement = alignementsGeometries.getLeftSide();
		System.out.println("lenght of left side : " + leftAlignement[0]);

		if (leftAlignement != null && (leftAlignement.length > 0)) {
			for (IGeometry geom : leftAlignement) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(par.getDouble("maxwidth") / 2)));
			}

			pred.setSide(ParcelBoundarySide.LEFT);

			// Run of the optimisation on a parcel with the predicate
			cc = oCB.process(new MersenneTwister(), bPU, par, env, i, pred, leftAlignement, iMSSamplinSurface);
		}

		// RIGHT SIDE IS TESTED

		IGeometry[] rightAlignement = alignementsGeometries.getRightSide();
		GraphConfiguration<Cuboid> cc2 = null;
		System.out.println("lenght of right side : " + leftAlignement[0]);
		if (rightAlignement != null && (rightAlignement.length > 0)) {

			iMSSamplinSurface = new GM_MultiSurface<>();
			oCB = new ParallelCuboidOptimizer();
			for (IGeometry geom : rightAlignement) {
				iMSSamplinSurface.addAll(FromGeomToSurface.convertGeom(geom.buffer(par.getDouble("maxwidth") / 2)));
			}

			pred.setSide(ParcelBoundarySide.RIGHT);

			cc2 = oCB.process(new MersenneTwister(), bPU, par, env, i, pred, rightAlignement, iMSSamplinSurface);
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

	private CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulationBySubParcel(
			BasicPropertyUnit bPU, SimpluParametersJSON p2, IFeatureCollection<Prescription> prescriptionUse, Environnement env) throws Exception {

		MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>(
				bPU, true, p2, prescriptionUse, env);

		return pred;
	}

	private static PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulation(
			BasicPropertyUnit bPU, SimpluParametersJSON p, IFeatureCollection<Prescription> prescriptionUse, Environnement env) throws Exception {
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

		System.out.println("Regulation code : " + regle.getInsee() + "-" + regle.getLibelle_de_dul());
		System.out.println("ArtiScalesRegulation : " + regle);
		// Instantiation of the rule checker
		PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicateArtiScales<>(bPU, true, regle, p,
				prescriptionUse, env);

		return pred;
	}
}
