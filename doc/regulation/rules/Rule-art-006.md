# Rule- art-006 -  Distance minimale � la voirie

Valeur par d�faut du recul des constructions par rapport � la voirie en m�tres

## Mod�le de phrase

> Les b�timents ne doivent pas �tre construits � une distance inf�rieure � **{{ART_6}}** de la voirie.

## Param�tres

### ART_6

Diff�rentes valeurs sont possibles :
- **0** : Alignement du b�timent le long de la voirie
- **n** : recul impos� de **n** m�tres au minimum par rapport � la voirie
- **n1-n2** : recul impos� de **n1** m�tres au minimum et ne devant pas �tre sup�rieur � **n2**


## Explications


![Image illustrant le recul par rapport � la voirie](img/rule-art-006.png)

## Impl�mentation

La v�rification du ratio s'effectue dans la m�thode CommonPredicateArtiScales.check
