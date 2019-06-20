package fr.ign.cogit.map.theseMC.nbHU;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class BuildingCollRatio extends MapRenderer {
	static String nameMap = "buildingCollRatio";
	static String text = "Ratio du nombre de logements collectifs sur le nombre de logements total simulés";

	public BuildingCollRatio(int imageWidth, int imageHeight, File rootMapstyle, File tomapshp, File outfolder) {
		super(imageWidth, imageHeight, nameMap, text, rootMapstyle, new File(rootMapstyle, "svgModel.svg"), tomapshp, outfolder);
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File indicFile = new File("/home/ubuntu/boulot/these/result2903/indic/bTH/DDense/base");
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");

		File outMap = new File(indicFile, "mapDepot");
		outMap.mkdirs();
		MapRenderer mpR = new BuildingCollRatio(1000, 1000, rootMapStyle, new File(indicFile, "commStatBTH.shp"), outMap);
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
