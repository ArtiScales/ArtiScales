package fr.ign.cogit.rules.regulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.simplu3d.model.AbstractBuilding;
import fr.ign.cogit.simplu3d.model.BasicPropertyUnit;
import fr.ign.cogit.simplu3d.model.CadastralParcel;
import fr.ign.cogit.simplu3d.model.Environnement;
import fr.ign.cogit.simplu3d.model.ParcelBoundary;
import fr.ign.cogit.simplu3d.model.ParcelBoundarySide;
import fr.ign.cogit.simplu3d.model.ParcelBoundaryType;
import fr.ign.cogit.simplu3d.model.SubParcel;

public class Alignements {

	public boolean hasAlignement = false;

	private BasicPropertyUnit currentBPU;

	private Environnement env;
	
	private AlignementType type;



	public enum AlignementType {
		ART7112(0), ART713(1), NONE(99);

		private int value;

		private AlignementType(int type) {
			value = type;
		}

		public int getValueType() {
			return value;
		}

	}

	public Alignements(List<ArtiScalesRegulation> allRegulation, BasicPropertyUnit currentBPU, Environnement env) {
		// Currently we only take the first
		ArtiScalesRegulation regulation = allRegulation.get(0);
		this.currentBPU = currentBPU;
		this.env = env;

		if (regulation.getArt_71() == 1 || regulation.getArt_71() == 2 ) {
			hasAlignement = true;
			this.type = AlignementType.ART7112;

		}
		
		if(regulation.getArt_71() == 3) {
			hasAlignement = true;
			this.type = AlignementType.ART713;
		}

	}

	public IGeometry[] getRightSide() {
		return getSide(ParcelBoundarySide.RIGHT);

	}

	public IGeometry[] getLeftSide() {
		return getSide(ParcelBoundarySide.LEFT);

	}
	
	public AlignementType getType() {
		return type;
	}

	public IGeometry[] getSideWithBuilding() {

		List<IGeometry> lGeom = new ArrayList<>();

		// For each parcel
		for (CadastralParcel cO : currentBPU.getCadastralParcels()) {
			// For each boundary
			boucleboundary: for (ParcelBoundary boundary : cO.getBoundariesByType(ParcelBoundaryType.LAT)) {

				// We check if there is some buildings near to the limit
				Collection<AbstractBuilding> buildingsSel = env.getBuildings().select(boundary.getGeom().buffer(0.1));

				// We have some buildings do they belong to the current CadastralParcel
				for (AbstractBuilding currentBuilding : buildingsSel) {
	
						// No !!! we add the geometry and go to the next parcel boundary
						if (!currentBuilding.getbPU().equals(currentBPU)) {
							lGeom.add(boundary.getGeom());
							continue boucleboundary;
					
					}

				}

			}

		}

		IGeometry[] geometryArray = new IGeometry[lGeom.size()];
		geometryArray = lGeom.toArray(geometryArray);

		return geometryArray;

	}

	private IGeometry[] getSide(ParcelBoundarySide side) {

		List<IGeometry> lGeom = new ArrayList<>();

		for (CadastralParcel cO : currentBPU.getCadastralParcels()) {
			lGeom.addAll(cO.getBoundariesBySide(side).stream().map(x -> x.getGeom()).collect(Collectors.toList()));
		}

		IGeometry[] geometryArray = new IGeometry[lGeom.size()];
		geometryArray = lGeom.toArray(geometryArray);

		return geometryArray;
	}

	public boolean getHasAlignement() {
		// TODO Auto-generated method stub
		return hasAlignement;
	}

}
