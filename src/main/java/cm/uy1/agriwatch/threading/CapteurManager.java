package cm.uy1.agriwatch.threading;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import java.util.HashMap;
import java.util.Map;

// Gère le cycle de vie des 5 threads capteurs individuellement ou en groupe
public class CapteurManager {

    private final Map<Zone, Thread> threads = new HashMap<>();
    private final Map<Zone, CapteurMeteo> capteurs = new HashMap<>();

    /**
     * Démarre tous les capteurs (comportement d'origine).
     */
    public synchronized void demarrer(MeteoListener listener) {
        for (Zone zone : Zone.values()) {
            demarrerZone(zone, listener);
        }
        System.out.println("AgriWatch : tous les capteurs démarrés.");
    }

    /**
     * Démarre le capteur pour une zone spécifique.
     */
    public synchronized void demarrerZone(Zone zone, MeteoListener listener) {
        if (isZoneActive(zone)) {
            return; // Déjà en cours d'exécution
        }
        CapteurMeteo capteur = new CapteurMeteo(zone);
        capteur.ajouterListener(listener);
        capteurs.put(zone, capteur);
        
        Thread t = new Thread(capteur, "Thread-" + zone.name());
        threads.put(zone, t);
        t.start();
    }

    /**
     * Arrête le capteur pour une zone spécifique.
     */
    public synchronized void arreterZone(Zone zone) {
        Thread t = threads.remove(zone);
        if (t != null) {
            t.interrupt();
            try {
                t.join(1000);  // timeout de 1s pour arrêt
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        capteurs.remove(zone);
    }

    /**
     * Arrête tous les capteurs.
     */
    public synchronized void arreter() {
        for (Zone zone : Zone.values()) {
            arreterZone(zone);
        }
        threads.clear();
        capteurs.clear();
        System.out.println("AgriWatch : tous les capteurs arrêtés.");
    }

    /**
     * Vérifie si le capteur d'une zone donnée est actif.
     */
    public synchronized boolean isZoneActive(Zone zone) {
        Thread t = threads.get(zone);
        return t != null && t.isAlive();
    }
}