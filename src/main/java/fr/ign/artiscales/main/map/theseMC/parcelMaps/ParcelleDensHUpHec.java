package fr.ign.artiscales.main.map.theseMC.parcelMaps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.artiscales.main.map.MapRenderer;

public class ParcelleDensHUpHec extends MapRenderer {
	static String nameMap = "ParcelleDensHUpHec";
	static String text = "Densité de logements par hectare pour chaque parcelle";

	public ParcelleDensHUpHec(int imageWidth, int imageHeight, File mapStyleFolder, File featureFile, File outFolder)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		super(imageWidth, imageHeight, nameMap, text, mapStyleFolder, new File(mapStyleFolder, "svgModel.svg"), featureFile, outFolder);
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");
		MapRenderer mpR = new ParcelleDensHUpHec(1000, 1000, rootMapStyle,
				new File("/home/ubuntu/boulot/these/result2903/indic/bTH/CDense/base/parcStat.shp"), new File(rootMapStyle, "out/"));
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
