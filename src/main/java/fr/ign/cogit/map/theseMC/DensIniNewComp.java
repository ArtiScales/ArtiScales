
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

	public void makeIniMap(File otherFeatureFile) {
		text = "densités de logements par hectare initiale";
		sldFile = new File(sldFile.getParentFile(), "DensIniComp.sld");
		toMapShapeFile = otherFeatureFile;
	}

	public void makeObjMap(File otherFeatureFile) {
		text = "objectif de densités de logements par hectare fixé par le SCoT";
		sldFile = new File(sldFile.getParentFile(), "DensObj.sld");
		toMapShapeFile = otherFeatureFile;
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");
		MapRenderer mpR = new DensIniNewComp(1000, 1000, rootMapStyle,
				new File("/home/ubuntu/boulot/these/result2903/indic/bTH/CDense/variantMvData1/mapDepot/commNewDens.shp"),
				new File(rootMapStyle, "out/"));
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
