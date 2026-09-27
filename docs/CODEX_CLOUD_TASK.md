# Codex Cloud — tâche maître MedTracker MVP

Construis le MVP complet de MedTracker dans ce dépôt.

Lis d'abord intégralement :
- `AGENTS.md`
- `docs/MVP_SPEC.md`

## Objectif

Livrer une application Android native, locale/offline, permettant de :
- saisir un traitement comme une ordonnance ;
- configurer des prises nommées avec horaires fixes ;
- configurer un intervalle minimal ;
- utiliser un mode « intervalle depuis la dernière prise » ;
- voir les prises prévues aujourd'hui ;
- confirmer une prise réelle, notamment une « prise du matin » ;
- enregistrer l'heure réelle séparément de l'heure prévue ;
- corriger ou annuler une prise ;
- consulter l'historique ;
- recevoir des notifications locales ;
- produire un APK debug via GitHub Actions.

## Contraintes

- Kotlin + Jetpack Compose + Material 3.
- Room pour la persistance.
- Architecture simple, testable, sans backend.
- Pas de compte utilisateur.
- Pas d'analytics.
- Aucune donnée médicale réelle dans le dépôt.
- Aucune recommandation médicale.
- Ne jamais inventer une dose, un intervalle ou un horaire.
- Respecter exactement les règles de `AGENTS.md`.

## Méthode demandée

1. Inspecte le dépôt et initialise un projet Android moderne.
2. Implémente d'abord le modèle métier et le moteur de planning pur.
3. Écris les tests unitaires du moteur avant ou en même temps que l'UI.
4. Implémente Room et les repositories.
5. Implémente les écrans :
   - Aujourd'hui
   - Traitements
   - Historique
   - Réglages
6. Implémente la création/édition d'un traitement.
7. Implémente la confirmation/correction/annulation d'une prise.
8. Implémente les notifications locales avec gestion propre des permissions Android.
9. Ajoute la CI GitHub Actions avec lint, tests et APK debug artifact.
10. Exécute tous les tests et corrige les erreurs avant de terminer.

## Exigences UX

L'écran Aujourd'hui doit être immédiatement utile.

Une occurrence doit montrer :
- heure prévue ;
- libellé (Matin, Midi, Soir, etc.) ;
- médicament ;
- dose textuelle ;
- statut ;
- action « Marquer comme prise ».

Après confirmation :
- afficher « Pris à HH:mm » ;
- offrir « Corriger l'heure » ;
- offrir « Annuler ».

Le flux principal doit demander le moins de taps possible.

## Règles de scheduling essentielles

### Horaires fixes
Une prise tardive ne déplace jamais les prochains horaires fixes.

### Intervalle depuis dernière prise
Le prochain horaire est calculé depuis `takenAt + intervalle saisi par l'utilisateur`.

### Horaires fixes + intervalle minimal
Afficher un avertissement factuel si l'espacement calculé est inférieur à l'intervalle configuré, sans conseil ni diagnostic médical.

## Définition de terminé

Ne considère pas la tâche terminée tant que :
- le projet compile ;
- les tests unitaires passent ;
- lint passe ;
- la CI est ajoutée ;
- l'APK debug peut être produit ;
- aucun TODO bloquant ne reste sur le flux principal ;
- le README explique comment lancer/build/tester le projet.

À la fin, fournis un résumé précis :
- architecture choisie ;
- fichiers majeurs ;
- fonctionnalités livrées ;
- tests exécutés ;
- limitations restantes ;
- instructions pour récupérer l'APK.
