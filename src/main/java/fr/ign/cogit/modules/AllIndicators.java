package fr.ign.cogit.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;

import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.indicators.CompVariant;
import fr.ign.cogit.indicators.CompatibleResult;
import fr.ign.cogit.indicators.ParcelStat;
import fr.ign.cogit.map.MapRenderer;
import fr.ign.cogit.map.theseMC.compVariant.MapNbHUCV;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;

public class AllIndicators {

	public static void main(String[] args) throws Exception {
		File rootFile = new File("");
		runAll(rootFile, false);
	}

	public static void runAll(File rootFile, boolean compatibleResult) throws Exception {

		File rootParam = new File(rootFile, "paramFolder");
		List<File> lF = new ArrayList<>();
		for (File fileS : (new File(rootFile, "SimPLUDepot/")).listFiles()) {
			String scenario = fileS.getName();
			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterTechnic.json"));
			lF.add(new File(rootParam, "/paramSet/" + scenario + "/parameterScenario.json"));
			SimpluParametersJSON p = new SimpluParametersJSON(lF);

			for (File f : (new File(rootFile, "SimPLUDepot/" + scenario + "/")).listFiles()) {
				String variant = f.getName();
				IndicForSimu(rootFile, p, scenario, variant, compatibleResult);
			}
			CompVariant parc = new CompVariant(p, rootFile, scenario);
			parc.createStat("bTH", "genStat.csv");
			List<MapRenderer> allOfTheMaps = new ArrayList<MapRenderer>();

			File commStatFile = parc.joinStatoBTHCommunities("compVariantbTHCityCoeffVar.csv");

			parc.createGraph(new File(parc.getIndicFile(), "compVariantbTHGen.csv"));

			MapRenderer mapNbHUCV = new MapNbHUCV(1000, 1000, parc.getMapStyle(), commStatFile, parc.getMapDepotFile());
			mapNbHUCV.renderCityInfo();
			mapNbHUCV.generateSVG();

			allOfTheMaps.add(mapNbHUCV);
		}
	}

	public static void IndicForSimu(File rootFile, SimpluParametersJSON p, String scenario, String variant, boolean compatibleResult)
			throws Exception {
		if (compatibleResult) {
			CompatibleResult cr = new CompatibleResult(rootFile, p, scenario, variant);
			cr.complete();
			cr.joinStatToCommunities("resume.csv");
		}
		// BHT
		BuildingToHousingUnit bhtU = new BuildingToHousingUnit(rootFile, p, scenario, variant);
		bhtU.distributionEstimate();
		bhtU.makeGenStat();
		bhtU.setCountToZero();
		// for every cities
		List<String> listInsee = FromGeom.getInsee(new File(bhtU.getRootFile(), "/dataGeo/old/communities.shp"), "DEPCOM");
		for (String city : listInsee) {
			bhtU.makeGenStat(city);
			bhtU.setCountToZero();
		}
		bhtU.joinStatoBTHParcels("housingUnits.csv");
		bhtU.createGraphDensity(new File(bhtU.getIndicFile(), "housingUnits.csv"));
		File parcelleStatFile = bhtU.joinStatoBTHParcels("housingUnits.csv");
		File commStatFile = bhtU.joinStatoBTHCommunities("genStat.csv");
		BuildingToHousingUnit.allOfTheMap(bhtU, commStatFile, parcelleStatFile);

		// Parcel
		ParcelStat parc = new ParcelStat(p, rootFile, scenario, "variantMvData1");
		SimpleFeatureCollection parcelStatSHP = parc.markSimuledParcels();
		parc.caclulateStatParcel();
		// parc.caclulateStatBatiParcel();
		parc.writeLine("AllZone", "ParcelStat");
		parc.setCountToZero();

		for (String city : listInsee) {
			SimpleFeatureCollection commParcel = ParcelFonction.getParcelByZip(parcelStatSHP, city);
			System.out.println("city " + city);
			parc.caclulateStatParcel(commParcel);
			// parc.caclulateStatBatiParcel(commParcel);
			parc.writeLine(city, "ParcelStat");
			parc.toString();
			parc.setCountToZero();
		}
		File commStatParcelFile = parc.joinStatToCommunities();
		parc.createMap(parc, commStatParcelFile);
		parc.createGraph(new File(parc.getIndicFile(), "ParcelStat.csv"));
	}

}
