#  Regle-art-12 -  Règle de stationnement

## Modèle de phrase

> La création de nouveaux logements imposent la création d'un certain nombre de places de parking. Ce nombre est fixé par la valeur : **{{ART_12}}**.

## Paramètres

### ART_12

Désigne comment est calculé le nombre de places de parkings sur la parcelle. Les valeurs possibles sont :
- **1** : un stationnement par logement ;
- **1+2** :  2 places de stationnement par logement (dont 1 hors clôture pour les maisons individuelles) ; 1 place par logement (pour les immeubles collectifs) ;
-  **2** : deux stationnements par logement ;
- **20_1-60_2** : un stationnement pour un logement en dessous de 60m2, 2 pour les logements plus **(Cas non traité)** ;
- **99** : Non renseigné.        


## Explications

Suivant la valeur de **{{ART_12}}** un ratio **r** = (places de parking) / (logements)  est déterminé.

- **r** = 1 si  **{{ART_12}}** = **1**  ou **{{ART_12}}** = **1 + 2**
- **r** = 2 si  **{{ART_12}}** = **2**

On doit s'assurer alors que la surface de la parcelle doit pouvoir contenir la surface construite et la surface des places de parking nécessaires. La surface d'une place de parking est définie dans le fichier **parametreScenario.xml** à travers la valeur **surfPlaceParking**. Le nombre de logements est obtenu à partir de la surface de plancher du bâtiment divisé par la taille moyenne d'un logement (valeur **HousingUnitSize**).

Ainsi, vérifier le respect de cette règle revient à vérifier si l'expression suivante est vraie :

> **surfaceParcelle** > **surfaceBatie** +  ((int) **surfacePlancher**/ **HousingUnitSize**) x **r** x **surfPlaceParking**

## Implémentation

La règle est implémentée dans la classe **CommonPredicateArtiScales** et le ratio des places de parkings est défini par la méthode abstraite  **getArt12Value();**. Dans le cas où plusieurs règlements intersectent la parcelle, seul le ratio r  le plus élevé est conservé.
