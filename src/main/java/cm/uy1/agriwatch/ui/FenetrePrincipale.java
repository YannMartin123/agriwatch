package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.CapteurMeteo;
import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.MeteoListener;
import cm.uy1.agriwatch.core.Zone;
import cm.uy1.agriwatch.persistence.PersistanceService;
import cm.uy1.agriwatch.threading.CapteurManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FenetrePrincipale extends JFrame implements MeteoListener {

    // --- Constantes Thème Clair Web ---
    private static final Color COLOR_BG           = new Color(244, 247, 252); // Gris-bleu très clair (#f4f7fc)
    private static final Color COLOR_CARD_BG      = Color.WHITE;
    private static final Color COLOR_BORDER       = new Color(233, 237, 242); // Bordure douce (#e9edf2)
    private static final Color COLOR_PRIMARY      = new Color(30, 43, 60);    // Bleu navy foncé (#1e2b3c)
    private static final Color COLOR_ACCENT       = new Color(45, 122, 110);   // Vert teal (#2d7a6e)
    private static final Color COLOR_ACCENT_LIGHT = new Color(233, 243, 240);  // Vert clair (#e9f3f0)
    private static final Color COLOR_TEXT         = new Color(26, 30, 43);     // Noir ardoise (#1a1e2b)
    private static final Color COLOR_TEXT_MUTED   = new Color(91, 103, 123);   // Gris text (#5b677b)
    private static final Color COLOR_ALERT_BG     = new Color(253, 233, 225);  // Rouge clair (#fde9e1)
    private static final Color COLOR_ALERT_TEXT   = new Color(179, 74, 44);    // Rouge brique (#b34a2c)
    private static final Color COLOR_NORMAL_BG    = new Color(230, 243, 237);  // Vert clair (#e6f3ed)
    private static final Color COLOR_NORMAL_TEXT  = new Color(29, 107, 92);    // Vert foncé (#1d6b5c)

    private static final DateTimeFormatter FMT_HORLOGE = DateTimeFormatter.ofPattern("HH:mm:ss");

    // --- Services injectés ---
    private final CentraleMeteo centrale;
    private final CapteurManager capteurManager;
    private final PersistanceService persistance;

    // --- Navigation & Layout ---
    private final CardLayout cardLayout;
    private final JPanel contentCards;
    private JButton[] navButtons;

    // --- Modèles ---
    private final MeteoTableModel summaryTableModel;
    private final HistoriqueTableModel historiqueTableModel;

    // --- Composants UI Dashboard ---
    // Grille 1 : Lignes de capteurs (Aperçu instantané)
    private final JLabel[] lblRowTemp = new JLabel[5];
    private final JLabel[] lblRowHum = new JLabel[5];
    private final JLabel[] lblRowAlertBadge = new JLabel[5];
    private final StatusDot[] rowDots = new StatusDot[5];
    private final JButton[] btnRowStart = new JButton[5];
    private final JButton[] btnRowStop = new JButton[5];

    // Grille 2 : État des capteurs (Cards à droite)
    private final JPanel[] panelStatusItems = new JPanel[5];
    private final StatusDot[] statusGridDots = new StatusDot[5];
    private final JLabel[] lblStatusGridTemp = new JLabel[5];
    private JLabel lblAlertCountBadge;
    private JLabel lblSensorStatusTextFooter;

    // Grille 3 : Paramètres actuels
    private JLabel lblParamSeuil;
    private JLabel lblParamFrequence;
    private JLabel lblParamClock;
    private ToggleSwitch paramSoundToggle;
    private JLabel lblParamSoundText;

    // Grille 4 : Alertes uniquement
    private JPanel panelAlertsListContainer;
    private JLabel lblAlertSummaryFooter;

    // --- Autres vues ---
    private RealTimeChartPanel chartPanel;
    private JComboBox<Object> comboChartZone;
    private JLabel lblTopActiveCountBadge;
    private int lastActiveCount = 0;

    // --- Configuration (Settings Panel) ---
    private JSlider sliderSeuilHum;
    private JLabel lblSliderSeuilValue;
    private JSlider sliderDelay;
    private JLabel lblSliderDelayValue;
    private ToggleSwitch settingsSoundToggle;
    private JLabel lblSettingsSoundText;

    // --- Timers ---
    private Timer timerHorloge;

    public static volatile boolean bipAlerteActif = true;

    public FenetrePrincipale(CentraleMeteo centrale,
                             CapteurManager capteurManager,
                             PersistanceService persistance) {
        this.centrale = centrale;
        this.capteurManager = capteurManager;
        this.persistance = persistance;

        // Charger l'historique enregistré
        List<MesureMeteo> histCharge = persistance.charger();
        centrale.chargerHistorique(histCharge);

        this.summaryTableModel = new MeteoTableModel();
        this.historiqueTableModel = new HistoriqueTableModel(centrale);

        // Fenêtre JFrame
        setTitle("AgriWatch — Contrôle & Supervision Agricole");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1320, 860);
        setMinimumSize(new Dimension(1100, 750));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);

        setLayout(new BorderLayout(0, 0));

        // 1. En-tête (Header + Menu de navigation)
        add(creerHeaderPanel(), BorderLayout.NORTH);

        // 2. Zone de contenu principale (CardLayout)
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setOpaque(false);
        contentCards.setBorder(new EmptyBorder(10, 30, 30, 30));

        contentCards.add(creerDashboardView(), "DASHBOARD");
        contentCards.add(creerChartsView(), "CHARTS");
        contentCards.add(creerLogsView(), "LOGS");
        contentCards.add(creerSettingsView(), "SETTINGS");

        add(contentCards, BorderLayout.CENTER);

        // Restaurer les valeurs
        restaurerDernieresMesuresDansSummaryTable(histCharge);
        initialiserTimers();
        centrale.abonner(this);

        // Fermeture
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sauvegarderEtFermer();
            }
        });

        // Afficher l'onglet initial
        selectTab(0, "DASHBOARD");
        majInterfaceStatuts();
    }

    // --- En-tête & Barre de Navigation ---

    private JPanel creerHeaderPanel() {
        JPanel headerOuter = new JPanel(new BorderLayout(0, 15));
        headerOuter.setOpaque(false);
        headerOuter.setBorder(new EmptyBorder(25, 30, 10, 30));

        // Top Row : Logo à gauche, Avatar & Statut à droite
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        // Logo
        JPanel logoArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoArea.setOpaque(false);
        
        JPanel logoIcon = new JPanel() {
            private Image imgLogo;
            {
                try {
                    imgLogo = javax.imageio.ImageIO.read(new java.io.File("logo.png"));
                } catch (Exception ex) {
                    System.err.println("Impossible de charger logo.png : " + ex.getMessage());
                }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (imgLogo != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(imgLogo, 0, 0, getWidth(), getHeight(), this);
                } else {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(COLOR_PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 72));
                    g2.drawString("🌱", 38, 100);
                }
            }
        };
        logoIcon.setPreferredSize(new Dimension(150, 150));
        logoIcon.setOpaque(false);

        JLabel lblLogoText = new JLabel("AgriWatch");
        lblLogoText.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblLogoText.setForeground(COLOR_PRIMARY);

        JLabel lblLogoSub = new JLabel("· contrôle");
        lblLogoSub.setFont(new Font("SansSerif", Font.PLAIN, 22));
        lblLogoSub.setForeground(COLOR_TEXT_MUTED);

        logoArea.add(logoIcon);
        logoArea.add(lblLogoText);
        logoArea.add(lblLogoSub);
        topRow.add(logoArea, BorderLayout.WEST);

        // Actions / Profil
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        profileArea.setOpaque(false);

        lblTopActiveCountBadge = new JLabel("🌱 5 capteurs", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_ACCENT_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
            }
        };
        lblTopActiveCountBadge.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTopActiveCountBadge.setForeground(COLOR_ACCENT);
        lblTopActiveCountBadge.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(208, 216, 227)); // #d0d8e3
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(45, 58, 78)); // #2d3a4e
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.drawString("JD", 9, 23);
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);

        profileArea.add(lblTopActiveCountBadge);
        profileArea.add(avatar);
        topRow.add(profileArea, BorderLayout.EAST);

        headerOuter.add(topRow, BorderLayout.NORTH);

        // Bottom Row : Navigation
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navRow.setOpaque(false);

        String[] labels = {"Dashboard", "Graphiques 2D", "Journal des Logs", "Configuration"};
        String[] cards = {"DASHBOARD", "CHARTS", "LOGS", "SETTINGS"};
        navButtons = new JButton[labels.length];

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            navButtons[i] = new JButton(labels[i]);
            navButtons[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            navButtons[i].setFocusPainted(false);
            navButtons[i].setBorderPainted(false);
            navButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            navButtons[i].setMargin(new Insets(8, 16, 8, 16));
            navButtons[idx].addActionListener(e -> selectTab(idx, cards[idx]));
            navRow.add(navButtons[i]);
        }
        headerOuter.add(navRow, BorderLayout.SOUTH);

        return headerOuter;
    }

    // --- Onglet 1 : DASHBOARD ---

    private JPanel creerDashboardView() {
        JPanel dashboard = new JPanel(new GridLayout(2, 2, 24, 24));
        dashboard.setOpaque(false);

        // CARD 1 : Aperçu instantané
        dashboard.add(creerCardApercuInstantane());

        // CARD 2 : État des capteurs
        dashboard.add(creerCardEtatCapteurs());

        // CARD 3 : Paramètres actuels
        dashboard.add(creerCardParametresActuels());

        // CARD 4 : Alertes uniquement
        dashboard.add(creerCardAlertesUniquement());

        return dashboard;
    }

    private JPanel creerCardApercuInstantane() {
        RoundedCardPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        // Header de la carte
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("📊 APERÇU INSTANTANÉ");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JLabel lblRefreshBadge = new JLabel("🕒 2s", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 243, 248));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
            }
        };
        lblRefreshBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblRefreshBadge.setForeground(COLOR_TEXT_MUTED);
        lblRefreshBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblRefreshBadge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Grid/Tableau
        JPanel tableGrid = new JPanel(new GridBagLayout());
        tableGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Entêtes
        String[] columns = {"Zone", "Temp", "Humidité", "Alerte", "État", "Contrôle"};
        gbc.gridy = 0;
        for (int col = 0; col < columns.length; col++) {
            gbc.gridx = col;
            JLabel lblCol = new JLabel(columns[col], SwingConstants.CENTER);
            lblCol.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblCol.setForeground(COLOR_TEXT_MUTED);
            lblCol.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, COLOR_BORDER));
            lblCol.setPreferredSize(new Dimension(80, 25));
            tableGrid.add(lblCol, gbc);
        }

        // Lignes pour chaque zone (A à E)
        Zone[] zones = Zone.values();
        for (int i = 0; i < 5; i++) {
            final int index = i;
            final Zone z = zones[i];
            gbc.gridy = i + 1;

            // Nom Zone
            gbc.gridx = 0;
            JLabel lblName = new JLabel(z.name(), SwingConstants.CENTER);
            lblName.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblName.setForeground(COLOR_PRIMARY);
            tableGrid.add(lblName, gbc);

            // Temp
            gbc.gridx = 1;
            lblRowTemp[i] = new JLabel("--.-°", SwingConstants.CENTER);
            lblRowTemp[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            lblRowTemp[i].setForeground(COLOR_TEXT);
            tableGrid.add(lblRowTemp[i], gbc);

            // Humidité
            gbc.gridx = 2;
            lblRowHum[i] = new JLabel("--.-%", SwingConstants.CENTER);
            lblRowHum[i].setFont(new Font("SansSerif", Font.PLAIN, 13));
            lblRowHum[i].setForeground(COLOR_TEXT_MUTED);
            tableGrid.add(lblRowHum[i], gbc);

            // Alerte Badge
            gbc.gridx = 3;
            lblRowAlertBadge[i] = new JLabel("Normal", SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getText().contains("AL")) {
                        g2.setColor(COLOR_ALERT_BG);
                    } else {
                        g2.setColor(COLOR_NORMAL_BG);
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    super.paintComponent(g);
                }
            };
            lblRowAlertBadge[i].setFont(new Font("SansSerif", Font.BOLD, 11));
            lblRowAlertBadge[i].setForeground(COLOR_NORMAL_TEXT);
            lblRowAlertBadge[i].setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            tableGrid.add(lblRowAlertBadge[i], gbc);

            // État Dot
            gbc.gridx = 4;
            rowDots[i] = new StatusDot();
            JPanel panelDot = new JPanel(new GridBagLayout());
            panelDot.setOpaque(false);
            panelDot.add(rowDots[i]);
            tableGrid.add(panelDot, gbc);

            // Contrôle
            gbc.gridx = 5;
            JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            ctrlPanel.setOpaque(false);

            btnRowStart[i] = creerMiniButton("▶", COLOR_ACCENT, new Color(220, 238, 233));
            btnRowStart[i].addActionListener(e -> {
                capteurManager.demarrerZone(z, centrale);
                majInterfaceStatuts();
            });

            btnRowStop[i] = creerMiniButton("■", COLOR_ALERT_TEXT, COLOR_ALERT_BG);
            btnRowStop[i].addActionListener(e -> {
                capteurManager.arreterZone(z);
                majInterfaceStatuts();
            });

            ctrlPanel.add(btnRowStart[i]);
            ctrlPanel.add(btnRowStop[i]);
            tableGrid.add(ctrlPanel, gbc);

            // Séparateur ligne
            // On peut ajouter une bordure fine sous chaque cellule en Swing
            lblName.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
            lblRowTemp[i].setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
            lblRowHum[i].setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
            lblRowAlertBadge[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            panelDot.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
            ctrlPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        }
        card.add(tableGrid, BorderLayout.CENTER);

        // Boutons Démarrer tout / Arrêter tout en bas
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton btnStartAll = new JButton("▶ Démarrer tout");
        btnStartAll.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnStartAll.setForeground(COLOR_ACCENT);
        btnStartAll.setBackground(COLOR_CARD_BG);
        btnStartAll.setFocusPainted(false);
        btnStartAll.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JButton btnStopAll = new JButton("■ Arrêter tout");
        btnStopAll.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnStopAll.setForeground(COLOR_ALERT_TEXT);
        btnStopAll.setBackground(COLOR_CARD_BG);
        btnStopAll.setFocusPainted(false);
        btnStopAll.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        btnStartAll.addActionListener(e -> {
            capteurManager.demarrer(centrale);
            majInterfaceStatuts();
        });
        btnStopAll.addActionListener(e -> {
            capteurManager.arreter();
            gererSignalAlerte(false);
            majInterfaceStatuts();
        });

        footer.add(btnStartAll);
        footer.add(btnStopAll);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private JPanel creerCardEtatCapteurs() {
        RoundedCardPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("📶 ÉTAT DES CAPTEURS");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        lblAlertCountBadge = new JLabel("0 en alerte", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getText().startsWith("0") ? COLOR_NORMAL_BG : COLOR_ALERT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
            }
        };
        lblAlertCountBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblAlertCountBadge.setForeground(COLOR_NORMAL_TEXT);
        lblAlertCountBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblAlertCountBadge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Grille d'état 5 zones
        JPanel gridStatus = new JPanel(new GridLayout(5, 1, 0, 6));
        gridStatus.setOpaque(false);

        Zone[] zones = Zone.values();
        for (int i = 0; i < 5; i++) {
            final Zone z = zones[i];
            panelStatusItems[i] = new JPanel(new BorderLayout(10, 0));
            panelStatusItems[i].setBackground(new Color(249, 250, 252));
            panelStatusItems[i].setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));

            statusGridDots[i] = new StatusDot();
            
            JPanel leftFlow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            leftFlow.setOpaque(false);
            leftFlow.add(statusGridDots[i]);
            JLabel lblZ = new JLabel(z.name());
            lblZ.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblZ.setForeground(COLOR_PRIMARY);
            leftFlow.add(lblZ);

            lblStatusGridTemp[i] = new JLabel("--.-°");
            lblStatusGridTemp[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            lblStatusGridTemp[i].setForeground(COLOR_TEXT_MUTED);

            panelStatusItems[i].add(leftFlow, BorderLayout.WEST);
            panelStatusItems[i].add(lblStatusGridTemp[i], BorderLayout.EAST);
            gridStatus.add(panelStatusItems[i]);
        }
        card.add(gridStatus, BorderLayout.CENTER);

        // Footer status bar
        lblSensorStatusTextFooter = new JLabel("Tous les capteurs hors ligne", SwingConstants.CENTER);
        lblSensorStatusTextFooter.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSensorStatusTextFooter.setForeground(COLOR_TEXT_MUTED);
        lblSensorStatusTextFooter.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        
        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setBackground(new Color(243, 246, 251));
        footerBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        footerBar.add(lblSensorStatusTextFooter, BorderLayout.CENTER);
        card.add(footerBar, BorderLayout.SOUTH);

        return card;
    }

    private JPanel creerCardParametresActuels() {
        RoundedCardPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("🛠 PARAMÈTRES ACTUELS");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JButton btnModifier = new JButton("modifier ✎");
        btnModifier.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnModifier.setForeground(COLOR_ACCENT);
        btnModifier.setBorderPainted(false);
        btnModifier.setContentAreaFilled(false);
        btnModifier.setFocusPainted(false);
        btnModifier.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnModifier.addActionListener(e -> selectTab(3, "SETTINGS"));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnModifier, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Grille de paramètres 2x2
        JPanel settingsGrid = new JPanel(new GridLayout(4, 1, 0, 10));
        settingsGrid.setOpaque(false);

        lblParamSeuil = creerSettingRow("Seuil irrigation", MeteoTableModel.seuilAlerte + " %");
        lblParamFrequence = creerSettingRow("Fréquence capteurs", (CapteurMeteo.simulationDelayMs / 1000.0) + " s");
        
        // Alerte Sonore (avec commutateur ToggleSwitch)
        JPanel soundRow = new JPanel(new BorderLayout());
        soundRow.setOpaque(false);
        soundRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        
        JLabel lblSoundLabel = new JLabel("Alerte sonore");
        lblSoundLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSoundLabel.setForeground(COLOR_TEXT_MUTED);
        
        JPanel rightSwitchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightSwitchPanel.setOpaque(false);
        lblParamSoundText = new JLabel(bipAlerteActif ? "Activée" : "Désactivée");
        lblParamSoundText.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblParamSoundText.setForeground(COLOR_PRIMARY);

        paramSoundToggle = new ToggleSwitch();
        paramSoundToggle.setSelected(bipAlerteActif);
        paramSoundToggle.setOnToggle(() -> {
            bipAlerteActif = paramSoundToggle.isSelected();
            lblParamSoundText.setText(bipAlerteActif ? "Activée" : "Désactivée");
            settingsSoundToggle.setSelected(bipAlerteActif);
            lblSettingsSoundText.setText(bipAlerteActif ? "Sonore" : "Muet");
        });

        rightSwitchPanel.add(paramSoundToggle);
        rightSwitchPanel.add(lblParamSoundText);

        soundRow.add(lblSoundLabel, BorderLayout.WEST);
        soundRow.add(rightSwitchPanel, BorderLayout.EAST);

        // Cours
        JPanel coursRow = new JPanel(new BorderLayout());
        coursRow.setOpaque(false);
        JLabel lblCoursLabel = new JLabel("Cours");
        lblCoursLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblCoursLabel.setForeground(COLOR_TEXT_MUTED);
        JLabel lblCoursValue = new JLabel("🎓 ICT308 - UY1");
        lblCoursValue.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCoursValue.setForeground(COLOR_PRIMARY);
        coursRow.add(lblCoursLabel, BorderLayout.WEST);
        coursRow.add(lblCoursValue, BorderLayout.EAST);

        settingsGrid.add(lblParamSeuil);
        settingsGrid.add(lblParamFrequence);
        settingsGrid.add(soundRow);
        settingsGrid.add(coursRow);

        card.add(settingsGrid, BorderLayout.CENTER);

        // Footer
        lblParamClock = new JLabel("Dernière synchro : --:--:--", SwingConstants.RIGHT);
        lblParamClock.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblParamClock.setForeground(COLOR_TEXT_MUTED);
        card.add(lblParamClock, BorderLayout.SOUTH);

        return card;
    }

    private JPanel creerCardAlertesUniquement() {
        RoundedCardPanel card = new RoundedCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("⚠ ALERTES UNIQUEMENT");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTitle.setForeground(COLOR_TEXT_MUTED);

        JLabel lblBadge = new JLabel("Alertes actives", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_ALERT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblBadge.setForeground(COLOR_ALERT_TEXT);
        lblBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblBadge, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // Conteneur de la liste d'alertes
        panelAlertsListContainer = new JPanel();
        panelAlertsListContainer.setOpaque(false);
        panelAlertsListContainer.setLayout(new BoxLayout(panelAlertsListContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(panelAlertsListContainer);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);

        // Footer
        lblAlertSummaryFooter = new JLabel("Aucune alerte active · irrigation non requise", SwingConstants.CENTER);
        lblAlertSummaryFooter.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblAlertSummaryFooter.setForeground(COLOR_TEXT_MUTED);
        
        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setBackground(new Color(243, 246, 251));
        footerBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        footerBar.add(lblAlertSummaryFooter, BorderLayout.CENTER);
        card.add(footerBar, BorderLayout.SOUTH);

        return card;
    }

    private JLabel creerSettingRow(String label, String value) {
        JLabel row = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(COLOR_BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        row.setLayout(new BorderLayout());
        JLabel lblL = new JLabel(label);
        lblL.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblL.setForeground(COLOR_TEXT_MUTED);

        JLabel lblV = new JLabel(value);
        lblV.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblV.setForeground(COLOR_PRIMARY);

        row.add(lblL, BorderLayout.WEST);
        row.add(lblV, BorderLayout.EAST);
        row.setPreferredSize(new Dimension(0, 30));
        return row;
    }

    private JButton creerMiniButton(String iconText, Color fgColor, Color bgColor) {
        JButton btn = new JButton(iconText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 10));
        btn.setForeground(fgColor);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // --- Onglet 2 : GRAPHIQUES 2D ---

    private JPanel creerChartsView() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Header filtre
        RoundedCardPanel filterPanel = new RoundedCardPanel();
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        filterPanel.setPreferredSize(new Dimension(0, 70));

        JLabel lblSelectZone = new JLabel("Choisir la zone à tracer : ");
        lblSelectZone.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSelectZone.setForeground(COLOR_PRIMARY);

        comboChartZone = new JComboBox<>();
        for (Zone z : Zone.values()) {
            comboChartZone.addItem(z);
        }
        comboChartZone.addItem("Toutes les zones (Sommaire)");
        comboChartZone.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboChartZone.setBackground(Color.WHITE);
        comboChartZone.setForeground(COLOR_TEXT);

        // Sélecteur de métrique pour le mode comparatif/sommaire
        JPanel panelMetricSelector = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelMetricSelector.setOpaque(false);
        panelMetricSelector.setVisible(false); // Masqué au début car Zone A est sélectionnée

        JLabel lblMetric = new JLabel("Métrique : ");
        lblMetric.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblMetric.setForeground(COLOR_PRIMARY);

        JButton btnTemp = new JButton("Température");
        JButton btnHum = new JButton("Humidité");
        
        stylePillButton(btnTemp, true);
        stylePillButton(btnHum, false);

        btnTemp.addActionListener(e -> {
            chartPanel.setMultiZoneMetric("Température");
            stylePillButton(btnTemp, true);
            stylePillButton(btnHum, false);
        });

        btnHum.addActionListener(e -> {
            chartPanel.setMultiZoneMetric("Humidité");
            stylePillButton(btnTemp, false);
            stylePillButton(btnHum, true);
        });

        panelMetricSelector.add(lblMetric);
        panelMetricSelector.add(btnTemp);
        panelMetricSelector.add(btnHum);

        comboChartZone.addActionListener(e -> {
            Object selected = comboChartZone.getSelectedItem();
            if (selected instanceof Zone z) {
                chartPanel.setSelectedZone(z);
                panelMetricSelector.setVisible(false);
            } else {
                chartPanel.setSelectedZone(null); // Mode comparatif
                panelMetricSelector.setVisible(true);
            }
            chargerHistoriquePourGraphique();
        });

        filterPanel.add(lblSelectZone);
        filterPanel.add(comboChartZone);
        filterPanel.add(panelMetricSelector);
        panel.add(filterPanel, BorderLayout.NORTH);

        // Graphique dans son panel arrondi
        RoundedCardPanel chartCard = new RoundedCardPanel();
        chartCard.setLayout(new BorderLayout());
        chartPanel = new RealTimeChartPanel();
        chartPanel.setCentrale(centrale);
        chartCard.add(chartPanel, BorderLayout.CENTER);
        panel.add(chartCard, BorderLayout.CENTER);

        return panel;
    }

    private void stylePillButton(JButton btn, boolean active) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (active) {
            btn.setBackground(COLOR_ACCENT);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_ACCENT, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        } else {
            btn.setBackground(new Color(240, 243, 248));
            btn.setForeground(COLOR_TEXT_MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
        }
    }

    // --- Onglet 3 : JOURNAL DES LOGS ---

    private JPanel creerLogsView() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setOpaque(false);

        // Barre d'outils filtres et export
        RoundedCardPanel toolbar = new RoundedCardPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        toolbar.setPreferredSize(new Dimension(0, 70));

        JLabel lblFilterZone = new JLabel("Zone : ");
        lblFilterZone.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblFilterZone.setForeground(COLOR_PRIMARY);

        JComboBox<String> comboFilterZone = new JComboBox<>();
        comboFilterZone.setFont(new Font("SansSerif", Font.PLAIN, 14));
        comboFilterZone.setBackground(Color.WHITE);
        comboFilterZone.setForeground(COLOR_TEXT);
        comboFilterZone.addItem("TOUTES");
        for (Zone z : Zone.values()) {
            comboFilterZone.addItem(z.name());
        }

        JCheckBox chkAlertOnly = new JCheckBox("Alertes uniquement");
        chkAlertOnly.setFont(new Font("SansSerif", Font.BOLD, 13));
        chkAlertOnly.setOpaque(false);
        chkAlertOnly.setForeground(COLOR_TEXT);

        JButton btnApplyFilter = new JButton("Filtrer");
        btnApplyFilter.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnApplyFilter.setBackground(COLOR_PRIMARY);
        btnApplyFilter.setForeground(Color.WHITE);
        btnApplyFilter.setFocusPainted(false);
        btnApplyFilter.addActionListener(e -> {
            String selected = (String) comboFilterZone.getSelectedItem();
            Zone z = "TOUTES".equals(selected) ? null : Zone.valueOf(selected);
            historiqueTableModel.setFiltres(z, chkAlertOnly.isSelected());
        });

        JButton btnExport = new JButton("📄 Exporter en CSV");
        btnExport.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnExport.setBackground(COLOR_ACCENT);
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);
        btnExport.addActionListener(e -> {
            persistance.exporterCSV(centrale.getHistorique());
            JOptionPane.showMessageDialog(this,
                    "Rapport exporté avec succès dans le fichier :\n[data/rapport_alertes.csv]",
                    "Exportation Réussie",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        toolbar.add(lblFilterZone);
        toolbar.add(comboFilterZone);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(chkAlertOnly);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnApplyFilter);
        toolbar.add(Box.createHorizontalStrut(40));
        toolbar.add(btnExport);

        panel.add(toolbar, BorderLayout.NORTH);

        // Tableau historique dans son panel arrondi
        RoundedCardPanel tableCard = new RoundedCardPanel();
        tableCard.setLayout(new BorderLayout());

        JTable tableHist = new JTable(historiqueTableModel);
        tableHist.setDefaultRenderer(Object.class, new MeteoTableCellRenderer());
        tableHist.setRowHeight(35);
        tableHist.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tableHist.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tableHist.getTableHeader().setBackground(COLOR_BG);
        tableHist.getTableHeader().setForeground(COLOR_PRIMARY);
        tableHist.setGridColor(COLOR_BORDER);

        JScrollPane scrollPane = new JScrollPane(tableHist);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(scrollPane, BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);

        return panel;
    }

    // --- Onglet 4 : CONFIGURATION ---

    private JPanel creerSettingsView() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        RoundedCardPanel innerPanel = new RoundedCardPanel();
        innerPanel.setLayout(new GridLayout(5, 2, 20, 25));
        innerPanel.setPreferredSize(new Dimension(750, 480));

        // 1. Seuil humidité
        JLabel lblSeuil = new JLabel("Seuil Alerte Humidité (%) :");
        lblSeuil.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSeuil.setForeground(COLOR_PRIMARY);

        JPanel panelSliderSeuil = new JPanel(new BorderLayout(10, 0));
        panelSliderSeuil.setOpaque(false);
        sliderSeuilHum = new JSlider(10, 90, (int) MeteoTableModel.seuilAlerte);
        sliderSeuilHum.setOpaque(false);
        sliderSeuilHum.addChangeListener(e -> {
            int val = sliderSeuilHum.getValue();
            MeteoTableModel.seuilAlerte = val;
            lblSliderSeuilValue.setText(val + " %");
            
            // Maj Dashboards
            lblParamSeuil.removeAll();
            lblParamSeuil.add(new JLabel("Seuil irrigation"), BorderLayout.WEST);
            JLabel valL = new JLabel(val + " %");
            valL.setFont(new Font("SansSerif", Font.BOLD, 13));
            valL.setForeground(COLOR_PRIMARY);
            lblParamSeuil.add(valL, BorderLayout.EAST);
            lblParamSeuil.revalidate();
            lblParamSeuil.repaint();
            
            historiqueTableModel.appliquerFiltre();
            majKpisEtAlerte();
        });
        lblSliderSeuilValue = new JLabel(sliderSeuilHum.getValue() + " %");
        lblSliderSeuilValue.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSliderSeuilValue.setForeground(COLOR_ACCENT);
        lblSliderSeuilValue.setPreferredSize(new Dimension(60, 0));
        panelSliderSeuil.add(sliderSeuilHum, BorderLayout.CENTER);
        panelSliderSeuil.add(lblSliderSeuilValue, BorderLayout.EAST);

        // 2. Vitesse de simulation
        JLabel lblSpeed = new JLabel("Délai de simulation (s) :");
        lblSpeed.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSpeed.setForeground(COLOR_PRIMARY);

        JPanel panelSliderDelay = new JPanel(new BorderLayout(10, 0));
        panelSliderDelay.setOpaque(false);
        sliderDelay = new JSlider(500, 5000, (int) CapteurMeteo.simulationDelayMs);
        sliderDelay.setOpaque(false);
        sliderDelay.addChangeListener(e -> {
            int val = sliderDelay.getValue();
            CapteurMeteo.simulationDelayMs = val;
            double sec = val / 1000.0;
            lblSliderDelayValue.setText(String.format("%.1f s", sec));
            
            // Maj Dashboards
            lblParamFrequence.removeAll();
            lblParamFrequence.add(new JLabel("Fréquence capteurs"), BorderLayout.WEST);
            JLabel valL = new JLabel(String.format("%.1f s", sec));
            valL.setFont(new Font("SansSerif", Font.BOLD, 13));
            valL.setForeground(COLOR_PRIMARY);
            lblParamFrequence.add(valL, BorderLayout.EAST);
            lblParamFrequence.revalidate();
            lblParamFrequence.repaint();
        });
        lblSliderDelayValue = new JLabel(String.format("%.1f s", sliderDelay.getValue() / 1000.0));
        lblSliderDelayValue.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSliderDelayValue.setForeground(COLOR_ACCENT);
        lblSliderDelayValue.setPreferredSize(new Dimension(60, 0));
        panelSliderDelay.add(sliderDelay, BorderLayout.CENTER);
        panelSliderDelay.add(lblSliderDelayValue, BorderLayout.EAST);

        // 3. Alerte Sonore
        JLabel lblSound = new JLabel("Alerte sonore :");
        lblSound.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSound.setForeground(COLOR_PRIMARY);

        JPanel switchLayout = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        switchLayout.setOpaque(false);
        settingsSoundToggle = new ToggleSwitch();
        settingsSoundToggle.setSelected(bipAlerteActif);
        
        lblSettingsSoundText = new JLabel(bipAlerteActif ? "Sonore" : "Muet");
        lblSettingsSoundText.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSettingsSoundText.setForeground(COLOR_TEXT_MUTED);

        settingsSoundToggle.setOnToggle(() -> {
            bipAlerteActif = settingsSoundToggle.isSelected();
            lblSettingsSoundText.setText(bipAlerteActif ? "Sonore" : "Muet");
            paramSoundToggle.setSelected(bipAlerteActif);
            lblParamSoundText.setText(bipAlerteActif ? "Activée" : "Désactivée");
        });
        switchLayout.add(settingsSoundToggle);
        switchLayout.add(lblSettingsSoundText);

        // 4. Boutons Sauvegarde manuelle
        JLabel lblSaveLabel = new JLabel("Actions d'historisation :");
        lblSaveLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblSaveLabel.setForeground(COLOR_PRIMARY);

        JPanel panelSaveBtns = new JPanel(new GridLayout(1, 2, 10, 0));
        panelSaveBtns.setOpaque(false);
        
        JButton btnSaveNow = new JButton("Sauvegarder");
        btnSaveNow.setBackground(COLOR_PRIMARY);
        btnSaveNow.setForeground(Color.WHITE);
        btnSaveNow.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSaveNow.setFocusPainted(false);
        btnSaveNow.addActionListener(e -> {
            persistance.sauvegarder(centrale.getHistorique());
            JOptionPane.showMessageDialog(this, "Historique sauvegardé avec succès !", "Sauvegarde", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnLoadNow = new JButton("Recharger");
        btnLoadNow.setBackground(new Color(240, 243, 248));
        btnLoadNow.setForeground(COLOR_PRIMARY);
        btnLoadNow.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnLoadNow.setFocusPainted(false);
        btnLoadNow.addActionListener(e -> {
            List<MesureMeteo> charge = persistance.charger();
            centrale.chargerHistorique(charge);
            restaurerDernieresMesuresDansSummaryTable(charge);
            historiqueTableModel.appliquerFiltre();
            majKpisEtAlerte();
            chargerHistoriquePourGraphique();
            JOptionPane.showMessageDialog(this, "Historique restauré avec succès !", "Chargement", JOptionPane.INFORMATION_MESSAGE);
        });
        
        panelSaveBtns.add(btnSaveNow);
        panelSaveBtns.add(btnLoadNow);

        // 5. Réinitialisation
        JLabel lblResetLabel = new JLabel("Zone Administration :");
        lblResetLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblResetLabel.setForeground(COLOR_PRIMARY);

        JButton btnClearHistory = new JButton("Vider la base historique");
        btnClearHistory.setBackground(COLOR_ALERT_TEXT);
        btnClearHistory.setForeground(Color.WHITE);
        btnClearHistory.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnClearHistory.setFocusPainted(false);
        btnClearHistory.addActionListener(e -> {
            int rep = JOptionPane.showConfirmDialog(this, 
                    "Êtes-vous sûr de vouloir supprimer définitivement l'historique ?", 
                    "Confirmer la suppression", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.WARNING_MESSAGE);
            if (rep == JOptionPane.YES_OPTION) {
                try {
                    java.lang.reflect.Field field = CentraleMeteo.class.getDeclaredField("historique");
                    field.setAccessible(true);
                    List<?> list = (List<?>) field.get(centrale);
                    list.clear();
                } catch (Exception ex) {
                    System.err.println("Erreur clear : " + ex.getMessage());
                }
                historiqueTableModel.appliquerFiltre();
                chartPanel.effacer();
                majKpisEtAlerte();
                JOptionPane.showMessageDialog(this, "Historique réinitialisé.", "Nettoyage", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        innerPanel.add(lblSeuil);
        innerPanel.add(panelSliderSeuil);
        innerPanel.add(lblSpeed);
        innerPanel.add(panelSliderDelay);
        innerPanel.add(lblSound);
        innerPanel.add(switchLayout);
        innerPanel.add(lblSaveLabel);
        innerPanel.add(panelSaveBtns);
        innerPanel.add(lblResetLabel);
        innerPanel.add(btnClearHistory);

        panel.add(innerPanel);
        return panel;
    }

    // --- Actions & Helpers ---

    private void selectTab(int index, String cardName) {
        for (int i = 0; i < navButtons.length; i++) {
            if (i == index) {
                navButtons[i].setBackground(COLOR_PRIMARY);
                navButtons[i].setForeground(Color.WHITE);
            } else {
                navButtons[i].setBackground(new Color(240, 243, 248));
                navButtons[i].setForeground(COLOR_TEXT_MUTED);
            }
        }
        cardLayout.show(contentCards, cardName);

        if ("CHARTS".equals(cardName)) {
            chargerHistoriquePourGraphique();
        }
    }

    private void initialiserTimers() {
        timerHorloge = new Timer(1000, e -> {
            String time = LocalTime.now().format(FMT_HORLOGE);
            lblParamClock.setText("Dernière synchro : " + time);
        });
        timerHorloge.start();
    }

    private void chargerHistoriquePourGraphique() {
        Object selected = comboChartZone.getSelectedItem();
        if (selected instanceof Zone z) {
            chartPanel.setSelectedZone(z);
        } else {
            chartPanel.setSelectedZone(null); // Mode comparatif
        }
        chartPanel.repaint();
    }

    private void majInterfaceStatuts() {
        int activeCount = 0;
        int offlineCount = 0;

        for (int i = 0; i < 5; i++) {
            Zone z = Zone.values()[i];
            boolean active = capteurManager.isZoneActive(z);

            // Mettre à jour les indicateurs de la table de gauche
            rowDots[indexToZone(i)].setOnline(active);
            btnRowStart[i].setEnabled(!active);
            btnRowStop[i].setEnabled(active);

            // Mettre à jour la grille de droite (État)
            statusGridDots[i].setOnline(active);
            
            if (active) {
                activeCount++;
                panelStatusItems[i].setBorder(new LineBorder(COLOR_ACCENT, 1, true));
                panelStatusItems[i].setBackground(Color.WHITE);
            } else {
                offlineCount++;
                panelStatusItems[i].setBorder(new LineBorder(COLOR_BORDER, 1, true));
                panelStatusItems[i].setBackground(new Color(249, 250, 252));
                
                // Mettre à vide si inactif
                lblRowTemp[i].setText("--.-°");
                lblRowHum[i].setText("--.-%");
                lblRowAlertBadge[i].setText("Normal");
                lblRowAlertBadge[i].repaint();
                lblStatusGridTemp[i].setText("--.-°");
            }
        }

        // Mettre à jour les badges
        lblTopActiveCountBadge.setText("🌱 " + activeCount + " capteur" + (activeCount > 1 ? "s" : ""));
        
        lblSensorStatusTextFooter.setText(offlineCount == 0 ? 
                "Tous les capteurs sont en ligne" : 
                offlineCount + " capteur" + (offlineCount > 1 ? "s" : "") + " hors ligne · Démarrer");

        // Déclencher le dialogue récapitulatif de session à l'arrêt
        if (lastActiveCount > 0 && activeCount == 0) {
            SwingUtilities.invokeLater(() -> {
                SessionReportDialog dialog = new SessionReportDialog(this, centrale, persistance);
                dialog.setVisible(true);
            });
        }
        lastActiveCount = activeCount;

        majKpisEtAlerte();
    }

    private void gererSignalAlerte(boolean enAlerte) {
        // En mode clair, l'alerte n'est pas clignotante brute, mais on adapte la liste d'alertes
    }

    private void majKpisEtAlerte() {
        int nbAlertes = 0;
        double tempSum = 0;
        double humSum = 0;
        int activeCount = 0;

        // Vider la liste d'alertes visuelle
        panelAlertsListContainer.removeAll();

        for (int i = 0; i < 5; i++) {
            Zone z = Zone.values()[i];
            if (capteurManager.isZoneActive(z)) {
                activeCount++;
                MesureMeteo m = summaryTableModel.getMesure(summaryZoneRow(z));
                if (m != null) {
                    tempSum += m.getTemperature();
                    humSum += m.getHumidite();

                    // Si alerte
                    if (m.getHumidite() < MeteoTableModel.seuilAlerte) {
                        nbAlertes++;
                        ajouterAlertePanel(z, m.getTemperature(), m.getHumidite());
                    }
                }
            }
        }

        // Si aucune alerte
        if (nbAlertes == 0) {
            JPanel emptyAlert = new JPanel(new GridBagLayout());
            emptyAlert.setOpaque(false);
            emptyAlert.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createDashedBorder(COLOR_TEXT_MUTED, 1, 5, 3, true),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            JLabel lblEmpty = new JLabel("✔ Aucune alerte active · irrigation non requise");
            lblEmpty.setFont(new Font("SansSerif", Font.BOLD, 13));
            lblEmpty.setForeground(COLOR_NORMAL_TEXT);
            emptyAlert.add(lblEmpty);
            panelAlertsListContainer.add(emptyAlert);
            
            lblAlertSummaryFooter.setText("Aucune alerte active · seuil d'irrigation respecté");
        } else {
            lblAlertSummaryFooter.setText(nbAlertes + " alerte" + (nbAlertes > 1 ? "s" : "") + " active" + (nbAlertes > 1 ? "s" : "") + " · irrigation urgente requise");
        }

        lblAlertCountBadge.setText(nbAlertes + " en alerte");

        // Pas de KPI box dans le thème clair, le badge supérieur suffit.

        panelAlertsListContainer.revalidate();
        panelAlertsListContainer.repaint();
    }

    private void ajouterAlertePanel(Zone zone, double temp, double hum) {
        JPanel alerteItem = new JPanel(new BorderLayout());
        alerteItem.setBackground(COLOR_ALERT_BG);
        alerteItem.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_ALERT_TEXT, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        alerteItem.setMaximumSize(new Dimension(500, 38));

        JLabel lblZ = new JLabel("⚠ " + zone.name() + " : Irrigation requise !");
        lblZ.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblZ.setForeground(COLOR_ALERT_TEXT);

        JLabel lblDetails = new JLabel(String.format("Temp: %.1f° | Hum: %.1f%%", temp, hum));
        lblDetails.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblDetails.setForeground(COLOR_ALERT_TEXT);

        alerteItem.add(lblZ, BorderLayout.WEST);
        alerteItem.add(lblDetails, BorderLayout.EAST);

        panelAlertsListContainer.add(alerteItem);
        panelAlertsListContainer.add(Box.createVerticalStrut(5));
    }

    private int summaryZoneRow(Zone zone) {
        for (int i = 0; i < summaryTableModel.getRowCount(); i++) {
            MesureMeteo m = summaryTableModel.getMesure(i);
            if (m != null && m.getZone() == zone) {
                return i;
            }
        }
        return -1;
    }

    private int indexToZone(int i) {
        // Enregistre les bons indices des zones
        return i;
    }

    private void restaurerDernieresMesuresDansSummaryTable(List<MesureMeteo> historique) {
        if (historique == null || historique.isEmpty()) return;
        
        for (Zone z : Zone.values()) {
            MesureMeteo plusRecent = null;
            for (MesureMeteo m : historique) {
                if (m.getZone() == z) {
                    if (plusRecent == null || m.getTimestamp().isAfter(plusRecent.getTimestamp())) {
                        plusRecent = m;
                    }
                }
            }
            if (plusRecent != null) {
                summaryTableModel.mettreAJour(plusRecent);
                mettreAJourLigneApercu(plusRecent);
            }
        }
    }

    private void mettreAJourLigneApercu(MesureMeteo m) {
        int idx = m.getZone().ordinal();
        if (idx >= 0 && idx < 5) {
            lblRowTemp[idx].setText(String.format("%.1f°", m.getTemperature()));
            lblRowHum[idx].setText(String.format("%.1f%%", m.getHumidite()));
            lblStatusGridTemp[idx].setText(String.format("%.1f°", m.getTemperature()));

            // Badge Alerte
            boolean alerte = m.getHumidite() < MeteoTableModel.seuilAlerte;
            if (alerte) {
                lblRowAlertBadge[idx].setText("⚠ AL");
                lblRowAlertBadge[idx].setForeground(COLOR_ALERT_TEXT);
            } else {
                lblRowAlertBadge[idx].setText("✔ Normal");
                lblRowAlertBadge[idx].setForeground(COLOR_NORMAL_TEXT);
            }
            lblRowAlertBadge[idx].repaint();
        }
    }

    private void sauvegarderEtFermer() {
        persistance.sauvegarder(centrale.getHistorique());
        capteurManager.arreter();
        System.out.println("AgriWatch — Fermeture.");
        System.exit(0);
    }

    // --- MeteoListener Callback ---

    @Override
    public void onMesureRecue(Zone zone, double temperature, double humidite) {
        SwingUtilities.invokeLater(() -> {
            MesureMeteo mesure = new MesureMeteo(zone, temperature, humidite);
            
            // 1. Mettre à jour le tableau instantané de résumé
            summaryTableModel.mettreAJour(mesure);

            // 2. Mettre à jour la ligne d'aperçu du Dashboard
            mettreAJourLigneApercu(mesure);

            // 3. Mettre à jour le journal d'historique
            historiqueTableModel.appliquerFiltre();

            // 4. Mettre à jour le graphique si nécessaire
            chartPanel.repaint();

            // 5. Mettre à jour l'état et les KPIs
            majInterfaceStatuts();
            
            // 6. Bip d'alerte
            if (humidite < MeteoTableModel.seuilAlerte && bipAlerteActif) {
                Toolkit.getDefaultToolkit().beep();
            }
        });
    }
}