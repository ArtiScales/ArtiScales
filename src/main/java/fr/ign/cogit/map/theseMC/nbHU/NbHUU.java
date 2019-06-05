package fr.ign.cogit.map.theseMC.nbHU;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class NbHUU extends MapRenderer {
	static String nameMap = "nbHUU";
	static String text = "Nombre de logements simulés dans des zones urbanisables (U)";

	public NbHUU(int imageWidth, int imageHeight, File rootMapstyle, File tomapshp, File outfolder) {
		super(imageWidth, imageHeight, nameMap, text, rootMapstyle, new File(rootMapstyle, "svgModel.svg"), tomapshp, outfolder);
		legendName = "nbHUCategorie";
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");
		File rootIndic = new File("/home/ubuntu/boulot/these/result2903/indic/bTH/CPeuDense/base");
		File outMap = new File(rootIndic, "mapDepot");
		outMap.mkdirs();
		MapRenderer mpR = new NbHUU(1000, 1000, rootMapStyle,
				new File(rootIndic,"commStat.shp"), outMap);
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
