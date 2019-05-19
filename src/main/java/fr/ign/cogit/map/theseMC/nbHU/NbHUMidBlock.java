package fr.ign.cogit.map.theseMC.nbHU;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class NbHUMidBlock extends MapRenderer {
	static String nameMap = "nbHUMidBlock";
	static String text = "Nombre de logements simulés de type 'immeuble collectif de taille moyenne'";

	public NbHUMidBlock(int imageWidth, int imageHeight, File rootMapstyle, File tomapshp, File outfolder) {
		super(imageWidth, imageHeight, nameMap, text, rootMapstyle, new File(rootMapstyle, "svgModel.svg"), tomapshp, outfolder);
		legendName = "nbHUCategorie";
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result0308/mapStyle/");
		File outMap = new File("/home/ubuntu/boulot/these/result0308/indic/parcelStat/DDense/variante0/map/");
		outMap.mkdirs();
		MapRenderer mpR = new NbHUMidBlock(1000, 1000, rootMapStyle,
				new File("/home/ubuntu/boulot/these/result0308/indic/parcelStat/DDense/variante0/commStat.shp"), outMap);
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
