package cm.uy1.agriwatch.core;

public interface MeteoListener {
    void onMesureRecue(Zone zone, double temperature, double humidite);
}
