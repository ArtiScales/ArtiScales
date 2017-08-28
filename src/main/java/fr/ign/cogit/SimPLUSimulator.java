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
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.Indicators.BuildingToHousehold;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.iauidf.regulation.Regulation;
import fr.ign.cogit.simplu3d.io.nonStructDatabase.shp.LoaderSHP;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
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
	IFeatureCollection<IFeature> iFeatGenC = new FT_FeatureCollection<>();

	File filePrescPonct;
	File filePrescLin;
	File filePrescSurf;
	File rootFile;
	int compteur;

	public SimPLUSimulator(File rootfile, SimpleFeature feat, String zipcode) throws Exception{
		this(rootfile,new File("") , zipcode);
		DefaultFeatureCollection tempShp = new DefaultFeatureCollection();
		tempShp.add(feat);
		parcelFile=new File(rootfile,"tmp.shp");
		SelectParcels.exportSFC(tempShp,new File(rootfile,"tmp.shp"));
	}
	
	public SimPLUSimulator(File rootfile, File parcelfile, String zipcode) throws Exception {
		rootFile = rootfile;
		zipCode = zipcode;

		zoningFile = selecZoningFile();

		parcelFile = parcelfile;
		buildFile = snapDatas(new File(rootFile, "donneeGeographiques/batiment.shp"),
				new File(parcelFile, "/snap/batiment.shp"));
		roadFile = snapDatas(new File(rootFile, "donneeGeographiques/route.shp"),
				new File(parcelFile, "/snap/route.shp"));
		// doc urba :: à quoi ça sert?
		codeFile = new File(rootFile, "pluZoning/codes/DOC_URBA.shp");
		filePrescPonct = new File(rootFile, "pluZoning/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(rootFile, "pluZoning/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(rootFile, "pluZoning/codes/PRESCRIPTION_SURF.shp");

		// Compteur pour savoir combien de simu différentes ont été faires par
		// simPLU
		compteur = 0;

	}

	public static void main(String[] args) throws Exception {
		run(new File("/home/mcolomb/donnee/couplage"),
				new File("/home/mcolomb/donnee/couplage/output/N6_St_Moy_ahpx_seed42-eval_anal-20.0/25245/notBuilt-Split"),
				"25245");
	}

	public static List<File> run(File rootFile, File parcelfiles, String zipcode) throws Exception {
		SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelfiles, zipcode);
		return SPLUS.run();
	}

	public int runOneSim(File f) throws Exception {
		compteur = Integer.parseInt(f.getParentFile().getName().replace("simu",""));
		Parameters p = Parameters.unmarshall(new File(paramFile,"param"+compteur+".xml"));
		
		Environnement env = LoaderSHP.load(f, codeFile, zoningFile,
				parcelFile, roadFile, buildFile, filePrescPonct, filePrescLin,
				filePrescSurf, null);
		int i=env.getBpU().size();
		System.out.println("i?" + i);
		File yoy = runSimulation(env, i, p);
		BuildingToHousehold greenBuilding = new BuildingToHousehold (yoy, 100);
		return greenBuilding.simpleEstimate(yoy);
	}

	public List<File> run() throws Exception {

		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration
		List<File> listBatiSimu = new ArrayList<File>();
		for (File pFile : paramFile.listFiles()) {
			if (pFile.toString().endsWith(".xml")){
			System.out.println(pFile);
			Parameters p = Parameters.unmarshall(pFile);
			// Load default environment
			simuFile = new File(parcelFile, "simu" + compteur);
			System.out.println(parcelFile);
			Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile,
					new File(parcelFile, "parcelSelected.shp"), roadFile, buildFile, filePrescPonct, filePrescLin,
					filePrescSurf, null);
			System.out.println("total de parcelles : " + env.getBpU().size());
			for (int i = 0; i < env.getBpU().size(); i++) {
				runSimulation(env, i, p);
			}
			buildingInOneShp();
			listBatiSimu.add(simuFile);
			compteur = compteur + 1;
		}
		}
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
				System.out.println("got it " + typez);
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

		System.out.println("for ex : " + regle.getArt_6());
		double distReculVoirie = regle.getArt_6();
		double distReculFond = regle.getArt_73(); // ,regle.getArt_74())
													// devrait prendre le
													// minimum de la valeur
													// fixe et du rapport à
													// la hauteur du
													// batiment à coté ::à
													// développer yo
		double distReculLat = regle.getArt_72();
		double distanceInterBati = regle.getArt_8();
		if (regle.getArt_8() == 99) {
			distanceInterBati = 0;
		}

		double maximalCES = regle.getArt_9();
		if (regle.getArt_8() == 99) {
			maximalCES = 0;
		}

		// définition de la hauteur. Si elle est exprimé en nombre d'étage,
		// on comptera
		// 3m pour le premier étage et 2.5m pour les étages supérieurs. Je
		// ne sais pas comment
		// on utilise ce paramètre car il n'est pas en argument dans le
		// predicate.
		// TODO utiliser cette hauteur
		double maximalhauteur = regle.getArt_10_m();

		// Instantiation of the rule checker
		SamplePredicate<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new SamplePredicate<>(
				bPU, distReculVoirie, distReculFond, distReculLat, distanceInterBati, maximalCES);
		// PredicateDensification<Cuboid, GraphConfiguration<Cuboid>,
		// BirthDeathModification<Cuboid>> pred = new PredicateIAUIDF();
		Double areaParcels = 0.0;
		for (CadastralParcel yo : bPU.getCadastralParcels()) {
			areaParcels = areaParcels + yo.getArea();
		}

		// Run of the optimisation on a parcel with the predicate
		GraphConfiguration<Cuboid> cc = oCB.process(bPU, p, env, 1, pred);

		// Witting the output
		IFeatureCollection<IFeature> iFeatC = new FT_FeatureCollection<>();
		// For all generated boxes
		for (GraphVertex<Cuboid> v : cc.getGraph().vertexSet()) {

			// Output feature with generated geometry
			IFeature feat = new DefaultFeature(v.getValue().generated3DGeom());

			// We write some attributes
			AttributeManager.addAttribute(feat, "Longueur", Math.max(v.getValue().length, v.getValue().width),
					"Double");
			AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width), "Double");
			AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
			AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
			AttributeManager.addAttribute(feat, "Surface", v.getValue().getArea(), "Double");
			AttributeManager.addAttribute(feat, "areaParcel", areaParcels, "Double");
			iFeatC.add(feat);
			iFeatGenC.add(feat);
		}

		// A shapefile is written as output
		// WARNING : 'out' parameter from configuration file have to be
		// change

		File output = new File(simuFile, "out-parcelle_" + i + ".shp");

		System.out.println("output" + output);
		output.getParentFile().mkdirs();
		ShapefileWriter.write(iFeatC, output.toString());
		System.out.println("That's all folks");

		return output;
	}

	public void buildingInOneShp() {
		File shpGen = (new File(simuFile,"TotBatiSimu/TotBatiSimu.shp"));
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

	public File snapDatas(File fileIn, File fileOut) throws IOException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {

		// load the input from the general folder
		ShapefileDataStore shpDSIn = new ShapefileDataStore(fileIn.toURI().toURL());
		SimpleFeatureCollection inCollection = shpDSIn.getFeatureSource().getFeatures();

		// load the zoning file of the studied town
		ShapefileDataStore shpDSZone = new ShapefileDataStore(selecZoningFile().toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();

		Geometry bBox = SelectParcels.unionSFC(zoneCollection);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

		String geometryInPropertyName = shpDSIn.getSchema().getGeometryDescriptor().getLocalName();

		Filter filterIn = ff.intersects(ff.property(geometryInPropertyName), ff.literal(bBox));

		SimpleFeatureCollection inTown = inCollection.subCollection(filterIn);
		System.out.println(fileOut);
		fileOut.getParentFile().mkdirs();
		SelectParcels.exportSFC(inTown, fileOut);

		return fileOut;

	}

}
