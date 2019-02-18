package fr.ign.cogit.rules.regulation.buildingType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.simplu3d.util.SimpluParametersJSON;
import fr.ign.cogit.util.FromGeom;
import fr.ign.cogit.util.ParcelFonction;

public class MultipleRepartitionBuildingType extends RepartitionBuildingType {
	HashMap<String, List<String>> parcelsInZone;

	public MultipleRepartitionBuildingType(SimpluParametersJSON p, File paramFile, File zoningFile, File communeFile, File parcelFile)
			throws NoSuchElementException, Exception {
		super(p, paramFile, zoningFile, communeFile, parcelFile);
		p = addRepartitionToParameters(p, zoningFile, communeFile, parcelles.get(0),
				new File(this.getClass().getClassLoader().getResource("locationBuildingType").getFile()));

		// we put all of the parcels into different lists regarding to their zones
		parcelsInZone = new HashMap<String, List<String>>();
		for (IFeature parcel : parcelles) {
			if (parcel.getAttribute("CODE") != null) {
				String bigZone = FromGeom.affectZoneAndTypoToLocation(p.getString("useRepartition"), p.getString("scenarioPMSP3D"), parcel,
						zoningFile, communeFile, true);
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
	}

	public BuildingType rangeInterest(double eval, String codeParcel, SimpluParametersJSON p) throws NoSuchElementException, Exception {

		List<String> parcelsWanted = new ArrayList<String>();
		for (List<String> parcels : parcelsInZone.values()) {
			if (parcels.contains(codeParcel)) {
				parcelsWanted = parcels;
			}
		}

		IFeatureCollection<IFeature> parcelRepart = ParcelFonction.getParcelByCode(parcelles, parcelsWanted);

		makeRepart(p, parcelRepart);

		return rangeInterest(eval);
	}

}
