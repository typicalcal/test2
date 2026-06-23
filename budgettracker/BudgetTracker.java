package budgettracker;

import budgettracker.data.AppData;
import budgettracker.persistence.DataManager;
import budgettracker.ui.MainWindow;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;

public class BudgetTracker {
    public static void main(String[] args) {
        // Apply dark theme system properties
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            applyDarkLookAndFeel();
            AppData data = DataManager.load();
            MainWindow window = new MainWindow(data);
            window.setVisible(true);
        });
    }

    private static void applyDarkLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global UI overrides for dark mode
        UIManager.put("Panel.background",            Theme.BG_PANEL);
        UIManager.put("OptionPane.background",       Theme.BG_PANEL);
        UIManager.put("OptionPane.messageForeground",Theme.TEXT_PRIMARY);
        UIManager.put("TextField.background",        Theme.BG_INPUT);
        UIManager.put("TextField.foreground",        Theme.TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground",   Theme.TEXT_PRIMARY);
        UIManager.put("FormattedTextField.background",Theme.BG_INPUT);
        UIManager.put("FormattedTextField.foreground",Theme.TEXT_PRIMARY);
        UIManager.put("TextArea.background",         Theme.BG_INPUT);
        UIManager.put("TextArea.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("ComboBox.background",         Theme.BG_INPUT);
        UIManager.put("ComboBox.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground",Theme.BG_SELECTED);
        UIManager.put("ComboBox.selectionForeground",Theme.TEXT_PRIMARY);
        UIManager.put("List.background",             Theme.BG_CARD);
        UIManager.put("List.foreground",             Theme.TEXT_PRIMARY);
        UIManager.put("List.selectionBackground",    Theme.BG_SELECTED);
        UIManager.put("List.selectionForeground",    Theme.TEXT_PRIMARY);
        UIManager.put("Table.background",            Theme.BG_PANEL);
        UIManager.put("Table.foreground",            Theme.TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground",   Theme.BG_SELECTED);
        UIManager.put("Table.selectionForeground",   Theme.TEXT_PRIMARY);
        UIManager.put("Table.gridColor",             Theme.BORDER);
        UIManager.put("TableHeader.background",      Theme.BG_CARD);
        UIManager.put("TableHeader.foreground",      Theme.TEXT_SECONDARY);
        UIManager.put("ScrollPane.background",       Theme.BG_DARK);
        UIManager.put("Viewport.background",         Theme.BG_DARK);
        UIManager.put("Button.background",           Theme.BG_BUTTON);
        UIManager.put("Button.foreground",           Theme.TEXT_PRIMARY);
        UIManager.put("Button.select",               Theme.BG_SELECTED);
        UIManager.put("Label.foreground",            Theme.TEXT_PRIMARY);
        UIManager.put("CheckBox.background",         Theme.BG_PANEL);
        UIManager.put("CheckBox.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("RadioButton.background",      Theme.BG_PANEL);
        UIManager.put("RadioButton.foreground",      Theme.TEXT_SECONDARY);
        UIManager.put("TabbedPane.background",       Theme.BG_PANEL);
        UIManager.put("TabbedPane.foreground",       Theme.TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected",         Theme.BG_CARD);
        UIManager.put("TabbedPane.contentAreaColor", Theme.BG_PANEL);
        UIManager.put("TabbedPane.tabAreaBackground",Theme.BG_PANEL);
        UIManager.put("Spinner.background",          Theme.BG_INPUT);
        UIManager.put("Spinner.foreground",          Theme.TEXT_PRIMARY);
        UIManager.put("Dialog.background",           Theme.BG_PANEL);
        UIManager.put("ColorChooser.background",     Theme.BG_PANEL);
        UIManager.put("ToolTip.background",          Theme.BG_CARD);
        UIManager.put("ToolTip.foreground",          Theme.TEXT_PRIMARY);
        UIManager.put("SplitPane.background",        Theme.BG_DARK);
        UIManager.put("Separator.foreground",        Theme.BORDER);
        UIManager.put("Menu.background",             Theme.BG_PANEL);
        UIManager.put("Menu.foreground",             Theme.TEXT_PRIMARY);
        UIManager.put("MenuItem.background",         Theme.BG_PANEL);
        UIManager.put("MenuItem.foreground",         Theme.TEXT_PRIMARY);
        UIManager.put("PopupMenu.background",        Theme.BG_PANEL);
        UIManager.put("PopupMenu.foreground",        Theme.TEXT_PRIMARY);

        // Fonts
        Font baseFont = Theme.FONT_MEDIUM;
        for (Object key : UIManager.getLookAndFeelDefaults().keySet()) {
            if (key instanceof String && ((String)key).endsWith(".font")) {
                UIManager.put(key, baseFont);
            }
        }
    }
}
