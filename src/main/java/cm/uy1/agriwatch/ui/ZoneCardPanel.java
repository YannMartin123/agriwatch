package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;
import cm.uy1.agriwatch.threading.CapteurManager;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class ZoneCardPanel extends JPanel {

    private final Zone zone;
    private final CapteurManager capteurManager;
    private final Runnable onToggleCallback;

    private double temperature = 0.0;
    private double humidite = 0.0;
    private boolean enAlerte = false;

    private final JLabel lblZoneName;
    private final JLabel lblStatus;
    private final JButton btnToggle;

    public ZoneCardPanel(Zone zone, CapteurManager capteurManager, Runnable onToggleCallback) {
        this.zone = zone;
        this.capteurManager = capteurManager;
        this.onToggleCallback = onToggleCallback;

        // Configuration du conteneur
        setBackground(new Color(30, 41, 59)); // Slate-800
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true), // Bordure Slate-700
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        setLayout(new BorderLayout(10, 10));

        // En-tête : Nom de la zone + État (Actif/Inactif)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        lblZoneName = new JLabel(zone.name());
        lblZoneName.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblZoneName.setForeground(new Color(248, 250, 252)); // Slate-50

        lblStatus = new JLabel("Inactif");
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblStatus.setForeground(new Color(148, 163, 184)); // Slate-400

        headerPanel.add(lblZoneName, BorderLayout.WEST);
        headerPanel.add(lblStatus, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Bouton de contrôle à la base
        btnToggle = new JButton("Démarrer");
        btnToggle.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnToggle.setFocusPainted(false);
        btnToggle.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        
        btnToggle.addActionListener(e -> toggleSensor());
        add(btnToggle, BorderLayout.SOUTH);

        rafraichirInterface();
    }

    public void mettreAJourMesure(double temp, double hum) {
        this.temperature = temp;
        this.humidite = hum;
        this.enAlerte = hum < MeteoTableModel.seuilAlerte;
        rafraichirInterface();
        repaint();
    }

    public void rafraichirInterface() {
        boolean active = capteurManager.isZoneActive(zone);

        // Mise à jour de l'état
        if (active) {
            lblStatus.setText(enAlerte ? "⚠ ALERTE" : "Actif");
            lblStatus.setForeground(enAlerte ? new Color(239, 68, 68) : new Color(34, 197, 94)); // Rouge ou Vert
            
            btnToggle.setText("Arrêter");
            btnToggle.setBackground(new Color(220, 38, 38)); // Rouge
            btnToggle.setForeground(Color.WHITE);

            // Ajustement de la bordure si en alerte
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(enAlerte ? new Color(239, 68, 68) : new Color(34, 197, 94), 2, true),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)
            ));
        } else {
            lblStatus.setText("Inactif");
            lblStatus.setForeground(new Color(148, 163, 184)); // Slate-400
            
            btnToggle.setText("Démarrer");
            btnToggle.setBackground(new Color(13, 148, 136)); // Teal
            btnToggle.setForeground(Color.WHITE);

            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(51, 65, 85), 1, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
        }
    }

    private void toggleSensor() {
        boolean active = capteurManager.isZoneActive(zone);
        if (active) {
            capteurManager.arreterZone(zone);
        } else {
            // Callback vers FenetrePrincipale pour démarrer en passant CentraleMeteo
            onToggleCallback.run();
        }
        rafraichirInterface();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean active = capteurManager.isZoneActive(zone);
        int w = getWidth();
        int yStart = 60;

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));

        if (active) {
            // 1. Barre Température
            g2.setColor(new Color(226, 232, 240)); // Slate-200
            g2.drawString(String.format("Température: %.1f °C", temperature), 15, yStart);
            drawProgressBar(g2, 15, yStart + 8, w - 30, 8, temperature, 50.0, new Color(249, 115, 22)); // Orange

            // 2. Barre Humidité
            g2.setColor(new Color(226, 232, 240));
            g2.drawString(String.format("Humidité: %.1f %%", humidite), 15, yStart + 36);
            drawProgressBar(g2, 15, yStart + 44, w - 30, 8, humidite, 100.0, 
                    enAlerte ? new Color(239, 68, 68) : new Color(6, 182, 212)); // Rouge ou Cyan
        } else {
            g2.setColor(new Color(148, 163, 184)); // Slate-400
            g2.setFont(new Font("SansSerif", Font.ITALIC, 16));
            String text = "Capteur hors ligne";
            int textW = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (w - textW) / 2, yStart + 25);
        }
    }

    private void drawProgressBar(Graphics2D g2, int x, int y, int width, int height, double val, double maxVal, Color color) {
        g2.setColor(new Color(71, 85, 105)); // Slate-600 (arrière-plan de la barre)
        g2.fillRoundRect(x, y, width, height, height, height);
        
        int fillW = (int) ((val / maxVal) * width);
        fillW = Math.max(0, Math.min(width, fillW));
        
        g2.setColor(color);
        g2.fillRoundRect(x, y, fillW, height, height, height);
    }
}
