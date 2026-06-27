package cm.uy1.agriwatch.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CentraleMeteo implements MeteoListener {

    private final List<MesureMeteo> historique =
        Collections.synchronizedList(new ArrayList<>());

    private final List<MeteoListener> listeners =
        Collections.synchronizedList(new ArrayList<>());

    public void abonner(MeteoListener listener) {
        listeners.add(listener);
    }

    public void desabonner(MeteoListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        MesureMeteo mesure = new MesureMeteo(zone, temperature, humidite);
        historique.add(mesure);
        for (MeteoListener l : listeners) {
            l.onMesureRecue(zone, temperature, humidite);
        }
    }

    public List<MesureMeteo> getHistorique() {
        return Collections.unmodifiableList(historique);
    }
}
