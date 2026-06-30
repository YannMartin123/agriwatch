package cm.uy1.agriwatch.core;

import java.util.ArrayList;
import java.util.List;

public class CapteurMeteo implements Runnable {

    // Permet de configurer le délai global de simulation (modifiable depuis l'IHM)
    public static volatile long simulationDelayMs = 2000;

    private final Zone zone;
    private final List<MeteoListener> listeners = new ArrayList<>();

    public CapteurMeteo(Zone zone) {
        this.zone = zone;
    }

    public void ajouterListener(MeteoListener listener) {
        listeners.add(listener);
    }

    public void retirerListener(MeteoListener listener) {
        listeners.remove(listener);
    }

    private void notifierListeners(double temp, double humidite) {
        synchronized (listeners) {
            for (MeteoListener l : listeners) {
                l.onMesureRecue(zone, temp, humidite);
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                // Génération cohérente des valeurs
                double temp     = 15 + Math.random() * 25; // [15, 40]
                double humidite = 15 + Math.random() * 70; // [15, 85]
                notifierListeners(temp, humidite);
                
                // Pause dynamique avec une légère variation aléatoire
                long delay = simulationDelayMs;
                long jitter = (long)(Math.random() * (delay / 4 + 100));
                Thread.sleep(Math.max(200, delay + (Math.random() > 0.5 ? jitter : -jitter)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Capteur " + zone + " arrêté proprement.");
            }
        }
    }
}
