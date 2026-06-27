package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import javax.swing.*;
import java.awt.*;

// TODO Ã‰quipe 3 â€” ImplÃ©menter l'IHM complÃ¨te
public class FenetrePrincipale extends JFrame implements MeteoListener {

    private final MeteoTableModel tableModel = new MeteoTableModel();
    private final JLabel lblAlerte = new JLabel("", SwingConstants.CENTER);
    private final JButton btnDemarrer = new JButton("DÃ©marrer les capteurs");
    private final JButton btnArreter  = new JButton("ArrÃªter les capteurs");
    private final JButton btnExporter = new JButton("Exporter CSV");

    public FenetrePrincipale() {
        setTitle("AgriWatch â€” Supervision MÃ©tÃ©o & Alerte Agricole");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        // TODO Ã‰quipe 3 : construire le layout BorderLayout complet
    }

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        SwingUtilities.invokeLater(() -> {
            // TODO Ã‰quipe 3 : mettre Ã  jour tableModel + vÃ©rifier alerte
        });
    }
}
