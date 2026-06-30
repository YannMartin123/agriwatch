# AgriWatch - Création de la structure du projet
# Encodage UTF-8 sans BOM forcé
$utf8NoBom = New-Object System.Text.UTF8Encoding $false

function Write-File($path, $content) {
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

$base = "src/main/java/cm/uy1/agriwatch"

# Dossiers
New-Item -ItemType Directory -Force -Path "$base/core"
New-Item -ItemType Directory -Force -Path "$base/persistence"
New-Item -ItemType Directory -Force -Path "$base/ui"
New-Item -ItemType Directory -Force -Path "$base/threading"
New-Item -ItemType Directory -Force -Path "data"
New-Item -ItemType Directory -Force -Path "docs"

# Zone.java
Write-File "$base/core/Zone.java" @"
package cm.uy1.agriwatch.core;

public enum Zone {
    ZONE_A, ZONE_B, ZONE_C, ZONE_D, ZONE_E
}
"@

# MeteoListener.java
Write-File "$base/core/MeteoListener.java" @"
package cm.uy1.agriwatch.core;

public interface MeteoListener {
    void onMesureRecue(Zone zone, double temperature, double humidite);
}
"@

# MesureMeteo.java
Write-File "$base/core/MesureMeteo.java" @"
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
        return zone + " | Temp: " + temperature + "C | Humidite: " + humidite + "%";
    }
}
"@

# CapteurMeteo.java
Write-File "$base/core/CapteurMeteo.java" @"
package cm.uy1.agriwatch.core;

import java.util.ArrayList;
import java.util.List;

public class CapteurMeteo implements Runnable {

    private final Zone zone;
    private final List<MeteoListener> listeners = new ArrayList<>();

    public CapteurMeteo(Zone zone) {
        this.zone = zone;
    }

    public void ajouterListener(MeteoListener listener) {
        listeners.add(listener);
    }

    public void retirerListener(MeteoListener listener) {
        listeners.remove(listener);
    }

    private void notifierListeners(double temp, double humidite) {
        for (MeteoListener l : listeners) {
            l.onMesureRecue(zone, temp, humidite);
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                double temp     = 15 + Math.random() * 25;
                double humidite = 15 + Math.random() * 70;
                notifierListeners(temp, humidite);
                Thread.sleep(2000 + (long)(Math.random() * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Capteur " + zone + " arrete proprement.");
            }
        }
    }
}
"@

# CentraleMeteo.java
Write-File "$base/core/CentraleMeteo.java" @"
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
"@

# PersistanceService.java
Write-File "$base/persistence/PersistanceService.java" @"
package cm.uy1.agriwatch.persistence;

import cm.uy1.agriwatch.core.MesureMeteo;
import java.util.List;

public class PersistanceService {

    // TODO[Eq2-1] : implementer la sauvegarde en .ser
    public void sauvegarder(List<MesureMeteo> historique) {
    }

    // TODO[Eq2-2] : implementer le chargement depuis .ser
    public List<MesureMeteo> charger() {
        return new java.util.ArrayList<>();
    }

    // TODO[Eq2-3] : ecrire dans data/rapport_alertes.csv
    // Format attendu : Zone,Temperature,Humidite,Timestamp,Statut
    public void exporterCSV(List<MesureMeteo> alertes) {
    }
}
"@

# MeteoTableModel.java
Write-File "$base/ui/MeteoTableModel.java" @"
package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MesureMeteo;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class MeteoTableModel extends AbstractTableModel {

    private final String[] colonnes = {"Zone", "Temp (C)", "Humidite (%)", "Statut"};
    private final List<MesureMeteo> donnees = new ArrayList<>();

    // TODO[Eq3-1] : mettre a jour la ligne correspondant a la zone
    // ou ajouter si elle n'existe pas encore, puis appeler fireTableDataChanged()
    public void mettreAJour(MesureMeteo mesure) {
    }

    // Donne acces au renderer pour lire l'humidite
    public MesureMeteo getMesure(int row) {
        return donnees.get(row);
    }

    @Override public int getRowCount()    { return donnees.size(); }
    @Override public int getColumnCount() { return colonnes.length; }
    @Override public String getColumnName(int col) { return colonnes[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        // TODO[Eq3-2] : retourner zone / temperature / humidite / statut selon col
        return null;
    }
}
"@

# MeteoTableCellRenderer.java
Write-File "$base/ui/MeteoTableCellRenderer.java" @"
package cm.uy1.agriwatch.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class MeteoTableCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        // TODO[Eq3-3] : recuperer MeteoTableModel, lire humidite de la ligne
        // si humidite < 30.0 -> setBackground(Color.RED)
        // sinon             -> setBackground(UIManager.getColor("Table.background"))

        return c;
    }
}
"@

# FenetrePrincipale.java
Write-File "$base/ui/FenetrePrincipale.java" @"
package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;
import javax.swing.*;
import java.awt.*;

public class FenetrePrincipale extends JFrame implements MeteoListener {

    private final MeteoTableModel tableModel = new MeteoTableModel();
    private final JLabel lblAlerte  = new JLabel("", SwingConstants.CENTER);
    private final JButton btnDemarrer = new JButton("Demarrer les capteurs");
    private final JButton btnArreter  = new JButton("Arreter les capteurs");
    private final JButton btnExporter = new JButton("Exporter CSV");

    public FenetrePrincipale() {
        setTitle("AgriWatch - Supervision Meteo & Alerte Agricole");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        // TODO[Eq3-4] : construire le BorderLayout
        //   NORTH  = titre + horloge
        //   CENTER = JTable avec MeteoTableCellRenderer applique
        //   SOUTH  = stats (moyenne temp, moyenne humidite, nb alertes) + boutons
    }

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        SwingUtilities.invokeLater(() -> {
            // TODO[Eq3-5] : creer MesureMeteo, appeler tableModel.mettreAJour()
            // si humidite < 30 -> lblAlerte visible + Toolkit.getDefaultToolkit().beep()
            // sinon verifier si encore d'autres zones en alerte avant de cacher lblAlerte
        });
    }
}
"@

# CapteurManager.java
Write-File "$base/threading/CapteurManager.java" @"
package cm.uy1.agriwatch.threading;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import java.util.ArrayList;
import java.util.List;

public class CapteurManager {

    private final List<Thread> threads       = new ArrayList<>();
    private final List<CapteurMeteo> capteurs = new ArrayList<>();

    // TODO[Eq4-1] : envisager ScheduledExecutorService a la place de Thread brut
    public void demarrer(MeteoListener listener) {
        for (Zone zone : Zone.values()) {
            CapteurMeteo capteur = new CapteurMeteo(zone);
            capteur.ajouterListener(listener);
            capteurs.add(capteur);
            Thread t = new Thread(capteur, "Thread-" + zone.name());
            threads.add(t);
            t.start();
        }
        System.out.println("AgriWatch : 5 capteurs demarres.");
    }

    // TODO[Eq4-2] : s'assurer que tous les threads sont bien termines (join())
    public void arreter() {
        threads.forEach(Thread::interrupt);
        threads.clear();
        capteurs.clear();
        System.out.println("AgriWatch : tous les capteurs arretes.");
    }
}
"@

# Main.java
Write-File "$base/Main.java" @"
package cm.uy1.agriwatch;

import cm.uy1.agriwatch.core.CentraleMeteo;

public class Main {
    public static void main(String[] args) {
        System.out.println("AgriWatch - demarrage...");
        CentraleMeteo centrale = new CentraleMeteo();
        // TODO[Main-1] : instancier FenetrePrincipale
        // TODO[Main-2] : centrale.abonner(fenetrePrincipale)
        // TODO[Main-3] : SwingUtilities.invokeLater(() -> fenetrePrincipale.setVisible(true))
        // TODO[Main-4] : CapteurManager.demarrer(centrale) sur clic btnDemarrer
    }
}
"@

# .gitignore
Write-File ".gitignore" @"
data/
*.class
out/
.idea/
*.iml
setup.ps1
"@

Write-Host ""
Write-Host "[OK] Structure AgriWatch creee avec succes !" -ForegroundColor Green
Write-Host ""
Write-Host "[i] Prochaine etape dans IntelliJ :" -ForegroundColor Yellow
Write-Host "    Clic droit sur src/main/java -> Mark Directory as -> Sources Root"