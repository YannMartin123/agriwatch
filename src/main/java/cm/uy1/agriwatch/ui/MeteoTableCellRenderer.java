package cm.uy1.agriwatch.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

// TODO Ã‰quipe 3 â€” Coloration rouge si humiditÃ© < 30%
public class MeteoTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        // TODO : rÃ©cupÃ©rer l'humiditÃ© de la ligne et appliquer Color.RED si < 30
        // else { setBackground(UIManager.getColor("Table.background")); }

        return c;
    }
}
