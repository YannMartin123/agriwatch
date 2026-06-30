package cm.uy1.agriwatch.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Colorie la ligne entière selon l'état d'alerte (style web clair).
 */
public class MeteoTableCellRenderer extends DefaultTableCellRenderer {

    private static final Color FOND_ALERTE = new Color(253, 233, 225);      // Rose-orange très clair (#fde9e1)
    private static final Color TEXTE_ALERTE = new Color(179, 74, 44);       // Rouge brique (#b34a2c)
    private static final Color FOND_NORMAL = Color.WHITE;
    private static final Color TEXTE_NORMAL = new Color(26, 30, 43);         // Slate-900 (#1a1e2b)
    private static final Color FOND_SELECTION = new Color(226, 232, 240);    // Slate-200 (#e2e8f0)

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        setHorizontalAlignment(SwingConstants.CENTER);

        TableModel model = table.getModel();
        boolean alerte = false;
        
        for (int col = 0; col < model.getColumnCount(); col++) {
            Object val = model.getValueAt(row, col);
            if (val != null && val.toString().contains("⚠")) {
                alerte = true;
                break;
            }
        }

        if (alerte) {
            c.setBackground(isSelected ? FOND_SELECTION : FOND_ALERTE);
            c.setForeground(TEXTE_ALERTE);
        } else {
            c.setBackground(isSelected ? FOND_SELECTION : FOND_NORMAL);
            c.setForeground(TEXTE_NORMAL);
        }
        
        if (c instanceof JLabel label) {
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        }

        return c;
    }
}
