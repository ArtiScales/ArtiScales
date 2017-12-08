package fr.ign.cogit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.Indicators.BuildingToHousehold;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.PLUCities.PredicatePLUCities;
import fr.ign.cogit.simplu3d.experiments.iauidf.predicate.PredicateIAUIDF;
import fr.ign.cogit.simplu3d.experiments.iauidf.regulation.Regulation;
import fr.ign.cogit.simplu3d.experiments.mupcity.util.PredicateDensification;
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
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;
import fr.ign.parameters.Parameters;

public class SimPLUSimulator {

	String zipCode;
	// String scenarName;
	File parcelFile;
	SimpleFeature feature;
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

	Parameters p = null;

	public SimPLUSimulator(File rootfile, File simufile, SimpleFeature feat, String zipcode, File snapfile, Parameters pa) throws Exception {
		this(rootfile, simufile.getParentFile(), zipcode, pa);
		DefaultFeatureCollection tempShp = new DefaultFeatureCollection();
		tempShp.add(feat);
		simuFile = simufile;
		parcelFile = new File(rootfile, "tmp.shp");
		SelectParcels.exportSFC(tempShp, parcelFile);
	}

	public SimPLUSimulator(File rootfile, File parcelfile, String zipcode) throws Exception {
		this(rootfile, parcelfile, zipcode, null);
	}

	public SimPLUSimulator(File rootfile, File parcelfile, String zipcode, Parameters pa) throws Exception {

		p = pa;
		rootFile = rootfile;
		zipCode = zipcode;
		zoningFile = selecZoningFile();

		parcelFile = parcelfile;

		// if the parameter file is not in a given file
		if (parcelFile.exists()) {
			if (p == null) {
				for (File pFile : paramFile.listFiles()) {
					if (pFile.toString().endsWith(".xml")) {
						System.out.println("found paramter file : " + pFile);
						p = Parameters.unmarshall(pFile);
						int numSimu = Integer.parseInt(pFile.getName().substring(5, 6));
						simuFile = new File(parcelFile, "simu" + numSimu);
					}
				}
			} else {
				simuFile = new File(parcelFile, "simu0");
			}
		}
		if (!(new File(simuFile.getParentFile(), "/snap/route.shp")).exists()) {
			buildFile = snapDatas(new File(rootFile, "donneeGeographiques/batiment.shp"), selecZoningFile(), new File(simuFile.getParentFile(), "/snap/batiment.shp"));
			roadFile = snapDatas(new File(rootFile, "donneeGeographiques/route.shp"), selecZoningFile(), new File(simuFile.getParentFile(), "/snap/route.shp"));
		} else {
			buildFile = new File(simuFile.getParentFile(), "/snap/batiment.shp");
			roadFile = new File(simuFile.getParentFile(), "/snap/route.shp");
		}
		// doc urba :: à quoi ça sert?
		codeFile = new File(rootFile, "pluZoning/codes/DOC_URBA.shp");
		filePrescPonct = new File(rootFile, "pluZoning/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(rootFile, "pluZoning/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(rootFile, "pluZoning/codes/PRESCRIPTION_SURF.shp");

	}

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"), new File("/home/mcolomb/workspace/PLUCities/donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25495/built-Split"),
				"25495");
	}

	public static List<File> run(File rootFile, File parcelfiles, String zipcode) throws Exception {
		SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelfiles, zipcode, null);
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
		if (p == null) {
			int numSimu = Integer.parseInt(parcelFile.getName().replace("simu", ""));
			p = Parameters.unmarshall(new File(paramFile, "param" + numSimu + ".xml"));
		}
		System.out.println("roadFile    " + roadFile);
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);

		File yoy = runSimulation(env, 0, p);
		BuildingToHousehold building = new BuildingToHousehold(yoy, p.getInteger("HousingUnitSize"));
		return building.simpleEstimate(yoy);
	}

	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		List<File> listBatiSimu = new ArrayList<File>();

		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, new File(parcelFile, "parcelSelected.shp"), roadFile, buildFile, filePrescPonct, filePrescLin,
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
//		PredicatePLUCities<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicatePLUCities<>(bPU, distReculVoirie, distReculFond, distReculLat,
//				distanceInterBati, maximalCES, maximalhauteur);
		PredicateIAUIDF<Cuboid, GraphConfiguration<Cuboid>,
		 BirthDeathModification<Cuboid>> pred = new PredicateIAUIDF(bPU,regle,regle);
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

	public File selecZoningFile() throws FileNotFoundException {
		File zoningsFile = new File(rootFile, "pluZoning");
		for (File f : zoningsFile.listFiles()) {
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				return f;
			}
		}
		throw new FileNotFoundException("Zoning file not found");
	}

	public File putNewBatiInBati() throws IOException {
		// TODO when a new building is created with the fill method, add it to
		// the building file
		ShapefileDataStore builsSnapSDS = new ShapefileDataStore(buildFile.toURI().toURL());
		SimpleFeatureCollection newBuild = builsSnapSDS.getFeatureSource().getFeatures();
		return buildFile;

	}

	public static File snapDatas(File fileIn, File bBoxFile, File fileOut) throws Exception {

		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection inCollection = shpDSIn.getFeatureSource().getFeatures();

		// load the file to make the bbox and selectin with
		ShapefileDataStore shpDSZone = new ShapefileDataStore(bBoxFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();
		Geometry bBox = SelectParcels.unionSFC(zoneCollection);

		return SelectParcels.exportSFC(snapDatas(inCollection, bBox), fileOut);
	}

	public static SimpleFeatureCollection snapDatas(SimpleFeatureCollection SFCIn, Geometry bBox) throws Exception {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryInPropertyName = SFCIn.getSchema().getGeometryDescriptor().getLocalName();
		Filter filterIn = ff.intersects(ff.property(geometryInPropertyName), ff.literal(bBox));
		SimpleFeatureCollection inTown = SFCIn.subCollection(filterIn);

		return inTown;

	}

}
