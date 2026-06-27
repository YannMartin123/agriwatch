package cm.uy1.agriwatch.core;

import java.util.ArrayList;
import java.util.List;

public class CapteurMeteo implements Runnable {

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
        for (MeteoListener l : listeners) {
            l.onMesureRecue(zone, temp, humidite);
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                double temp     = 15 + Math.random() * 25;
                double humidite = 15 + Math.random() * 70;
                notifierListeners(temp, humidite);
                Thread.sleep(2000 + (long)(Math.random() * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Capteur " + zone + " arrÃªtÃ© proprement.");
            }
        }
    }
}
