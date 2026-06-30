package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;
import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de tableau filtrable pour l'historique complet de toutes les mesures.
 */
public class HistoriqueTableModel extends AbstractTableModel {

    private final CentraleMeteo centrale;
    private final List<MesureMeteo> filteredList = new ArrayList<>();
    private final String[] colonnes = {"Horodatage", "Zone", "Température (°C)", "Humidité (%)", "Statut"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Zone zoneFiltre = null; // null = Toutes les zones
    private boolean alertesSeulement = false;

    public HistoriqueTableModel(CentraleMeteo centrale) {
        this.centrale = centrale;
        appliquerFiltre();
    }

    /**
     * Applique les filtres courants sur la liste source et rafraîchit l'affichage.
     */
    public synchronized void appliquerFiltre() {
        filteredList.clear();
        List<MesureMeteo> sourceList = centrale.getHistorique();
        for (MesureMeteo m : sourceList) {
            if (zoneFiltre != null && m.getZone() != zoneFiltre) {
                continue;
            }
            if (alertesSeulement && m.getHumidite() >= MeteoTableModel.seuilAlerte) {
                continue;
            }
            filteredList.add(m);
        }
        fireTableDataChanged();
    }

    public void setFiltres(Zone zone, boolean alertesSeulement) {
        this.zoneFiltre = zone;
        this.alertesSeulement = alertesSeulement;
        appliquerFiltre();
    }

    @Override
    public int getRowCount() {
        return filteredList.size();
    }

    @Override
    public int getColumnCount() {
        return colonnes.length;
    }

    @Override
    public String getColumnName(int col) {
        return colonnes[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row >= filteredList.size()) return null;
        MesureMeteo m = filteredList.get(row);
        return switch (col) {
            case 0 -> m.getTimestamp().format(DATE_FORMATTER);
            case 1 -> m.getZone().name();
            case 2 -> String.format("%.1f", m.getTemperature());
            case 3 -> String.format("%.1f", m.getHumidite());
            case 4 -> m.getHumidite() < MeteoTableModel.seuilAlerte ? "⚠ ALERTE" : "Normal";
            default -> null;
        };
    }
}
