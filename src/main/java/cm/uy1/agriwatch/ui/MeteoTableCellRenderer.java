package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MesureMeteo;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Colorie la ligne entière en rouge si l'humidité de la zone est < 30%.
 * Respecte la règle du else : réinitialise la couleur de fond quand l'humidité
 * repasse au-dessus du seuil.
 */
public class MeteoTableCellRenderer extends DefaultTableCellRenderer {

    private static final double SEUIL_ALERTE = 30.0;
    private static final Color FOND_ALERTE   = new Color(255, 80, 80);  // rouge vif

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        TableModel model = table.getModel();
        if (model instanceof MeteoTableModel meteoModel) {
            MesureMeteo mesure = meteoModel.getMesure(row);
            if (mesure != null && mesure.getHumidite() < SEUIL_ALERTE) {
                c.setBackground(FOND_ALERTE);
                c.setForeground(Color.WHITE);
            } else {
                c.setBackground(UIManager.getColor("Table.background"));
                c.setForeground(UIManager.getColor("Table.foreground"));
            }
        }
        return c;
    }
}
