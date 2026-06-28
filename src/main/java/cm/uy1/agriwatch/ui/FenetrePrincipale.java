package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import cm.uy1.agriwatch.persistence.PersistanceService;
import cm.uy1.agriwatch.threading.CapteurManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Fenêtre principale de l'application AgriWatch.
 *
 * Layout BorderLayout :
 *   NORTH  — Titre + horloge temps réel
 *   CENTER — JTable (Zone, Temp, Humidité, Statut) avec coloration alerte
 *   SOUTH  — Statistiques (moyennes, alertes) + boutons (Démarrer, Arrêter, Exporter)
 *
 * Implémente MeteoListener pour recevoir les mesures sans référence directe aux capteurs.
 */
public class FenetrePrincipale extends JFrame implements MeteoListener {

    // --- Constantes ---
    private static final String TITRE_APP = "AgriWatch — Supervision Météo & Alerte Agricole";
    private static final DateTimeFormatter FMT_HORLOGE =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Color COULEUR_ALERTE = new Color(200, 30, 30);
    private static final Font POLICE_TITRE = new Font("SansSerif", Font.BOLD, 36);
    private static final Font POLICE_HORLOGE = new Font("Monospaced", Font.BOLD, 32);
    private static final Font POLICE_ALERTE = new Font("SansSerif", Font.BOLD, 42);
    private static final Font POLICE_STATS = new Font("SansSerif", Font.PLAIN, 26);

    // --- Composants ---
    private final CentraleMeteo centrale;
    private final CapteurManager capteurManager;
    private final PersistanceService persistance;

    // Modèle de tableau
    private final MeteoTableModel tableModel;
    private final JTable table;

    // Labels
    private final JLabel lblHorloge;
    private final JLabel lblAlerte;
    private final JLabel lblTempMoy;
    private final JLabel lblHumMoy;
    private final JLabel lblNbAlertes;

    // Boutons
    private final JButton btnDemarrer;
    private final JButton btnArreter;
    private final JButton btnExporter;

    // Timers
    private final Timer timerHorloge;
    private final Timer timerAlerte;

    // État
    private boolean capteursActifs = false;

    // --- Constructeur ---

    public FenetrePrincipale(CentraleMeteo centrale,
                             CapteurManager capteurManager,
                             PersistanceService persistance) {
        this.centrale       = centrale;
        this.capteurManager = capteurManager;
        this.persistance    = persistance;
        this.tableModel     = new MeteoTableModel();

        // === Configuration de la JFrame ===
        setTitle(TITRE_APP);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // === Composants instanciés ici pour la lisibilité ===
        lblHorloge = new JLabel("", SwingConstants.CENTER);
        lblAlerte  = new JLabel("⚠ ALERTE IRRIGATION REQUISE ⚠", SwingConstants.CENTER);
        lblTempMoy = new JLabel();
        lblHumMoy  = new JLabel();
        lblNbAlertes = new JLabel();

        btnDemarrer = new JButton("▶ Démarrer les capteurs");
        btnArreter  = new JButton("■ Arrêter les capteurs");
        btnExporter = new JButton("📄 Exporter CSV");

        // JTable
        table = new JTable(tableModel);
        configurerTable();

        // Timers
        timerHorloge = new Timer(1000, e -> rafraichirHorloge());
        timerAlerte  = new Timer(500,  e -> basculerAlerte());

        // === Assemblage ===
        add(creerPanneauNord(),   BorderLayout.NORTH);
        add(creerPanneauCentre(), BorderLayout.CENTER);
        add(creerPanneauSud(),    BorderLayout.SOUTH);

        // === État initial ===
        majEtatBoutons();
        rafraichirStats();
        timerHorloge.start();

        // S'abonner à la centrale pour recevoir les mesures
        centrale.abonner(this);
    }

    // --- Construction des panneaux ---

    /**
     * NORTH : Titre de l'application + horloge temps réel.
     */
    private JPanel creerPanneauNord() {
        JPanel panneau = new JPanel(new BorderLayout());
        panneau.setBorder(new EmptyBorder(10, 15, 5, 15));
        panneau.setBackground(new Color(34, 139, 34)); // vert plantation

        JLabel lblTitre = new JLabel(TITRE_APP, SwingConstants.CENTER);
        lblTitre.setFont(POLICE_TITRE);
        lblTitre.setForeground(Color.WHITE);

        lblHorloge.setFont(POLICE_HORLOGE);
        lblHorloge.setForeground(new Color(220, 255, 220));

        // Alerte clignotante entre le titre et l'horloge
        lblAlerte.setFont(POLICE_ALERTE);
        lblAlerte.setForeground(COULEUR_ALERTE);
        lblAlerte.setVisible(false);

        // Regrouper titre + alerte au centre
        JPanel centreNord = new JPanel(new GridLayout(2, 1));
        centreNord.setOpaque(false);
        centreNord.add(lblTitre);
        centreNord.add(lblAlerte);

        panneau.add(centreNord, BorderLayout.CENTER);
        panneau.add(lblHorloge, BorderLayout.EAST);

        return panneau;
    }

    /**
     * CENTER : JTable dans un JScrollPane.
     * Chaque colonne a une largeur adaptée à son contenu.
     */
    private JPanel creerPanneauCentre() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel panneau = new JPanel(new BorderLayout());
        panneau.add(scrollPane, BorderLayout.CENTER);
        return panneau;
    }

    /**
     * SOUTH : Statistiques (moyennes temp/hum, nb alertes) + boutons d'action.
     */
    private JPanel creerPanneauSud() {
        JPanel panneau = new JPanel(new BorderLayout(10, 5));
        panneau.setBorder(new EmptyBorder(5, 15, 10, 15));

        // --- Stats ---
        JPanel panneauStats = new JPanel(new GridLayout(3, 1, 2, 2));
        TitledBorder bordureStats = new TitledBorder("Statistiques plantation");
        bordureStats.setTitleFont(new Font("SansSerif", Font.BOLD, 32));
        bordureStats.setTitleColor(new Color(34, 139, 34));
        panneauStats.setBorder(bordureStats);

        lblTempMoy.setFont(POLICE_STATS);
        lblHumMoy.setFont(POLICE_STATS);
        lblNbAlertes.setFont(POLICE_STATS);

        panneauStats.add(lblTempMoy);
        panneauStats.add(lblHumMoy);
        panneauStats.add(lblNbAlertes);

        // --- Boutons ---
        JPanel panneauBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // Styles
        for (JButton btn : new JButton[]{btnDemarrer, btnArreter, btnExporter}) {
            btn.setFont(new Font("SansSerif", Font.BOLD, 22));
            btn.setFocusPainted(false);
        }
        btnDemarrer.setBackground(new Color(60, 179, 113));
        btnDemarrer.setForeground(Color.WHITE);
        btnArreter.setBackground(new Color(220, 80, 80));
        btnArreter.setForeground(Color.WHITE);

        // Actions
        btnDemarrer.addActionListener(e -> demarrerCapteurs());
        btnArreter.addActionListener(e  -> arreterCapteurs());
        btnExporter.addActionListener(e  -> exporterCSV());

        panneauBoutons.add(btnDemarrer);
        panneauBoutons.add(btnArreter);
        panneauBoutons.add(btnExporter);

        panneau.add(panneauStats, BorderLayout.CENTER);
        panneau.add(panneauBoutons, BorderLayout.EAST);

        return panneau;
    }

    /**
     * Configure la JTable : renderer personnalisé + largeurs de colonnes.
     */
    private void configurerTable() {
        table.setDefaultRenderer(Object.class, new MeteoTableCellRenderer());
        table.setRowHeight(52);
        table.setFont(new Font("SansSerif", Font.PLAIN, 26));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 22));
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Largeurs de colonnes
        TableColumnModel colModel = table.getColumnModel();
        int[] largeurs = {180, 200, 220, 200};
        for (int i = 0; i < largeurs.length && i < colModel.getColumnCount(); i++) {
            TableColumn col = colModel.getColumn(i);
            col.setPreferredWidth(largeurs[i]);
            col.setMinWidth(largeurs[i]);
        }
    }

    // --- Horloge ---

    private void rafraichirHorloge() {
        lblHorloge.setText(LocalTime.now().format(FMT_HORLOGE));
    }

    // --- Alerte ---

    /**
     * Active/désactive le clignotement du label d'alerte.
     */
    private void gererAlerte(boolean enAlerte) {
        if (enAlerte && !timerAlerte.isRunning()) {
            lblAlerte.setVisible(true);
            timerAlerte.start();
        } else if (!enAlerte && timerAlerte.isRunning()) {
            timerAlerte.stop();
            lblAlerte.setVisible(false);
        }
    }

    private void basculerAlerte() {
        lblAlerte.setVisible(!lblAlerte.isVisible());
    }

    // --- Statistiques ---

    private void rafraichirStats() {
        int nbAlertes = tableModel.getNombreAlertes();
        lblTempMoy.setText(String.format("Température moyenne : %.1f °C",
                tableModel.getTemperatureMoyenne()));
        lblHumMoy.setText(String.format("Humidité moyenne    : %.1f %%",
                tableModel.getHumiditeMoyenne()));
        lblNbAlertes.setText(String.format("Zones en alerte     : %d",
                nbAlertes));
    }

    // --- Boutons ---

    private void demarrerCapteurs() {
        if (!capteursActifs) {
            capteurManager.demarrer(centrale);
            capteursActifs = true;
            majEtatBoutons();
        }
    }

    private void arreterCapteurs() {
        if (capteursActifs) {
            capteurManager.arreter();
            capteursActifs = false;
            majEtatBoutons();
            // Désactiver l'alerte clignotante si elle était active
            gererAlerte(false);
        }
    }

    private void exporterCSV() {
        // TODO Équipe 2 : quand PersistanceService sera implémenté
        // persistance.exporterCSV(centrale.getHistorique());
        JOptionPane.showMessageDialog(this,
                "Export CSV — fonctionnalité à implémenter (Équipe 2)",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void majEtatBoutons() {
        btnDemarrer.setEnabled(!capteursActifs);
        btnArreter.setEnabled(capteursActifs);
    }

    // --- MeteoListener (appelé depuis les threads capteurs) ---

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        // RÈGLE EDT : toute modification Swing dans l'Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MesureMeteo mesure = new MesureMeteo(zone, temperature, humidite);
            tableModel.mettreAJour(mesure);
            rafraichirStats();

            // Vérifier si cette mesure déclenche une alerte
            if (humidite < 30.0) {
                gererAlerte(true);
                Toolkit.getDefaultToolkit().beep();
            } else {
                // Vérifier si d'autres zones sont encore en alerte
                gererAlerte(tableModel.getNombreAlertes() > 0);
            }
        });
    }
}