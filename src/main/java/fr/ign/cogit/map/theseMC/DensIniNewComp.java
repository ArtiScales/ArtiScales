
package fr.ign.cogit.map.theseMC;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class DensIniNewComp extends MapRenderer {
	static String nameMap = "DensIniNewComp";
	static String text = "densités de logements par hectare en tenant compte des logements crées par la simulation";

	public DensIniNewComp(int imageWidth, int imageHeight, File mapStyleFolder, File featureFile, File outFolder)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		super(imageWidth, imageHeight, nameMap, text, mapStyleFolder, new File(mapStyleFolder, "svgModel.svg"), featureFile, outFolder);
	}

	public void makeDensIniNetMap() {
		super.text = "densités nettes initiale de logements par hectare";
		super.sldFile = new File(sldFile.getParentFile(), "DensIniNet.sld");
	}

	public void makeDensNewNetMap() {
		super.text = "densités nettes de logements par hectare après simulation";
		super.sldFile = new File(sldFile.getParentFile(), "DensNewNet.sld");
	}

	public void makeDensIniBrutMap() {
		super.text = "densités brutes initiale de logements par hectare";
		super.sldFile = new File(sldFile.getParentFile(), "DensIniBrt.sld");
	}

	public void makeDensNewBrutMap() {
		super.text = "densités brutes de logements par hectare après simulation";
		super.sldFile = new File(sldFile.getParentFile(), "DensNewBrt.sld");
	}

	public void makeObjMap() {
		super.text = "objectif de densités nettes de logements par hectare fixé par le SCoT";
		super.sldFile = new File(sldFile.getParentFile(), "DensObj.sld");
	}

	public void makeDiffObjMap() {
		super.text = "différence entre les objectif de densités de logements et la densité simulée des communes";
		super.sldFile = new File(sldFile.getParentFile(), "DifDObjN.sld");
	}
	
	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootIndic = new File("/home/ubuntu/boulot/these/result2903/indic/bTH/DPeuDense/base/");
		DensIniNewComp map = new DensIniNewComp(1000, 1000, new File("/home/ubuntu/boulot/these/result2903/mapStyle/"),
				new File(rootIndic, "commNewDens.shp"), new File(rootIndic, "mapDepot/"));
		
		map.makeDensIniNetMap();
		map.renderCityInfo("DensNetIni");
		map.generateSVG(new File(rootIndic, "mapDepot/DensNetIni.svg"), "DensNetIni");

		map.makeDensNewNetMap();
		map.renderCityInfo("DensNetNew");
		map.generateSVG(new File(rootIndic, "mapDepot/DensNetNew.svg"), "DensNetNew");

		map.makeObjMap();
		map.renderCityInfo("DensObj");
		map.generateSVG(new File(rootIndic, "mapDepot/DensObj.svg"), "DensObj");

		map.makeDensIniBrutMap();
		map.renderCityInfo("DensBrtIni");
		map.generateSVG(new File(rootIndic, "mapDepot/DensBrtIni.svg"), "DensBrtIni");

		map.makeDensNewBrutMap();
		map.renderCityInfo("DensBrtNew");
		map.generateSVG(new File(rootIndic, "mapDepot/DensBrtNew.svg"), "DensBrtNew");
		
		map.makeDiffObjMap();
		map.renderCityInfo("DifDObjN");
		map.generateSVG(new File(rootIndic, "mapDepot/DifDObjN.svg"), "DifDObjN");
		
	}
}
