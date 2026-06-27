package cm.uy1.agriwatch.threading;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import java.util.ArrayList;
import java.util.List;

// TODO Ã‰quipe 4 â€” GÃ©rer le cycle de vie des 5 threads capteurs
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
        System.out.println("AgriWatch : 5 capteurs dÃ©marrÃ©s.");
    }

    public void arreter() {
        threads.forEach(Thread::interrupt);
        threads.clear();
        capteurs.clear();
        System.out.println("AgriWatch : tous les capteurs arrÃªtÃ©s.");
    }
}
