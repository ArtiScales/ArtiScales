package fr.ign.cogit.map.theseMC.parcelMaps;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class ParcelleDensSDPpHec extends MapRenderer {
	static String nameMap = "ParcelleDensSDPpHec";
	static String text = "Densité de surface de plancher des bâtiments par hectare pour chaque parcelle";

	public ParcelleDensSDPpHec(int imageWidth, int imageHeight, File mapStyleFolder, File featureFile, File outFolder)
			throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		super(imageWidth, imageHeight, nameMap, text, mapStyleFolder, new File(mapStyleFolder, "svgModel.svg"), featureFile, outFolder);
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result0308/mapStyle/");
		MapRenderer mpR = new ParcelleDensSDPpHec(1000, 1000, rootMapStyle,
				new File("/home/ubuntu/boulot/these/result0308/indic/bTH/DDense/variante0/commStat.shp"), new File(rootMapStyle, "out/"));
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
