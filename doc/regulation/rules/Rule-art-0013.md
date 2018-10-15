#  Regle-art-13 - Part minimale d'espace libre

## Modèle de phrase

> La part d'espace libre dans la parcelle représente au minimum **{{ART_13}}** fois l'aire de la parcelle.

## Paramètres

### ART_13

Désigne le ratio minimum d'espace libre au sein de la parcelle. Valeur entre 0 et 1.

Remarque : Si valeur exprimée en %, convertir en ratio. Ex : 5%= 5/100 = 0,05 ; 85%= 85/100 = 0.85

## Explications

![Image montrant la contrainte de hauteur maximale d'un bâtiment](img/rule-art-013.png)

## Implémentation

La méthode est implémentée en même temps que l'article 9 dans la méthode getCesMax(). Ainsi, on prend le règlement le plus contraignant entre l'article 9 et l'article 13.
