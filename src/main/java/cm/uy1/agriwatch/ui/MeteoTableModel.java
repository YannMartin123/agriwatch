package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MesureMeteo;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

// TODO Ã‰quipe 3 â€” ImplÃ©menter le TableModel
public class MeteoTableModel extends AbstractTableModel {

    private final String[] colonnes = {"Zone", "Temp (Â°C)", "HumiditÃ© (%)", "Statut"};
    private final List<MesureMeteo> donnees = new ArrayList<>();

    public void mettreAJour(MesureMeteo mesure) {
        // TODO : mettre Ã  jour donnees et appeler fireTableDataChanged()
    }

    @Override public int getRowCount()    { return donnees.size(); }
    @Override public int getColumnCount() { return colonnes.length; }
    @Override public String getColumnName(int col) { return colonnes[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        // TODO : retourner les bonnes valeurs selon la colonne
        return null;
    }
}
