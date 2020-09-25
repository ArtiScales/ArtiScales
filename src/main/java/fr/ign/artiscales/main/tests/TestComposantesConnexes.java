package fr.ign.artiscales.main.tests;

import java.util.ArrayList;
import java.util.List;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.ILineString;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableCurve;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Arc;
import fr.ign.cogit.geoxygene.contrib.cartetopo.CarteTopo;
import fr.ign.cogit.geoxygene.contrib.cartetopo.Groupe;
import fr.ign.cogit.geoxygene.convert.FromGeomToLineString;
import fr.ign.cogit.geoxygene.feature.DefaultFeature;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileReader;
import fr.ign.cogit.geoxygene.util.conversion.ShapefileWriter;
import fr.ign.cogit.simplu3d.generator.FootprintGenerator;

public class TestComposantesConnexes {

	public static void main(String[] args) throws NoSuchAuthorityCodeException, FactoryException {
		String shapeFileName = "/home/mcolomb/donnee/autom/besancon/dataOut/routeSys.shp";
		String folderOut = "/home/mcolomb/donnee/autom/besancon/dataOut/";

		IFeatureCollection<IFeature> featColl = ShapefileReader.read(shapeFileName);

		List<ILineString> lS = new ArrayList<>();
		for (IFeature feat : featColl) {
			for (IOrientableCurve os : FromGeomToLineString.convert(feat.getGeom())) {

				lS.add((ILineString) os);
			}
		}
		CarteTopo cT = FootprintGenerator.newCarteTopo("Toto", lS, 0.0);

		Groupe gr = cT.getPopGroupes().nouvelElement();
		gr.setListeArcs(cT.getListeArcs());
		gr.setListeFaces(cT.getListeFaces());
		gr.setListeNoeuds(cT.getListeNoeuds());

		// on récupère les différents groupes
		List<Groupe> lG = gr.decomposeConnexes();

		int count = 0;

		for (Groupe g : lG) {
			System.out.println("Nombre arc composante : " + g.getListeArcs().size());

			IFeatureCollection<IFeature> featC = new FT_FeatureCollection<>();

			for (Arc a : g.getListeArcs()) {
				featC.add(new DefaultFeature(a.getGeometrie()));
			}

			ShapefileWriter.write(featC, folderOut + "comp" + (count++) + ".shp", CRS.decode("EPSG:2154"));
		}

	}

}
