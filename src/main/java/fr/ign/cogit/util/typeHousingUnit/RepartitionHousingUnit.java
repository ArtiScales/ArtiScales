package fr.ign.cogit.util.typeHousingUnit;

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

public class RepartitionHousingUnit {
	HashMap<TypeHousingUnit, Double> repartition;
	HashMap<TypeHousingUnit, String> distribution;
	SimpleFeatureCollection parcelles;
	double pSingleHouses, pLotHouse, pSharedHouse, pSmallDwelling, pMediumDwelling;
	DescriptiveStatistics dsc;

	public RepartitionHousingUnit(Parameters p, File parcelFile) throws IOException {

		ShapefileDataStore shpDSZone = new ShapefileDataStore(parcelFile.toURI().toURL());
		SimpleFeatureCollection parcelles = shpDSZone.getFeatureSource().getFeatures();

		HashMap<TypeHousingUnit, Double> rep = new HashMap<TypeHousingUnit, Double>();

		pSingleHouses = p.getDouble("singleHouse");
		rep.put(TypeHousingUnit.SINGLEHOUSE, pSingleHouses);

		pLotHouse = p.getDouble("lotHouse");
		rep.put(TypeHousingUnit.LOTHOUSE, pLotHouse);

		pSharedHouse = p.getDouble("sharedHouse");
		rep.put(TypeHousingUnit.SHAREDHOUSE, pSharedHouse);

		pSmallDwelling = p.getDouble("smallDwelling");
		rep.put(TypeHousingUnit.SMALLDWELLING, pSmallDwelling);

		pMediumDwelling = p.getDouble("mediumDwelling");
		rep.put(TypeHousingUnit.MEDIUMDWELLING, pMediumDwelling);

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
		String distribSingleHouse = distribEval.getPercentile(pLotHouse) + "-" + distribEval.getPercentile(pLotHouse + pSingleHouses);
		String distribSharedHouse = distribEval.getPercentile(pLotHouse + pSingleHouses) + "-" + distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse);
		String distribSmallDwelling = distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse) + "-"
				+ distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse + pSmallDwelling);
		String distribMediumDwelling = distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse + pSmallDwelling) + "-"
				+ distribEval.getPercentile(pLotHouse + pSingleHouses + pSharedHouse + pSharedHouse + pSmallDwelling + pMediumDwelling);

		HashMap<TypeHousingUnit, String> distrib = new HashMap<TypeHousingUnit, String>();
		distrib.put(TypeHousingUnit.LOTHOUSE, distribLotHouse);
		distrib.put(TypeHousingUnit.SINGLEHOUSE, distribSingleHouse);
		distrib.put(TypeHousingUnit.SHAREDHOUSE, distribSharedHouse);
		distrib.put(TypeHousingUnit.SMALLDWELLING, distribSmallDwelling);
		distrib.put(TypeHousingUnit.MEDIUMDWELLING, distribMediumDwelling);
		distribution = distrib;
	}

	public TypeHousingUnit rangeInterest(Double interest) throws Exception {
		System.out.println("distribution : " + distribution);
		System.out.println(interest);
		for (TypeHousingUnit type : distribution.keySet()) {
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

	private HashMap<TypeHousingUnit, String> adjustDistribution(SimpleFeature downgradedParcel, TypeHousingUnit takenTypeHousingUnit, boolean upOrDown) throws Exception {
		return adjustDistribution((double) downgradedParcel.getAttribute("eval"), takenTypeHousingUnit, upOrDown);
	}

	private HashMap<TypeHousingUnit, String> adjustDistribution(double evalParcel, TypeHousingUnit takenTypeHousingUnit, boolean upOrDown) throws Exception {

		return adjustDistribution(evalParcel, takenTypeHousingUnit, rangeInterest(evalParcel), upOrDown);

	}

	/**
	 * 
	 * @param evalParcel
	 * @param takenTypeHousingUnit
	 * @param upOrDown
	 *            adjust in an up (true) or down (false) direction
	 * @return
	 * @throws Exception
	 */
	private HashMap<TypeHousingUnit, String> adjustDistribution(double evalParcel, TypeHousingUnit takenTypeHousingUnit, TypeHousingUnit normalTypeHousingUnit, boolean upOrDown)
			throws Exception {
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

		TypeHousingUnit normalType = rangeInterest(evalParcel);
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

		String oldDistribDowngraded = distribution.remove(takenTypeHousingUnit);

		Double distribInfDG = 0.0;
		Double distribSupDG = 0.0;
		if (upOrDown) {
			distribInfDG = Double.valueOf(oldDistribDowngraded.split("-")[0]) - ecart;
			distribSupDG = Double.valueOf(oldDistribDowngraded.split("-")[1]);
		} else {
			distribInfDG = Double.valueOf(oldDistribDowngraded.split("-")[0]);
			distribSupDG = Double.valueOf(oldDistribDowngraded.split("-")[1]) + ecart;
		}

		this.distribution.put(takenTypeHousingUnit, distribInfDG + "-" + distribSupDG);

		System.out.println("new distribution of housing unit : " + distribution);
		return this.distribution;
	}

	public HashMap<TypeHousingUnit, String> adjustDistributionUp(double evalParcel, TypeHousingUnit takenTypeHousingUnit, TypeHousingUnit normalTypeHousingUnit) throws Exception {
		return adjustDistribution(evalParcel, takenTypeHousingUnit, normalTypeHousingUnit, true);
	}

	public HashMap<TypeHousingUnit, String> adjustDistributionDown(double evalParcel, TypeHousingUnit takenTypeHousingUnit, TypeHousingUnit normalTypeHousingUnit)
			throws Exception {
		return adjustDistribution(evalParcel, takenTypeHousingUnit, normalTypeHousingUnit, false);
	}

	public Parameters getRepartition(Parameters p, SimpleFeature parcel) throws Exception {
		File profileBuildings = new File(this.getClass().getClassLoader().getResource("distributionHousingUnit").getFile());

		String[] tabRepart = p.getString("useRepartition").split("_");

		// possibilities of différent locations (pour l'instant, typo ou zone du PLU)
		String[] diffTypo = { "rural", "periUrbain", "banlieue", "centre" };
		String[] diffZones = { "U", "AU", "NC" };

		String typo = GetFromGeom.parcelInBigZone(new File(p.getString("rootFile"),"dataRegul"),parcel);
		
		String zone = GetFromGeom.parcelInTypo(new File(p.getString("rootFile"),"dataRegul"),parcel); ;
		
		for (String s : tabRepart) {
			// If the paramFile speak for a particular scenario
			String[] scenarRepart = s.split(":");
			// if its no longer than 1, no particular scénario
			if (scenarRepart.length > 1) {
				// if codes doesnt match, we continue with another one
				if (!scenarRepart.equals(p.getString("code"))) {
					continue;
				}
			}
			// the different special locations
			String[] locRepart = s.split("_");

		}

		return p;
	}

	public Parameters getParam(TypeHousingUnit type) throws Exception {
		File profileBuildings = new File(this.getClass().getClassLoader().getResource("profileHousingUnit").getFile());
		switch (type) {
		case LOTHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "lotHouse.xml"));
		case SINGLEHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "singleHouse.xml"));
		case SHAREDHOUSE:
			return Parameters.unmarshall(new File(profileBuildings, "sharedHouse.xml"));
		case MEDIUMDWELLING:
			return Parameters.unmarshall(new File(profileBuildings, "mediumDwelling.xml"));
		case SMALLDWELLING:
			return Parameters.unmarshall(new File(profileBuildings, "smallDwelling.xml"));
		}
		throw new Exception("no parameter file found");
	}

	public TypeHousingUnit up(TypeHousingUnit fType) throws Exception {
		TypeHousingUnit result = null;

		switch (fType) {
		case LOTHOUSE:
			result = TypeHousingUnit.SINGLEHOUSE;
		case SINGLEHOUSE:
			result = TypeHousingUnit.SHAREDHOUSE;
		case SHAREDHOUSE:
			result = TypeHousingUnit.SMALLDWELLING;
		case SMALLDWELLING:
			result = TypeHousingUnit.MEDIUMDWELLING;
		default:
			System.out.println("ain't got nothing bigger");
			result = TypeHousingUnit.MEDIUMDWELLING;
		}
		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == 0.0) {
			return fType;
		}
		return result;
	}

	public TypeHousingUnit down(TypeHousingUnit fType) throws Exception {
		TypeHousingUnit result = null;

		switch (fType) {
		case SINGLEHOUSE:
			result = TypeHousingUnit.LOTHOUSE;
		case SHAREDHOUSE:
			result = TypeHousingUnit.SINGLEHOUSE;
		case SMALLDWELLING:
			result = TypeHousingUnit.SHAREDHOUSE;
		case MEDIUMDWELLING:
			result = TypeHousingUnit.SMALLDWELLING;
		default:
			System.out.println("ain't got nothing smaller");
			result = TypeHousingUnit.LOTHOUSE;
		}

		// if the type is not in the prediction, we don't return it
		if (repartition.get(result) == 0.0) {
			System.out.println(result + " : that's a forbidden type");
			return fType;
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		File rootParam = new File("/home/mcolomb/workspace/ArtiScales/src/main/resources/paramSet/etalIntenseRegul");
		List<File> lF = new ArrayList<>();
		lF.add(new File(rootParam, "parametreTechnique.xml"));
		lF.add(new File(rootParam, "parametreScenario.xml"));

		Parameters p = Parameters.unmarshall(lF);

		RepartitionHousingUnit u = new RepartitionHousingUnit(p,
				new File("/home/mcolomb/informatique/ArtiScales2/ParcelSelectionFile/intenseRegulatedSpread/variant0/parcelGenExport.shp"));
		System.out.println(u.rangeInterest(0.52));
		System.out.println(u.distribution);
		System.out.println(u.adjustDistribution(0.35285416, TypeHousingUnit.SHAREDHOUSE, false));

	}

}
