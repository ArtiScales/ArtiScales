package fr.ign.cogit.modules;

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

import fr.ign.cogit.annexeTools.SDPCalcPolygonizer;
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
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.mpp.configuration.BirthDeathModification;
import fr.ign.mpp.configuration.GraphConfiguration;
import fr.ign.mpp.configuration.GraphVertex;

public class SimPLUSimulator {


  // parcels containing all of them and the code if we make a simulation on them
  // or not
  File parcelsFile;

  // one single parcel to study
  SimpleFeature singleFeat;
  boolean isSingleFeat = false;

  // File rootFile;

  // Parameters from technical parameters and scenario parameters files
  SimpluParametersJSON p;
  // backup when p has been overwritted
  // Parameters pSaved;

  File paramFile;
  File folderOut;

  // Building file
  File buildFile;
  // Road file
  File roadFile;
  // Communities file
  File communitiesFile;

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

    File rootParam = new File("./src/main/resources/paramSet/DDense");
    List<File> lF = new ArrayList<>();
    lF.add(new File(rootParam, "parameterTechnic.xml"));
    lF.add(new File(rootParam, "parameterScenario.xml"));

    SimpluParametersJSON p = new SimpluParametersJSON(lF);
    // AttribNames.setATT_CODE_PARC("CODE");
    // USE_DIFFERENT_REGULATION_FOR_ONE_PARCEL = false;

    File paramFile = new File("./src/main/resources/");
    File f = new File("./ArtiScalesTest/ParcelSelectionFile/DDense/variante0/");
    File fOut = new File("./ArtiScalesTest/SimPLUDepot/DDense/variante0/");
    List<File> listBatiSimu = new ArrayList<File>();
    for (File ff : f.listFiles()) {
      if (ff.isDirectory()) {
        System.out.println("start pack " + ff);
        SimPLUSimulator sim = new SimPLUSimulator(paramFile, ff, p, fOut);
        List<File> simued = sim.run();
        if (simued != null) {
          listBatiSimu.addAll(simued);
        }
        System.out.println("done with pack " + ff.getName());
      }
    }
    FromGeom.mergeBatis(listBatiSimu);
  }

  /**
   * Constructor to make a new object to run SimPLU3D simulations.
   * 
   * @param rootfile
   *          : folder where profileBuildingType and locationBuildingType directories are
   * @param packFile
   *          : main folder of an artiscale simulation
   * @param geoFile
   *          : folder for geographic data
   * @param regulFile
   *          : folder for PLU data
   * @param parcels
   *          : Folder containing the selection of parcels
   * @param feat
   *          : single parcel to simulate
   * @param zipcode
   *          : zipcode of the city that is simulated
   * @param pa
   *          : parameters file
   * @param lF
   *          : list of the initials parameters
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

  public List<File> run(BuildingType type, SimpluParametersJSON par) throws Exception {
    Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);

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

      pTemp.add(RepartitionBuildingType.getParam(new File(paramFile, "profileBuildingType"), type));

      System.out.println("nombre de boites autorisées : " + pTemp.getString("nbCuboid"));

      building = runSimulation(env, i, pTemp, type, prescriptionUse);

      // if it's null, we skip to another parcel
      if (building == null) {
        continue bpu;
      }

      // saving the output
      folderOut.mkdirs();

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

    Environnement env = LoaderSHP.load(simuFile, codeFile, zoningFile, parcelsFile, roadFile, buildFile, filePrescPonct, filePrescLin, filePrescSurf, null);
    SimpluParametersJSON pUsed = p;
    ///////////
    // asses repartition to pacels
    ///////////
    // know if there's only one or multiple zones in the parcel pack
    List<String> sectors = new ArrayList<String>();
    IFeatureCollection<CadastralParcel> parcels = env.getCadastralParcels();

    for (CadastralParcel parcel : parcels) {
      String tmp = FromGeom.affectZoneAndTypoToLocation(pUsed.getString("useRepartition"), pUsed.getString("scenarioPMSP3D"), parcel, zoningFile, communitiesFile, true);
      if (!sectors.contains(tmp)) {
        sectors.add(tmp);
      }
    }

    // loading the type of housing to build
    RepartitionBuildingType housingUnit = new RepartitionBuildingType(pUsed, paramFile, zoningFile, communitiesFile, parcelsFile);
    boolean multipleRepartitionBuildingType = false;
    if (sectors.size() > 1) {
      System.out.println("multiple zones in the same parcel lot : there's gon be approximations");
      System.out.println("zones are ");
      for (String s : sectors) {
        System.out.println(s);
      }
      System.out.println();
      housingUnit = new MultipleRepartitionBuildingType(pUsed, paramFile, zoningFile, communitiesFile, parcelsFile);
      multipleRepartitionBuildingType = true;
    } else {
      System.out.println("it's all normal");
    }

    // Prescription setting
    IFeatureCollection<Prescription> prescriptions = env.getPrescriptions();
    IFeatureCollection<Prescription> prescriptionUse = PrescriptionPreparator.preparePrescription(prescriptions, pUsed);

    boolean association = ZoneRulesAssociation.associate(env, predicateFile, zoningFile, willWeAssociateAnyway(pUsed));

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
      pUsed = new SimpluParametersJSON(p);
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

      double eval = getParcelEval(codeParcel);

      // of which type should be the housing unit
      BuildingType type;
      if (multipleRepartitionBuildingType) {
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
        // we add the parameters for the building type want to simulate
        SimpluParametersJSON pTemp = new SimpluParametersJSON(pUsed);
        pTemp.add(RepartitionBuildingType.getParam(new File(paramFile, "profileBuildingType"), type));
        System.out.println("new height back to reg val " + pTemp.getDouble("maxheight"));

        building = runSimulation(env, i, pTemp, type, prescriptionUse);

        // if it's null, we skip to another parcel
        if (building == null) {
          continue bpu;
        }

        // if it's empty, or the size of floor is inferior to the minimum we set, we downsize to see if a smaller type fits
        if (building.isEmpty() || (double) building.get(0).getAttribute("SDPShon") < pTemp.getDouble("areaMin")) {
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
      // saving the output
      folderOut.mkdirs();

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

  /**
   * small method to know if we need to perform the simulation on zones that are not open to the urbanization.
   * 
   * @param p2
   *          : paramterer file, containing the answer to our question
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
   * @param par
   * @param prescriptionUse
   *          the prescriptions in Use prepared with PrescriptionPreparator
   * @return if null, we pass to another parcel. if an empty collection, we downsize the type
   * 
   * @throws Exception
   */
  @SuppressWarnings({ "deprecation" })
  public IFeatureCollection<IFeature> runSimulation(Environnement env, int i, SimpluParametersJSON par, BuildingType type, IFeatureCollection<Prescription> prescriptionUse)
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
        cc = article71Case12(alignementsGeometries, pred, env, i, bPU, par);
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
    // the -0.1 is set to avoid uncounting storeys when its very close to make one storey (which is very frequent)
    double surfacePlancherTotal = 0.0;
    double surfaceAuSol = 0.0;
    SDPCalcPolygonizer surfGen = new SDPCalcPolygonizer(par.getDouble("heightStorey") - 0.1);
    if (RepartitionBuildingType.hasAttic(type)) {
      surfGen = new SDPCalcPolygonizer(par.getDouble("heightStorey") - 0.1, par.getInteger("nbStoreysAttic"), par.getDouble("ratioAttic"));
    }

    List<Cuboid> cubes = cc.getGraph().vertexSet().stream().map(x -> x.getValue()).collect(Collectors.toList());
    surfacePlancherTotal = surfGen.process(cubes);
    surfaceAuSol = surfGen.processSurface(cubes);

    // Getting cuboid into list (we have to redo it because the cuboids are
    // dissapearing during this procces)

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
      AttributeManager.addAttribute(feat, "SurfaceSol", surfaceAuSol, "Double");
      AttributeManager.addAttribute(feat, "CODE", bPU.getCadastralParcels().get(0).getCode(), "String");
      AttributeManager.addAttribute(feat, "LIBELLE", libellesFinal, "String");
      AttributeManager.addAttribute(feat, "TYPEZONE", typeZonesFinal, "String");
      AttributeManager.addAttribute(feat, "BUILDTYPE", type, "String");
      iFeatC.add(feat);
    }

    if (iFeatC.isEmpty()) {
      return null;
    }
    return iFeatC;
  }

  @SuppressWarnings("deprecation")
  private GraphConfiguration<Cuboid> graphConfigurationWithAlignements(Alignements alignementsGeometries,
      CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i, BasicPropertyUnit bPU, IGeometry[] geoms,
      SimpluParametersJSON par) throws Exception {

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

  @SuppressWarnings("deprecation")
  private GraphConfiguration<Cuboid> article71Case12(Alignements alignementsGeometries,
      CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred, Environnement env, int i, BasicPropertyUnit bPU, SimpluParametersJSON par)
      throws Exception {

    GraphConfiguration<Cuboid> cc = null;
    // Instantiation of the sampler
    ParallelCuboidOptimizer oCB = new ParallelCuboidOptimizer();

    IMultiSurface<IOrientableSurface> iMSSamplinSurface = new GM_MultiSurface<>();
    // art-0071 implentation (begin)
    // LEFT SIDE IS TESTED
    IGeometry[] leftAlignement = alignementsGeometries.getLeftSide();

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

  private CommonPredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulationBySubParcel(BasicPropertyUnit bPU,
      SimpluParametersJSON p2, IFeatureCollection<Prescription> prescriptionUse, Environnement env) throws Exception {

    MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new MultiplePredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>>(
        bPU, true, p2, prescriptionUse, env);

    return pred;
  }

  private static PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> preparePredicateOneRegulation(BasicPropertyUnit bPU, SimpluParametersJSON p,
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

    System.out.println("Regulation code : " + regle.getInsee() + "-" + regle.getLibelle_de_dul());
    System.out.println("ArtiScalesRegulation : " + regle);
    // Instantiation of the rule checker
    PredicateArtiScales<Cuboid, GraphConfiguration<Cuboid>, BirthDeathModification<Cuboid>> pred = new PredicateArtiScales<>(bPU, true, regle, p, prescriptionUse, env);

    return pred;

  }
>>>>>>> 756b2240d7b77db738b55ebdd756927253741322
}
