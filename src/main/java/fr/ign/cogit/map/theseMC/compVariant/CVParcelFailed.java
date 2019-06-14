
package fr.ign.cogit.map.theseMC.compVariant;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.map.MapRenderer;

public class CVParcelFailed extends MapRenderer {
	static String nameMap = "CVParcelFailed";

	public CVParcelFailed(int imageWidth, int imageHeight, File rootMapstyle, File tomapshp, File outfolder, String solo) {
		super(imageWidth, imageHeight, nameMap,
				"Coefficient de variation de la distribution du nombre de bâtiments simulées pour les variantes " + solo + " d'un scénario",
				rootMapstyle, new File(rootMapstyle, "svgModel.svg"), tomapshp, outfolder);
	}

	public static void main(String[] args) throws MalformedURLException, NoSuchAuthorityCodeException, IOException, FactoryException {
		File rootMapStyle = new File("/home/ubuntu/boulot/these/result2903/mapStyle/");
		File outMap = new File("/home/ubuntu/boulot/these/result2903/indic/parcelStat/DDense/variante0/map/");
		outMap.mkdirs();
		MapRenderer mpR = new CVParcelFailed(1000, 1000, rootMapStyle,
				new File("/home/ubuntu/boulot/these/result0308/indic/parcelStat/DDense/variante0/commStat.shp"), outMap, "Seed");
		mpR.renderCityInfo();
		mpR.generateSVG();
	}
}
