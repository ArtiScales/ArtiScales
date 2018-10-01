package fr.ign.cogit.rules.regulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.geomaggr.IMultiSurface;
import fr.ign.cogit.geoxygene.api.spatial.geomprim.IOrientableSurface;
import fr.ign.cogit.geoxygene.util.conversion.AdapterFactory;
import fr.ign.cogit.simplu3d.model.IZoneRegulation;
import fr.ign.cogit.simplu3d.model.UrbaZone;

public class ArtiScalesRegulation implements IZoneRegulation {

	private final static String CSV_SEPARATOR = ",";
	
	private static Logger log = Logger.getLogger(ArtiScalesRegulation.class);

	// Les intitulés des colonnes
	private int code_imu, insee,oap, date_approbation, fonctions, top_zac, zonage_coherent, correction_zonage, typ_bande,art_3,art_4, bande, art_71, art_74, art_10_top,art_10;
	private String libelle_zone, libelle_de_base, libelle_de_dul,art_6_opt,art_6_optD, art_10_m, art_12,art_14;

	private double art_5, art_6, art_72, art_73, art_8, art_9, art_13 ;

	public ArtiScalesRegulation(String fLine, String line) {

		String[] fLineSplited = fLine.split(CSV_SEPARATOR);
		String[] lineSplited = line.split(CSV_SEPARATOR);

		for (int i = 0; i < fLineSplited.length; i++) {
			switch (fLineSplited[i].toLowerCase()) {
			case "libelle_zone":
				libelle_zone= lineSplited[i];
				break;
			case "insee":
				insee= Integer.valueOf(lineSplited[i]);
				break;
			case "libelle_de_base":
				libelle_de_base= lineSplited[i];
				break;
			case "libelle_de_dul":
				libelle_de_dul=lineSplited[i];
				break;
			case "OAP":
				oap= Integer.valueOf(lineSplited[i]);
				break;
			case "art_3":
				art_3=Integer.valueOf(lineSplited[i]);
				break;
			case "art_4":
				art_4=Integer.valueOf(lineSplited[i]);
				break;
			case "art_5":
				art_5=Double.valueOf(lineSplited[i]);
				break;
			case "art_6":
				art_6=Double.valueOf(lineSplited[i]);
				break;
			case "art_6_opt":
				art_6_opt=lineSplited[i];
				break;
			case "art_6_optD":
				art_6_optD=lineSplited[i];
				break;
			case "art_71":
				art_71=Integer.valueOf(lineSplited[i]);
				break;
			case "art_72":
				art_72=Double.valueOf(lineSplited[i]);
				break;
			case "art_73":
				art_73=Double.valueOf(lineSplited[i]);
				break;
			case "art_8":
				art_8=Double.valueOf(lineSplited[i]);
				break;
			case "art_9":
				art_9=Double.valueOf(lineSplited[i]);
				break;
			case "art_10_top":
				art_10_top=Integer.valueOf(lineSplited[i]);
				break;
			case "art_101":
				art_10 = Integer.valueOf(lineSplited[i]);
				break;
			case "art_102":
				art_10_m=lineSplited[i];
				break;
			case "art_12":
				art_12= lineSplited[i];
				break;
			case "art_13":
				art_13=Double.valueOf( lineSplited[i]);
				break;
			case "art_14":
				art_14=lineSplited[i];
				break;
			}
		}
	}

//	public ArtiScalesRegulation(int code_imu, String libelle_zone, int insee, int date_approbation, String libelle_de_base, String libelle_de_dul, int fonctions, int top_zac,
//			int zonage_coherent, int correction_zonage, int typ_bande, int bande, double art_5, double art_6, int art_71, double art_72, double art_73, int art_74, double art_8,
//			double art_9, int art_10_top, int art_10, double art_10_m, String art_12, double art_13, String art_14) {
//		super();
//		this.code_imu = code_imu;
//		this.libelle_zone = libelle_zone;
//		this.insee = insee;
//		this.date_approbation = date_approbation;
//		this.libelle_de_base = libelle_de_base;
//		this.libelle_de_dul = libelle_de_dul;
//		this.fonctions = fonctions;
//		this.top_zac = top_zac;
//		this.zonage_coherent = zonage_coherent;
//		this.correction_zonage = correction_zonage;
//		this.typ_bande = typ_bande;
//		this.bande = bande;
//		this.art_5 = art_5;
//		this.art_6 = art_6;
//		this.art_71 = art_71;
//		this.art_72 = art_72;
//		this.art_73 = art_73;
//		this.art_74 = art_74;
//		this.art_8 = art_8;
//		this.art_9 = art_9;
//		this.art_10_top = art_10_top;
//		this.art_10 = art_10;
//		this.art_10_m = art_10_m;
//		this.art_12 = art_12;
//		this.art_13 = art_13;
//		this.art_14 = art_14;
//	}

	public ArtiScalesRegulation() {

	}

//	public ArtiScalesRegulation(IFeature feat) {
//		this(returnObjTab(feat));
//
//	}

//	public static Object[] returnObjTab(IFeature feat) {
//		Object[] caract = { 0, feat.getAttribute("INSEE_1"), feat.getAttribute("TARGET_FID"), feat.getAttribute("Annee_muta"), feat.getAttribute("INSEE_1"),
//				feat.getAttribute("INSEE_1"), feat.getAttribute("FONCTION_1"), feat.getAttribute("TOP_ZAC_1"), feat.getAttribute("ZONAGE_C_1"), feat.getAttribute("CORRECTI_1"),
//				feat.getAttribute("CORRECTI_1"), feat.getAttribute("BANDE_1"), feat.getAttribute("ART_56"), feat.getAttribute("ART_67"), feat.getAttribute("ART_71_72"),
//				feat.getAttribute("ART_72_73"), feat.getAttribute("ART_73_74"), feat.getAttribute("ART_74_75"), feat.getAttribute("ART_89"), feat.getAttribute("ART_9_10"),
//				feat.getAttribute("ART_10_T_1"), feat.getAttribute("ART_10_11"), feat.getAttribute("ART_102"), feat.getAttribute("ART_12_13"), feat.getAttribute("ART_13_14"),
//				feat.getAttribute("ART_14_15") };
//		return caract;
//	}
//
//	public static Object[] returnObjTab2(IFeature feat) {
//		Object[] caract = { 0, feat.getAttribute("INSEE_1"), feat.getAttribute("TARGET_FID"), feat.getAttribute("Annee_muta"), feat.getAttribute("INSEE_1"),
//				feat.getAttribute("INSEE_1"), feat.getAttribute("FONCTION_1"), feat.getAttribute("TOP_ZAC_1"), feat.getAttribute("ZONAGE_C_1"), feat.getAttribute("CORRECTI_1"),
//				feat.getAttribute("CORRECTI_1"), feat.getAttribute("2_BANDE"), feat.getAttribute("2_ART_5"), feat.getAttribute("2_ART_6"), feat.getAttribute("2_ART_71"),
//				feat.getAttribute("2_ART_72"), feat.getAttribute("2_ART_73"), feat.getAttribute("2_ART_74"), feat.getAttribute("2_ART_8"), feat.getAttribute("2_ART_9"),
//				feat.getAttribute("2_ART_10_T"), feat.getAttribute("2_ART_10"), feat.getAttribute("2_ART_102"), feat.getAttribute("2_ART_12"), feat.getAttribute("2_ART_13"),
//				feat.getAttribute("2_ART_14") };
//		return caract;
//	}

//	public ArtiScalesRegulation(String line) {
//
//		this(line.split(CSV_SEPARATOR));
//
//	}

//	public ArtiScalesRegulation(String[] split) {
//		this(Integer.parseInt(split[0]), split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]), split[4], split[5], Integer.parseInt(split[6]),
//				Integer.parseInt(split[7]), Integer.parseInt(split[8]), Integer.parseInt(split[9]), Integer.parseInt(split[10]), Integer.parseInt(split[11]),
//				Integer.parseInt(split[12]), Double.parseDouble(split[13]), Integer.parseInt(split[14]), Double.parseDouble(split[15]), Double.parseDouble(split[16]),
//				Integer.parseInt(split[17]), Integer.parseInt(split[18]), Double.parseDouble(split[19]), Integer.parseInt(split[20]), Integer.parseInt(split[21]),
//				Double.parseDouble(split[22]), split[23], Double.parseDouble(split[24]), Double.parseDouble(split[25]));
//	}
//
//	public ArtiScalesRegulation(Object[] split) {
//		this(Integer.parseInt(split[0].toString()), split[1].toString(), Integer.parseInt(split[2].toString()), (int) Double.parseDouble(split[3].toString()), split[4].toString(),
//				split[5].toString(), (int) Double.parseDouble(split[6].toString()), (int) Double.parseDouble(split[7].toString()), (int) Double.parseDouble(split[8].toString()),
//				(int) Double.parseDouble(split[9].toString()), (int) Double.parseDouble(split[10].toString()), (int) Double.parseDouble(split[11].toString()),
//				Double.parseDouble(split[12].toString()), Double.parseDouble(split[13].toString()), (int) Double.parseDouble(split[14].toString()),
//				Double.parseDouble(split[15].toString()), Double.parseDouble(split[16].toString()), (int) Double.parseDouble(split[17].toString()),
//				(int) Double.parseDouble(split[18].toString()), Integer.getInteger(split[19].toString()), (int) Double.parseDouble(split[20].toString()),
//				(int) Double.parseDouble(split[21].toString()), Double.parseDouble(split[22].toString()), split[23].toString(),
//				Double.parseDouble(split[24].toString()), Double.parseDouble(split[25].toString()));
//	}

	/**
	 * Charge les règlements et les stockes dans une Map avec Integer = Code_Imu et List<Regulation> = la liste des règlements (= lignes du tableau) pour un code IMU donné
	 * utilisation avec les objets de maxou
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ArtiScalesRegulation> loadRegulationSet(File file) throws IOException {

		// On initialise la map
		Map<String, ArtiScalesRegulation> table = new Hashtable<>();

		if (!file.exists()) {
			return table;
		}

		// On lit le fichier
		BufferedReader in = new BufferedReader(new FileReader(file));
		
		//La première ligne est la plus importante 
		String fLine = in.readLine();
		String line ="";
		// On traite chaque ligne
		while ((line = in.readLine()) != null) {
			log.info(line);
			// On instancier la réglementation
			ArtiScalesRegulation r = new ArtiScalesRegulation(fLine,line);
			// On regarde si le code imu a été rencontré auparavant
			if (r != null) {
				table.put(r.getLibelle_de_dul(), r);
			}

		}

		in.close();

		return table;
	}
	
	/**
	 * Charge les règlements et les stockes dans une Map avec Integer = Code_Imu et List<Regulation> = la liste des règlements (= lignes du tableau) pour un code IMU donné
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static Map<String, ArtiScalesRegulation> loadRegulationSet(String file) throws IOException {

		// On initialise la map
		Map<String, ArtiScalesRegulation> table = new Hashtable<>();

		// On charge le fichier CSV avec modèle IAU
		File f = new File(file);
		if (!f.exists()) {
			return table;
		}

		// On lit le fichier
		BufferedReader in = new BufferedReader(new FileReader(f));
		// On saute la première ligne car c'est une en-tête
		String fLine = in.readLine();
		String line;
		// On traite chaque ligne
		while ((line = in.readLine()) != null) {
		
			log.info(line);
			// On instancier la réglementation
			ArtiScalesRegulation r = new ArtiScalesRegulation(fLine,line);

			// On regarde si le code imu a été rencontré auparavant

			if (r != null) {

				table.put(r.getLibelle_de_dul(), r);
			}

		}

		in.close();

		return table;
	}

	public int getCode_imu() {
		return code_imu;
	}

	public String getLibelle_zone() {
		return libelle_zone;
	}

	// INSEE Code de la commune
	public int getInsee() {
		return insee;
	}

	// DATE_DUL Date d'approbation du PLU
	public int getDate_approbation() {
		return date_approbation;
	}

	// LIBELLE_DE_BASE Type de zone
	public String getLibelle_de_base() {
		return libelle_de_base;
	}

	// LIBELLE_DE_DUL Type de zone
	public String getLibelle_de_dul() {
		return libelle_de_dul;
	}

	// FONCTIONS Fonction de la zone 0 : logements uniquement ; 1 : activité
	// mais possibilité de logements ; 2 : exclusivement activité
	public int getFonctions() {
		return fonctions;
	}

	// TOP_ZAC Présence d'une ZAC 0 : NON // 1 : OUI
	public int getTop_zac() {
		return top_zac;
	}

	// ZONAGE_COHERENT Zonage cohérent entre la base et le plan de zonage du DUL
	// 0 : NON // 1 : OUI
	public int getZonage_coherent() {
		return zonage_coherent;
	}

	// CORRECTION_ZONAGE Indicateur du zonage à faire prévaloir en cas
	// d'incohérence des zonages 0 : Conserver le dessin de zonage de Carto PLU
	// 1 : Numeriser le zonage du PLU analysé
	public int getCorrection_zonage() {
		return correction_zonage;
	}

	// TYP_BANDE Information concernant l'existence d'une bande principale ou
	// secondaire 0 : pas de bande // 1 : principale // 2 : secondaire
	public int getTyp_bande() {
		return typ_bande;
	}

	// BANDE Profondeur de la bande principale x > 0 profondeur de la bande par
	// rapport à la voirie
	public int getBande() {
		return bande;
	}

	//SERVITUDE DE PASSAGE 1 : La parcelle qui n’a pas acces à la voirie doit mettre à disposition une servitude de passage
	//99 : rien
	public int getArt_3() {
		return art_3;
	}

	//RACCORDEMENT À L'ASSAINISSEMENT 77 : info pas disponible
	//0 : non réglementé
	//1 : doit être raccordé au réseau collectif
	//20 : doit être dans une zone où l’assainissement individuel est autorisées
	public int getArt_4() {
		return art_4;
	}
	
	// ART_5 Superficie minimale 88= non renseignable, 99= non réglementé

	public double getArt_5() {
		return art_5;
	}

	// ART_6 Distance minimale des constructions par rapport à la voirie imposée
	// en mètre
	// 55= alignement obligatoire OU distance => Art_6_optD 88= non renseignable, 99= non réglementé
	// 44 = en fonction des bâtiments de l'autre coté (règle du RNU)
	public double getArt_6() {
		return art_6;
	}

	public String getArt_6_optD() {
		return art_6_optD;
	}

	public String getArt_6_opt() {
		return art_6_opt;
	}

	// ART_71 Implantation en limite séparative 0 : non, retrait imposé (cf.72)
	// // 1 : Oui //2:Oui, ou retrait imposé //TODO 3 : oui seulement si un bâtiment est déjà en limite de propriété
	// 4 : Oui, mais sur un côté seulement
	public int getArt_71() {
		return art_71;
	}

	// ART_72 Distance minimale des constructions par rapport aux limites
	// séparatives imposée en mètre 88= non renseignable, 99= non réglementé
	public double getArt_72() {
		return art_72;
	}

	// ART_73 Distance minimale des constructions par rapport à la limte
	// séparative de fond de parcelle 88= non renseignable, 99= non réglementé
	public double getArt_73() {
		return art_73;
	}

	// ART_74 Distance minimum des constructions par rapport aux limites
	// séparatives, exprimée par rapport à la hauteur du bâtiment 0 : NON // 1 :
	// Retrait égal à la hauteur // 2 : Retrait égal à la hauteur divisé par
	// deux // 3 : Retrait égal à la hauteur divisé par trois // 4 : Retrait
	// égal à la hauteur divisé par quatre // 5 : Retrait égal à la hauteur
	// divisé par cinq // 6 : Retrait égal à la hauteur divisé par deux moins
	// trois mètres // 7 : Retrait égal à la hauteur moins trois mètres divisé
	// par deux // 8 : retrait égal à la hauteur divisé par deux moins un mètre
	// // 9 : retrait égal aux deux tiers de la hauteur // 10 : retrait égal aux
	// trois quarts de la hauteur
	public int getArt_74() {
		return art_74;
	}

	// ART_8 Distance minimale des constructions par rapport aux autres sur une
	// même propriété imposée en mètre 88= non renseignable, 99= non réglementé
	public double getArt_8() {
		return art_8;
	}

	// ART_9 Pourcentage d'emprise au sol maximum autorisé Valeur comprise de 0
	// à 1, 88= non renseignable, 99= non réglementé
	public double getArt_9() {
		return art_9;
	}

	// ART_10_TOP Indicateur de l'unité de mesure de la hauteur du batiment
	// 1_niveaux ; 2_metres du sol au point le plus haut du batiment ; 3_hauteur
	// plafond ; 4_ point le plus haut ; 5_Hauteur de façade à l'égout, TODO 6 a ne
	// pas dépasser les batiments des parcelles adjacentes, sinon hauteur à l'égout
	// TODO 7 a ne pas dépasser les batiments des parcelles adjacentes, sinon
	// hauteur entre les points //TODO 8 doit être de la hauteur des batiments des
	// parcelles adjacentes :
	// TODO 20: spécial si sur limites séparatives : 20-v6-a4,5 signifie qu'elle ne
	// deva pas dépasser 6m du point le plus bas au point le plus haut en limite de
	// voirie, et 4,5m en limite séparative
	// 88= non renseignable ; 99= non règlementé
	public int getArt_10_top() {
		return art_10_top;
	}

	// ART_101 Hauteur maximum autorisée 88= non renseignable, 99= non
	// réglementé
	public int getArt_10() {
		return art_10;
	}

	// ART_102 Hauteur maximum autorisée En mètres
	public String getArt_10_m() {
		return art_10_m;
	}

	// ART_12 Nombre de places par logement 88= non renseignable, 99= non
	// réglementé
	public String getArt_12() {
		return art_12;
	}

	// ART_13 Part minimale d'espaces libre de toute construction exprimée par
	// rapport à la surface totale de la parcelle Valeur comprise de 0 à 1, 88
	// si non renseignable, 99 si non règlementé
	public double getArt_13() {
		return art_13;
	}

	// ART_14 Coefficient d'occupation du sol 88= non renseignable, 99= non
	// réglementé
	public String getArt_14() {
		return art_14;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Regulation [code_imu=" + code_imu + ", insee=" + insee + ", date_approbation=" + date_approbation + ", fonctions=" + fonctions + ", top_zac=" + top_zac
				+ ", zonage_coherent=" + zonage_coherent + ", correction_zonage=" + correction_zonage + ", typ_bande=" + typ_bande + ", bande=" + bande + ", art_5=" + art_5
				+ ", art_6=" + art_6 + ", art_71=" + art_71 + ", art_72=" + art_72 + ", art_73=" + art_73 + ", art_74=" + art_74 + ", art_8=" + art_8 + ", art_9=" + art_9
				+ ", art_10_top=" + art_10_top + ", art_10=" + art_10 + ", art_13=" + art_13 + ", libelle_zone=" + libelle_zone + ", libelle_de_base=" + libelle_de_base
				+ ", libelle_de_dul=" + libelle_de_dul + ", art_9=" + art_9 + ", art_10_m=" + art_10_m + ", art_12=" + art_12 + ", art_14=" + art_14 + "]";
	}

	IMultiSurface<IOrientableSurface> geomBande = null;

	/**
	 * @return the geomBande
	 */
	public IMultiSurface<IOrientableSurface> getGeomBande() {
		return geomBande;
	}

	public Geometry getEpsilonBuffer() {

		if (epsilonBuffer == null) {
			epsilonBuffer = this.getJTSBand().buffer(0.5);
		}

		return epsilonBuffer;
	}

	Geometry epsilonBuffer = null;

	/**
	 * @param geomBande
	 *            the geomBande to set
	 */
	public void setGeomBande(IMultiSurface<IOrientableSurface> geomBande) {
		this.geomBande = geomBande;
		this.jtsGeometry = null;
		this.epsilonBuffer = null;
	}

	Geometry jtsGeometry = null;

	private static GeometryFactory gf = new GeometryFactory();

	public Geometry getJTSBand() {

		if (jtsGeometry == null) {
			try {
				jtsGeometry = AdapterFactory.toGeometry(gf, this.getGeomBande());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return jtsGeometry;
	}

	private UrbaZone zone;

	@Override
	public UrbaZone getUrbaZone() {
		return zone;
	}

	public void setUrbaZone(UrbaZone zone) {
		this.zone = zone;
	}

	@Override
	public String toText() {
		return this.toString();
	}

	/**
	 * Replace some fake value by value used in simulator
	 */
	public void clean() {

		if (this.getArt_6() == 77) {
			this.art_6 = 0;
		}

		if (this.getArt_8() == 88.0 || this.getArt_8() == 99.0) {
			this.art_8 = 50;
		}

		if (this.art_9 == 99) {
			this.art_9 = 1;
		}
	}

	public int getOap() {
		return oap;
	}

}
