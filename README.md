# AgriWatch — Supervision Météo & Alerte Agricole

Application Java Swing de supervision agricole en temps réel.  
Simule 5 capteurs météo (zones A à E) avec détection d'alerte irrigation (humidité < 30%).

---

## Architecture

```
cm.uy1.agriwatch
├── core/              Modèle métier + pattern Observer
│   ├── Zone.java              Enum ZONE_A → ZONE_E
│   ├── MeteoListener.java     Interface observer
│   ├── MesureMeteo.java       POJO (zone, température, humidité, timestamp)
│   ├── CapteurMeteo.java      Runnable générant des mesures aléatoires
│   └── CentraleMeteo.java     Agrégateur : historique + redistribution
├── persistence/       Sauvegarde & export
│   └── PersistanceService.java   Sérialisation .ser + export CSV (TODO)
├── ui/                Interface Swing
│   ├── MeteoTableModel.java        AbstractTableModel + statistiques
│   ├── MeteoTableCellRenderer.java Coloration rouge si alerte
│   └── FenetrePrincipale.java      JFrame principale (BorderLayout)
├── threading/         Gestion des threads capteurs
│   └── CapteurManager.java     Démarre/arrête 5 threads
├── Main.java                  Point d'entrée, câblage DI manuel
├── TestConsole.java           Test console 10s (pattern Observer)
└── TestTableModel.java        Tests unitaires du modèle de tableau
```

---

## Tailles de police actuelles (FenetrePrincipale.java)

| Élément | Police | Taille |
|---------|--------|--------|
| Titre application | SansSerif BOLD | **36px** |
| Horloge temps réel | Monospaced BOLD | **32px** |
| Alerte clignotante | SansSerif BOLD | **42px** |
| Titre « Statistiques plantation » | SansSerif BOLD | **32px** |
| Labels statistiques | SansSerif PLAIN | **26px** |
| Données du tableau | SansSerif PLAIN | **26px** |
| En-têtes du tableau | SansSerif BOLD | **22px** |
| Boutons (Démarrer, Arrêter, Exporter) | SansSerif BOLD | **22px** |

### Dimensions

| Propriété | Valeur |
|-----------|--------|
| Taille fenêtre par défaut | 1300 × 800 |
| Taille minimum | 1000 × 650 |
| Hauteur lignes tableau | 52px |
| Largeurs colonnes | 180 / 200 / 220 / 200 |

---

## Compilation & exécution

```bash
# Compiler
javac -d out -sourcepath src/main/java src/main/java/cm/uy1/agriwatch/Main.java

# Exécuter l'interface graphique
java -cp out cm.uy1.agriwatch.Main

# Test console (10 secondes)
java -cp out cm.uy1.agriwatch.TestConsole

# Test du modèle de tableau
java -cp out cm.uy1.agriwatch.TestTableModel
```

---

## État du projet

| Module | Statut |
|--------|--------|
| Core (capteurs, mesures, Observer) | ✅ Terminé |
| UI (tableau, renderer, fenêtre) | ✅ Terminé |
| Threading (gestion threads) | ✅ Basique (améliorable avec ExecutorService) |
| Persistance (.ser + CSV) | ❌ Coquille vide |
| Tests | ✅ Console + TableModel |

---

Projet pédagogique — ICT308, Université de Yaoundé 1.
