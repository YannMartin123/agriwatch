package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.persistence.PersistanceService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

public class SessionReportDialog extends JDialog {

    public SessionReportDialog(Frame parent, CentraleMeteo centrale, PersistanceService persistance) {
        super(parent, "Rapport d'activité — Simulation AgriWatch", true);
        setSize(850, 580);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(244, 247, 252)); // Gris clair du thème

        // Calculs des statistiques de la session
        List<MesureMeteo> historique = centrale.getHistorique();
        int totalEnregistrements = historique.size();
        
        double tempSum = 0;
        double humSum = 0;
        int alerteCount = 0;
        for (MesureMeteo m : historique) {
            tempSum += m.getTemperature();
            humSum += m.getHumidite();
            if (m.getHumidite() < MeteoTableModel.seuilAlerte) {
                alerteCount++;
            }
        }
        double tempMoy = totalEnregistrements > 0 ? tempSum / totalEnregistrements : 0;
        double humMoy = totalEnregistrements > 0 ? humSum / totalEnregistrements : 0;

        setLayout(new BorderLayout(0, 15));

        // --- EN-TÊTE ---
        JPanel headerPanel = new JPanel(new BorderLayout(15, 5));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(20, 25, 5, 25));

        // Logo Panel
        JPanel logoIcon = new JPanel() {
            private Image imgLogo;
            {
                try {
                    imgLogo = javax.imageio.ImageIO.read(new java.io.File("logo.png"));
                } catch (Exception ex) {
                    System.err.println("Impossible de charger logo.png dans SessionReportDialog : " + ex.getMessage());
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
                    g2.setColor(new Color(30, 43, 60));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 72));
                    g2.drawString("🌱", 38, 100);
                }
            }
        };
        logoIcon.setPreferredSize(new Dimension(150, 150));
        logoIcon.setOpaque(false);

        JPanel titleAndSub = new JPanel(new BorderLayout(5, 2));
        titleAndSub.setOpaque(false);

        JLabel lblTitle = new JLabel("Rapport de Session Historique");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 43, 60));

        JLabel lblSub = new JLabel("La simulation a été suspendue. Voici l'état récapitulatif des capteurs.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSub.setForeground(new Color(91, 103, 123));

        titleAndSub.add(lblTitle, BorderLayout.NORTH);
        titleAndSub.add(lblSub, BorderLayout.SOUTH);

        headerPanel.add(logoIcon, BorderLayout.WEST);
        headerPanel.add(titleAndSub, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // --- ZONE CENTRAL DE STATISTIQUES ET TABLEAU ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 25, 0, 25));

        // Grille de KPI
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiGrid.setOpaque(false);
        kpiGrid.setPreferredSize(new Dimension(0, 75));

        kpiGrid.add(creerKpiBox("Mesures totales", String.valueOf(totalEnregistrements), new Color(59, 130, 246)));
        kpiGrid.add(creerKpiBox("Température Moyenne", String.format("%.1f °C", tempMoy), new Color(245, 158, 11)));
        kpiGrid.add(creerKpiBox("Humidité Moyenne", String.format("%.1f %%", humMoy), new Color(20, 184, 166)));
        kpiGrid.add(creerKpiBox("Alertes déclenchées", String.valueOf(alerteCount), new Color(179, 74, 44)));

        centerPanel.add(kpiGrid, BorderLayout.NORTH);

        // Tableau historique
        RoundedCardPanel tableCard = new RoundedCardPanel();
        tableCard.setLayout(new BorderLayout());

        HistoriqueTableModel tableModel = new HistoriqueTableModel(centrale);
        JTable tableHist = new JTable(tableModel);
        tableHist.setDefaultRenderer(Object.class, new MeteoTableCellRenderer());
        tableHist.setRowHeight(32);
        tableHist.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tableHist.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tableHist.getTableHeader().setBackground(new Color(244, 247, 252));
        tableHist.getTableHeader().setForeground(new Color(30, 43, 60));
        tableHist.setGridColor(new Color(233, 237, 242));

        JScrollPane scroll = new JScrollPane(tableHist);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tableCard.add(scroll, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // --- BARRE DE BOUTONS EN BAS ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(5, 25, 10, 25));

        JButton btnExport = new JButton("📄 Exporter ce rapport (CSV)");
        btnExport.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnExport.setBackground(new Color(45, 122, 110));
        btnExport.setForeground(Color.WHITE);
        btnExport.setFocusPainted(false);
        btnExport.setMargin(new Insets(8, 16, 8, 16));
        btnExport.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "L'exportation en CSV a été désactivée.",
                    "Exportation Désactivée",
                    JOptionPane.WARNING_MESSAGE);
        });

        JButton btnFermer = new JButton("Fermer");
        btnFermer.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnFermer.setBackground(new Color(30, 43, 60));
        btnFermer.setForeground(Color.WHITE);
        btnFermer.setFocusPainted(false);
        btnFermer.setMargin(new Insets(8, 20, 8, 20));
        btnFermer.addActionListener(e -> dispose());

        buttonPanel.add(btnExport);
        buttonPanel.add(btnFermer);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel creerKpiBox(String title, String value, Color accentColor) {
        JPanel box = new JPanel(new BorderLayout(3, 3));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(233, 237, 242), 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblTitle.setForeground(new Color(91, 103, 123));

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblVal.setForeground(accentColor);

        box.add(lblTitle, BorderLayout.NORTH);
        box.add(lblVal, BorderLayout.CENTER);
        return box;
    }
}
