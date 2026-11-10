package pl.warsztatpro.service;

import pl.warsztatpro.model.Appointment;
import pl.warsztatpro.model.AppointmentStatus;
import pl.warsztatpro.repository.AppointmentRepository;
import pl.warsztatpro.repository.ClientRepository;
import pl.warsztatpro.repository.VehicleRepository;
import pl.warsztatpro.repository.WorkshopRepository;
import pl.warsztatpro.repository.ServiceOrderRepository;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AppointmentService {
    private final AppointmentRepository appointments;
    private final ClientRepository clients;
    private final VehicleRepository vehicles;
    private final WorkshopRepository workshops;
    private final ServiceOrderRepository orders;

    public AppointmentService(AppointmentRepository appointments, ClientRepository clients, VehicleRepository vehicles) {
        this(appointments, clients, vehicles, new WorkshopRepository(), new ServiceOrderRepository());
    }

    public AppointmentService(AppointmentRepository appointments, ClientRepository clients, VehicleRepository vehicles,
                              WorkshopRepository workshops) {
        this(appointments,clients,vehicles,workshops,new ServiceOrderRepository());
    }

    public AppointmentService(AppointmentRepository appointments, ClientRepository clients, VehicleRepository vehicles,
                              WorkshopRepository workshops, ServiceOrderRepository orders) {
        this.appointments = appointments;
        this.clients = clients;
        this.vehicles = vehicles;
        this.workshops = workshops;
        this.orders = orders;
    }

    public Appointment create(Appointment appointment) throws SQLException {
        if (appointment == null) throw new IllegalArgumentException("Wizyta jest wymagana.");
        appointment.setStatus(AppointmentStatus.ZAPLANOWANA);
        validate(appointment);
        validateClientAndVehicle(appointment);
        validateWorkshop(appointment.getWorkshopId(), null);
        validateServiceOrder(appointment);
        validateUniqueServiceOrderEvent(appointment);
        validateNoCollision(appointment, null);
        return appointments.insert(appointment);
    }

    public List<Appointment> findAll() throws SQLException { return appointments.findAll(); }
    public List<Appointment> findByDate(LocalDate date) throws SQLException {
        if (date == null) throw new IllegalArgumentException("Data wizyty jest wymagana.");
        return appointments.findByDate(date);
    }
    public List<Appointment> findForDay(LocalDate date) throws SQLException {
        if (date == null) throw new IllegalArgumentException("Data wizyty jest wymagana.");
        return appointments.findBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
    public List<Appointment> findForWeek(LocalDate anyDay) throws SQLException {
        if (anyDay == null) throw new IllegalArgumentException("Data tygodnia jest wymagana.");
        LocalDate monday = anyDay.with(DayOfWeek.MONDAY);
        return appointments.findBetween(monday.atStartOfDay(), monday.plusWeeks(1).atStartOfDay());
    }
    public List<Appointment> findForMonth(LocalDate anyDay) throws SQLException {
        if (anyDay == null) throw new IllegalArgumentException("Data miesiąca jest wymagana.");
        LocalDate first = anyDay.withDayOfMonth(1);
        return appointments.findBetween(first.atStartOfDay(), first.plusMonths(1).atStartOfDay());
    }
    public Optional<Appointment> findById(long id) throws SQLException { return appointments.findById(id); }
    public List<Appointment> findByServiceOrderId(long id)throws SQLException {
        if(orders.findById(id).isEmpty())throw new IllegalArgumentException("Nie znaleziono zlecenia.");
        return appointments.findByServiceOrderId(id);
    }
    public void linkToServiceOrder(long appointmentId,long serviceOrderId)throws SQLException {
        Appointment appointment=appointments.findById(appointmentId)
                .orElseThrow(()->new IllegalArgumentException("Nie znaleziono wizyty."));
        appointment.setServiceOrderId(serviceOrderId);
        validateServiceOrder(appointment);
        validateUniqueServiceOrderEvent(appointment);
        if(!appointments.updateServiceOrderId(appointmentId,serviceOrderId))throw new IllegalArgumentException("Nie udało się powiązać wizyty ze zleceniem.");
    }

    public Appointment update(Appointment appointment) throws SQLException {
        if (appointment == null || appointment.getId() == null) throw new IllegalArgumentException("Nie znaleziono wizyty.");
        Appointment previous = appointments.findById(appointment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono wizyty."));
        validate(appointment);
        validateClientAndVehicle(appointment);
        validateWorkshop(appointment.getWorkshopId(), previous.getWorkshopId());
        validateServiceOrder(appointment);
        validateUniqueServiceOrderEvent(appointment);
        validateNoCollision(appointment, appointment.getId());
        if (!appointments.update(appointment)) throw new IllegalArgumentException("Nie udało się zaktualizować wizyty.");
        return appointments.findById(appointment.getId()).orElse(appointment);
    }

    public void changeStatus(long id, AppointmentStatus status) throws SQLException {
        if (status == null) throw new IllegalArgumentException("Status wizyty jest wymagany.");
        Appointment appointment=appointments.findById(id).orElseThrow(()->new IllegalArgumentException("Nie znaleziono wizyty."));
        appointment.setStatus(status);
        validateNoCollision(appointment,id);
        if (!appointments.updateStatus(id, status)) throw new IllegalArgumentException("Nie udało się zmienić statusu.");
    }

    public void delete(long id) throws SQLException {
        if (appointments.findById(id).isEmpty()) throw new IllegalArgumentException("Nie znaleziono wizyty.");
        if (!appointments.delete(id)) throw new IllegalArgumentException("Nie udało się usunąć wizyty.");
    }

    public void cancel(long id) throws SQLException {
        changeStatus(id, AppointmentStatus.ANULOWANA);
    }

    private void validateClientAndVehicle(Appointment appointment) throws SQLException {
        if (appointment.getClientId()!=null && clients.findById(appointment.getClientId()).isEmpty()) throw new IllegalArgumentException("Nie znaleziono klienta.");
        var vehicle = vehicles.findById(appointment.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pojazdu."));
        if (appointment.getClientId()!=null && !java.util.Objects.equals(vehicle.getClientId(), appointment.getClientId())) {
            throw new IllegalArgumentException("Pojazd nie jest przypisany do wskazanego klienta.");
        }
    }

    private void validateWorkshop(Long workshopId, Long previousWorkshopId) throws SQLException {
        if (workshopId == null) return;
        var workshop = workshops.findById(workshopId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono stanowiska."));
        if (!workshop.isActive() && !workshopId.equals(previousWorkshopId)) {
            throw new IllegalArgumentException("Można przypisać wyłącznie aktywne stanowisko.");
        }
    }

    private void validateNoCollision(Appointment candidate, Long excludedId) throws SQLException {
        if (candidate.getWorkshopId() == null || candidate.getStatus() == AppointmentStatus.ANULOWANA) return;
        LocalDateTime start = candidate.getScheduledAt();
        LocalDateTime end = start.plusMinutes(candidate.getDurationMinutes());
        for (Appointment existing : appointments.findByWorkshopId(candidate.getWorkshopId())) {
            if ((excludedId != null && excludedId.equals(existing.getId()))
                    || existing.getStatus() == AppointmentStatus.ANULOWANA) continue;
            LocalDateTime existingStart = existing.getScheduledAt();
            LocalDateTime existingEnd = existingStart.plusMinutes(existing.getDurationMinutes());
            if (start.isBefore(existingEnd) && existingStart.isBefore(end)) {
                throw new IllegalArgumentException("Stanowisko jest już zarezerwowane w tym przedziale czasu (wizyta ID "
                        + existing.getId() + ").");
            }
        }
    }

    private void validateServiceOrder(Appointment appointment)throws SQLException {
        if(appointment.getServiceOrderId()==null)return;
        var order=orders.findById(appointment.getServiceOrderId())
                .orElseThrow(()->new IllegalArgumentException("Nie znaleziono powiązanego zlecenia."));
        if(!java.util.Objects.equals(order.getClientId(),appointment.getClientId())||order.getVehicleId()!=appointment.getVehicleId()) {
            throw new IllegalArgumentException("Klient i pojazd wizyty muszą być zgodni ze zleceniem.");
        }
    }

    private void validateUniqueServiceOrderEvent(Appointment appointment)throws SQLException {
        if(appointment.getServiceOrderId()==null)return;
        for(Appointment linked:appointments.findByServiceOrderId(appointment.getServiceOrderId())) {
            if(!linked.getId().equals(appointment.getId()))
                throw new IllegalArgumentException("To zlecenie ma już przypisany wpis w terminarzu.");
        }
    }

    private static void validate(Appointment appointment) {
        if (appointment == null || appointment.getClientId()!=null && appointment.getClientId()<=0 || appointment.getVehicleId() <= 0) {
            throw new IllegalArgumentException("Wybierz prawidłowy pojazd.");
        }
        if (appointment.getScheduledAt() == null) throw new IllegalArgumentException("Termin wizyty jest wymagany.");
        if (appointment.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Termin wizyty nie może być w przeszłości.");
        }
        if (appointment.getDurationMinutes() <= 0 || appointment.getDurationMinutes() > 1440) {
            throw new IllegalArgumentException("Czas trwania musi wynosić od 1 do 1440 minut.");
        }
        if (appointment.getDescription() == null || appointment.getDescription().isBlank()) {
            throw new IllegalArgumentException("Opis wizyty jest wymagany.");
        }
        if (appointment.getStatus() == null) appointment.setStatus(AppointmentStatus.ZAPLANOWANA);
    }
}
