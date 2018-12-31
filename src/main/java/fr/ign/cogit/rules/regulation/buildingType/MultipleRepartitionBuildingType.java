package fr.ign.cogit.rules.regulation.buildingType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.parameters.Parameters;
import fr.ign.cogit.rules.regulation.buildingType.RepartitionBuildingType;
import fr.ign.cogit.util.GetFromGeom;
import fr.ign.cogit.util.VectorFct;

public class MultipleRepartitionBuildingType extends RepartitionBuildingType {
	HashMap<String, List<String>> parcelsInZone;

	public MultipleRepartitionBuildingType(Parameters p, File parcelFile) throws NoSuchElementException, Exception {
		super(p, parcelFile);
		p = getRepartition(p, parcelles.features().next());

		parcelsInZone = new HashMap<String, List<String>>();
		SimpleFeatureIterator parcelsIt = parcelles.features();
		try {
			while (parcelsIt.hasNext()) {
				SimpleFeature feat = parcelsIt.next();
				String bigZone = GetFromGeom.affectToZoneAndTypo(p, feat, true);

				if (parcelsInZone.containsKey(bigZone)) {
					List<String> tmpList = parcelsInZone.get(bigZone);
					tmpList.add((String) feat.getAttribute("CODE"));
					parcelsInZone.put(bigZone, tmpList);
				} else {
					List<String> tmpList = new ArrayList<String>();
					tmpList.add((String) feat.getAttribute("CODE"));
					parcelsInZone.put(bigZone, tmpList);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelsIt.close();
		}

	}

	public BuildingType rangeInterest(double eval, String codeParcel, Parameters p) throws NoSuchElementException, Exception {
		
		List<String> parcelsWanted = new ArrayList<String>();
		
		for (List<String> parcels : parcelsInZone.values()) {
			if (parcelsWanted.contains(codeParcel)){
				parcelsWanted = parcels;
			}
		}
		
		SimpleFeatureCollection parcelRepart = VectorFct.getParcelByCode(parcelles,parcelsWanted );
		
		makeRepart(p,parcelRepart );
	
		return rangeInterest(eval);
	}

}
