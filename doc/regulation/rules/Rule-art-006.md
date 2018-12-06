# Rule- art-006 -  Distance minimale à la voirie

Valeur par défaut du recul des constructions par rapport à la voirie en mètres

## Modèle de phrase

> Les bâtiments ne doivent pas être construits à une distance inférieure à **{{ART_6}}** de la voirie.

## Paramètres

### ART_6

Différentes valeurs sont possibles :
- **0** : Alignement du bâtiment le long de la voirie
- **n** : recul imposé de **n** mètres au minimum par rapport à la voirie
- **n1-n2** : recul imposé de **n1** mètres au minimum et ne devant pas être supérieur à **n2**


## Explications


![Image illustrant le recul par rapport à la voirie](img/rule-art-006.png)

## Implémentation

La vérification du ratio s'effectue dans la méthode CommonPredicateArtiScales.check
