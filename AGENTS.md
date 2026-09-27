# AGENTS.md — MedTracker

## Mission

Construire un MVP Android fiable, local-first et simple pour suivre des prises de médicaments prescrites.

## Stack cible

Préférer une application Android native moderne :
- Kotlin
- Jetpack Compose
- Material 3
- Room pour le stockage local
- Coroutines / Flow
- APIs Android de notifications
- architecture simple, testable et maintenable

Éviter un backend ou un compte utilisateur dans le MVP.

## Règles produit obligatoires

1. L'application ne donne **aucun conseil médical**.
2. Elle ne déduit, ne modifie et ne recommande jamais une dose ou un horaire.
3. Toute règle de prise (horaire, quantité textuelle, intervalle minimal, durée) vient de l'utilisateur.
4. Une prise prévue et une prise réelle sont deux concepts différents.
5. Une prise réelle conserve son timestamp réel et peut être corrigée.
6. Une prise planifiée peut avoir un libellé comme Matin, Midi, Soir, Coucher ou un libellé libre.
7. Les horaires fixes ne doivent pas être déplacés automatiquement après une prise tardive.
8. Si un traitement est configuré en mode intervalle à partir de la dernière prise, le calcul doit se baser uniquement sur l'intervalle saisi par l'utilisateur.
9. Si une prise intervient avant l'intervalle minimal saisi, afficher un avertissement factuel basé sur la configuration utilisateur, sans qualifier la situation médicalement.
10. Ne jamais bloquer l'accès à l'historique ou aux données lorsque les notifications sont refusées.

## Données sensibles

Le dépôt est public.
- Ne jamais committer de données médicales réelles.
- Utiliser uniquement des noms de médicaments fictifs dans les tests et screenshots.
- Ne pas ajouter d'analytics, télémétrie ou tracking tiers au MVP.
- Les données utilisateur doivent rester locales à l'appareil.

## UX MVP

L'écran d'accueil doit répondre immédiatement à :
- Qu'est-ce qui est prévu aujourd'hui ?
- Qu'est-ce qui a déjà été pris ?
- Quelle est la prochaine prise ?
- Puis-je confirmer la prise actuelle en un geste ?

L'action principale d'une occurrence planifiée doit être explicite : « Marquer comme prise ».

Après confirmation :
- afficher « Pris à HH:mm » ;
- permettre Annuler ;
- permettre Corriger l'heure.

## Modèle métier attendu

Au minimum :
- Medication
- Treatment
- ScheduleRule
- ScheduledDose
- IntakeEvent

Ne pas stocker l'état « pris » uniquement sur la dose planifiée : conserver un événement de prise séparé pour préserver l'historique.

## Qualité

Exiger :
- tests unitaires du moteur de planning ;
- tests des règles horaires fixes vs intervalle ;
- tests de correction/annulation de prise ;
- tests de persistance Room ;
- build Android automatisé dans GitHub Actions ;
- lint/tests dans la CI.

## Hors scope MVP

- backend cloud
- synchronisation multi-appareils
- comptes
- partage médecin/pharmacie
- OCR d'ordonnance
- reconnaissance automatique de médicaments
- interactions médicamenteuses
- recommandations de doses
- diagnostic
- IA médicale
- widgets complexes
- intégration wearables

## Définition de fini

Le MVP est fini lorsqu'un utilisateur peut :
1. créer un traitement ;
2. renseigner nom/libellé, dose textuelle, dates et règles de prise ;
3. saisir des horaires fixes et/ou un intervalle minimal selon le mode choisi ;
4. voir les occurrences de la journée ;
5. confirmer « la prise du matin » ou toute autre occurrence ;
6. enregistrer/corriger/annuler l'heure réelle ;
7. recevoir une notification locale ;
8. consulter l'historique ;
9. fermer et rouvrir l'application sans perte de données ;
10. construire un APK debug via CI.
