package pl.warsztatpro.ui.desktop;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Function;

final class FormDialog {
    private enum FieldKind { TEXT, CHOICE, SCHEDULE }
    record Option<T>(T value, String label) {
        @Override public String toString() { return label; }
    }
    record Field(String name, Object value, List<?> choices, String dependsOn, Function<Object, List<?>> dependentChoices, FieldKind kind) {
        static Field text(String name, String value) { return new Field(name, value == null ? "" : value, null, null, null, FieldKind.TEXT); }
        static Field choice(String name, List<?> choices, Object selected) { return new Field(name, selected, choices, null, null, FieldKind.CHOICE); }
        static Field schedule(String name, LocalDateTime value) { return new Field(name, value, null, null, null, FieldKind.SCHEDULE); }
        static Field dependentChoice(String name, List<?> choices, Object selected, String dependsOn, Function<Object, List<?>> selector) {
            return new Field(name, selected, choices, dependsOn, selector, FieldKind.CHOICE);
        }
    }
    private FormDialog() { }

    static Map<String, Object> show(Component parent, String title, List<Field> fields) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DesktopFrame.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        Map<String, JComponent> inputs = new LinkedHashMap<>();
        Map<String, JLabel> labels = new LinkedHashMap<>();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4); c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            c.gridx = 0; c.gridy = i; c.weightx = 0; c.fill = GridBagConstraints.NONE;
            JLabel label = new JLabel(field.name());
            label.setForeground(DesktopFrame.INK);
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));
            labels.put(field.name(), label);
            form.add(label, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            JComponent input;
            if (field.kind() == FieldKind.TEXT) {
                JTextField text = new JTextField(field.value() == null ? "" : field.value().toString(), 24);
                text.setForeground(DesktopFrame.INK);
                text.setBackground(new Color(15, 17, 23));
                text.setCaretColor(DesktopFrame.INK);
                text.setSelectionColor(new Color(49, 46, 129));
                text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                input = text;
            } else if (field.kind() == FieldKind.SCHEDULE) {
                input = new ScheduleInput((LocalDateTime) field.value());
            } else {
                JComboBox<Object> combo = new JComboBox<>(field.choices().toArray());
                combo.setSelectedItem(field.value());
                combo.setForeground(DesktopFrame.INK);
                combo.setBackground(new Color(15, 17, 23));
                combo.setOpaque(true);
                combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                combo.setRenderer(new DefaultListCellRenderer() {
                    @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                        Component cell = super.getListCellRendererComponent(list, choiceLabel(value), index, selected, focus);
                        cell.setForeground(DesktopFrame.INK);
                        cell.setBackground(selected ? new Color(49, 46, 129) : new Color(15, 17, 23));
                        return cell;
                    }
                });
                input = combo;
            }
            Dimension inputSize = input.getPreferredSize();
            input.setPreferredSize(field.kind() == FieldKind.SCHEDULE
                    ? new Dimension(540, Math.max(44, inputSize.height))
                    : new Dimension(380, Math.max(32, inputSize.height)));
            inputs.put(field.name(), input);
            form.add(input, c);
            if (!(input instanceof ScheduleInput)) installFocusFeedback(input);
        }
        Runnable refreshClientFields = () -> {
            JComponent type = inputs.get("Typ");
            JComponent nip = inputs.get("NIP");
            if (type != null && nip != null) {
                boolean company = type instanceof JComboBox<?> combo
                        && combo.getSelectedItem() == pl.warsztatpro.model.ClientType.FIRMA;
                nip.setVisible(company);
                labels.get("NIP").setVisible(company);
            }
        };
        if (inputs.get("Typ") instanceof JComboBox<?> typeChoice && inputs.containsKey("NIP")) {
            typeChoice.addActionListener(e -> refreshClientFields.run());
            refreshClientFields.run();
        }
        for (Field field : fields) {
            if (field.dependentChoices() == null) continue;
            JComboBox<?> parentChoice = (JComboBox<?>) inputs.get(field.dependsOn());
            JComboBox<?> dependentChoice = (JComboBox<?>) inputs.get(field.name());
            if (parentChoice == null || dependentChoice == null) continue;
            Runnable refresh = () -> {
                Object previous = dependentChoice.getSelectedItem();
                List<?> values = field.dependentChoices().apply(parentChoice.getSelectedItem());
                @SuppressWarnings("unchecked") JComboBox<Object> target = (JComboBox<Object>) dependentChoice;
                target.removeAllItems(); values.forEach(target::addItem);
                Object retained = values.stream().filter(value -> sameRecord(value, previous)).findFirst().orElse(null);
                if (retained != null) target.setSelectedItem(retained);
            };
            parentChoice.addActionListener(e -> refresh.run());
            refresh.run();
        }
        JComponent[] focusOrder = inputs.values().toArray(JComponent[]::new);
        for (int i = 0; i < focusOrder.length; i++) {
            if (focusOrder[i] instanceof JComboBox<?> || focusOrder[i] instanceof JSpinner || focusOrder[i] instanceof ScheduleInput) continue;
            final int position = i;
            InputMap keys = focusOrder[i].getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap actions = focusOrder[i].getActionMap();
            keys.put(KeyStroke.getKeyStroke("alt DOWN"), "form-next-field");
            keys.put(KeyStroke.getKeyStroke("alt UP"), "form-previous-field");
            actions.put("form-next-field", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { focusOrder[(position + 1) % focusOrder.length].requestFocusInWindow(); } });
            actions.put("form-previous-field", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) { focusOrder[(position + focusOrder.length - 1) % focusOrder.length].requestFocusInWindow(); } });
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(DesktopFrame.SURFACE);
        body.setBorder(BorderFactory.createLineBorder(DesktopFrame.BORDER));
        body.add(form, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        footer.setBackground(DesktopFrame.SURFACE);
        JButton cancel = DesktopFrame.button("Anuluj", false);
        JButton save = DesktopFrame.button("Zapisz", true);
        footer.add(cancel); footer.add(save); body.add(footer, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        Map<String, Object> values = new LinkedHashMap<>();
        save.addActionListener(e -> {
            try {
                inputs.forEach((name, component) -> values.put(name, readValue(component)));
            } catch (IllegalArgumentException exception) {
                values.clear();
                JOptionPane.showMessageDialog(dialog, exception.getMessage(), "Nieprawidłowy termin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(save);
        dialog.pack();
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension preferred = dialog.getSize();
        int maxWidth = Math.max(320, usable.width - 48);
        int maxHeight = Math.max(300, usable.height - 48);
        dialog.setSize(Math.min(preferred.width, maxWidth), Math.min(preferred.height, maxHeight));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        if (values.isEmpty()) return null;
        return values;
    }

    private static Object readValue(JComponent component) {
        if (component instanceof JTextField text) return text.getText().trim();
        if (component instanceof JComboBox<?> combo) return combo.getSelectedItem();
        if (component instanceof ScheduleInput schedule) return schedule.readValue();
        throw new IllegalStateException("Nieobsługiwane pole formularza.");
    }

    private static final class ScheduleInput extends JPanel {
        private final JCheckBox enabled = new JCheckBox("Ustaw termin");
        private final JSpinner date;
        private final JSpinner time;

        private ScheduleInput(LocalDateTime initial) {
            super(new FlowLayout(FlowLayout.LEFT, 7, 0));
            setOpaque(false);
            enabled.setSelected(initial != null);
            enabled.setForeground(DesktopFrame.INK);
            enabled.setOpaque(false);
            LocalDate initialDate = initial == null ? LocalDate.now() : initial.toLocalDate();
            LocalTime initialTime = initial == null ? LocalTime.of(9, 0) : initial.toLocalTime();
            date = new JSpinner(new SpinnerDateModel(asDate(initialDate, LocalTime.MIDNIGHT), null, null, java.util.Calendar.DAY_OF_MONTH));
            time = new JSpinner(new SpinnerDateModel(asDate(LocalDate.of(2000, 1, 1), initialTime), null, null, java.util.Calendar.MINUTE));
            configureSpinner(date, "dd.MM.yyyy", 170);
            configureSpinner(time, "HH:mm", 120);
            add(enabled);
            add(schedulePart("Data", date));
            add(schedulePart("Godzina", time));
            enabled.addActionListener(event -> setScheduleEnabled(enabled.isSelected()));
            setScheduleEnabled(enabled.isSelected());
        }

        private JPanel schedulePart(String labelText, JSpinner spinner) {
            JPanel part = new JPanel();
            part.setOpaque(false);
            part.setLayout(new BoxLayout(part, BoxLayout.Y_AXIS));
            JLabel label = new JLabel(labelText);
            label.setForeground(DesktopFrame.MUTED);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            part.add(label);
            part.add(spinner);
            return part;
        }

        private void setScheduleEnabled(boolean value) {
            date.setEnabled(value);
            time.setEnabled(value);
            enabled.setToolTipText(value ? "Odznacz, aby zapisać zlecenie bez terminu" : "Zaznacz, aby dodać termin do terminarza");
        }

        private LocalDateTime readValue() {
            if (!enabled.isSelected()) return null;
            try {
                date.commitEdit();
                time.commitEdit();
            } catch (java.text.ParseException exception) {
                throw new IllegalArgumentException("Wpisz poprawną datę oraz godzinę terminu.");
            }
            LocalDate localDate = ((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalTime localTime = ((Date) time.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            return LocalDateTime.of(localDate, localTime.withSecond(0).withNano(0));
        }

        private static void configureSpinner(JSpinner spinner, String pattern, int width) {
            spinner.setEditor(new JSpinner.DateEditor(spinner, pattern));
            ((JSpinner.DateEditor) spinner.getEditor()).getFormat().setLenient(false);
            spinner.setPreferredSize(new Dimension(width, 38));
            spinner.setBackground(new Color(15, 17, 23));
            spinner.setForeground(DesktopFrame.INK);
            spinner.setBorder(BorderFactory.createLineBorder(DesktopFrame.BORDER));
            JFormattedTextField text = ((JSpinner.DateEditor) spinner.getEditor()).getTextField();
            text.setBackground(new Color(15, 17, 23));
            text.setForeground(DesktopFrame.INK);
            text.setCaretColor(DesktopFrame.INK);
            text.setSelectionColor(new Color(49, 46, 129));
            text.setHorizontalAlignment(SwingConstants.CENTER);
            for (Component child : spinner.getComponents()) {
                if (child instanceof JButton arrow) {
                    arrow.setBackground(new Color(15, 17, 23));
                    arrow.setForeground(DesktopFrame.MUTED);
                    arrow.setOpaque(true);
                    arrow.setContentAreaFilled(true);
                }
            }
        }

        private static Date asDate(LocalDate date, LocalTime time) {
            return Date.from(LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant());
        }
    }

    private static String choiceLabel(Object value) {
        if (value == null) return "";
        if (value instanceof pl.warsztatpro.model.Client client) return client.displayName();
        if (value instanceof pl.warsztatpro.model.Vehicle vehicle) return vehicle.getRegistrationNumber() + " — " + vehicle.getBrand() + " " + vehicle.getModel();
        if (value instanceof pl.warsztatpro.model.Service service) return service.getName();
        if (value instanceof pl.warsztatpro.model.ServiceOrder order) return order.getOrderNumber();
        if (value instanceof pl.warsztatpro.model.Workshop workshop) return workshop.getName();
        if (value instanceof Option<?> option) return option.label();
        return switch (value) {
            case pl.warsztatpro.model.ClientType type -> type == pl.warsztatpro.model.ClientType.FIRMA ? "Firma" : "Osoba prywatna";
            case pl.warsztatpro.model.VehicleType type -> new pl.warsztatpro.model.VehicleTypeOption(type, true).label();
            case pl.warsztatpro.model.VehicleSize size -> switch (size) { case MALY -> "Mały"; case SREDNI -> "Średni"; case DUZY -> "Duży"; };
            default -> value.toString();
        };
    }

    static Object optionValue(Object value) { return value instanceof Option<?> option ? option.value() : value; }

    private static boolean sameRecord(Object left, Object right) {
        if (left == right || java.util.Objects.equals(left, right)) return true;
        if (left instanceof Option<?> a && right instanceof Option<?> b) return sameRecord(a.value(), b.value());
        if (left instanceof pl.warsztatpro.model.Vehicle a && right instanceof pl.warsztatpro.model.Vehicle b) return java.util.Objects.equals(a.getId(), b.getId());
        if (left instanceof pl.warsztatpro.model.Client a && right instanceof pl.warsztatpro.model.Client b) return java.util.Objects.equals(a.getId(), b.getId());
        return false;
    }

    private static void installFocusFeedback(JComponent component) {
        var original = component.getBorder();
        component.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { component.setBorder(BorderFactory.createLineBorder(DesktopFrame.FOCUS, 2)); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { component.setBorder(original); }
        });
    }
}
