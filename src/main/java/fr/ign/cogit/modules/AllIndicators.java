package fr.ign.cogit.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.ign.cogit.indicators.BuildingToHousingUnit;
import fr.ign.cogit.indicators.CompScenario;
import fr.ign.cogit.indicators.CompVariant;
import fr.ign.cogit.indicators.CompatibleResult;
import fr.ign.cogit.indicators.ParcelStat;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class AllIndicators {

	public static void main(String[] args) throws Exception {
		File rootFile = new File("/home/ubuntu/boulot/these/result2903");
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
			CompVariant.run(p, rootFile, scenario);
		}
		CompScenario.run(rootFile);
	}

	public static void IndicForSimu(File rootFile, SimpluParametersJSON p, String scenario, String variant, boolean compatibleResult)
			throws Exception {
		if (compatibleResult) {
			CompatibleResult cr = new CompatibleResult(rootFile, p, scenario, variant);
			cr.complete();
			cr.joinStatToCommunities("resume.csv");
		}

		String[] typeDocs = { "RNU", "PLU", "CC" };

		// ParcelStat
		ParcelStat.run(p, rootFile, scenario, variant);
//		for (String typeDoc : typeDocs) {
//			List<String> listDoc = FromGeom.getZipByTypeDoc(new File(rootFile, "dataRegulation"), typeDoc);
//			ParcelStat.setIndicName(("bTH-" + typeDoc)b);
//			ParcelStat.run(p, rootFile, scenario, variant, listDoc);
//		}

		// BHT
		BuildingToHousingUnit.run(p, rootFile, scenario, variant);

//		for (String typeDoc : typeDocs) {
//			List<String> listDoc = FromGeom.getZipByTypeDoc(new File(rootFile, "dataRegulation"), typeDoc);
//			BuildingToHousingUnit.setIndicName("bTH-" + typeDoc);
//			BuildingToHousingUnit.run(p, rootFile, scenario, variant, listDoc);
//		}
	}
}
