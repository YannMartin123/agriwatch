package cm.uy1.agriwatch.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ToggleSwitch extends JComponent {

    private boolean selected = true;
    private Runnable onToggle;

    public ToggleSwitch() {
        setPreferredSize(new Dimension(36, 20));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = !selected;
                repaint();
                if (onToggle != null) {
                    onToggle.run();
                }
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setOnToggle(Runnable onToggle) {
        this.onToggle = onToggle;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Arrière-plan
        if (selected) {
            g2.setColor(new Color(45, 122, 110)); // Vert
        } else {
            g2.setColor(new Color(176, 188, 202)); // Gris
        }
        g2.fillRoundRect(0, 0, w, h, h, h);

        // Bouton blanc coulissant
        g2.setColor(Color.WHITE);
        int margin = 3;
        int d = h - 2 * margin;
        if (selected) {
            g2.fillOval(w - d - margin, margin, d, d);
        } else {
            g2.fillOval(margin, margin, d, d);
        }
    }
}
