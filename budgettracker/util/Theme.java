package budgettracker.util;

import java.awt.*;

public class Theme {
    // Backgrounds
    public static final Color BG_DARK     = new Color(15, 15, 25);
    public static final Color BG_PANEL    = new Color(22, 22, 38);
    public static final Color BG_CARD     = new Color(32, 32, 52);
    public static final Color BG_HOVER    = new Color(42, 42, 68);
    public static final Color BG_SELECTED = new Color(55, 55, 90);
    public static final Color BG_INPUT    = new Color(28, 28, 46);
    public static final Color BG_BUTTON   = new Color(45, 45, 72);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(220, 220, 235);
    public static final Color TEXT_SECONDARY = new Color(155, 155, 180);
    public static final Color TEXT_MUTED     = new Color(95, 95, 130);
    public static final Color TEXT_RED       = new Color(231, 76, 60);
    public static final Color TEXT_GREEN     = new Color(46, 204, 113);
    public static final Color TEXT_YELLOW    = new Color(241, 196, 15);

    // Accents
    public static final Color ACCENT_BLUE   = new Color(74, 144, 226);
    public static final Color ACCENT_GREEN  = new Color(46, 204, 113);
    public static final Color ACCENT_RED    = new Color(231, 76, 60);
    public static final Color ACCENT_ORANGE = new Color(243, 156, 18);
    public static final Color ACCENT_PURPLE = new Color(155, 89, 182);

    // Borders
    public static final Color BORDER       = new Color(45, 45, 72);
    public static final Color BORDER_LIGHT = new Color(65, 65, 100);

    // Fonts
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_LARGE   = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_MEDIUM  = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD_SM = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO    = new Font("Monospaced", Font.PLAIN, 12);

    public static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color hexToColor(String hex) {
        if (hex == null || hex.isEmpty()) return Color.GRAY;
        try {
            if (!hex.startsWith("#")) hex = "#" + hex;
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }

    /** Creates a styled button with dark theme appearance */
    public static javax.swing.JButton makeButton(String text, Color accent) {
        javax.swing.JButton btn = new javax.swing.JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? accent.darker() :
                           getModel().isRollover() ? accent.brighter() : accent;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD_SM);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 32));
        return btn;
    }

    /** Creates a subtle secondary button */
    public static javax.swing.JButton makeSecondaryButton(String text) {
        return makeButton(text, BG_BUTTON);
    }

    /** Applies dark theme styling to a JScrollPane */
    public static void styleScrollPane(javax.swing.JScrollPane sp) {
        sp.getViewport().setBackground(BG_DARK);
        sp.setBackground(BG_DARK);
        sp.setBorder(javax.swing.BorderFactory.createLineBorder(BORDER));
        sp.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BORDER_LIGHT;
                trackColor = BG_PANEL;
            }
            @Override protected javax.swing.JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override protected javax.swing.JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private javax.swing.JButton createZeroButton() {
                javax.swing.JButton btn = new javax.swing.JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }
        });
        sp.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = BORDER_LIGHT;
                trackColor = BG_PANEL;
            }
            @Override protected javax.swing.JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override protected javax.swing.JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private javax.swing.JButton createZeroButton() {
                javax.swing.JButton btn = new javax.swing.JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                return btn;
            }
        });
    }

    /**
     * Makes a spinner's text field select-all on focus when the current value is 0,
     * so the user can just start typing without manually deleting the zero.
     */
    public static void addClearOnZeroFocus(javax.swing.JSpinner spinner) {
        javax.swing.JComponent ed = spinner.getEditor();
        if (ed instanceof javax.swing.JSpinner.DefaultEditor) {
            javax.swing.JTextField tf = ((javax.swing.JSpinner.DefaultEditor) ed).getTextField();
            tf.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        Object val = spinner.getValue();
                        if (val instanceof Number && ((Number) val).doubleValue() == 0.0) {
                            tf.selectAll();
                        }
                    });
                }
            });
        }
    }

    /** Style a JTextField for dark mode */
    public static void styleTextField(javax.swing.JTextField tf) {
        tf.setBackground(BG_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(TEXT_PRIMARY);
        tf.setFont(FONT_MEDIUM);
        tf.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(BORDER_LIGHT),
            javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    /** Style a JComboBox for dark mode */
    @SuppressWarnings("unchecked")
    public static void styleComboBox(javax.swing.JComboBox<?> cb) {
        cb.setBackground(BG_INPUT);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_MEDIUM);
        cb.setBorder(javax.swing.BorderFactory.createLineBorder(BORDER_LIGHT));
        cb.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? BG_SELECTED : BG_INPUT);
                setForeground(TEXT_PRIMARY);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });
        cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected javax.swing.JButton createArrowButton() {
                javax.swing.JButton btn = new javax.swing.JButton("v");
                btn.setBackground(BG_BUTTON);
                btn.setForeground(TEXT_SECONDARY);
                btn.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                btn.setFont(FONT_SMALL);
                return btn;
            }
        });
    }
}
