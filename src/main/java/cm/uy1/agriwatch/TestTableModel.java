package cm.uy1.agriwatch;

import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;
import cm.uy1.agriwatch.ui.MeteoTableModel;

/**
 * Test unitaire du MeteoTableModel (ne nécessite pas d'affichage graphique).
 */
public class TestTableModel {
    public static void main(String[] args) {
        MeteoTableModel model = new MeteoTableModel();
        int erreurs = 0;

        // Test 1 : modèle vide
        if (model.getRowCount() != 0) {
            System.out.println("❌ Test 1 échoué : rowCount devrait être 0");
            erreurs++;
        } else {
            System.out.println("✅ Test 1 : modèle vide OK");
        }

        // Test 2 : ajout de mesures (une par zone)
        model.mettreAJour(new MesureMeteo(Zone.ZONE_A, 30.0, 65.0));
        model.mettreAJour(new MesureMeteo(Zone.ZONE_B, 28.0, 25.0));  // alerte
        model.mettreAJour(new MesureMeteo(Zone.ZONE_C, 32.0, 70.0));
        model.mettreAJour(new MesureMeteo(Zone.ZONE_D, 35.0, 18.0));  // alerte
        model.mettreAJour(new MesureMeteo(Zone.ZONE_E, 27.0, 55.0));

        if (model.getRowCount() != 5) {
            System.out.println("❌ Test 2 échoué : rowCount=" + model.getRowCount());
            erreurs++;
        } else {
            System.out.println("✅ Test 2 : 5 zones insérées OK");
        }

        // Test 3 : contenu des colonnes
        Object val = model.getValueAt(0, 0);  // Zone
        if (!"ZONE_A".equals(val)) {
            System.out.println("❌ Test 3a échoué : zone=" + val);
            erreurs++;
        }
        val = model.getValueAt(1, 3);  // Statut alerte
        if (!"⚠ ALERTE".equals(val)) {
            System.out.println("❌ Test 3b échoué : statut=" + val);
            erreurs++;
        }
        val = model.getValueAt(2, 3);  // Statut normal
        if (!"Normal".equals(val)) {
            System.out.println("❌ Test 3c échoué : statut=" + val);
            erreurs++;
        }
        if (erreurs == 0) {
            System.out.println("✅ Test 3 : contenu des colonnes OK");
        }

        // Test 4 : mise à jour (remplacer une zone existante)
        model.mettreAJour(new MesureMeteo(Zone.ZONE_A, 31.0, 60.0));
        String temp = (String) model.getValueAt(0, 1);
        if (!"31,0".equals(temp)) {
            System.out.println("❌ Test 4 échoué : temp après maj=" + temp);
            erreurs++;
        } else {
            System.out.println("✅ Test 4 : mise à jour zone OK");
        }

        // Test 5 : statistiques
        double tempMoy = model.getTemperatureMoyenne();
        double humMoy  = model.getHumiditeMoyenne();
        int nbAlertes  = model.getNombreAlertes();

        // Temp moyenne = (31+28+32+35+27)/5 = 30.6
        if (Math.abs(tempMoy - 30.6) > 0.01) {
            System.out.println("❌ Test 5a échoué : tempMoy=" + tempMoy);
            erreurs++;
        }
        // Hum moyenne = (60+25+70+18+55)/5 = 45.6
        if (Math.abs(humMoy - 45.6) > 0.01) {
            System.out.println("❌ Test 5b échoué : humMoy=" + humMoy);
            erreurs++;
        }
        // 2 zones en alerte (B à 25%, D à 18%)
        if (nbAlertes != 2) {
            System.out.println("❌ Test 5c échoué : nbAlertes=" + nbAlertes);
            erreurs++;
        }
        if (erreurs == 0) {
            System.out.println("✅ Test 5 : statistiques OK");
        }

        // Test 6 : estEnAlerte
        if (!model.estEnAlerte(Zone.ZONE_B)) {
            System.out.println("❌ Test 6a échoué : ZONE_B devrait être en alerte");
            erreurs++;
        }
        if (model.estEnAlerte(Zone.ZONE_A)) {
            System.out.println("❌ Test 6b échoué : ZONE_A ne devrait pas être en alerte");
            erreurs++;
        }
        if (erreurs == 0) {
            System.out.println("✅ Test 6 : estEnAlerte OK");
        }

        // Bilan
        System.out.println("\n=== " + (erreurs == 0 ? "TOUS LES TESTS PASSENT ✅" : erreurs + " ERREUR(S) ❌") + " ===");
    }
}
