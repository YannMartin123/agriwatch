package cm.uy1.agriwatch;

import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.persistence.PersistanceService;
import cm.uy1.agriwatch.threading.CapteurManager;
import cm.uy1.agriwatch.ui.FenetrePrincipale;

import javax.swing.*;

/**
 * Point d'entrée de l'application AgriWatch.
 *
 * Câblage :
 *   CentraleMeteo ← FenetrePrincipale (abonnée via MeteoListener)
 *   CapteurManager → CentraleMeteo (les capteurs notifient la centrale)
 *
 * La fenêtre est affichée dans l'EDT via SwingUtilities.invokeLater().
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("AgriWatch — démarrage...");

        // Instancier les services
        CentraleMeteo centrale = new CentraleMeteo();
        CapteurManager capteurManager = new CapteurManager();
        PersistanceService persistance = new PersistanceService();

        // Afficher la fenêtre dans l'EDT
        SwingUtilities.invokeLater(() -> {
            FenetrePrincipale fenetre = new FenetrePrincipale(
                    centrale, capteurManager, persistance);
            fenetre.setVisible(true);
        });

        System.out.println("AgriWatch — interface prête.");
    }
}
