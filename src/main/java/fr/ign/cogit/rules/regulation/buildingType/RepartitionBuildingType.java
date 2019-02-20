package fr.ign.cogit.rules.regulation.buildingType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.simplu3d.util.SimpluParameters;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;

public class RepartitionBuildingType {

	HashMap<BuildingType, Double> repartition;
	HashMap<BuildingType, String> distribution;
	IFeatureCollection<IFeature> parcelles;
	double pDetachedHouse, pSmallHouse, pMultifamilyHouse, pSmallBlockFlat, pMidBlockFlat;
	DescriptiveStatistics dsc;
	BuildingType defautBT;
	File paramFile;
	File zoningFile;
	File communeFile;

	public RepartitionBuildingType(SimpluParametersJSON p, File paramFile, File zoningFile, File communeFile, File parcelFile)
			throws NoSuchElementException, Exception {
		System.out.println("parcelFile = " + parcelFile);
		this.parcelles = ShapefileReader.read(parcelFile.getAbsolutePath());
		this.paramFile = paramFile;
		this.zoningFile = zoningFile;
		this.communeFile = communeFile;
		makeRepart(p, parcelles);
	}

	public RepartitionBuildingType(SimpluParametersJSON p, File paramFile, File zoningFile, File communeFile, IFeatureCollection<IFeature> parcel)
			throws NoSuchElementException, Exception {
		this.parcelles = parcel;
		this.paramFile = paramFile;
		this.zoningFile = zoningFile;
		this.communeFile = communeFile;
		makeRepart(p, parcelles);

	}

	public void makeRepart(SimpluParametersJSON p, IFeatureCollection<IFeature> parcelles) throws NoSuchElementException, Exception {

		HashMap<BuildingType, Double> rep = new HashMap<BuildingType, Double>();

		// add the zone parameters with the first parcel (redone for each parcel if it's a multizone type)
		File location = new File(paramFile, "locationBuildingType");
		p = addRepartitionToParameters(p, zoningFile, communeFile, parcelles.get(0), location);

		pDetachedHouse = p.getDouble("detachedHouse");

		if (pDetachedHouse == -1) {
			pDetachedHouse = 0;
		}
		double max = pDetachedHouse;
		defautBT = BuildingType.DETACHEDHOUSE;

		pSmallHouse = p.getDouble("smallHouse");
		if (pSmallHouse == -1) {
			pSmallHouse = 0;
		}
		if (pSmallHouse > max) {
			max = pSmallHouse;
			defautBT = BuildingType.SMALLHOUSE;
		}

		pMultifamilyHouse = p.getDouble("multifamilyHouse");
		if (pMultifamilyHouse == -1) {
			pMultifamilyHouse = 0;
		}
		if (pMultifamilyHouse > max) {
			max = pMultifamilyHouse;
			defautBT = BuildingType.MULTIFAMILYHOUSE;
		}

		pSmallBlockFlat = p.getDouble("smallBlockFlat");
		if (pSmallBlockFlat == -1) {
			pSmallBlockFlat = 0;
		}
		if (pSmallBlockFlat > max) {
			max = pSmallBlockFlat;
			defautBT = BuildingType.SMALLBLOCKFLAT;
		}

		pMidBlockFlat = p.getDouble("midBlockFlat");
		if (pMidBlockFlat == -1) {
			pMidBlockFlat = 0;
		}
		if (pMidBlockFlat > max) {
			max = pMidBlockFlat;
			defautBT = BuildingType.MIDBLOCKFLAT;
		}

		rep.put(BuildingType.DETACHEDHOUSE, pDetachedHouse);
		rep.put(BuildingType.SMALLHOUSE, pSmallHouse);
		rep.put(BuildingType.MULTIFAMILYHOUSE, pMultifamilyHouse);
		rep.put(BuildingType.SMALLBLOCKFLAT, pSmallBlockFlat);
		rep.put(BuildingType.MIDBLOCKFLAT, pMidBlockFlat);

		if ((pDetachedHouse + pSmallHouse + pMultifamilyHouse + pSmallBlockFlat + pMidBlockFlat) != 100.0) {
			System.out.println("there's a sum probleme here (yes, I know how to count to 100). It's "
					+ (pDetachedHouse + pSmallHouse + pMultifamilyHouse + pSmallBlockFlat + pMidBlockFlat) + " instead");
		}

		this.repartition = rep;

		// this.parcelles = parcelles;

		makeParcelRepartition();

		System.out.println("Household unit distribution : " + distribution);
	}

	private void makeParcelRepartition() {
		makeParcelRepartition(parcelles);
	}

	private void makeParcelRepartition(IFeatureCollection<IFeature> parcels) {
		DescriptiveStatistics distribEval = new DescriptiveStatistics();

		for (IFeature parcel : parcels) {
			try {
				if (!((String) parcel.getAttribute("eval")).equals("0")) {
					distribEval.addValue(Double.valueOf((String) parcel.getAttribute("eval")));
				}
			} catch (NullPointerException np) {
			}
		}

		dsc = distribEval;

		List<Double> toBeQuantile = new ArrayList<Double>();

		toBeQuantile.add(pSmallHouse);
		String distribSmallHouse = distribEval.getPercentile(0.00000001) + "-" + distribEval.getPercentile(safeQuantile(toBeQuantile));

		double tmpInfValue = safeQuantile(toBeQuantile);
		toBeQuantile.add(pDetachedHouse);
		String distribDetachedHouse = distribEval.getPercentile(tmpInfValue) + "-" + distribEval.getPercentile(safeQuantile(toBeQuantile));

		tmpInfValue = safeQuantile(toBeQuantile);
		toBeQuantile.add(pMultifamilyHouse);
		String distribMultifamilyHouse = String.valueOf(distribEval.getPercentile(tmpInfValue)).concat("-")
				.concat(String.valueOf(distribEval.getPercentile(safeQuantile(toBeQuantile))));

		tmpInfValue = safeQuantile(toBeQuantile);
		toBeQuantile.add(pSmallBlockFlat);
		String distribSmallBlockFlat = distribEval.getPercentile(tmpInfValue) + "-" + distribEval.getPercentile(safeQuantile(toBeQuantile));

		tmpInfValue = safeQuantile(toBeQuantile);
		toBeQuantile.add(pMidBlockFlat);
		String distribMidBlockFlat = distribEval.getPercentile(tmpInfValue) + "-" + distribEval.getPercentile(safeQuantile(toBeQuantile));

		HashMap<BuildingType, String> distrib = new HashMap<BuildingType, String>();
		distrib.put(BuildingType.SMALLHOUSE, distribSmallHouse);
		distrib.put(BuildingType.DETACHEDHOUSE, distribDetachedHouse);
		distrib.put(BuildingType.MULTIFAMILYHOUSE, distribMultifamilyHouse);
		distrib.put(BuildingType.SMALLBLOCKFLAT, distribSmallBlockFlat);
		distrib.put(BuildingType.MIDBLOCKFLAT, distribMidBlockFlat);

		distribution = distrib;
	}

	public static double safeQuantile(List<Double> list) {
		double result = 0.0;
		for (double d : list) {
			result = result + d;
		}

		if (result == 0.0) {
			result = 0.0000001;
		} else if (result > 100.0) {
			result = 100.0;
		}
		return result;

	}

	public BuildingType rangeInterest(Double interest) throws Exception {
		System.out.println("distribution : " + distribution);
		System.out.println("interest : " + interest);
		for (BuildingType type : distribution.keySet()) {
			String val = distribution.get(type);
			Double inf = Double.valueOf(val.split("-")[0]);
			Double sup = Double.valueOf(val.split("-")[1]);

			if (interest >= inf && interest <= sup) {
				if (repartition.get(type) > 0.0) {
					return type;
				}
			}
		}
		System.out.println("we return the defalut type ");
		return defautBT;
		// throw new Exception("value not in the range");
	}

	private HashMap<BuildingType, String> adjustDistribution(double evalParcel, BuildingType takenBuildingType, boolean upOrDown) throws Exception {

		return adjustDistribution(evalParcel, takenBuildingType, rangeInterest(evalParcel), upOrDown);

	}

	/**
	 * 
	 * @param evalParcel
	 * @param takenBuildingType
	 * @param upOrDown
	 *            adjust in an up (true) or down (false) direction
	 * @return
	 * @throws Exception
	 */
	private HashMap<BuildingType, String> adjustDistribution(double evalParcel, BuildingType takenBuildingType, BuildingType normalBuildingType,
			boolean upOrDown) throws Exception {
		double[] sortedVal = dsc.getSortedValues();

		double ecart = 0.0;
		boolean next = false;
		double val1 = 0.0;
		for (double val : sortedVal) {
			if (next) {
				ecart = val - val1;
				break;
			}
			if (val == evalParcel) {
				next = true;
				val1 = val;
			}
		}

		BuildingType normalType = rangeInterest(evalParcel);
		String oldDistrib = this.distribution.remove(normalType);

		Double distribInf = 0.0;
		Double distribSup = 0.0;
		if (upOrDown) {
			distribInf = Double.valueOf(oldDistrib.split("-")[0]);
			distribSup = Double.valueOf(oldDistrib.split("-")[1]) - ecart;
		} else {
			distribInf = Double.valueOf(oldDistrib.split("-")[0]) + ecart;
			distribSup = Double.valueOf(oldDistrib.split("-")[1]);
		}

		this.distribution.put(normalType, distribInf + "-" + distribSup);

		String oldDistribDowngraded = distribution.remove(takenBuildingType);

		Double distribInfDG = 0.0;
		Double distribSupDG = 0.0;
		if (upOrDown) {
			distribInfDG = Double.valueOf(oldDistribDowngraded.split("-")[0]) - ecart;
			distribSupDG = Double.valueOf(oldDistribDowngraded.split("-")[1]);
		} else {
			distribInfDG = Double.valueOf(oldDistribDowngraded.split("-")[0]);
			distribSupDG = Double.valueOf(oldDistribDowngraded.split("-")[1]) + ecart;
		}

		this.distribution.put(takenBuildingType, distribInfDG + "-" + distribSupDG);

		System.out.println("new distribution of housing unit : " + distribution);
		return this.distribution;
	}

	public HashMap<BuildingType, String> adjustDistributionUp(double evalParcel, BuildingType takenBuildingType, BuildingType normalBuildingType)
			throws Exception {
		return adjustDistribution(evalParcel, takenBuildingType, normalBuildingType, true);
	}

	public HashMap<BuildingType, String> adjustDistributionDown(double evalParcel, BuildingType takenBuildingType, BuildingType normalBuildingType)
			throws Exception {
		return adjustDistribution(evalParcel, takenBuildingType, normalBuildingType, false);
	}

	/**
	 * return the parameter file added with the repartition of the concerned zone in which the parcel is.
	 * 
	 * @param p
	 * @param parcel
	 * @return
	 * @throws Exception
	 */
	public static SimpluParametersJSON addRepartitionToParameters(SimpluParametersJSON p, File zoningFile, File communeFile, IFeature parcel, File profileBuildings)
			throws Exception {
		String affect = FromGeom.affectZoneAndTypoToLocation(
		    p.getString("useRepartition"), p.getString("scenarioPMSP3D"), parcel, zoningFile, communeFile, true);
//		System.out.println("profileBuildings = " + profileBuildings);
		// if nothing is returned, we use the default parameter file
		if (affect.equals("")) {
			for (File f : profileBuildings.listFiles()) {
				String name = f.getName();
				//first if there is a special default comportment for the scenario
				if (name.startsWith(p.getString("scenarioPMSP3D")) && name.contains("default")) {
					affect = f.getName().replace(".json", "");
					break;
				}
				//else the default default
				else  {
					affect = "default";
				}
			}
		}

		SimpluParametersJSON addParam = new SimpluParametersJSON(new File(profileBuildings + "/" + affect + ".json"));

		System.out.println("we affect the " + affect + ".json" + " folder");

		p.add(addParam);
		return p;
	}

	/**
	 * return the json data related to the given building type 
	 * @param ressourceFolder : Folder where the .xml files are stored
	 * @param type : given Building Type
	 * @return
	 * @throws Exception
	 */
	public static SimpluParametersJSON getParam(File ressourceFolder, BuildingType type) throws Exception {
		switch (type) {
		case DETACHEDHOUSE:
			return new SimpluParametersJSON(new File(ressourceFolder, "detachedHouse.json"));
		case SMALLHOUSE:
			return new SimpluParametersJSON(new File(ressourceFolder, "smallHouse.json"));
		case MULTIFAMILYHOUSE:
			return new SimpluParametersJSON(new File(ressourceFolder, "multifamilyHouse.json"));
		case MIDBLOCKFLAT:
			return new SimpluParametersJSON(new File(ressourceFolder, "midBlockFlat.json"));
		case SMALLBLOCKFLAT:
			return new SimpluParametersJSON(new File(ressourceFolder, "smallBlockFlat.json"));
		}
		throw new Exception("no parameter file found");
	}

	public BuildingType up(BuildingType fType) throws Exception {
		BuildingType result = null;

		switch (fType) {
		case DETACHEDHOUSE:
			result = BuildingType.SMALLHOUSE;
		case SMALLHOUSE:
			result = BuildingType.MULTIFAMILYHOUSE;
		case MULTIFAMILYHOUSE:
			result = BuildingType.SMALLBLOCKFLAT;
		case SMALLBLOCKFLAT:
			result = BuildingType.MIDBLOCKFLAT;
		default:
			System.out.println("ain't got nothing bigger");
			result = BuildingType.MIDBLOCKFLAT;
		}
		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == -1) {
			return fType;
		}
		return result;
	}

	public BuildingType down(BuildingType fType) throws Exception {
		BuildingType result = null;

		switch (fType) {
		case SMALLHOUSE:
			result = BuildingType.DETACHEDHOUSE;
		case MULTIFAMILYHOUSE:
			result = BuildingType.SMALLHOUSE;
		case SMALLBLOCKFLAT:
			result = BuildingType.MULTIFAMILYHOUSE;
		case MIDBLOCKFLAT:
			result = BuildingType.SMALLBLOCKFLAT;
		default:
			System.out.println("ain't got nothing smaller");
			result = BuildingType.SMALLHOUSE;
		}

		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == -1) {
			System.out.println(result + " : that's a forbidden type");
			return fType;
		}
		return result;
	}

//	public static void main(String[] args) throws Exception {
//		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/etalIntenseRegul");
//		List<File> lF = new ArrayList<>();
//		lF.add(new File(rootParam, "parameterTechnic.xml"));
//		lF.add(new File(rootParam, "parameterScenario.xml"));
//
//		SimpluParametersJSON p = new SimpluParametersJSON(lF);
//
//		RepartitionBuildingType u = new RepartitionBuildingType(p, new File(""), new File(""), new File(""),
//				new File("/home/mcolomb/informatique/ArtiScales2/ParcelSelectionFile/intenseRegulatedSpread/variant0/parcelGenExport.shp"));
//		System.out.println(u.rangeInterest(0.52));
//		System.out.println(u.distribution);
//		System.out.println(u.adjustDistribution(0.35285416, BuildingType.MULTIFAMILYHOUSE, false));
//
//	}

	/**
	 * says if the building possess an attic
	 * 
	 * @param type
	 * @return
	 */
	public static boolean hasAttic(BuildingType type) {
		if (type == BuildingType.SMALLHOUSE || type == BuildingType.DETACHEDHOUSE) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean hasAttic(String type) {
		return hasAttic(BuildingType.valueOf(type.toUpperCase()));
	}

	/**
	 * return the buildingType with the most ignore the -1 values which means that is a forbidden type
	 * 
	 * @param p
	 * @return
	 */
	public static BuildingType getBiggestRepartition(SimpluParameters p) {
		Integer max = 0;
		BuildingType result = null;
		if (p.getInteger("detachedHouse") > max) {
			max = p.getInteger("detachedHouse");
			result = BuildingType.DETACHEDHOUSE;
		}
		if (p.getInteger("smallHouse") > max ) {
			max = p.getInteger("smallHouse");
			result = BuildingType.SMALLHOUSE;
		}
		if (p.getInteger("multifamilyHouse") > max ) {
			max = p.getInteger("multifamilyHouse");
			result = BuildingType.MULTIFAMILYHOUSE;
		}
		if (p.getInteger("smallBlockFlat") > max) {
			max = p.getInteger("smallBlockFlat");
			result = BuildingType.SMALLBLOCKFLAT;
		}
		if (p.getInteger("midBlockFlat") > max ) {
			max = p.getInteger("midBlockFlat");
			result = BuildingType.MIDBLOCKFLAT;
		}
		return result;
	}
}
