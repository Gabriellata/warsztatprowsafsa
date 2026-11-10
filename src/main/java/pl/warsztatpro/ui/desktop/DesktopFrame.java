package pl.warsztatpro.ui.desktop;

import pl.warsztatpro.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class DesktopFrame extends JFrame {
    static final Color INK = new Color(248, 250, 252);
    static final Color MUTED = new Color(148, 163, 184);
    static final Color BORDER = new Color(43, 49, 69);
    static final Color SURFACE = new Color(9, 10, 15);
    static final Color FOCUS = new Color(99, 102, 241);
    static final Color PANEL = new Color(22, 25, 34);
    private static final Color NAV = new Color(15, 17, 23);
    private static final Color NAV_HOVER = new Color(31, 36, 50);
    private static final Color NAV_SELECTED = new Color(79, 70, 229);
    static final Color ACCENT = new Color(79, 70, 229);
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final Map<String, JButton> navigationByKey = new HashMap<>();
    private final Map<Long, Client> clientNames = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, Vehicle> vehicleNames = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Long, Workshop> workshopNames = new java.util.concurrent.ConcurrentHashMap<>();
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedCalendarDate = LocalDate.now();
    private CalendarView calendarView = CalendarView.MONTH;
    private JPanel calendarGrid;
    private JLabel calendarHeading;
    private final List<JButton> calendarDateButtons = new ArrayList<>();
    private final Map<LocalDate, List<ServiceOrder>> calendarOrders = new HashMap<>();
    private ServiceOrder selectedCalendarOrder;
    private JButton removeSelectedOrderScheduleButton;
    private JButton openSelectedOrderButton;
    private EntityPanel<?> currentEntityPanel;
    private final ApplicationServices app;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JLabel pageTitle = new JLabel("Pulpit");
    private final JLabel status = new JLabel("Gotowy");
    private String currentModule = "start";

    private enum CalendarView { DAY, WEEK, MONTH }

    public DesktopFrame(ApplicationServices app) {
        super("WarsztatPro"); this.app = app;
        applyThemeDefaults();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620)); setSize(1220, 800); setLocationRelativeTo(null);
        setLayout(new BorderLayout()); add(buildSidebar(), BorderLayout.WEST); add(buildWorkspace(), BorderLayout.CENTER);
        showModule("start");
    }

    private static void applyThemeDefaults() {
        // Windows' native button painter may ignore per-button colors and render a
        // light face with the light foreground used by this dark theme. Use the
        // standard Swing painter so each button honors its explicit colors.
        UIManager.put("ButtonUI", "javax.swing.plaf.basic.BasicButtonUI");
        UIManager.put("ComboBoxUI", "javax.swing.plaf.basic.BasicComboBoxUI");
        UIManager.put("SpinnerUI", "javax.swing.plaf.basic.BasicSpinnerUI");
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.foreground", INK);
        UIManager.put("Button.background", PANEL);
        UIManager.put("TextField.foreground", INK);
        UIManager.put("TextField.background", NAV);
        UIManager.put("TextField.caretForeground", INK);
        UIManager.put("TextField.selectionBackground", new Color(49, 46, 129));
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("ComboBox.background", PANEL);
        UIManager.put("ComboBox.selectionForeground", INK);
        UIManager.put("ComboBox.selectionBackground", new Color(49, 46, 129));
        UIManager.put("ComboBox.buttonBackground", new Color(15, 17, 23));
        UIManager.put("ComboBox.buttonForeground", MUTED);
        UIManager.put("Spinner.background", new Color(15, 17, 23));
        UIManager.put("Spinner.foreground", INK);
        UIManager.put("Spinner.arrowButtonBackground", new Color(15, 17, 23));
        UIManager.put("Spinner.arrowButtonForeground", MUTED);
        UIManager.put("List.foreground", INK);
        UIManager.put("List.background", PANEL);
        UIManager.put("List.selectionForeground", INK);
        UIManager.put("List.selectionBackground", new Color(49, 46, 129));
        UIManager.put("Table.foreground", INK);
        UIManager.put("Table.background", PANEL);
        UIManager.put("Table.selectionForeground", INK);
        UIManager.put("Table.selectionBackground", new Color(49, 46, 129));
        UIManager.put("TableHeader.foreground", INK);
        UIManager.put("TableHeader.background", NAV_HOVER);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.background", NAV_HOVER);
        UIManager.put("Component.disabledForeground", MUTED);
        UIManager.put("OptionPane.foreground", INK);
        UIManager.put("ScrollPane.background", PANEL);
        UIManager.put("Viewport.background", PANEL);
        UIManager.put("TextArea.background", NAV);
        UIManager.put("TextArea.foreground", INK);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout()); side.setPreferredSize(new Dimension(250, 0)); side.setBackground(NAV);
        JLabel brand = new JLabel("  WARSZTATPRO", SwingConstants.LEFT); brand.setForeground(INK);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20)); brand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(24, 12, 22, 8))); side.add(brand, BorderLayout.NORTH);
        JPanel nav = new JPanel(); nav.setOpaque(false); nav.setBorder(new EmptyBorder(6, 10, 6, 10)); nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.add(navButton("Pulpit", "start")); nav.add(Box.createVerticalStrut(7));
        nav.add(section("OBSŁUGA")); nav.add(navButton("Klienci", "clients")); nav.add(navButton("Pojazdy", "vehicles"));
        nav.add(navButton("Usługi i cennik", "services")); nav.add(navButton("Zlecenia", "orders")); nav.add(navButton("Terminarz", "calendar"));
        nav.add(Box.createVerticalStrut(10)); nav.add(section("ZARZĄDZANIE")); nav.add(navButton("Raporty", "reports"));
        nav.add(navButton("Konfiguracja", "settings")); nav.add(navButton("Stanowiska", "workshops")); nav.add(navButton("Kopie zapasowe", "backups"));
        side.add(nav, BorderLayout.CENTER);
        JLabel footer = new JLabel("  Tryb jednoosobowy", SwingConstants.LEFT); footer.setForeground(MUTED);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(14, 12, 18, 8))); side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildWorkspace() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(SURFACE);
        JPanel top = new JPanel(new BorderLayout()); top.setBackground(NAV); top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(16, 26, 16, 26)));
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 20)); pageTitle.setForeground(INK);
        JLabel appLabel = new JLabel("WARSZTAT SAMOCHODOWY"); appLabel.setForeground(MUTED); appLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        top.add(pageTitle, BorderLayout.WEST); top.add(appLabel, BorderLayout.EAST); root.add(top, BorderLayout.NORTH);
        content.setOpaque(false); content.setBorder(new EmptyBorder(26, 28, 18, 28)); root.add(content, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setBackground(SURFACE);
        bottom.setBorder(new EmptyBorder(0, 28, 12, 28)); status.setForeground(MUTED); bottom.add(status, BorderLayout.WEST); root.add(bottom, BorderLayout.SOUTH);
        return root;
    }

    private JButton navButton(String label, String key) {
        JButton button = new JButton(label); button.setHorizontalAlignment(SwingConstants.LEFT); button.setFocusPainted(true);
        button.setForeground(INK); button.setBackground(NAV); button.setOpaque(true); button.setContentAreaFilled(true); button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2), new EmptyBorder(9, 12, 9, 8)));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (!key.equals(currentModule)) button.setBackground(NAV_HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { if (!key.equals(currentModule)) button.setBackground(NAV); }
        });
        button.addActionListener(e -> showModule(key));
        navigationButtons.add(button); navigationByKey.put(key, button); return button;
    }
    private JLabel section(String text) { JLabel l = new JLabel(text); l.setForeground(MUTED); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setBorder(new EmptyBorder(12, 12, 8, 4)); return l; }

    private void showModule(String key) {
        currentModule = key; currentEntityPanel = null;
        navigationButtons.forEach(button -> { button.setBackground(NAV); button.setForeground(INK); button.setFont(button.getFont().deriveFont(Font.PLAIN)); });
        JButton selectedNavigation = navigationByKey.get(key);
        if (selectedNavigation != null) { selectedNavigation.setBackground(NAV_SELECTED); selectedNavigation.setForeground(Color.WHITE); selectedNavigation.setFont(selectedNavigation.getFont().deriveFont(Font.BOLD)); }
        String title = switch (key) {
            case "clients" -> "Klienci"; case "vehicles" -> "Pojazdy"; case "services" -> "Usługi i cennik";
            case "orders" -> "Zlecenia serwisowe"; case "calendar" -> "Terminarz wizyt"; case "reports" -> "Raporty";
            case "settings" -> "Konfiguracja warsztatu"; case "workshops" -> "Stanowiska robocze"; case "backups" -> "Kopie zapasowe"; default -> "Pulpit";
        };
        pageTitle.setText(title); content.removeAll();
        JPanel view = switch (key) {
            case "clients" -> clientsPanel(); case "vehicles" -> vehiclesPanel(); case "services" -> servicesPanel();
            case "orders" -> ordersPanel(); case "calendar" -> calendarPanel(); case "reports" -> reportsPanel();
            case "settings" -> settingsPanel(); case "workshops" -> workshopsPanel(); case "backups" -> backupsPanel(); default -> dashboard();
        };
        content.add(view, BorderLayout.CENTER); content.revalidate(); content.repaint();
    }

    private JPanel dashboard() {
        JPanel panel = new JPanel(new BorderLayout(0, 20)); panel.setOpaque(false);
        JLabel title = new JLabel("Dzień dobry"); title.setFont(new Font("Segoe UI", Font.BOLD, 28)); title.setForeground(INK);
        JLabel subtitle = new JLabel("Wybierz moduł z menu, aby rozpocząć pracę."); subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15)); subtitle.setForeground(MUTED);
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS)); heading.add(title); heading.add(Box.createVerticalStrut(6)); heading.add(subtitle);
        JPanel cards = new JPanel(new GridLayout(0, 3, 14, 14)); cards.setOpaque(false);
        String[][] items = {{"Klienci", "clients", "Kartoteka klientów"}, {"Pojazdy", "vehicles", "Pojazdy i właściciele"}, {"Zlecenia", "orders", "Naprawy i usługi"},
                {"Terminarz", "calendar", "Wizyty i stanowiska"}, {"Raporty", "reports", "Podsumowanie działalności"}, {"Kopie zapasowe", "backups", "Bezpieczna kopia bazy"}};
        for (String[] item : items) {
            JButton card = new JButton("<html><div style='font-size:16px;font-weight:bold;color:#f8fafc'>" + item[0] + "</div><br><span style='color:#94a3b8'>" + item[2] + "</span></html>");
            card.setHorizontalAlignment(SwingConstants.LEFT); card.setBackground(PANEL); card.setForeground(INK); card.setOpaque(true); card.setContentAreaFilled(true); card.setFocusPainted(true); card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 18, 18, 18)));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    card.setBackground(new Color(34, 39, 54));
                    card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT, 2), new EmptyBorder(17, 17, 17, 17)));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    card.setBackground(PANEL);
                    card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), new EmptyBorder(18, 18, 18, 18)));
                }
            });
            card.addActionListener(e -> showModule(item[1])); cards.add(card);
        }
        JPanel card = whiteCard(new BorderLayout()); card.add(cards, BorderLayout.CENTER); panel.add(heading, BorderLayout.NORTH); panel.add(card, BorderLayout.CENTER); return panel;
    }

    private EntityPanel<Client> clientsPanel() {
        EntityPanel<Client> p = new EntityPanel<>("Klienci", "Kartoteka osób prywatnych i firm", new String[]{"Typ", "Nazwa", "NIP", "Telefon", "E-mail", "Miejscowość"},
                app.clients()::findAll, c -> new Object[]{c.getType()==ClientType.FIRMA?"Firma":"Osoba prywatna", c.displayName(), display(c.getNip()), c.getPhone(), c.getEmail(), c.getCity()}, c -> true);
        p.setActions(() -> addClient(p), c -> editClient(p, c), c -> deleteClient(p, c), p::reload);
        p.setRemoveLabel("Usuń klienta");
        p.setExtraAction("Historia", c -> showHistory(c)); return bind(p);
    }

    private EntityPanel<Vehicle> vehiclesPanel() {
        EntityPanel<Vehicle> p = new EntityPanel<>("Pojazdy", "Pojazdy z opcjonalnym przypisaniem do klienta", new String[]{"Właściciel", "Typ", "Marka", "Model", "Rejestracja", "Rok"},
                () -> { cacheClients(); return app.vehicles().findAll(); }, v -> new Object[]{clientName(v.getClientId()), new VehicleTypeOption(v.getType(),true).label(), v.getBrand(), v.getModel(), v.getRegistrationNumber(), v.getProductionYear()}, v -> true);
        p.setActions(() -> addVehicle(p), v -> editVehicle(p, v), v -> deleteVehicle(p, v), p::reload);
        p.setRemoveLabel("Usuń pojazd");
        p.setExtraAction("Historia", v -> showVehicleHistory(v)); return bind(p);
    }

    private EntityPanel<pl.warsztatpro.model.Service> servicesPanel() {
        EntityPanel<pl.warsztatpro.model.Service> p = new EntityPanel<>("Usługi i cennik", "Ceny nowych pozycji; zlecenia zachowują historyczne kwoty", new String[]{"Usługa", "Kategoria", "Mały", "Średni", "Duży", "Status"},
                app.services()::findAll, s -> new Object[]{s.getName(), s.getCategory(), money(s.getPriceSmall()), money(s.getPriceMedium()), money(s.getPriceLarge()), s.isActive() ? "Aktywna" : "Nieaktywna"}, s -> true);
        p.setActions(() -> addService(p), s -> editService(p, s), s -> toggleService(p, s), p::reload);
        p.setRemoveLabel("Aktywuj / dezaktywuj"); return bind(p);
    }

    private EntityPanel<ServiceOrder> ordersPanel() {
        EntityPanel<ServiceOrder> p = new EntityPanel<>("Zlecenia serwisowe", "Zlecenia, terminy w terminarzu, statusy, pozycje usług i historia", new String[]{"Numer", "Klient", "Pojazd", "Status", "Przyjęto", "Termin", "Stanowisko", "Planowane zakończenie", "Wartość"},
                () -> { cacheClients(); cacheVehicles(); cacheWorkshops(); return app.orders().findAll(); }, o -> new Object[]{o.getOrderNumber(), clientName(o.getClientId()), vehicleName(o.getVehicleId()), o.getStatus(), o.getReceivedDate(), displayDateTime(o.getScheduledAt()), workshopName(o.getWorkshopId()), display(o.getPlannedCompletionDate()), money(o.getTotalCost())}, o -> true);
        p.setActions(() -> addOrder(p), o -> editOrder(p, o), o -> cancelOrder(p, o), p::reload);
        p.setRemoveLabel("Anuluj zlecenie");
        p.setExtraAction("Pozycje usług", o -> manageOrderItems(p, o)); return bind(p);
    }

    private JPanel calendarPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 12)); root.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout()); toolbar.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); controls.setOpaque(false);
        JButton previous = button("‹", false), today = button("Dzisiaj", false), next = button("›", false), add = button("＋ Dodaj zdarzenie", true);
        JButton dayView = button("Dzień", false), weekView = button("Tydzień", false), monthView = button("Miesiąc", false);
        removeSelectedOrderScheduleButton = button("Usuń termin", false);
        openSelectedOrderButton = button("Otwórz zlecenie", false);
        removeSelectedOrderScheduleButton.setVisible(false);
        openSelectedOrderButton.setVisible(false);
        JPanel eventActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0)); eventActions.setOpaque(false);
        eventActions.add(removeSelectedOrderScheduleButton); eventActions.add(openSelectedOrderButton); eventActions.add(add);
        previous.setToolTipText("Poprzedni okres"); next.setToolTipText("Następny okres");
        calendarHeading = new JLabel(); calendarHeading.setFont(new Font("Segoe UI", Font.BOLD, 18)); calendarHeading.setForeground(INK);
        controls.add(previous); controls.add(today); controls.add(next);
        JPanel viewControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)); viewControls.setOpaque(false);
        viewControls.add(dayView); viewControls.add(weekView); viewControls.add(monthView);
        JPanel navigationAndViews = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); navigationAndViews.setOpaque(false);
        navigationAndViews.add(controls); navigationAndViews.add(viewControls);
        JPanel headingAndControls = new JPanel(new BorderLayout(10, 0)); headingAndControls.setOpaque(false);
        headingAndControls.add(navigationAndViews, BorderLayout.WEST); headingAndControls.add(calendarHeading, BorderLayout.CENTER);
        toolbar.add(headingAndControls, BorderLayout.WEST); toolbar.add(eventActions, BorderLayout.EAST); root.add(toolbar, BorderLayout.NORTH);
        calendarGrid = new JPanel(new GridLayout(0, 7, 7, 7)); calendarGrid.setBackground(SURFACE);
        JScrollPane calendarScroll = new JScrollPane(calendarGrid); calendarScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        calendarScroll.getVerticalScrollBar().setUnitIncrement(18); root.add(calendarScroll, BorderLayout.CENTER);
        previous.addActionListener(e -> moveCalendar(-1));
        next.addActionListener(e -> moveCalendar(1));
        today.addActionListener(e -> { selectedCalendarDate = LocalDate.now(); displayedMonth = YearMonth.from(selectedCalendarDate); reloadCalendar(); });
        dayView.addActionListener(e -> setCalendarView(CalendarView.DAY));
        weekView.addActionListener(e -> setCalendarView(CalendarView.WEEK));
        monthView.addActionListener(e -> setCalendarView(CalendarView.MONTH));
        add.addActionListener(e -> addOrderFromCalendar(suggestedSchedule(selectedCalendarDate)));
        removeSelectedOrderScheduleButton.addActionListener(e -> {
            if (selectedCalendarOrder != null) deleteOrderSchedule(selectedCalendarOrder);
        });
        openSelectedOrderButton.addActionListener(e -> {
            if (selectedCalendarOrder != null) orderCalendarDetails(selectedCalendarOrder);
        });
        reloadCalendar(); return root;
    }

    private void setCalendarView(CalendarView view) {
        calendarView = view;
        reloadCalendar();
    }

    private void moveCalendar(int direction) {
        selectedCalendarDate = switch (calendarView) {
            case DAY -> selectedCalendarDate.plusDays(direction);
            case WEEK -> selectedCalendarDate.plusWeeks(direction);
            case MONTH -> selectedCalendarDate.plusMonths(direction);
        };
        displayedMonth = YearMonth.from(selectedCalendarDate);
        reloadCalendar();
    }

    private void reloadCalendar() {
        if (calendarGrid == null) return;
        selectedCalendarOrder = null;
        updateSelectedOrderActions();
        calendarHeading.setText(calendarHeadingText());
        calendarOrders.clear();
        LocalDate firstVisibleDate = firstVisibleCalendarDate();
        int visibleDayCount = calendarView == CalendarView.MONTH ? 42 : calendarView == CalendarView.WEEK ? 7 : 1;
        for (int i = 0; i < visibleDayCount; i++) calendarOrders.put(firstVisibleDate.plusDays(i), List.of());
        renderCalendar(calendarOrders);
        LocalDate requestedStart = firstVisibleDate;
        LocalDate requestedEnd = firstVisibleDate.plusDays(visibleDayCount - 1);
        CalendarView requestedView = calendarView;
        status.setText("Wczytywanie wizyt…");
        new SwingWorker<List<ServiceOrder>, Void>() {
            @Override protected List<ServiceOrder> doInBackground() throws Exception {
                cacheClients(); cacheVehicles(); cacheWorkshops();
                return app.orders().findAll().stream().filter(order -> order.getScheduledAt() != null
                        && !order.getScheduledAt().toLocalDate().isBefore(requestedStart)
                        && !order.getScheduledAt().toLocalDate().isAfter(requestedEnd)).toList();
            }
            @Override protected void done() {
                try {
                    List<ServiceOrder> scheduledOrders = get();
                    if (requestedView != calendarView || !requestedStart.equals(firstVisibleCalendarDate()) || !requestedEnd.equals(lastVisibleCalendarDate())) return;
                    Map<LocalDate, List<ServiceOrder>> byDay = new HashMap<>();
                    for (ServiceOrder order : scheduledOrders) byDay.computeIfAbsent(order.getScheduledAt().toLocalDate(), day -> new ArrayList<>()).add(order);
                    calendarOrders.clear(); calendarOrders.putAll(byDay);
                    renderCalendar(calendarOrders); status.setText("Terminarz wczytany");
                } catch (Exception e) { status.setText("Nie udało się wczytać terminarza"); showError(DesktopFrame.this, e); }
            }
        }.execute();
    }

    private void renderCalendar(Map<LocalDate, List<ServiceOrder>> byDay) {
        calendarGrid.removeAll(); calendarDateButtons.clear();
        LocalDate first = firstVisibleCalendarDate();
        int dayCount = calendarView == CalendarView.MONTH ? 42 : calendarView == CalendarView.WEEK ? 7 : 1;
        calendarGrid.setLayout(new GridLayout(0, dayCount == 1 ? 1 : 7, 7, 7));
        int headerCount = 0;
        for (int i = 0; i < headerCount; i++) {
            LocalDate day = first.plusDays(i);
            String labelText = day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("pl-PL"));
            if (calendarView != CalendarView.MONTH) labelText += " · " + day.getDayOfMonth() + " " + day.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("pl-PL"));
            JLabel label = new JLabel(labelText, SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12)); label.setForeground(MUTED); label.setOpaque(true); label.setBackground(NAV_HOVER);
            label.setBorder(new EmptyBorder(7, 2, 7, 2)); calendarGrid.add(label);
        }
        for (int i = 0; i < dayCount; i++) {
            LocalDate day = first.plusDays(i);
            calendarGrid.add(calendarDayPanel(day, byDay.getOrDefault(day, List.of())));
        }
        calendarGrid.revalidate(); calendarGrid.repaint();
    }

    private LocalDate firstVisibleCalendarDate() {
        if (calendarView == CalendarView.MONTH) return displayedMonth.atDay(1).with(java.time.DayOfWeek.MONDAY);
        if (calendarView == CalendarView.WEEK) return selectedCalendarDate.with(java.time.DayOfWeek.MONDAY);
        return selectedCalendarDate;
    }

    private LocalDate lastVisibleCalendarDate() {
        int dayCount = calendarView == CalendarView.MONTH ? 42 : calendarView == CalendarView.WEEK ? 7 : 1;
        return firstVisibleCalendarDate().plusDays(dayCount - 1);
    }

    private String calendarHeadingText() {
        Locale polish = Locale.forLanguageTag("pl-PL");
        if (calendarView == CalendarView.DAY) {
            return selectedCalendarDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, polish) + ", " + selectedCalendarDate.getDayOfMonth() + " "
                    + selectedCalendarDate.getMonth().getDisplayName(java.time.format.TextStyle.FULL, polish) + " " + selectedCalendarDate.getYear();
        }
        if (calendarView == CalendarView.WEEK) {
            LocalDate start = firstVisibleCalendarDate(), end = start.plusDays(6);
            return start.getDayOfMonth() + "–" + end.getDayOfMonth() + " " + end.getMonth().getDisplayName(java.time.format.TextStyle.FULL, polish) + " " + end.getYear();
        }
        return displayedMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL, polish) + " " + displayedMonth.getYear();
    }

    private JPanel calendarDayPanel(LocalDate day, List<ServiceOrder> orders) {
        JPanel cell = new JPanel(); cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        boolean inMonth = YearMonth.from(day).equals(displayedMonth), isToday = day.equals(LocalDate.now());
        cell.setBackground(isToday ? new Color(34, 39, 67) : !orders.isEmpty() ? new Color(25, 31, 47) : inMonth ? PANEL : NAV);
        cell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(isToday ? ACCENT : BORDER, isToday ? 2 : 1), new EmptyBorder(5, 6, 6, 6)));
        String weekdayShort = day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("pl-PL"));
        JButton date = new JButton(day.getDayOfMonth() + " · " + weekdayShort + (isToday ? "  • Dzisiaj" : "")); date.setHorizontalAlignment(SwingConstants.LEFT); date.setForeground(inMonth ? INK : MUTED);
        date.setFont(new Font("Segoe UI", Font.BOLD, 12)); date.setAlignmentX(Component.LEFT_ALIGNMENT); date.setBackground(cell.getBackground()); date.setOpaque(true); date.setFocusPainted(true);
        date.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); date.setToolTipText("Dodaj wizytę na " + day);
        int dayIndex = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(displayedMonth.atDay(1).with(java.time.DayOfWeek.MONDAY), day));
        bindCalendarArrow(date, "LEFT", dayIndex - 1); bindCalendarArrow(date, "RIGHT", dayIndex + 1); bindCalendarArrow(date, "UP", dayIndex - 7); bindCalendarArrow(date, "DOWN", dayIndex + 7);
        date.addActionListener(e -> { selectedCalendarDate = day; if(!YearMonth.from(day).equals(displayedMonth)){displayedMonth=YearMonth.from(day);reloadCalendar();} addOrderFromCalendar(suggestedSchedule(day)); });
        calendarDateButtons.add(date); cell.add(date);
        for (ServiceOrder order : orders.stream().sorted(Comparator.comparing(ServiceOrder::getScheduledAt)).toList()) {
            Color visitColor = switch (order.getStatus()) {
                case ZAKONCZONE -> new Color(20, 48, 39);
                case ANULOWANE -> new Color(55, 30, 35);
                default -> new Color(25, 31, 47);
            };
            Color visitBorder = switch (order.getStatus()) {
                case ZAKONCZONE -> new Color(16, 185, 129);
                case ANULOWANE -> new Color(239, 68, 68);
                default -> new Color(6, 182, 212);
            };
            boolean selectedOrder = selectedCalendarOrder != null && selectedCalendarOrder.getId().equals(order.getId());
            if (selectedOrder) { visitColor = new Color(49, 46, 90); visitBorder = FOCUS; }
            String orderDescription = order.getWorkDescription()==null||order.getWorkDescription().isBlank()?order.getFaultDescription():order.getWorkDescription();
            JButton visit = new JButton("<html><b>ZLECENIE " + html(order.getOrderNumber()) + "</b><br>" + order.getScheduledAt().toLocalTime() + " · " + html(vehicleSummary(order.getVehicleId())) + "<br>" + html(shortLabel(clientName(order.getClientId()), 14)) + " · " + html(order.getStatus().toString()) + "<br>" + html(shortLabel(orderDescription, 24)) + "</html>");
            visit.setToolTipText("Kliknij, aby otworzyć zlecenie. Termin jest częścią zlecenia."); visit.setHorizontalAlignment(SwingConstants.LEFT); visit.setAlignmentX(Component.LEFT_ALIGNMENT);
            visit.setFont(new Font("Segoe UI", Font.PLAIN, 11)); visit.setForeground(INK); visit.setBackground(visitColor); visit.setOpaque(true); visit.setFocusPainted(true);
            visit.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(visitBorder), new EmptyBorder(3, 5, 3, 5)));
            visit.addActionListener(e -> selectCalendarOrder(order)); cell.add(Box.createVerticalStrut(3)); cell.add(visit);
        }
        cell.add(Box.createVerticalGlue()); return cell;
    }

    private void bindCalendarArrow(JButton button, String key, int targetIndex) {
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), "calendar-focus-" + key);
        button.getActionMap().put("calendar-focus-" + key, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (targetIndex >= 0 && targetIndex < calendarDateButtons.size()) calendarDateButtons.get(targetIndex).requestFocusInWindow();
            }
        });
    }

    private void orderCalendarDetails(ServiceOrder order) {
        String description=order.getWorkDescription()==null||order.getWorkDescription().isBlank()?order.getFaultDescription():order.getWorkDescription();
        String details = "Zlecenie " + order.getOrderNumber()
                + "\nTermin: " + displayDateTime(order.getScheduledAt()) + "\nKlient: " + clientName(order.getClientId())
                + "\nPojazd: " + vehicleName(order.getVehicleId()) + "\nStanowisko: " + workshopName(order.getWorkshopId())
                + "\nStatus: " + order.getStatus() + "\nOpis: " + display(description)
                + "\nWartość: " + money(order.getTotalCost());
        Object[] options = {"Edytuj zlecenie", "Anuluj zlecenie", "Usuń termin", "Pozycje usług", "Otwórz zlecenie", "Zamknij"};
        int choice = JOptionPane.showOptionDialog(this, details, "Szczegóły zlecenia", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0) editOrderForm(null,order,false);
        else if (choice == 1) cancelScheduledOrder(order);
        else if (choice == 2) deleteOrderSchedule(order);
        else if (choice == 3) manageOrderItems(null,order);
        else if (choice == 4) linkedOrder(order);
    }

    private void selectCalendarOrder(ServiceOrder order) {
        selectedCalendarOrder = order;
        updateSelectedOrderActions();
        renderCalendar(calendarOrders);
    }

    private void updateSelectedOrderActions() {
        boolean selected = selectedCalendarOrder != null;
        if (removeSelectedOrderScheduleButton != null) {
            removeSelectedOrderScheduleButton.setVisible(selected);
            removeSelectedOrderScheduleButton.setText("Usuń termin");
        }
        if (openSelectedOrderButton != null) openSelectedOrderButton.setVisible(selected);
    }

    private void deleteOrderSchedule(ServiceOrder order) {
        confirm("Usunąć termin zlecenia „" + order.getOrderNumber() + "”? Zlecenie i jego historia pozostaną.",
                () -> runTask(() -> {ServiceOrder current=app.orders().findById(order.getId()).orElseThrow(()->new IllegalArgumentException("Nie znaleziono zlecenia."));current.setScheduledAt(null);app.orders().update(current);return null;}, "Termin zlecenia usunięty", this::reloadCalendar));
    }

    private void cancelScheduledOrder(ServiceOrder order){
        confirm("Anulować zlecenie „"+order.getOrderNumber()+"”? Zmiana statusu będzie widoczna także w terminarzu.",()->runTask(()->{app.orders().changeStatus(order.getId(),ServiceOrderStatus.ANULOWANE);return null;},"Zlecenie anulowane",this::reloadCalendar));
    }

    private static String html(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private static String shortLabel(String value, int limit) { return value.length() <= limit ? value : value.substring(0, limit - 1) + "…"; }

    private void cacheClients() throws SQLException { clientNames.clear(); app.clients().findAll().forEach(client -> clientNames.put(client.getId(), client)); }
    private void cacheVehicles() throws SQLException { vehicleNames.clear(); app.vehicles().findAll().forEach(vehicle -> vehicleNames.put(vehicle.getId(), vehicle)); }
    private void cacheWorkshops() throws SQLException { workshopNames.clear(); app.workshops().findAll().forEach(workshop -> workshopNames.put(workshop.getId(), workshop)); }
    private String clientName(Long id) { if (id == null) return "Bez przypisania"; Client client = clientNames.get(id); return client == null ? "Brak danych klienta" : client.displayName(); }
    private String vehicleName(Long id) { Vehicle vehicle = vehicleNames.get(id); return vehicle == null ? "Brak danych pojazdu" : vehicle.getRegistrationNumber() + " — " + vehicle.getBrand() + " " + vehicle.getModel(); }
    private String vehicleSummary(Long id) { Vehicle vehicle = vehicleNames.get(id); return vehicle == null ? "Pojazd" : vehicle.getRegistrationNumber(); }
    private String workshopName(Long id) {
        if (id == null) return "Bez przypisania";
        Workshop workshop = workshopNames.get(id);
        return workshop == null ? "Stanowisko nieaktywne" : workshop.getName();
    }

    private EntityPanel<Workshop> workshopsPanel() {
        EntityPanel<Workshop> p = new EntityPanel<>("Stanowiska robocze", "Dezaktywacja zachowuje przypisania w terminarzu", new String[]{"Nazwa", "Status", "Opis"},
                app.workshops()::findAll, w -> new Object[]{w.getName(), w.isActive() ? "Aktywne" : "Nieaktywne", display(w.getDescription())}, w -> true);
        p.setActions(() -> addWorkshop(p), w -> editWorkshop(p, w), w -> deactivateWorkshop(p, w), p::reload);
        p.setRemoveLabel("Dezaktywuj stanowisko"); return bind(p);
    }

    private JPanel settingsPanel() {
        EntityPanel<VehicleTypeOption> p = new EntityPanel<>("Typy pojazdów", "Aktywne typy są dostępne przy dodawaniu i edycji pojazdu", new String[]{"Typ pojazdu", "Status"},
                app.vehicles()::vehicleTypes, t -> new Object[]{t.label(), t.active() ? "Aktywny" : "Nieaktywny"}, t -> true);
        p.setActions(null, null, t -> toggleVehicleType(p, t), p::reload);
        JPanel root = new JPanel(new BorderLayout(0, 14)); root.setOpaque(false); root.add(p, BorderLayout.CENTER);
        JLabel note = new JLabel("Usługi, ich kategorie i ceny edytujesz w module „Usługi i cennik”. Stanowiska mają osobny moduł."); note.setForeground(MUTED); root.add(note, BorderLayout.SOUTH);
        return bind(p, root);
    }

    private JPanel reportsPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 18)); root.setOpaque(false);
        JLabel intro = new JLabel("Raporty zleceń oraz eksport prac do pliku"); intro.setFont(new Font("Segoe UI", Font.PLAIN, 15)); intro.setForeground(MUTED); root.add(intro, BorderLayout.NORTH);
        JPanel body = whiteCard(new GridBagLayout()); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(9, 10, 9, 10); c.anchor = GridBagConstraints.WEST;
        JComboBox<String> report = new JComboBox<>(new String[]{"Liczba zleceń", "Według statusu", "Usługi według kategorii", "Wartości usług i zleceń", "Stan realizacji"});
        JComboBox<String> period = new JComboBox<>(new String[]{"Bieżący miesiąc", "Wybrany okres"});
        LocalDate today = LocalDate.now();
        JTextField from = new JTextField(today.withDayOfMonth(1).toString(), 12), to = new JTextField(today.toString(), 12);
        JButton show = button("Pokaż raport", true), csv = button("Pobierz CSV", false), pdf = button("Pobierz PDF", false);
        JTextArea output = new JTextArea(14, 70); output.setEditable(false); output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); output.setBorder(new EmptyBorder(10, 10, 10, 10));
        c.gridx=0;c.gridy=0;body.add(new JLabel("Raport"),c);c.gridx=1;body.add(report,c);c.gridx=0;c.gridy=1;body.add(new JLabel("Okres"),c);c.gridx=1;body.add(period,c);c.gridx=0;c.gridy=2;body.add(new JLabel("Od (RRRR-MM-DD)"),c);c.gridx=1;body.add(from,c);c.gridx=2;body.add(new JLabel("Do"),c);c.gridx=3;body.add(to,c);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false); actions.add(show); actions.add(csv); actions.add(pdf);
        c.gridx=1;c.gridy=3;c.gridwidth=3;body.add(actions,c);
        c.gridx=0;c.gridy=4;c.gridwidth=4;c.fill=GridBagConstraints.BOTH;c.weightx=1;c.weighty=1;body.add(new JScrollPane(output),c);
        period.addActionListener(e -> { boolean custom = period.getSelectedIndex() == 1; from.setEditable(custom); to.setEditable(custom); });
        show.addActionListener(e -> {
            try {
                LocalDate[] range = reportRange(from, to); LocalDate start=range[0], end=range[1]; String selected=(String)report.getSelectedItem();
                runTaskResult(() -> buildReport(selected,start,end), "Raport gotowy", output::setText);
            } catch (DateTimeParseException | IllegalArgumentException ex) { showError(this, new IllegalArgumentException("Podaj prawidłowe daty w formacie RRRR-MM-DD i zakres od wcześniejszej do późniejszej daty.")); }
        });
        csv.addActionListener(e -> chooseWorkReportExport(from, to, false));
        pdf.addActionListener(e -> chooseWorkReportExport(from, to, true));
        root.add(body, BorderLayout.CENTER); return root;
    }

    private LocalDate[] reportRange(JTextField from, JTextField to) {
        if (from.isEditable()) return new LocalDate[]{LocalDate.parse(from.getText().trim()), LocalDate.parse(to.getText().trim())};
        YearMonth month = YearMonth.now();
        return new LocalDate[]{month.atDay(1), month.atEndOfMonth()};
    }

    private void chooseWorkReportExport(JTextField from, JTextField to, boolean asPdf) {
        try {
            LocalDate[] range = reportRange(from, to);
            String extension = asPdf ? ".pdf" : ".csv";
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Zapisz raport prac");
            chooser.setSelectedFile(new java.io.File("raport_prac_" + range[0] + "_" + range[1] + extension));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            java.nio.file.Path path = chooser.getSelectedFile().toPath();
            if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) path = path.resolveSibling(path.getFileName() + extension);
            java.nio.file.Path destination = path;
            runTask(() -> {
                List<WorkReportRow> rows = app.reports().workReport(range[0], range[1]);
                if (asPdf) pl.warsztatpro.service.WorkReportExporter.writePdf(destination, rows, range[0], range[1]);
                else pl.warsztatpro.service.WorkReportExporter.writeCsv(destination, rows);
                return null;
            }, "Raport zapisany: " + destination.getFileName(), null);
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            showError(this, new IllegalArgumentException("Podaj prawidłowe daty w formacie RRRR-MM-DD i zakres od wcześniejszej do późniejszej daty."));
        }
    }

    private JPanel backupsPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 14)); root.setOpaque(false);
        JLabel title = new JLabel("Kopie lokalnej bazy danych"); title.setFont(new Font("Segoe UI", Font.PLAIN, 15)); title.setForeground(MUTED); root.add(title, BorderLayout.NORTH);
        DefaultListModel<String> model = new DefaultListModel<>(); JList<String> list = new JList<>(model); list.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JPanel card = whiteCard(new BorderLayout(0, 12)); card.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        JButton refresh = button("Odśwież", false), create = button("Utwórz i zweryfikuj kopię", true); actions.add(refresh); actions.add(create); card.add(actions, BorderLayout.SOUTH);
        Consumer<Boolean> load = createCopy -> runTask(() -> {
            if (createCopy) app.backups().createBackup();
            List<java.nio.file.Path> paths=app.backups().listBackups(); SwingUtilities.invokeLater(() -> { model.clear(); paths.forEach(path -> model.addElement(path.toString())); });
            return null;
        }, createCopy ? "Kopia została utworzona i sprawdzona" : "Lista kopii odświeżona", null);
        refresh.addActionListener(e -> load.accept(false)); create.addActionListener(e -> load.accept(true));
        root.add(card, BorderLayout.CENTER); load.accept(false); return root;
    }

    private void addClient(EntityPanel<Client> p) { Client x=new Client(); x.setType(ClientType.OSOBA_PRYWATNA); editClientForm(p,x,true); }
    private void editClient(EntityPanel<Client> p, Client x) { editClientForm(p,x,false); }
    private void editClientForm(EntityPanel<Client> p, Client x, boolean create) {
        Map<String,Object> f=FormDialog.show(this, create?"Nowy klient":"Edytuj klienta", List.of(
                FormDialog.Field.choice("Typ",List.of(ClientType.values()),x.getType()), FormDialog.Field.text("Imię",x.getFirstName()),FormDialog.Field.text("Nazwisko",x.getLastName()),
                FormDialog.Field.text("Nazwa firmy",x.getCompanyName()),FormDialog.Field.text("NIP",x.getNip()),FormDialog.Field.text("Telefon",x.getPhone()),FormDialog.Field.text("E-mail",x.getEmail()),
                FormDialog.Field.text("Kod pocztowy",x.getPostalCode()),FormDialog.Field.text("Ulica",x.getStreet()),FormDialog.Field.text("Miejscowość",x.getCity())));
        if(f==null)return;
        try { x.setType((ClientType)f.get("Typ"));x.setFirstName(text(f,"Imię"));x.setLastName(text(f,"Nazwisko"));x.setCompanyName(text(f,"Nazwa firmy"));x.setNip(text(f,"NIP"));x.setPhone(text(f,"Telefon"));x.setEmail(text(f,"E-mail"));x.setPostalCode(text(f,"Kod pocztowy"));x.setStreet(text(f,"Ulica"));x.setCity(text(f,"Miejscowość"));
            runTask(() -> create?app.clients().add(x):app.clients().update(x), create?"Klient dodany":"Dane klienta zapisane", p::reload);
        } catch(Exception e){showError(this,e);}
    }
    private void deleteClient(EntityPanel<Client> p,Client x){confirm("Usunąć klienta „"+x.displayName()+"”? Operacja jest możliwa tylko wtedy, gdy klient nie ma pojazdów.",()->runTask(()->{app.clients().deleteById(x.getId());return null;},"Klient usunięty",p::reload));}

    private void addVehicle(EntityPanel<Vehicle> p){Vehicle x=new Vehicle();editVehicleForm(p,x,true);}
    private void editVehicle(EntityPanel<Vehicle> p,Vehicle x){editVehicleForm(p,x,false);}
    private void editVehicleForm(EntityPanel<Vehicle> p,Vehicle x,boolean create){
        try {
            List<Client> clients=app.clients().findAll();
            List<VehicleTypeOption> all=app.vehicles().vehicleTypes();List<VehicleType> types=all.stream().filter(VehicleTypeOption::active).map(VehicleTypeOption::type).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if(x.getType()!=null&&!types.contains(x.getType()))types.add(x.getType());
            if(types.isEmpty()){JOptionPane.showMessageDialog(this,"Brak aktywnych typów pojazdów. Włącz typ w konfiguracji.","Brak typów",JOptionPane.INFORMATION_MESSAGE);return;}
            List<FormDialog.Option<Client>> ownerChoices=new ArrayList<>();ownerChoices.add(new FormDialog.Option<>(null,"Bez właściciela"));
            clients.forEach(client->ownerChoices.add(new FormDialog.Option<>(client,client.displayName())));
            FormDialog.Option<Client> owner=ownerChoices.stream().filter(option->option.value()==null?x.getClientId()==null:option.value().getId().equals(x.getClientId())).findFirst().orElse(ownerChoices.getFirst());
            List<FormDialog.Field> fields=List.of(FormDialog.Field.choice("Właściciel pojazdu",ownerChoices,owner),FormDialog.Field.choice("Typ pojazdu",types,x.getType()),
                    FormDialog.Field.text("VIN (opcjonalnie)",x.getVin()),FormDialog.Field.text("Numer rejestracyjny",x.getRegistrationNumber()),FormDialog.Field.text("Marka",x.getBrand()),FormDialog.Field.text("Model",x.getModel()),
                    FormDialog.Field.text("Rok produkcji",display(x.getProductionYear())),FormDialog.Field.text("Pojemność silnika l (opcjonalnie)",display(x.getEngineCapacity())),FormDialog.Field.text("Paliwo (opcjonalnie)",x.getFuelType()),
                    FormDialog.Field.text("Moc KM (opcjonalnie)",display(x.getEnginePower())),FormDialog.Field.text("Napęd (opcjonalnie)",x.getDriveType()),FormDialog.Field.text("Nadwozie (opcjonalnie)",x.getBodyType()));
            Map<String,Object> f=FormDialog.show(this,create?"Nowy pojazd":"Edytuj pojazd",fields);if(f==null)return;
            @SuppressWarnings("unchecked") FormDialog.Option<Client> selectedOwner=(FormDialog.Option<Client>)f.get("Właściciel pojazdu");
            x.setClientId(selectedOwner==null||selectedOwner.value()==null?null:selectedOwner.value().getId());x.setType((VehicleType)f.get("Typ pojazdu"));x.setVin(text(f,"VIN (opcjonalnie)"));x.setRegistrationNumber(text(f,"Numer rejestracyjny"));x.setBrand(text(f,"Marka"));x.setModel(text(f,"Model"));x.setProductionYear(integer(f,"Rok produkcji"));
            x.setEngineCapacity(optionalDecimal(f,"Pojemność silnika l (opcjonalnie)"));x.setFuelType(text(f,"Paliwo (opcjonalnie)"));x.setEnginePower(optionalDecimal(f,"Moc KM (opcjonalnie)"));x.setDriveType(text(f,"Napęd (opcjonalnie)"));x.setBodyType(text(f,"Nadwozie (opcjonalnie)"));
            runTask(()->create?app.vehicles().add(x):app.vehicles().update(x),create?"Pojazd dodany":"Pojazd zapisany",p::reload);
        }catch(Exception e){showError(this,e);}
    }
    private void deleteVehicle(EntityPanel<Vehicle> p,Vehicle x){confirm("Usunąć pojazd? Operacja jest blokowana, jeśli istnieją powiązane zlecenia lub wizyty.",()->runTask(()->{app.vehicles().deleteById(x.getId());return null;},"Pojazd usunięty",p::reload));}

    private void addService(EntityPanel<pl.warsztatpro.model.Service> p){pl.warsztatpro.model.Service x=new pl.warsztatpro.model.Service();editServiceForm(p,x,true);}
    private void editService(EntityPanel<pl.warsztatpro.model.Service> p,pl.warsztatpro.model.Service x){editServiceForm(p,x,false);}
    private void editServiceForm(EntityPanel<pl.warsztatpro.model.Service> p,pl.warsztatpro.model.Service x,boolean create){
        Map<String,Object> f=FormDialog.show(this,create?"Nowa usługa":"Edytuj usługę",List.of(FormDialog.Field.text("Nazwa",x.getName()),FormDialog.Field.text("Opis",x.getDescription()),
                FormDialog.Field.choice("Kategoria",List.of(ServiceCategory.values()),x.getCategory()),FormDialog.Field.text("Cena mały pojazd (zł)",moneyValue(x.getPriceSmall())),
                FormDialog.Field.text("Cena średni pojazd (zł)",moneyValue(x.getPriceMedium())),FormDialog.Field.text("Cena duży pojazd (zł)",moneyValue(x.getPriceLarge()))));if(f==null)return;
        try{x.setName(text(f,"Nazwa"));x.setDescription(text(f,"Opis"));x.setCategory((ServiceCategory)f.get("Kategoria"));x.setPriceSmall(parseMoney(f,"Cena mały pojazd (zł)"));x.setPriceMedium(parseMoney(f,"Cena średni pojazd (zł)"));x.setPriceLarge(parseMoney(f,"Cena duży pojazd (zł)"));
            runTask(()->create?app.services().add(x):app.services().update(x),create?"Usługa dodana":"Usługa zapisana",p::reload);
        }catch(Exception e){showError(this,e);}
    }
    private void toggleService(EntityPanel<pl.warsztatpro.model.Service> p,pl.warsztatpro.model.Service s){
        if(s.isActive())confirm("Dezaktywować usługę „"+s.getName()+"”? Zapisane pozycje zleceń pozostaną bez zmian.",()->runTask(()->{app.services().setActive(s.getId(),false);return null;},"Usługa dezaktywowana",p::reload));
        else runTask(()->{app.services().setActive(s.getId(),true);return null;},"Usługa aktywowana",p::reload);
    }

    private void addOrder(EntityPanel<ServiceOrder> p){ServiceOrder o=new ServiceOrder();o.setReceivedDate(LocalDate.now());editOrderForm(p,o,true);}
    private void addOrderFromCalendar(LocalDateTime scheduledAt){
        ServiceOrder order=new ServiceOrder();order.setReceivedDate(LocalDate.now());order.setScheduledAt(scheduledAt);order.setPlannedDurationMinutes(60);
        editOrderForm(null,order,true);
    }
    private LocalDateTime suggestedSchedule(LocalDate day){
        if(!day.equals(LocalDate.now()))return day.atTime(9,0);
        return LocalDateTime.now().plusMinutes(15).withSecond(0).withNano(0);
    }
    private void editOrder(EntityPanel<ServiceOrder> p,ServiceOrder o){editOrderForm(p,o,false);}
    private void editOrderForm(EntityPanel<ServiceOrder> p,ServiceOrder o,boolean create){
        try {
            List<FormDialog.Field> fields=new ArrayList<>();
            if(create){
                List<Vehicle> vehicles=app.vehicles().findAll();
                if(vehicles.isEmpty()){JOptionPane.showMessageDialog(this,"Najpierw zarejestruj pojazd w module Pojazdy.","Brak pojazdów",JOptionPane.INFORMATION_MESSAGE);return;}
                Vehicle selectedVehicle=vehicles.stream().filter(v->Objects.equals(v.getId(),o.getVehicleId())).findFirst().orElse(vehicles.getFirst());
                fields.add(FormDialog.Field.choice("Pojazd",vehicles,selectedVehicle));
            }
            List<FormDialog.Option<Workshop>> workshopChoices=new ArrayList<>();
            workshopChoices.add(new FormDialog.Option<>(null,"Bez przypisania"));
            for(Workshop workshop:app.workshops().findActive())workshopChoices.add(new FormDialog.Option<>(workshop,workshop.getName()));
            if(o.getWorkshopId()!=null&&workshopChoices.stream().noneMatch(option->option.value()!=null&&option.value().getId().equals(o.getWorkshopId())))
                app.workshops().findById(o.getWorkshopId()).ifPresent(workshop->workshopChoices.add(new FormDialog.Option<>(workshop,workshop.getName()+" (nieaktywne)")));
            FormDialog.Option<Workshop> selectedWorkshop=workshopChoices.stream().filter(option->option.value()==null?o.getWorkshopId()==null:option.value().getId().equals(o.getWorkshopId())).findFirst().orElse(workshopChoices.getFirst());
            fields.add(FormDialog.Field.text("Data przyjęcia (RRRR-MM-DD)",display(o.getReceivedDate())));
            fields.add(FormDialog.Field.text("Planowane zakończenie (RRRR-MM-DD, opcjonalnie)",display(o.getPlannedCompletionDate())));
            fields.add(FormDialog.Field.schedule("Termin w terminarzu",o.getScheduledAt()));
            fields.add(FormDialog.Field.text("Czas zlecenia w terminarzu (minuty)",Integer.toString(o.getPlannedDurationMinutes())));
            fields.add(FormDialog.Field.choice("Stanowisko robocze",workshopChoices,selectedWorkshop));
            fields.add(FormDialog.Field.text("Opis usterki (wymagane)",o.getFaultDescription()));fields.add(FormDialog.Field.text("Opis prac",o.getWorkDescription()));fields.add(FormDialog.Field.text("Uwagi",o.getNotes()));
            if(!create)fields.add(FormDialog.Field.choice("Status",List.of(ServiceOrderStatus.values()),o.getStatus()));
            Map<String,Object> f=FormDialog.show(this,create?"Nowe zlecenie":"Edytuj zlecenie",fields);if(f==null)return;
            ServiceOrderStatus previous=o.getStatus();if(!create)o.setStatus((ServiceOrderStatus)f.get("Status"));if(create){Object chosenVehicle=f.get("Pojazd");if(chosenVehicle instanceof Vehicle vehicle){o.setClientId(vehicle.getClientId());o.setVehicleId(vehicle.getId());}else{JOptionPane.showMessageDialog(this,"Wybierz pojazd.","Brak pojazdu",JOptionPane.INFORMATION_MESSAGE);return;}}o.setReceivedDate(parseDate(text(f,"Data przyjęcia (RRRR-MM-DD)")));o.setPlannedCompletionDate(optionalDate(text(f,"Planowane zakończenie (RRRR-MM-DD, opcjonalnie)")));
            o.setScheduledAt((LocalDateTime)f.get("Termin w terminarzu"));o.setPlannedDurationMinutes(integer(f,"Czas zlecenia w terminarzu (minuty)"));
            if(create&&p==null&&o.getScheduledAt()==null)throw new IllegalArgumentException("Termin jest wymagany przy tworzeniu zlecenia z terminarza.");
            @SuppressWarnings("unchecked") FormDialog.Option<Workshop> workshop=(FormDialog.Option<Workshop>)f.get("Stanowisko robocze");o.setWorkshopId(workshop==null||workshop.value()==null?null:workshop.value().getId());
            o.setFaultDescription(text(f,"Opis usterki (wymagane)"));o.setWorkDescription(text(f,"Opis prac"));o.setNotes(text(f,"Uwagi"));
            runTask(()->{if(create)app.orders().create(o);else{app.orders().update(o);if(o.getStatus()!=previous)app.orders().changeStatus(o.getId(),o.getStatus());}return null;},create?"Zlecenie utworzone":"Zlecenie zapisane",()->{if(p!=null)p.reload();if("calendar".equals(currentModule))reloadCalendar();});
        }catch(Exception e){showError(this,e);}
    }
    private void cancelOrder(EntityPanel<ServiceOrder> p,ServiceOrder o){confirm("Anulować zlecenie „"+o.getOrderNumber()+"”?",()->runTask(()->{app.orders().changeStatus(o.getId(),ServiceOrderStatus.ANULOWANE);return null;},"Zlecenie anulowane",p::reload));}
    private void manageOrderItems(EntityPanel<ServiceOrder> p,ServiceOrder order){
        JDialog dialog=new JDialog(this,"Pozycje zlecenia "+order.getOrderNumber(),Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(760,430);dialog.setLocationRelativeTo(this);dialog.setLayout(new BorderLayout(10,10));
        DefaultListModel<OrderItem> model=new DefaultListModel<>();JList<OrderItem> list=new JList<>(model);list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setVisibleRowCount(10);
        list.setCellRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> source,Object value,int index,boolean selected,boolean focus){String label=value instanceof OrderItem item?item.getServiceName()+" · "+item.getQuantity()+" × "+money(item.getUnitPrice())+" = "+money(item.getTotalValue()):"";return super.getListCellRendererComponent(source,label,index,selected,focus);}});
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));JButton add=button("Dodaj usługę",true),edit=button("Edytuj pozycję",false),remove=button("Usuń pozycję",false),delete=button("Usuń puste zlecenie",false),close=button("Zamknij",false);
        buttons.add(add);buttons.add(edit);buttons.add(remove);buttons.add(delete);buttons.add(close);dialog.add(new JScrollPane(list),BorderLayout.CENTER);dialog.add(buttons,BorderLayout.SOUTH);
        Runnable reload=()->new SwingWorker<List<OrderItem>,Void>(){protected List<OrderItem> doInBackground()throws Exception{return app.orders().items(order.getId());}protected void done(){try{model.clear();for(OrderItem item:get())model.addElement(item);}catch(Exception e){showError(dialog,e);}}}.execute();
        Runnable refreshOrderViews=()->{if(p!=null)p.reload();if("calendar".equals(currentModule))reloadCalendar();};
        add.addActionListener(e->runTaskResult(()->app.services().findAll().stream().filter(pl.warsztatpro.model.Service::isActive).toList(),"Usługi wczytane",active->{
            if(active.isEmpty()){JOptionPane.showMessageDialog(dialog,"Brak aktywnych usług.");return;}
            Map<String,Object> f=FormDialog.show(dialog,"Dodaj usługę",List.of(FormDialog.Field.choice("Usługa",active,active.getFirst()),FormDialog.Field.choice("Rozmiar",List.of(VehicleSize.values()),VehicleSize.SREDNI),FormDialog.Field.text("Ilość","1")));
            if(f==null)return;try{var service=(pl.warsztatpro.model.Service)f.get("Usługa");var size=(VehicleSize)f.get("Rozmiar");int quantity=integer(f,"Ilość");runTask(()->app.orders().addItem(order.getId(),service.getId(),size,quantity),"Usługa dodana z ceną z cennika",()->{refreshOrderViews.run();reload.run();});}catch(Exception ex){showError(dialog,ex);}
        }));
        edit.addActionListener(e->{OrderItem item=list.getSelectedValue();if(item==null){JOptionPane.showMessageDialog(dialog,"Zaznacz pozycję.");return;}
            Map<String,Object> f=FormDialog.show(dialog,"Edytuj pozycję",List.of(FormDialog.Field.text("Ilość",Integer.toString(item.getQuantity())),FormDialog.Field.text("Cena jednostkowa (zł)",moneyValue(item.getUnitPrice()))));if(f==null)return;
            try{item.setQuantity(integer(f,"Ilość"));item.setUnitPrice(parseMoney(f,"Cena jednostkowa (zł)"));runTask(()->{app.orders().editItem(order.getId(),item);return null;},"Pozycja zapisana",()->{refreshOrderViews.run();reload.run();});}catch(Exception ex){showError(dialog,ex);}
        });
        remove.addActionListener(e->{OrderItem item=list.getSelectedValue();if(item==null){JOptionPane.showMessageDialog(dialog,"Zaznacz pozycję.");return;}confirm("Usunąć zaznaczoną pozycję zlecenia?",()->runTask(()->{app.orders().removeItem(order.getId(),item.getId());return null;},"Pozycja usunięta",()->{refreshOrderViews.run();reload.run();}));});
        delete.addActionListener(e->confirm("Usunąć puste zlecenie wraz z jego terminem w terminarzu?",()->runTask(()->{app.orders().deleteIfEmpty(order.getId());return null;},"Puste zlecenie usunięte",()->{dialog.dispose();refreshOrderViews.run();})));
        close.addActionListener(e->dialog.dispose());reload.run();dialog.setVisible(true);
    }

    private void linkedOrder(ServiceOrder selectedOrder){
        runTaskResult(()->{
            ServiceOrder order=app.orders().findById(selectedOrder.getId()).orElse(null);
            if(order==null)return "Nie znaleziono zlecenia.";
            cacheClients(); cacheVehicles();
            StringBuilder text=new StringBuilder("Klient: ").append(clientName(order.getClientId())).append("\nPojazd: ").append(vehicleName(order.getVehicleId()))
                    .append("\nStatus: ").append(order.getStatus()).append("\nPrzyjęto: ").append(order.getReceivedDate())
                    .append("\nUsterka: ").append(order.getFaultDescription()).append("\nWartość: ").append(money(order.getTotalCost())).append("\n\nPozycje usług:\n");
            List<OrderItem> items=app.orders().items(order.getId());
            if(items.isEmpty())text.append("Brak pozycji usług.");else items.forEach(item->text.append(item.getServiceName()).append(" · ").append(item.getQuantity()).append(" × ").append(money(item.getUnitPrice())).append(" = ").append(money(item.getTotalValue())).append('\n'));
            return text.toString();
        },"Zlecenie wczytane",text->{JTextArea area=new JTextArea(text,18,55);area.setEditable(false);JOptionPane.showMessageDialog(this,new JScrollPane(area),"Powiązane zlecenie",JOptionPane.INFORMATION_MESSAGE);});
    }

    private void addWorkshop(EntityPanel<Workshop> p){Workshop w=new Workshop();editWorkshopForm(p,w,true);}
    private void editWorkshop(EntityPanel<Workshop> p,Workshop w){editWorkshopForm(p,w,false);}
    private void editWorkshopForm(EntityPanel<Workshop> p,Workshop w,boolean create){Map<String,Object> f=FormDialog.show(this,create?"Nowe stanowisko":"Edytuj stanowisko",List.of(FormDialog.Field.text("Nazwa",w.getName()),FormDialog.Field.text("Opis",w.getDescription())));if(f==null)return;w.setName(text(f,"Nazwa"));w.setDescription(text(f,"Opis"));runTask(()->create?app.workshops().create(w):app.workshops().update(w),create?"Stanowisko dodane":"Stanowisko zapisane",p::reload);}
    private void deactivateWorkshop(EntityPanel<Workshop> p,Workshop w){if(!w.isActive())return;confirm("Dezaktywować stanowisko „"+w.getName()+"”? Powiązane wizyty pozostaną w historii.",()->runTask(()->{app.workshops().deactivate(w.getId());return null;},"Stanowisko dezaktywowane",p::reload));}
    private void toggleVehicleType(EntityPanel<VehicleTypeOption> p,VehicleTypeOption type){runTask(()->{app.vehicles().setVehicleTypeActive(type.type(),!type.active());return null;},type.active()?"Typ pojazdu dezaktywowany":"Typ pojazdu aktywowany",p::reload);}

    private void showHistory(Client c){runTaskResult(()->app.history().forClient(c.getId()),"Historia klienta",entries->showHistoryDialog("Historia klienta: "+c.displayName(),entries));}
    private void showVehicleHistory(Vehicle v){runTaskResult(()->app.history().forVehicle(v.getId()),"Historia pojazdu",entries->showHistoryDialog("Historia pojazdu: "+v.getRegistrationNumber(),entries));}
    private void showHistoryDialog(String title,List<ServiceHistoryEntry> entries){String content=entries.isEmpty()?"Brak historii.":String.join("\n",entries.stream().map(entry->entry.occurredAt().toString().replace('T',' ') + " | " + entry.type() + ("Zlecenie".equals(entry.type())?" "+entry.reference():"") + " | " + entry.status() + " | " + entry.description()).toList());JTextArea area=new JTextArea(content,18,70);area.setEditable(false);JOptionPane.showMessageDialog(this,new JScrollPane(area),title,JOptionPane.INFORMATION_MESSAGE);}

    private String buildReport(String selected,LocalDate from,LocalDate to)throws SQLException{
        StringBuilder b=new StringBuilder("Zakres dat przyjęcia: ").append(from).append(" – ").append(to).append("\n\n");
        switch(selected){
            case "Liczba zleceń" -> b.append("Liczba zleceń: ").append(app.reports().countOrders(from,to));
            case "Według statusu" -> {var rows=app.reports().ordersByStatus(from,to);if(rows.isEmpty())return "Brak zleceń w wybranym okresie.";for(var row:rows)b.append(String.format("%-26s %6d%n",row.status(),row.count()));}
            case "Usługi według kategorii" -> {var rows=app.reports().servicesByCategory(from,to);if(rows.isEmpty())return "Brak pozycji usług w wybranym okresie.";b.append(String.format("%-28s %8s %8s %16s%n","Kategoria","Pozycje","Ilość","Wartość"));for(var row:rows)b.append(String.format("%-28s %8d %8d %16s%n",row.category(),row.lineCount(),row.quantity(),money(row.valueGrosze())));}
            case "Wartości usług i zleceń" -> {var r=app.reports().financialSummary(from,to);if(r.orderCount()==0)return "Brak zleceń w wybranym okresie.";b.append("Zlecenia: ").append(r.orderCount()).append("\nZapisane pozycje usług: ").append(money(r.serviceValueGrosze())).append("\nŁączna wartość zleceń: ").append(money(r.orderValueGrosze()));}
            case "Stan realizacji" -> {var r=app.reports().lifecycleSummary(from,to);if(r.total()==0)return "Brak zleceń w wybranym okresie.";b.append("Nowe: ").append(r.newOrders()).append("\nW toku: ").append(r.inProgress()).append("\nZakończone: ").append(r.completed()).append("\nOpóźnione: ").append(r.overdue()).append("\nAnulowane: ").append(r.cancelled());}
        }
        return b.toString();
    }

    private <T> EntityPanel<T> bind(EntityPanel<T> panel){currentEntityPanel=panel;return panel;}
    private JPanel bind(EntityPanel<?> panel,Component component){currentEntityPanel=panel;return (JPanel)component;}
    private JPanel whiteCard(LayoutManager layout){JPanel p=new JPanel(layout);p.setBackground(PANEL);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(18,18,18,18)));return p;}
    static JButton button(String text,boolean primary){JButton b=new JButton(text);b.setFocusPainted(true);b.setOpaque(true);b.setContentAreaFilled(true);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setFont(new Font("Segoe UI",Font.BOLD,13));b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary?ACCENT:BORDER),new EmptyBorder(8,12,8,12)));b.setBackground(primary?ACCENT:PANEL);b.setForeground(INK);return b;}
    private void confirm(String message,Runnable operation){if(JOptionPane.showConfirmDialog(this,message,"Potwierdzenie",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION)operation.run();}

    private <T> void runTaskResult(Callable<T> task,String success,Consumer<T> result){
        status.setText("Przetwarzanie…");
        new SwingWorker<T,Void>(){
            protected T doInBackground()throws Exception{return task.call();}
            protected void done(){try{T value=get();status.setText(success);if(result!=null)result.accept(value);else if(currentEntityPanel!=null)currentEntityPanel.reload();}
                catch(Exception e){status.setText("Operacja nie powiodła się");showError(DesktopFrame.this,e);}}
        }.execute();
    }
    static void showError(Component parent,Throwable error){
        Throwable cause=error;while(cause.getCause()!=null&&(cause instanceof java.util.concurrent.ExecutionException||cause instanceof java.util.concurrent.CompletionException))cause=cause.getCause();
        String message=cause instanceof IllegalArgumentException?cause.getMessage():cause instanceof SQLException?"Nie udało się wykonać operacji na bazie danych. Sprawdź dane i spróbuj ponownie.":"Operacja nie powiodła się. Spróbuj ponownie.";
        if(message==null||message.isBlank())message="Operacja nie powiodła się. Spróbuj ponownie.";
        JOptionPane.showMessageDialog(parent,message,"Nie udało się wykonać operacji",JOptionPane.ERROR_MESSAGE);
    }
    private void runTask(Callable<?> task,String success,Runnable refresh){runTaskResult(task,success,value->{if(refresh!=null)refresh.run();else if(currentEntityPanel!=null)currentEntityPanel.reload();});}

    private void showOrderItemError(Exception e){showError(this,e);}
    private static String text(Map<String,Object> f,String key){Object v=f.get(key);return v==null?"":v.toString();}
    private static int integer(Map<String,Object> f,String key){try{return Integer.parseInt(text(f,key));}catch(NumberFormatException e){throw new IllegalArgumentException("Pole „"+key+"” musi zawierać liczbę całkowitą.");}}
    private static Double optionalDecimal(Map<String,Object> f,String key){String v=text(f,key);if(v.isBlank())return null;try{return Double.parseDouble(v.replace(',','.'));}catch(NumberFormatException e){throw new IllegalArgumentException("Pole „"+key+"” musi zawierać liczbę.");}}
    private static LocalDate parseDate(String text){try{return LocalDate.parse(text);}catch(DateTimeParseException e){throw new IllegalArgumentException("Wpisz datę w formacie RRRR-MM-DD.");}}
    private static LocalDate optionalDate(String text){return text==null||text.isBlank()?null:parseDate(text);}
    private static String displayDateTime(LocalDateTime value){return value==null?"":value.toString().replace('T',' ');}
    private static long parseMoney(Map<String,Object> f,String key){try{return new BigDecimal(text(f,key).replace(',','.')).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();}catch(NumberFormatException|ArithmeticException e){throw new IllegalArgumentException("Pole „"+key+"” wymaga kwoty z maksymalnie dwoma miejscami po przecinku.");}}
    private static String money(long grosze){return BigDecimal.valueOf(grosze,2).toPlainString()+" zł";}
    private static String moneyValue(long grosze){return BigDecimal.valueOf(grosze,2).toPlainString();}
    private static String display(Object value){return value==null?"":value.toString();}
}
