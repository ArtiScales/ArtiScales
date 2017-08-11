package fr.ign.cogit;

import java.io.File;
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
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.math.plot.utils.Array;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.attribute.AttributeManager;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.experiments.iauidf.regulation.Regulation;
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

	String zipCode;
	// String scenarName;
	File parcelFile;
	File buildFile;
	File roadFile;
	File paramFile = new File("donnee/couplage/pluZoning/codes/param.xml");
	File codeFile;
	File zoningFile;
	File filePrescPonct;
	File filePrescLin;
	File filePrescSurf;
	File rootFile;
	int compteur;

	public SimPLUSimulator(File rootfile, File parcelfiles, String zipcode) throws Exception {
		rootFile = rootfile;
		zipCode = zipcode;

		File zoningsFile = new File(rootFile, "pluZoning/reproj");
		for (File f : zoningsFile.listFiles()) {
			Pattern insee = Pattern.compile("INSEE_");
			String[] list = insee.split(f.toString());
			if (list.length > 1 && list[1].equals(zipCode + ".shp")) {
				System.out.println("hehre");
				zoningFile=f;
			}
		}
		System.out.println(zoningFile);
		// scenarName = scenarname;
		if (!new File(rootFile, "donneeGeographiques/snap/" + zipCode).exists()) {
			snapDatas();
		}

		parcelFile = parcelfiles;
		buildFile = new File(rootFile, "donneeGeographiques/snap/" + zipCode + "/batiment.shp");
		roadFile = new File(rootFile, "donneeGeographiques/snap/" + zipCode + "/route.shp");
		// doc urba :: à quoi ça sert?
		codeFile = new File(rootFile, "pluZoning/codes/DOC_URBA.shp");
		filePrescPonct = new File(rootFile, "pluZoning/codes/PRESCRIPTION_PONCT.shp");
		filePrescLin = new File(rootFile, "pluZoning/codes/PRESCRIPTION_LIN.shp");
		filePrescSurf = new File(rootFile, "pluZoning/codes/PRESCRIPTION_SURF.shp");
		
		//Compteur pour savoir combien de simu différentes ont été faires par simPLU
		compteur = 0;

	}

	public static List<File> run(File rootFile, File parcelfiles, String zipcode) throws Exception {
		SimPLUSimulator SPLUS = new SimPLUSimulator(rootFile, parcelfiles, zipcode);
		return SPLUS.run();
	}

	public List<File> run() throws Exception {
		List<File> listBatiSimu = new ArrayList<File>();
		// Loading of configuration file that contains sampling space
		// information and simulated annealing configuration

		Parameters p = Parameters.unmarshall(paramFile);
		int nInsee = Integer.valueOf(zipCode);

		// Load default environment
		File simuFile = new File(parcelFile, "simu");
		Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelFile, roadFile, buildFile,
				filePrescPonct, filePrescLin, filePrescSurf, null);
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
			Map<Integer, List<Regulation>> regles = Regulation.loadRegulationSet(codeFile.getParent() + "/predicate.csv");
			for (UrbaZone zone : env.getUrbaZones()) {
				if (zone.getGeom().contains(bPU.getGeom())) {
					typez = zone.getLibelle();
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

			System.out.println("nbiter = "+p.get("nbiter"));
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
				AttributeManager.addAttribute(feat, "Largeur", Math.min(v.getValue().length, v.getValue().width),
						"Double");
				AttributeManager.addAttribute(feat, "Hauteur", v.getValue().height, "Double");
				AttributeManager.addAttribute(feat, "Rotation", v.getValue().orientation, "Double");
				AttributeManager.addAttribute(feat, "Surface", v.getValue().getArea(), "Double");
				iFeatC.add(feat);
			}

			// A shapefile is written as output
			// WARNING : 'out' parameter from configuration file have to be
			// change

			File output = new File (parcelFile.getParent(), "/simu"+ compteur+"/out-parcelle_" + i + ".shp");
			listBatiSimu.add(output);
			
			System.out.println("output"+output);
			output.mkdirs();
			
			ShapefileWriter.write(iFeatC,output.toString());

			System.out.println("That's all folks");
		}
		return listBatiSimu;
	}

	public File snapDatas() throws IOException, NoSuchAuthorityCodeException, FactoryException,
			MismatchedDimensionException, TransformException {

		// load the roads from the general folder
		ShapefileDataStore shpDSRoad = new ShapefileDataStore(
				new File(rootFile, "donneeGeographiques/route.shp").toURI().toURL());
		SimpleFeatureCollection roadCollection = shpDSRoad.getFeatureSource().getFeatures();

		// load the buildings from the general folder
		ShapefileDataStore shpDSBuild = new ShapefileDataStore(
				new File(rootFile, "donneeGeographiques/batiment.shp").toURI().toURL());
		SimpleFeatureCollection buildCollection = shpDSBuild.getFeatureSource().getFeatures();

		// load the zoning file of the studied town
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection zoneCollection = shpDSZone.getFeatureSource().getFeatures();

		CoordinateReferenceSystem sourceZoneCRS = CRS.decode("epsg:3947");
		CoordinateReferenceSystem targetZoneCRS = shpDSRoad.getSchema().getCoordinateReferenceSystem();

		MathTransform transform = CRS.findMathTransform(sourceZoneCRS, targetZoneCRS);
		Geometry bBox = JTS.transform(SelectParcels.unionSFC(zoneCollection), transform);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

		String geometryBuildPropertyName = shpDSBuild.getSchema().getGeometryDescriptor().getLocalName();
		String geometryRoadPropertyName = shpDSRoad.getSchema().getGeometryDescriptor().getLocalName();

		Filter filterBuild = ff.intersects(ff.property(geometryBuildPropertyName), ff.literal(bBox));
		Filter filterRoad = ff.intersects(ff.property(geometryRoadPropertyName), ff.literal(bBox));

		SimpleFeatureCollection roadTown = roadCollection.subCollection(filterRoad);
		SimpleFeatureCollection buildTown = buildCollection.subCollection(filterBuild);

		File snapFile = new File(rootFile, "donneeGeographiques/snap/" + zipCode);
		snapFile.mkdirs();
		SelectParcels.exportSFC(roadTown, new File(snapFile, "route.shp"));
		SelectParcels.exportSFC(buildTown, new File(snapFile, "batiment.shp"));

		return snapFile;

	}

}
