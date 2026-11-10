package pl.warsztatpro.ui.desktop;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

final class EntityPanel<T> extends JPanel {
    @FunctionalInterface interface Loader<T> { List<T> load() throws Exception; }
    private final Loader<T> loader;
    private final Function<T, Object[]> rowMapper;
    private final Predicate<T> matcher;
    private final JTable table = new JTable();
    private final DefaultTableModel model;
    private final JLabel emptyState = new JLabel("Ładowanie…");
    private final JTextField search = new JTextField(22);
    private final JButton extra = DesktopFrame.button("", false);
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private List<T> visibleRecords = List.of();
    private List<T> records = List.of();
    private Consumer<T> editAction = item -> { };
    private Consumer<T> removeAction = item -> { };
    private Runnable addAction = () -> { };
    private Runnable refreshAction = () -> { };

    EntityPanel(String title, String description, String[] columns, Loader<T> loader,
                Function<T, Object[]> rowMapper, Predicate<T> matcher) {
        super(new BorderLayout(0, 16));
        this.loader = loader; this.rowMapper = rowMapper; this.matcher = matcher;
        setOpaque(false);
        model = new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int col) { return false; } };
        build(title, description);
        reload();
    }

    void setActions(Runnable add, Consumer<T> edit, Consumer<T> remove, Runnable refresh) {
        addAction = add; editAction = edit; removeAction = remove; refreshAction = refresh;
        if (addButton != null) addButton.setVisible(add != null);
        if (editButton != null) editButton.setVisible(edit != null);
        if (removeButton != null) removeButton.setVisible(remove != null);
    }
    void setExtraAction(String label, Consumer<T> action) {
        extra.setText(label); extra.setVisible(label != null && !label.isBlank());
        for (java.awt.event.ActionListener listener : extra.getActionListeners()) extra.removeActionListener(listener);
        if (action != null) extra.addActionListener(e -> { T item = selected(); if (item != null) action.accept(item); });
    }
    void setRemoveLabel(String label) { if (removeButton != null) removeButton.setText(label); }

    T selected() {
        int view = table.getSelectedRow();
        if (view < 0) { JOptionPane.showMessageDialog(this, "Najpierw zaznacz rekord na liście.", "Brak wyboru", JOptionPane.INFORMATION_MESSAGE); return null; }
        int index = table.convertRowIndexToModel(view);
        return index >= 0 && index < visibleRecords.size() ? visibleRecords.get(index) : null;
    }

    void reload() {
        emptyState.setText("Ładowanie danych…");
        new SwingWorker<List<T>, Void>() {
            @Override protected List<T> doInBackground() throws Exception { return loader.load(); }
            @Override protected void done() {
                try { records = get(); applyFilter(); }
                catch (Exception e) { emptyState.setText("Nie udało się wczytać danych."); DesktopFrame.showError(EntityPanel.this, e); }
            }
        }.execute();
    }
    void setRecords(List<T> value) { records = List.copyOf(value); applyFilter(); }

    private void build(String title, String description) {
        JPanel top = new JPanel(new BorderLayout(0, 12)); top.setOpaque(false);
        JPanel labels = new JPanel(); labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS)); labels.setOpaque(false);
        JLabel heading = new JLabel(title); heading.setFont(new Font("Segoe UI", Font.BOLD, 25)); heading.setForeground(DesktopFrame.INK);
        JLabel sub = new JLabel(description); sub.setFont(new Font("Segoe UI", Font.PLAIN, 13)); sub.setForeground(DesktopFrame.MUTED);
        labels.add(heading); labels.add(Box.createVerticalStrut(4)); labels.add(sub);
        JPanel tools = new JPanel(); tools.setOpaque(false); tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); filters.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        search.setToolTipText("Szukaj na liście"); search.setPreferredSize(new Dimension(230, 36)); search.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton refresh = DesktopFrame.button("Odśwież", false); refresh.addActionListener(e -> refreshAction.run());
        addButton = DesktopFrame.button("＋  Dodaj", true); addButton.addActionListener(e -> addAction.run());
        editButton = DesktopFrame.button("Edytuj", false); editButton.addActionListener(e -> { T item = selected(); if (item != null) editAction.accept(item); });
        removeButton = DesktopFrame.button("Dezaktywuj / anuluj", false); removeButton.addActionListener(e -> { T item = selected(); if (item != null) removeAction.accept(item); });
        extra.setVisible(false);
        filters.add(new JLabel("Szukaj")); filters.add(search); filters.add(refresh);
        actions.add(addButton); actions.add(editButton); actions.add(removeButton); actions.add(extra);
        tools.add(filters); tools.add(actions);
        top.add(labels, BorderLayout.NORTH); top.add(tools, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        table.setModel(model); table.setRowHeight(38); table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(DesktopFrame.INK); table.setBackground(DesktopFrame.PANEL);
        table.setSelectionForeground(DesktopFrame.INK); table.setSelectionBackground(new Color(49, 46, 129));
        table.setGridColor(DesktopFrame.BORDER); table.setShowHorizontalLines(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setForeground(DesktopFrame.MUTED); table.getTableHeader().setBackground(new Color(31, 36, 50));
        table.getTableHeader().setOpaque(true); table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
                                                                       boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                cell.setForeground(DesktopFrame.INK);
                cell.setBackground(selected ? new Color(49, 46, 129) : row % 2 == 0 ? DesktopFrame.PANEL : new Color(19, 22, 30));
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return cell;
            }
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true); table.setFillsViewportHeight(true); table.setShowVerticalLines(false);
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(DesktopFrame.BORDER));
        JPanel center = new JPanel(new BorderLayout()); center.setBackground(DesktopFrame.PANEL); center.add(scroll, BorderLayout.CENTER);
        emptyState.setHorizontalAlignment(SwingConstants.CENTER); emptyState.setForeground(DesktopFrame.MUTED);
        center.add(emptyState, BorderLayout.SOUTH); add(center, BorderLayout.CENTER);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    private void applyFilter() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        model.setRowCount(0);
        List<T> shownRecords = new ArrayList<>();
        int shown = 0;
        for (T record : records) {
            Object[] row;
            boolean matches;
            try {
                row = rowMapper.apply(record);
                matches = query.isEmpty() || matcher.test(record)
                        && Arrays.stream(row).map(String::valueOf).reduce("", (a, b) -> a + " " + b)
                        .toLowerCase(Locale.ROOT).contains(query);
            } catch (RuntimeException error) {
                row = new Object[model.getColumnCount()];
                if (row.length > 0) row[0] = "Błąd wiersza: " + error.getClass().getSimpleName()
                        + (error.getMessage() == null || error.getMessage().isBlank() ? "" : " — " + error.getMessage());
                matches = query.isEmpty();
            }
            if (row == null || row.length != model.getColumnCount()) {
                Object[] normalized = new Object[model.getColumnCount()];
                if (row != null) System.arraycopy(row, 0, normalized, 0, Math.min(row.length, normalized.length));
                row = normalized;
            }
            if (matches) {
                model.addRow(row); shownRecords.add(record); shown++;
            }
        }
        visibleRecords = shownRecords;
        emptyState.setText(records.isEmpty() ? "Nie ma jeszcze żadnych rekordów." : shown == 0 ? "Brak wyników dla tego wyszukiwania." : "Wyświetlono " + shown + " rekordów");
    }
}
