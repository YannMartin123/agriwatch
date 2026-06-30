package cm.uy1.agriwatch.ui;

import javax.swing.*;
import java.awt.*;

public class StatusDot extends JComponent {

    private boolean online = false;

    public StatusDot() {
        setPreferredSize(new Dimension(14, 14));
    }

    public void setOnline(boolean online) {
        this.online = online;
        repaint();
    }

    public boolean isOnline() {
        return online;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = 8;
        int x = (w - size) / 2;
        int y = (h - size) / 2;

        if (online) {
            // Halo vert translucide
            g2.setColor(new Color(45, 122, 110, 60));
            g2.fillOval(x - 2, y - 2, size + 4, size + 4);
            // Point vert vif
            g2.setColor(new Color(45, 122, 110));
            g2.fillOval(x, y, size, size);
        } else {
            // Halo rouge translucide
            g2.setColor(new Color(179, 74, 44, 40));
            g2.fillOval(x - 2, y - 2, size + 4, size + 4);
            // Point rouge brique
            g2.setColor(new Color(179, 74, 44));
            g2.fillOval(x, y, size, size);
        }
    }
}
