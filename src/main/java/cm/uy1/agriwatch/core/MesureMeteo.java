package cm.uy1.agriwatch.core;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MesureMeteo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Zone zone;
    private final double temperature;
    private final double humidite;
    private final LocalDateTime timestamp;

    public MesureMeteo(Zone zone, double temperature, double humidite) {
        this.zone = zone;
        this.temperature = temperature;
        this.humidite = humidite;
        this.timestamp = LocalDateTime.now();
    }

    public Zone getZone()               { return zone; }
    public double getTemperature()      { return temperature; }
    public double getHumidite()         { return humidite; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return zone + " | Temp: " + temperature + "Â°C | HumiditÃ©: " + humidite + "%";
    }
}
