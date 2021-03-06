package fr.ign.artiscales.main.rules.regulation.buildingType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.artiscales.main.util.FromGeom;
import fr.ign.artiscales.pm.parcelFunction.ParcelGetter;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;

public class MultipleRepartitionBuildingType extends RepartitionBuildingType {
	HashMap<String, List<String>> parcelsInZone;

	/**
	 * 
	 * @param p
	 * @param paramFolder
	 * @param zoningFile
	 * @param communeFile
	 * @param parcelFile
	 * @throws NoSuchElementException
	 * @throws Exception
	 */
	public MultipleRepartitionBuildingType(SimpluParametersJSON p, File paramFolder, File zoningFile, File communeFile, File parcelFile)
			throws NoSuchElementException, Exception {
		super(p, paramFolder, zoningFile, communeFile, parcelFile);
		p = addRepartitionToParameters(p, this.zoningFile, this.communeFile, parcelles.features().next(), new File(paramFolder, "locationBuildingType"));

		// we put all of the parcels into different lists regarding to their zones
		parcelsInZone = new HashMap<String, List<String>>();
	
		SimpleFeatureIterator par = parcelles.features();
		try {
			while (par.hasNext()) {
				SimpleFeature parcel = par.next();
				if (parcel.getAttribute("CODE") != null) {
					String bigZone = FromGeom.affectZoneAndTypoToLocation(p.getString("useRepartition"), p.getString("scenarioPMSP3D"), parcel,
							this.zoningFile, this.communeFile, true);
					if (parcelsInZone.containsKey(bigZone)) {
						List<String> tmpList = parcelsInZone.get(bigZone);
						tmpList.add((String) parcel.getAttribute("CODE"));
						parcelsInZone.put(bigZone, tmpList);
					} else {
						List<String> tmpList = new ArrayList<String>();
						tmpList.add((String) parcel.getAttribute("CODE"));
						parcelsInZone.put(bigZone, tmpList);
					}
				}
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			par.close();
		}

	}

	public BuildingType rangeInterest(double eval, String codeParcel, SimpluParametersJSON p) throws NoSuchElementException, Exception {

		List<String> parcelsWanted = new ArrayList<String>();
		for (List<String> parcels : parcelsInZone.values()) {
			if (parcels.contains(codeParcel)) {
				parcelsWanted = parcels;
			}
		}

		SimpleFeatureCollection parcelRepart = ParcelGetter.getFrenchParcelByZip(parcelles, parcelsWanted);

		makeRepart(p, parcelRepart);

		return rangeInterest(eval);
	}

}
