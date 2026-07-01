# Modifications apportées au dossier UI (`src/main/java/cm/uy1/agriwatch/ui`)

Ce document récapitule l'ensemble des modifications apportées aux classes de l'interface graphique (dossier `ui`) du projet **AgriWatch**. Il couvre les modifications fonctionnelles récentes 


## A. Structure Globale du Dossier UI

Le dossier `ui` contient la refonte complète de l'interface sous un thème clair moderne de style SaaS Web. Voici le rôle de chaque classe modifiée/créée :

| Classe / Composant | Rôle et Responsabilité |
| :--- | :--- |
| **[FenetrePrincipale.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/FenetrePrincipale.java)** | Fenêtre principale de l'application. Gère la navigation par onglets (`CardLayout`), l'agencement du dashboard, le contrôle global des capteurs (démarrer/arrêter tout) et l'écoute des événements météo. |
| **[SessionReportDialog.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/SessionReportDialog.java)** | Boîte de dialogue modale qui s'affiche automatiquement à l'arrêt de la simulation pour présenter le bilan sous forme de KPI (mesures totales, moyennes de température et d'humidité, nombre d'alertes). |
| **[RealTimeChartPanel.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/RealTimeChartPanel.java)** | Composant personnalisé de tracé 2D pour afficher les graphiques en temps réel. Supporte le mode mono-zone ainsi que le mode comparatif multi-zones. |
| **[RoundedCardPanel.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/RoundedCardPanel.java)** | Conteneur graphique réutilisable offrant des bordures arrondies et une ombre douce pour une esthétique moderne. |
| **[ToggleSwitch.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/ToggleSwitch.java)** | Commutateur graphique interactif (on/off) pour activer/désactiver le signal sonore des alertes. |
| **[StatusDot.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/StatusDot.java)** | Petit indicateur lumineux (vert/rouge) représentant le statut en ligne ou hors ligne d'un capteur. |
| **[MeteoTableModel.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/MeteoTableModel.java)** | Modèle de données pour le tableau de synthèse instantané du dashboard. |
| **[HistoriqueTableModel.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/HistoriqueTableModel.java)** | Modèle de données avec filtres intégrés (par zone ou alertes) pour l'affichage du journal d'historique. |
| **[MeteoTableCellRenderer.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/MeteoTableCellRenderer.java)** | Gestionnaire d'affichage personnalisé pour mettre en valeur visuellement les alertes dans les cellules des tableaux. |
| **[ZoneCardPanel.java](file:///home/stalker/Musique/JAVA/agriwatch-master/src/main/java/cm/uy1/agriwatch/ui/ZoneCardPanel.java)** | Composant d'affichage détaillé par zone météo individuelle. |
