package budgettracker.ui.dialog;

import budgettracker.data.AppData;
import budgettracker.persistence.DataManager;
import budgettracker.persistence.SnapshotManager;
import budgettracker.util.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class SnapshotManagerDialog extends JDialog {
    private AppData appData;
    private AppData restoredData = null;
    private DefaultListModel<String> listModel;
    private JList<String> snapshotList;

    public SnapshotManagerDialog(Frame parent, AppData appData) {
        super(parent, "Snapshot Manager", true);
        this.appData = appData;
        buildUI();
        setSize(520, 400);
        setResizable(true);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0, Theme.BORDER),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        JLabel title = new JLabel("Reset Snapshots");
        title.setFont(Theme.FONT_LARGE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        JLabel info = new JLabel("Last 5 snapshots kept");
        info.setFont(Theme.FONT_SMALL);
        info.setForeground(Theme.TEXT_MUTED);
        header.add(info, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // List
        listModel = new DefaultListModel<>();
        refreshList();
        snapshotList = new JList<>(listModel);
        snapshotList.setBackground(Theme.BG_CARD);
        snapshotList.setForeground(Theme.TEXT_PRIMARY);
        snapshotList.setFont(Theme.FONT_MEDIUM);
        snapshotList.setSelectionBackground(Theme.BG_SELECTED);
        snapshotList.setSelectionForeground(Theme.TEXT_PRIMARY);
        snapshotList.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        snapshotList.setFixedCellHeight(36);
        snapshotList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? Theme.BG_SELECTED : (index % 2 == 0 ? Theme.BG_CARD : Theme.BG_PANEL));
                setForeground(Theme.TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                String filename = (String) value;
                setText(SnapshotManager.formatSnapshotName(filename));
                return this;
            }
        });

        JScrollPane sp = new JScrollPane(snapshotList);
        Theme.styleScrollPane(sp);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        add(sp, BorderLayout.CENTER);

        // Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createMatteBorder(1,0,0,0, Theme.BORDER));

        JButton restoreBtn = Theme.makeButton("Restore Selected", Theme.ACCENT_BLUE);
        restoreBtn.addActionListener(e -> doRestore());

        JButton deleteBtn = Theme.makeButton("Delete Selected", Theme.ACCENT_RED);
        deleteBtn.addActionListener(e -> doDelete());

        JButton closeBtn = Theme.makeSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        actions.add(restoreBtn);
        actions.add(deleteBtn);
        actions.add(closeBtn);
        add(actions, BorderLayout.SOUTH);
    }

    private void refreshList() {
        listModel.clear();
        for (String name : SnapshotManager.listSnapshotNames()) {
            listModel.addElement(name);
        }
        if (listModel.isEmpty()) {
            listModel.addElement("(No snapshots available)");
        }
    }

    private void doRestore() {
        String sel = snapshotList.getSelectedValue();
        if (sel == null || sel.startsWith("(")) return;

        int choice = JOptionPane.showOptionDialog(this,
            "Restore snapshot \"" + SnapshotManager.formatSnapshotName(sel) + "\"?\n" +
            "Current data will be replaced.",
            "Restore Snapshot",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[]{"Save current & Restore", "Restore without saving", "Cancel"},
            "Save current & Restore");

        if (choice == 2 || choice < 0) return; // Cancel

        try {
            if (choice == 0) {
                // Save current state first
                SnapshotManager.createSnapshot(appData);
            }
            restoredData = SnapshotManager.restoreSnapshot(sel);
            DataManager.save(restoredData);
            JOptionPane.showMessageDialog(this, "Snapshot restored successfully.", "Done",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to restore: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDelete() {
        String sel = snapshotList.getSelectedValue();
        if (sel == null || sel.startsWith("(")) return;
        int r = JOptionPane.showConfirmDialog(this,
            "Delete this snapshot? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        try {
            SnapshotManager.deleteSnapshot(sel);
            refreshList();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete: " + ex.getMessage());
        }
    }

    public AppData getRestoredData() { return restoredData; }
}
