package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de tableau Swing pour AgriWatch.
 *
 * Colonnes : Zone | Temp (°C) | Humidité (%) | Statut
 * Une ligne par zone (5 max). La méthode mettreAJour() remplace la ligne
 * correspondant à la zone ou l'ajoute si elle n'existe pas encore.
 */
public class MeteoTableModel extends AbstractTableModel {

    private static final double SEUIL_ALERTE = 30.0;

    private final String[] colonnes = {"Zone", "Temp (°C)", "Humidité (%)", "Statut"};
    private final List<MesureMeteo> donnees = new ArrayList<>();

    /**
     * Met à jour ou ajoute la mesure pour la zone concernée.
     * Appelle fireTableDataChanged() pour rafraîchir la JTable.
     */
    public void mettreAJour(MesureMeteo mesure) {
        for (int i = 0; i < donnees.size(); i++) {
            if (donnees.get(i).getZone() == mesure.getZone()) {
                donnees.set(i, mesure);
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        // Nouvelle zone
        donnees.add(mesure);
        fireTableRowsInserted(donnees.size() - 1, donnees.size() - 1);
    }

    /**
     * Accès direct à une mesure pour le renderer.
     */
    public MesureMeteo getMesure(int row) {
        if (row >= 0 && row < donnees.size()) {
            return donnees.get(row);
        }
        return null;
    }

    // --- Statistiques pour le panneau SOUTH ---

    /**
     * Température moyenne sur toutes les zones actives.
     */
    public double getTemperatureMoyenne() {
        if (donnees.isEmpty()) return 0.0;
        double somme = 0.0;
        for (MesureMeteo m : donnees) {
            somme += m.getTemperature();
        }
        return somme / donnees.size();
    }

    /**
     * Humidité moyenne sur toutes les zones actives.
     */
    public double getHumiditeMoyenne() {
        if (donnees.isEmpty()) return 0.0;
        double somme = 0.0;
        for (MesureMeteo m : donnees) {
            somme += m.getHumidite();
        }
        return somme / donnees.size();
    }

    /**
     * Nombre de zones actuellement en alerte (humidité < 30%).
     */
    public int getNombreAlertes() {
        int count = 0;
        for (MesureMeteo m : donnees) {
            if (m.getHumidite() < SEUIL_ALERTE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Vérifie si une zone donnée est en alerte.
     */
    public boolean estEnAlerte(Zone zone) {
        for (MesureMeteo m : donnees) {
            if (m.getZone() == zone && m.getHumidite() < SEUIL_ALERTE) {
                return true;
            }
        }
        return false;
    }

    // --- AbstractTableModel ---

    @Override
    public int getRowCount() {
        return donnees.size();
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
        MesureMeteo m = donnees.get(row);
        return switch (col) {
            case 0  -> m.getZone().name();
            case 1  -> String.format("%.1f", m.getTemperature());
            case 2  -> String.format("%.1f", m.getHumidite());
            case 3  -> m.getHumidite() < SEUIL_ALERTE ? "⚠ ALERTE" : "Normal";
            default -> null;
        };
    }
}
