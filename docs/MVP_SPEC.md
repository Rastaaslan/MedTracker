# MedTracker — Spécification MVP

## 1. But

Créer une application Android locale permettant de suivre fidèlement un traitement saisi par l'utilisateur, sous une forme proche d'une ordonnance :
- médicament / traitement ;
- dose textuelle ;
- dates de début et de fin ;
- prises nommées (Matin, Midi, Soir, Coucher ou libre) ;
- horaires prévus ;
- intervalle minimal éventuel entre prises ;
- confirmation de la prise réelle ;
- notifications locales ;
- historique.

L'application suit les données saisies. Elle ne remplace pas un professionnel de santé et ne doit jamais proposer de modification de traitement.

---

## 2. Parcours principal

### 2.1 Créer un traitement

Champs MVP :
- nom du médicament ou libellé libre ;
- dosage/libellé de prise en texte libre, ex. « 1 comprimé » ou « 1/2 comprimé » ;
- date de début ;
- date de fin optionnelle ;
- notes optionnelles ;
- mode de planification ;
- règles de prise.

### 2.2 Modes de planification

#### A. Horaires fixes

Exemple fictif :
- Matin — 08:00
- Midi — 13:00
- Soir — 20:00

Chaque occurrence conserve son horaire théorique, même si la prise précédente a été enregistrée en retard.

#### B. Intervalle depuis la dernière prise

L'utilisateur saisit explicitement un intervalle, par exemple « 8 h ».

Le prochain créneau calculé est :
`heure de la dernière prise réelle + intervalle saisi`.

Aucune valeur par défaut médicale ne doit être inventée.

#### C. Horaires fixes + intervalle minimal

Des horaires fixes peuvent également comporter un intervalle minimal saisi par l'utilisateur.

Exemple :
- prise prévue : 20:00 ;
- intervalle minimal configuré : 6 h ;
- dernière prise réelle : 16:30.

L'application peut afficher factuellement :
« Selon l'intervalle de 6 h que vous avez configuré, cette prise est espacée de 3 h 30. »

Ne pas afficher :
- « dangereux » ;
- « surdosage » ;
- « vous devez attendre » ;
- ou toute autre conclusion médicale.

L'utilisateur doit pouvoir corriger une erreur d'enregistrement.

---

## 3. Accueil / Aujourd'hui

L'écran principal affiche les occurrences du jour, regroupées chronologiquement.

Chaque carte affiche :
- médicament ;
- dose textuelle ;
- libellé de prise ;
- heure prévue ;
- état.

États minimum :
- À venir
- À prendre
- Pris
- Non enregistré

Une prise confirmée affiche :
`Pris à HH:mm`.

Actions :
- Marquer comme prise ;
- Corriger l'heure ;
- Annuler la prise.

Exemple :

```
Aujourd'hui

08:00 — Matin
Médicament Exemple — 1 comprimé
[ Marquer comme prise ]

13:00 — Midi
Médicament Exemple — 1 comprimé
À venir
```

---

## 4. Enregistrer une prise

Lorsqu'une occurrence est confirmée :
- créer un `IntakeEvent` séparé ;
- enregistrer la date/heure réelle ;
- relier l'événement à l'occurrence prévue si elle existe ;
- ne jamais écraser l'horaire théorique.

L'utilisateur peut :
- accepter l'heure actuelle ;
- choisir une heure passée ;
- corriger l'heure ;
- annuler l'événement.

Un historique de modifications complet n'est pas exigé en MVP, mais le modèle ne doit pas empêcher son ajout ultérieur.

---

## 5. Historique

Vue par jour.

Afficher :
- prises prévues ;
- prises enregistrées ;
- heure prévue ;
- heure réelle ;
- prises manquées/non enregistrées.

Aucune interprétation médicale.

---

## 6. Notifications

Notifications locales uniquement.

Pour chaque occurrence fixe active :
- programmer une notification à l'heure prévue ;
- permettre l'ouverture directe de l'occurrence concernée.

Pour le mode intervalle :
- recalculer la prochaine notification après une prise réelle, uniquement à partir de l'intervalle utilisateur.

Si Android refuse une permission liée aux notifications ou aux alarmes exactes :
- l'application reste totalement utilisable ;
- afficher clairement que les rappels peuvent ne pas être exacts ou ne pas fonctionner ;
- ne jamais perdre les données.

Après reboot du téléphone ou mise à jour du planning :
- restaurer/reprogrammer les notifications nécessaires.

---

## 7. Modèle de données

### Medication

```text
id
name
optionalNotes
createdAt
updatedAt
```

### Treatment

```text
id
medicationId
doseText
startDate
endDate?
scheduleMode
minIntervalMinutes?
active
createdAt
updatedAt
```

### ScheduleRule

```text
id
treatmentId
label
timeOfDay?
sortOrder
enabled
```

### ScheduledDose

Peut être calculée dynamiquement ou matérialisée selon l'architecture retenue.

Doit représenter :
```text
treatmentId
scheduleRuleId?
scheduledAt
label
```

### IntakeEvent

```text
id
treatmentId
scheduledDoseKey?
takenAt
createdAt
updatedAt
```

---

## 8. Navigation MVP

Navigation recommandée :

```
Aujourd'hui
Traitements
Historique
Réglages
```

### Aujourd'hui
Planning et confirmation rapide.

### Traitements
Liste, ajout, modification, archivage.

### Historique
Consultation journalière.

### Réglages
Notifications, préférence horaire 24 h, informations de confidentialité.

---

## 9. UX

Principes :
- gros boutons tactiles ;
- très peu d'étapes pour confirmer une prise ;
- fonctionnement évident sans tutoriel ;
- design Material 3 ;
- clair/sombre via thème système ;
- états visuels lisibles ;
- ne pas utiliser la couleur seule pour transmettre un état.

L'application doit être utilisable facilement d'une seule main.

---

## 10. Architecture technique

Cible recommandée :
- Kotlin récent ;
- Jetpack Compose ;
- Material 3 ;
- Room ;
- ViewModel ;
- Coroutines / StateFlow ;
- couche repository ;
- moteur de planification pur/testable séparé d'Android ;
- abstraction `ReminderScheduler` ;
- AlarmManager/notifications Android selon les capacités et permissions disponibles.

Structure indicative :

```
app/
  data/
    db/
    dao/
    entity/
    repository/
  domain/
    model/
    scheduling/
    usecase/
  notifications/
  ui/
    today/
    treatments/
    history/
    settings/
  navigation/
```

Éviter la surarchitecture. Un seul module Android est suffisant pour le MVP.

---

## 11. Tests obligatoires

### Scheduling
- horaires fixes ;
- prise en retard ne déplace pas les horaires fixes ;
- intervalle depuis prise réelle ;
- changement d'heure ;
- changement de jour ;
- date de fin ;
- traitement inactif ;
- heure d'été / timezone locale si pertinent.

### Intake
- créer une prise ;
- corriger une prise ;
- annuler une prise ;
- prise liée à une occurrence.

### Persistence
- traitements persistés ;
- événements persistés ;
- migration DB initiale testable.

### UI
Au minimum quelques tests ciblés sur :
- écran Aujourd'hui ;
- confirmation d'une prise ;
- création d'un traitement.

---

## 12. CI

GitHub Actions doit :
1. configurer Java/Gradle ;
2. exécuter lint ;
3. exécuter les tests unitaires ;
4. construire l'APK debug ;
5. publier l'APK debug comme artifact de workflow.

Aucun secret médical ou donnée utilisateur dans la CI.

---

## 13. Hors scope MVP

- cloud sync ;
- authentification ;
- backend ;
- export PDF médical ;
- interaction médicamenteuse ;
- base pharmaceutique ;
- scan d'ordonnance ;
- OCR ;
- IA ;
- recommandations ;
- calcul de dose ;
- contact médecin/pharmacien ;
- statistiques médicales ;
- suivi de symptômes.

---

## 14. Critères d'acceptation MVP

Le MVP est accepté si :

1. L'application compile en APK Android.
2. L'utilisateur peut créer/modifier/archiver un traitement.
3. Il peut configurer des prises nommées avec horaires.
4. Il peut saisir un intervalle minimal.
5. Il peut choisir un mode intervalle depuis la dernière prise.
6. L'écran Aujourd'hui affiche correctement les occurrences.
7. Il peut appuyer sur « Marquer comme prise ».
8. L'heure réelle est enregistrée séparément de l'heure prévue.
9. Il peut corriger ou annuler la prise.
10. L'historique reste correct après redémarrage de l'application.
11. Les notifications locales fonctionnent avec gestion propre des permissions.
12. Les tests du moteur de planning sont verts.
13. La CI produit un APK debug téléchargeable.
14. Aucune donnée médicale réelle n'est présente dans le dépôt.
