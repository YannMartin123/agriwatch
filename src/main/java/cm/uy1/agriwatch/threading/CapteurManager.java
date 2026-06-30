package cm.uy1.agriwatch.threading;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Gère le cycle de vie des 5 threads capteurs via un ExecutorService
public class CapteurManager {

    private final List<CapteurMeteo> capteurs = new ArrayList<>();
    private ExecutorService executor;

    public void demarrer(MeteoListener listener) {
        // Pool de taille fixe : exactement 5 threads, un par zone (Zone.values().length == 5)
        executor = Executors.newFixedThreadPool(Zone.values().length);

        for (Zone zone : Zone.values()) {
            CapteurMeteo capteur = new CapteurMeteo(zone);
            capteur.ajouterListener(listener);
            capteurs.add(capteur);
            executor.execute(capteur); // soumet le Runnable au pool, pas de Thread créé à la main
        }
        System.out.println("AgriWatch : 5 capteurs démarrés (ExecutorService).");
    }

    /**
     * Interrompt toutes les tâches capteurs et attend leur terminaison.
     */
    public void arreter() {
        if (executor == null) {
            return; // déjà arrêté, ou jamais démarré
        }

        executor.shutdownNow(); // envoie interrupt() à toutes les tâches en cours

        try {
            // attend la fin effective des tâches, timeout de sécurité
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.out.println("AgriWatch : certains capteurs n'ont pas répondu à temps.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        capteurs.clear();
        executor = null;
        System.out.println("AgriWatch : tous les capteurs arrêtés.");
    }
}