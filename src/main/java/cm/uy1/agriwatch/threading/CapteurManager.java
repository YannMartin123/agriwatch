package cm.uy1.agriwatch.threading;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import java.util.ArrayList;
import java.util.List;

// Gère le cycle de vie des 5 threads capteurs
// TODO Équipe 4 : envisager ExecutorService en remplacement de Thread brut
public class CapteurManager {

    private final List<Thread> threads = new ArrayList<>();
    private final List<CapteurMeteo> capteurs = new ArrayList<>();

    public void demarrer(MeteoListener listener) {
        for (Zone zone : Zone.values()) {
            CapteurMeteo capteur = new CapteurMeteo(zone);
            capteur.ajouterListener(listener);
            capteurs.add(capteur);
            Thread t = new Thread(capteur, "Thread-" + zone.name());
            threads.add(t);
            t.start();
        }
        System.out.println("AgriWatch : 5 capteurs démarrés.");
    }

    /**
     * Interrompt tous les threads capteurs et attend leur terminaison.
     */
    public void arreter() {
        threads.forEach(Thread::interrupt);
        for (Thread t : threads) {
            try {
                t.join(3000);  // timeout de 3s par thread
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        threads.clear();   // ne clear qu'après join()
        capteurs.clear();
        System.out.println("AgriWatch : tous les capteurs arrêtés.");
    }
}