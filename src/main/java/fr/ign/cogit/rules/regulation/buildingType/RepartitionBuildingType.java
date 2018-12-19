package fr.ign.cogit.rules.regulation.buildingType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.cogit.util.GetFromGeom;
import fr.ign.parameters.Parameters;

public class RepartitionBuildingType {

	HashMap<BuildingType, Double> repartition;
	HashMap<BuildingType, String> distribution;
	SimpleFeatureCollection parcelles;
	double pSingleHouses, pLotHouse, pSharedHouse, pSmallDwelling, pMediumDwelling;
	DescriptiveStatistics dsc;

	public RepartitionBuildingType(Parameters p, File parcelFile) throws IOException {

			ShapefileDataStore shpDSZone = new ShapefileDataStore(parcelFile.toURI().toURL());
			SimpleFeatureCollection parcelles = shpDSZone.getFeatureSource().getFeatures();

			HashMap<BuildingType, Double> rep = new HashMap<BuildingType, Double>();

			pSingleHouses = p.getDouble("singleHouse");
			rep.put(BuildingType.DETACHEDHOUSE, pSingleHouses);

			pLotHouse = p.getDouble("lotHouse");
			rep.put(BuildingType.SMALLHOUSE, pLotHouse);

			pSharedHouse = p.getDouble("sharedHouse");
			rep.put(BuildingType.MULTIFAMILYHOUSE, pSharedHouse);

			pSmallDwelling = p.getDouble("smallDwelling");
			rep.put(BuildingType.SMALLBLOCKFLAT, pSmallDwelling);

			pMediumDwelling = p.getDouble("mediumDwelling");
			rep.put(BuildingType.MIDBLOCKFLATS, pMediumDwelling);

			if ((pSingleHouses + pLotHouse + pSharedHouse + pSmallDwelling + pMediumDwelling) != 100.0) {
				System.out.println("there's a sum probleme here (yes, I know how to count to 100)");
			}

			this.repartition = rep;

			this.parcelles = parcelles;

			makeParcelRepartition();
			shpDSZone.dispose();
			System.out.println("Household unit distribution : " + distribution);
		}

	private void makeParcelRepartition() {
		makeParcelRepartition(parcelles);
	}

	private void makeParcelRepartition(SimpleFeatureCollection parcels) {
		DescriptiveStatistics distribEval = new DescriptiveStatistics();

		SimpleFeatureIterator parcelIt = parcelles.features();

		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feature = parcelIt.next();
				if (!((String) feature.getAttribute("eval")).equals("0")) {
					distribEval.addValue(Double.valueOf((String) feature.getAttribute("eval")));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		dsc = distribEval;

		String distribLotHouse = distribEval.getPercentile(0.000000001) + "-" + distribEval.getPercentile(pLotHouse);
		String distribSingleHouse = distribEval.getPercentile(pLotHouse) + "-"
				+ distribEval.getPercentile(pLotHouse + pSingleHouses);
		String distribSharedHouse = distribEval.getPercentile(pLotHouse + pSingleHouses) + "-"
				+ distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse);
		String distribSmallDwelling = distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse) + "-"
				+ distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse + pSmallDwelling);
		String distribMediumDwelling = distribEval
				.getPercentile(pLotHouse + pSingleHouses + pSharedHouse + pSmallDwelling) + "-"
				+ distribEval.getPercentile(
						pLotHouse + pSingleHouses + pSharedHouse + pSharedHouse + pSmallDwelling + pMediumDwelling);

		HashMap<BuildingType, String> distrib = new HashMap<BuildingType, String>();
		distrib.put(BuildingType.SMALLHOUSE, distribLotHouse);
		distrib.put(BuildingType.DETACHEDHOUSE, distribSingleHouse);
		distrib.put(BuildingType.MULTIFAMILYHOUSE, distribSharedHouse);
		distrib.put(BuildingType.SMALLBLOCKFLAT, distribSmallDwelling);
		distrib.put(BuildingType.MIDBLOCKFLATS, distribMediumDwelling);
		distribution = distrib;
	}

	public BuildingType rangeInterest(Double interest) throws Exception {
		System.out.println("distribution : " + distribution);
		System.out.println(interest);
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
		throw new Exception("value not in the range");
	}

	private HashMap<BuildingType, String> adjustDistribution(SimpleFeature downgradedParcel,
			BuildingType takenBuildingType, boolean upOrDown) throws Exception {
		return adjustDistribution((double) downgradedParcel.getAttribute("eval"), takenBuildingType, upOrDown);
	}

	private HashMap<BuildingType, String> adjustDistribution(double evalParcel, BuildingType takenBuildingType,
			boolean upOrDown) throws Exception {

		return adjustDistribution(evalParcel, takenBuildingType, rangeInterest(evalParcel), upOrDown);

	}

	/**
	 * 
	 * @param evalParcel
	 * @param takenBuildingType
	 * @param upOrDown          adjust in an up (true) or down (false) direction
	 * @return
	 * @throws Exception
	 */
	private HashMap<BuildingType, String> adjustDistribution(double evalParcel, BuildingType takenBuildingType,
			BuildingType normalBuildingType, boolean upOrDown) throws Exception {
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

	public HashMap<BuildingType, String> adjustDistributionUp(double evalParcel, BuildingType takenBuildingType,
			BuildingType normalBuildingType) throws Exception {
		return adjustDistribution(evalParcel, takenBuildingType, normalBuildingType, true);
	}

	public HashMap<BuildingType, String> adjustDistributionDown(double evalParcel, BuildingType takenBuildingType,
			BuildingType normalBuildingType) throws Exception {
		return adjustDistribution(evalParcel, takenBuildingType, normalBuildingType, false);
	}

	public Parameters getRepartition(Parameters p, SimpleFeature parcel) throws Exception {
		File profileBuildings = new File(
				this.getClass().getClassLoader().getResource("locationBuildingType").getFile());

		String[] tabRepart = p.getString("useRepartition").split("_");
		String typo = GetFromGeom.parcelInBigZone(new File(p.getString("rootFile"), "dataRegulation"), parcel);

		String zone = GetFromGeom.parcelInTypo(new File(p.getString("rootFile"), "dataGeo"), parcel);
		;
		Parameters addParam = null;
		// TODO finish to code that
		for (String s : tabRepart) {
			// If the paramFile speak for a particular scenario
			String[] scenarRepart = s.split(":");
			// if its no longer than 1, no particular scénario
			if (scenarRepart.length > 1) {
				// if codes doesnt match, we continue with another one
				if (!scenarRepart[0].equals(p.getString("code"))) {
					continue;
				}
			}
			// the different special locations
			String[] locRepart = s.split("-");
			for (String st : locRepart) {
				if (st.contains(typo) || st.contains(zone))
					;
			}

			for (File f : profileBuildings.listFiles()) {
			}
		}
		p.add(addParam);
		return p;
	}

	public Parameters getParam(BuildingType type) throws Exception {
		File profileBuildings = new File(this.getClass().getClassLoader().getResource("profileBuildingType").getFile());
		switch (type) {
		case DETACHEDHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "detachedHouse.xml"));
		case SMALLHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "smallHouse.xml"));
		case MULTIFAMILYHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "multifamilyHouse.xml"));
		case MIDBLOCKFLATS:
			return Parameters.unmarshall(new File(profileBuildings, "midBlockFlat.xml"));
		case SMALLBLOCKFLAT:
			return Parameters.unmarshall(new File(profileBuildings, "smallBlockFlat.xml"));
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
			result = BuildingType.MIDBLOCKFLATS;
		default:
			System.out.println("ain't got nothing bigger");
			result = BuildingType.MIDBLOCKFLATS;
		}
		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == 0.0) {
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
		case MIDBLOCKFLATS:
			result = BuildingType.SMALLBLOCKFLAT;
		default:
			System.out.println("ain't got nothing smaller");
			result = BuildingType.SMALLHOUSE;
		}

		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == 99.0) {
			System.out.println(result + " : that's a forbidden type");
			return fType;
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/etalIntenseRegul");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parameterTechnic.xml"));
		lF.add(new File(rootParam, "parameterScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		RepartitionBuildingType u = new RepartitionBuildingType(p, new File(
				"/home/mcolomb/informatique/ArtiScales2/ParcelSelectionFile/intenseRegulatedSpread/variant0/parcelGenExport.shp"));
		System.out.println(u.rangeInterest(0.52));
		System.out.println(u.distribution);
		System.out.println(u.adjustDistribution(0.35285416, BuildingType.MULTIFAMILYHOUSE, false));

	}

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
			return hasAttic(BuildingType.valueOf(type));
	}
}
