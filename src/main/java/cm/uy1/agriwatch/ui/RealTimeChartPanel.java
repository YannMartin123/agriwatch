package cm.uy1.agriwatch.ui;

import cm.uy1.agriwatch.core.CentraleMeteo;
import cm.uy1.agriwatch.core.MesureMeteo;
import cm.uy1.agriwatch.core.Zone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class RealTimeChartPanel extends JPanel {

    private CentraleMeteo centrale;
    private Zone selectedZone = Zone.ZONE_A; // Si null = Mode Multi-Zone
    private String multiZoneMetric = "Température"; // "Température" ou "Humidité"

    private static final int MAX_POINTS = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Pour l'interactivité du curseur
    private Point mousePoint = null;

    // Couleurs distinctes pour les 5 zones (Thème moderne)
    private static final Color[] ZONE_COLORS = {
            new Color(59, 130, 246),  // Zone A : Bleu
            new Color(16, 185, 129),  // Zone B : Vert
            new Color(245, 158, 11),  // Zone C : Orange
            new Color(139, 92, 246),  // Zone D : Violet
            new Color(20, 184, 166)   // Zone E : Teal
    };

    public RealTimeChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 350));

        // Listeners de souris pour le survol
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePoint = null;
                repaint();
            }
        };
        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    public void setCentrale(CentraleMeteo centrale) {
        this.centrale = centrale;
    }

    public void setSelectedZone(Zone zone) {
        this.selectedZone = zone;
        repaint();
    }

    public void setMultiZoneMetric(String metric) {
        this.multiZoneMetric = metric;
        repaint();
    }

    public void effacer() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (centrale == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padding = 55;
        int chartW = w - 2 * padding;
        int chartH = h - 2 * padding - 20;

        // 1. Dessiner le fond du graphique
        g2.setColor(new Color(244, 247, 252)); // Gris clair bleuté
        g2.fillRoundRect(padding, padding, chartW, chartH, 12, 12);

        // Dessiner les grilles horizontales
        g2.setColor(new Color(225, 230, 238));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f));
        for (int i = 0; i <= 4; i++) {
            int y = padding + i * (chartH / 4);
            g2.drawLine(padding, y, padding + chartW, y);
        }

        // 2. Extraire et filtrer les données
        List<MesureMeteo> hist = centrale.getHistorique();
        boolean isMulti = (selectedZone == null);

        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(new Color(91, 103, 123)); // Couleur du texte d'échelle
        g2.setStroke(new BasicStroke(1.5f));

        if (!isMulti) {
            // --- MODE SINGLE ZONE ---
            // Échelle Ordonnées gauche (Température 0 - 50 °C)
            for (int i = 0; i <= 4; i++) {
                int val = 50 - i * 12;
                int y = padding + i * (chartH / 4);
                g2.drawString(val + "°C", padding - 38, y + 4);
            }
            // Échelle Ordonnées droite (Humidité 0 - 100 %)
            for (int i = 0; i <= 4; i++) {
                int val = 100 - i * 25;
                int y = padding + i * (chartH / 4);
                g2.drawString(val + "%", padding + chartW + 8, y + 4);
            }

            // Filtrer la zone sélectionnée
            List<MesureMeteo> points = filtrerMesures(hist, selectedZone);

            if (points.size() < 2) {
                dessinerMessageAttente(g2, w, h);
                return;
            }

            double stepX = (double) chartW / (MAX_POINTS - 1);
            int size = points.size();
            double startX = padding + (MAX_POINTS - size) * stepX;

            // Courbe Température (Orange)
            GeneralPath pathTemp = new GeneralPath();
            pathTemp.moveTo(startX, calculerYTemp(points.get(0).getTemperature(), padding, chartH));
            for (int i = 1; i < size; i++) {
                double x = startX + i * stepX;
                double y = calculerYTemp(points.get(i).getTemperature(), padding, chartH);
                pathTemp.lineTo(x, y);
            }
            // Remplissage sous courbe
            GeneralPath fillTemp = new GeneralPath(pathTemp);
            fillTemp.lineTo(padding + (MAX_POINTS - 1) * stepX, padding + chartH);
            fillTemp.lineTo(startX, padding + chartH);
            fillTemp.closePath();
            g2.setPaint(new GradientPaint(0, padding, new Color(249, 115, 22, 40), 0, padding + chartH, new Color(249, 115, 22, 0)));
            g2.fill(fillTemp);

            g2.setColor(new Color(249, 115, 22));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(pathTemp);

            // Courbe Humidité (Teal)
            GeneralPath pathHum = new GeneralPath();
            pathHum.moveTo(startX, calculerYHum(points.get(0).getHumidite(), padding, chartH));
            for (int i = 1; i < size; i++) {
                double x = startX + i * stepX;
                double y = calculerYHum(points.get(i).getHumidite(), padding, chartH);
                pathHum.lineTo(x, y);
            }
            GeneralPath fillHum = new GeneralPath(pathHum);
            fillHum.lineTo(padding + (MAX_POINTS - 1) * stepX, padding + chartH);
            fillHum.lineTo(startX, padding + chartH);
            fillHum.closePath();
            g2.setPaint(new GradientPaint(0, padding, new Color(45, 122, 110, 40), 0, padding + chartH, new Color(45, 122, 110, 0)));
            g2.fill(fillHum);

            g2.setColor(new Color(45, 122, 110));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(pathHum);

            // Dessiner Légende
            dessinerLegendeSingleZone(g2, padding, h);

            // Gestion de l'interactivité
            gererHoverSingleZone(g2, points, stepX, padding, chartH, chartW);

        } else {
            // --- MODE MULTI-ZONE (RÉCAPITULATIF) ---
            boolean isTemp = "Température".equals(multiZoneMetric);
            int maxVal = isTemp ? 50 : 100;
            
            // Échelle ordonnées (gauche)
            for (int i = 0; i <= 4; i++) {
                int val = maxVal - i * (maxVal / 4);
                int y = padding + i * (chartH / 4);
                g2.drawString(val + (isTemp ? "°C" : "%"), padding - 38, y + 4);
            }

            // Récupérer les données récentes des 5 zones
            List<List<MesureMeteo>> allSeries = new ArrayList<>();
            int maxPointsCount = 0;
            for (Zone z : Zone.values()) {
                List<MesureMeteo> series = filtrerMesures(hist, z);
                allSeries.add(series);
                maxPointsCount = Math.max(maxPointsCount, series.size());
            }

            if (maxPointsCount < 2) {
                dessinerMessageAttente(g2, w, h);
                return;
            }

            double stepX = (double) chartW / (MAX_POINTS - 1);

            // Tracer la courbe pour chaque zone
            for (int zIdx = 0; zIdx < 5; zIdx++) {
                List<MesureMeteo> series = allSeries.get(zIdx);
                if (series.size() < 2) continue;

                int size = series.size();
                double startX = padding + (MAX_POINTS - size) * stepX;

                GeneralPath path = new GeneralPath();
                double val0 = isTemp ? series.get(0).getTemperature() : series.get(0).getHumidite();
                double y0 = isTemp ? calculerYTemp(val0, padding, chartH) : calculerYHum(val0, padding, chartH);
                path.moveTo(startX, y0);

                for (int i = 1; i < size; i++) {
                    double x = startX + i * stepX;
                    double val = isTemp ? series.get(i).getTemperature() : series.get(i).getHumidite();
                    double y = isTemp ? calculerYTemp(val, padding, chartH) : calculerYHum(val, padding, chartH);
                    path.lineTo(x, y);
                }

                g2.setColor(ZONE_COLORS[zIdx]);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(path);
            }

            // Légende
            dessinerLegendeMultiZone(g2, padding, h);

            // Hover interactif multi-zone
            gererHoverMultiZone(g2, allSeries, stepX, padding, chartH, chartW, isTemp);
        }
    }

    // --- Helpers de dessin et calculs ---

    private List<MesureMeteo> filtrerMesures(List<MesureMeteo> hist, Zone zone) {
        List<MesureMeteo> filtered = new ArrayList<>();
        for (MesureMeteo m : hist) {
            if (m.getZone() == zone) {
                filtered.add(m);
            }
        }
        if (filtered.size() > MAX_POINTS) {
            filtered = filtered.subList(filtered.size() - MAX_POINTS, filtered.size());
        }
        return filtered;
    }

    private double calculerYTemp(double temp, int padding, int chartH) {
        double y = padding + chartH - (temp / 50.0) * chartH;
        return Math.max(padding, Math.min(padding + chartH, y));
    }

    private double calculerYHum(double hum, int padding, int chartH) {
        double y = padding + chartH - (hum / 100.0) * chartH;
        return Math.max(padding, Math.min(padding + chartH, y));
    }

    private void dessinerMessageAttente(Graphics2D g2, int w, int h) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(new Color(91, 103, 123));
        String msg = "En attente de mesures pour afficher le graphique...";
        int msgW = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (w - msgW) / 2, h / 2 - 10);
    }

    private void dessinerLegendeSingleZone(Graphics2D g2, int padding, int h) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(249, 115, 22)); // Temp Orange
        g2.fillRect(padding + 10, h - 35, 10, 10);
        g2.setColor(new Color(30, 43, 60));
        g2.drawString("Température (°C)", padding + 25, h - 26);

        g2.setColor(new Color(45, 122, 110)); // Hum Teal
        g2.fillRect(padding + 160, h - 35, 10, 10);
        g2.setColor(new Color(30, 43, 60));
        g2.drawString("Humidité (%)", padding + 175, h - 26);
    }

    private void dessinerLegendeMultiZone(Graphics2D g2, int padding, int h) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        int startX = padding + 10;
        Zone[] zones = Zone.values();
        for (int i = 0; i < 5; i++) {
            g2.setColor(ZONE_COLORS[i]);
            g2.fillRect(startX, h - 35, 8, 8);
            g2.setColor(new Color(30, 43, 60));
            g2.drawString(zones[i].name(), startX + 13, h - 27);
            startX += 90;
        }
    }

    // --- Hover et Tooltips ---

    private void gererHoverSingleZone(Graphics2D g2, List<MesureMeteo> points, double stepX, int padding, int chartH, int chartW) {
        int size = points.size();
        if (mousePoint != null && mousePoint.x >= padding && mousePoint.x <= padding + chartW) {
            int i = (int) Math.round((mousePoint.x - padding) / stepX - (MAX_POINTS - size));
            i = Math.max(0, Math.min(size - 1, i));

            double ptX = padding + (MAX_POINTS - size + i) * stepX;
            MesureMeteo m = points.get(i);

            // Ligne verticale de repère
            g2.setColor(new Color(148, 163, 184, 180));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f));
            g2.drawLine((int) ptX, padding, (int) ptX, padding + chartH);

            // Points en surbrillance
            double yTemp = calculerYTemp(m.getTemperature(), padding, chartH);
            double yHum = calculerYHum(m.getHumidite(), padding, chartH);

            g2.setStroke(new BasicStroke(2f));
            // Rond Température (Orange)
            g2.setColor(Color.WHITE);
            g2.fillOval((int) ptX - 5, (int) yTemp - 5, 10, 10);
            g2.setColor(new Color(249, 115, 22));
            g2.drawOval((int) ptX - 5, (int) yTemp - 5, 10, 10);

            // Rond Humidité (Teal)
            g2.setColor(Color.WHITE);
            g2.fillOval((int) ptX - 5, (int) yHum - 5, 10, 10);
            g2.setColor(new Color(45, 122, 110));
            g2.drawOval((int) ptX - 5, (int) yHum - 5, 10, 10);

            // Dessiner la Tooltip Card
            int boxW = 150;
            int boxH = 75;
            int boxX = (ptX + boxW + 15 < getWidth()) ? (int) ptX + 15 : (int) ptX - boxW - 15;
            int boxY = Math.max(padding + 10, mousePoint.y - boxH / 2);

            g2.setColor(new Color(30, 43, 60, 245)); // Fond sombre translucide
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
            g2.setColor(new Color(233, 237, 242));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);

            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(new Color(208, 216, 227));
            g2.drawString("Heure : " + m.getTimestamp().format(TIME_FORMATTER), boxX + 12, boxY + 20);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(249, 115, 22)); // Orange
            g2.drawString(String.format("Temp: %.1f °C", m.getTemperature()), boxX + 12, boxY + 40);
            g2.setColor(new Color(34, 211, 238)); // Cyan/Teal clair
            g2.drawString(String.format("Hum: %.1f %%", m.getHumidite()), boxX + 12, boxY + 58);
        }
    }

    private void gererHoverMultiZone(Graphics2D g2, List<List<MesureMeteo>> allSeries, double stepX, int padding, int chartH, int chartW, boolean isTemp) {
        // Chercher l'index commun
        if (mousePoint != null && mousePoint.x >= padding && mousePoint.x <= padding + chartW) {
            // Trouver la série qui a le plus d'éléments pour calculer l'index au survol
            int maxSize = 0;
            for (List<MesureMeteo> s : allSeries) {
                maxSize = Math.max(maxSize, s.size());
            }
            if (maxSize == 0) return;

            int i = (int) Math.round((mousePoint.x - padding) / stepX - (MAX_POINTS - maxSize));
            i = Math.max(0, Math.min(maxSize - 1, i));

            double ptX = padding + (MAX_POINTS - maxSize + i) * stepX;

            // Ligne verticale
            g2.setColor(new Color(148, 163, 184, 180));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f));
            g2.drawLine((int) ptX, padding, (int) ptX, padding + chartH);

            List<String> lines = new ArrayList<>();
            String hourText = "--:--:--";

            // Parcourir les 5 zones pour dessiner le point et extraire la valeur
            g2.setStroke(new BasicStroke(2f));
            for (int zIdx = 0; zIdx < 5; zIdx++) {
                List<MesureMeteo> series = allSeries.get(zIdx);
                if (i < series.size()) {
                    MesureMeteo m = series.get(i);
                    hourText = m.getTimestamp().format(TIME_FORMATTER);
                    double val = isTemp ? m.getTemperature() : m.getHumidite();
                    double y = isTemp ? calculerYTemp(val, padding, chartH) : calculerYHum(val, padding, chartH);

                    // Rond sur la courbe
                    g2.setColor(Color.WHITE);
                    g2.fillOval((int) ptX - 4, (int) y - 4, 8, 8);
                    g2.setColor(ZONE_COLORS[zIdx]);
                    g2.drawOval((int) ptX - 4, (int) y - 4, 8, 8);

                    lines.add(String.format("Zone %C: %.1f%s", (char) ('A' + zIdx), val, isTemp ? "°C" : "%"));
                }
            }

            // Dessiner la Tooltip Card Multi-Zone
            int boxW = 160;
            int boxH = 30 + lines.size() * 16;
            int boxX = (ptX + boxW + 15 < getWidth()) ? (int) ptX + 15 : (int) ptX - boxW - 15;
            int boxY = Math.max(padding + 5, mousePoint.y - boxH / 2);

            g2.setColor(new Color(30, 43, 60, 245));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
            g2.setColor(new Color(233, 237, 242));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);

            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.setColor(new Color(208, 216, 227));
            g2.drawString("Heure : " + hourText, boxX + 12, boxY + 18);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            for (int l = 0; l < lines.size(); l++) {
                g2.setColor(ZONE_COLORS[l]);
                g2.drawString(lines.get(l), boxX + 12, boxY + 36 + l * 15);
            }
        }
    }
}
