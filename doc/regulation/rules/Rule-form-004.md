# Rule-form-004 - Largeur maximale d'un bâtiment simulé

## Définition

>  Le simulateur ne peut autoriser la construction d'un bâtiment d'une largeur supérieure à **{{largeur}}** m et se faisant de l'ombre sur lui même en évitant les vis-à-vis à moins de **{{d_vis_a_vis}}** m.

## Paramètres

- **{{largeur]}** :  si les intersections entre boîtes sont autorisées (paramètre **{{intersection}}**) la largeur de l'intersection ne doit pas dépasser **{{largeur]}** m.
- **{{d_vis_a_vis}}** : de plus, une distance de  **{{d_vis_a_vis}}** m doit être préservée entre les parties extérieures du bâtiment afin d'éviter qu'il ne se fasse de l'ombre à lui même.



## Explications

Cette contrainte a été implémentée pour produire des bâtiments réalistes dans le sens où ils permettent l'accessibilité de la lumière dans le centre du bâtiment.


![Contraintes portant sur les bâtiments pour assurer une accessibilité à la lumière](img/rule-form-004.png)





## Implémentation
Les valeurs des paramètres **{{largeur]}** et **{{d_vis_a_vis}}** sont implémentées en dur dans CommonPredicateArtiScales et ont été suggérées par l'IAUIDF (respectivement 15 m et 10 m).
