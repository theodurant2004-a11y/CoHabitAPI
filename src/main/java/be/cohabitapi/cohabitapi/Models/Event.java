package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.EventDAO;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

// An event of the calendar. It belongs to one roomshare and every member of the roomshare can see it
@Entity
@Table(name = "Event")
public class Event {
    // ------------------------- Attributes and JPA Annotations -------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer id; // Integer because it is null before the insert

    @Column(name = "event_title", length = 100, nullable = false)
    private String title;

    // Nullable in the db so it can be null here too
    @Column(name = "event_description", length = 255)
    private String description;

    // LocalDate matches the SQL DATE type
    @Column(name = "event_date", nullable = false)
    private LocalDate date;

    // The time is optional. It is stored as a text like "18:00" (see TimeConverter)
    @Convert(converter = TimeConverter.class)
    @Column(name = "event_start_time", length = 5)
    private LocalTime startTime;

    // Same as the start time
    @Convert(converter = TimeConverter.class)
    @Column(name = "event_end_time", length = 5)
    private LocalTime endTime;

    // Stored as its code, for example "social" (see EventType.DbConverter)
    @Convert(converter = EventType.DbConverter.class)
    @Column(name = "event_type", length = 50, nullable = false)
    private EventType type;

    // An event belongs to one roomshare
    @ManyToOne(optional = false)
    @JoinColumn(name = "roomshare_id", nullable = false)
    private RoomShare roomshare;


    // ------------------------- Constructors -------------------------

    // Not public because only Hibernate uses it to load an event from the db
    protected Event() {
    }

    // The setters check every value
    public Event(String title, String description, String date, String startTime, String endTime, String type) {
        setTitle(title);
        setDescription(description);
        setDate(parseDate(date, "event date"));
        setTimes(parseTime(startTime), parseTime(endTime));
        setType(EventType.fromCode(type));
    }


    // ------------------------- Getters and Setters -------------------------

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    // The title is required and maximum 100 characters
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty())
            throw new IllegalArgumentException("The event title is required.");

        String trimmed = title.trim();
        if (trimmed.length() > 100)
            throw new IllegalArgumentException("The event title cannot be longer than 100 characters.");

        this.title = trimmed;
    }

    public String getDescription() {
        return description;
    }

    // The description is optional so null or only spaces means no description
    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            this.description = null;
            return;
        }

        String trimmed = description.trim();
        if (trimmed.length() > 255)
            throw new IllegalArgumentException("The event description cannot be longer than 255 characters.");

        this.description = trimmed;
    }

    public LocalDate getDate() {
        return date;
    }

    // The date is required, an event always happens on a day
    // One event = one day, so there is no end date: the end TIME is checked in setTimes
    public void setDate(LocalDate date) {
        if (date == null)
            throw new IllegalArgumentException("The event date is required.");
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    // Both times are optional but if there are two, the end must be after the start
    // One setter for the two times because the rule needs both
    public void setTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime))
            throw new IllegalArgumentException("The end must be after the start.");

        this.startTime = startTime;
        this.endTime = endTime;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        if (type == null)
            throw new IllegalArgumentException("The event type is required.");
        this.type = type;
    }

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Private because an event never moves to another roomshare
    public void setRoomshare(RoomShare roomshare) {
        if (roomshare == null)
            throw new IllegalArgumentException("The roomshare cannot be null.");
        this.roomshare = roomshare;
    }


    // ------------------------- Business methods -------------------------

    // SAVE : the event saves itself in the db, which gives it its id
    // It is called by RoomShare.addEvent, once the event knows its roomshare
    public void create(EventDAO eventDAO) {

        // Without a roomshare the insert would fail: the column roomshare_id cannot be null
        if (roomshare == null)
            throw new IllegalArgumentException("The event must be added to a roomshare first.");

        //  Save it in the db
        eventDAO.create(this);
    }


    // ------------------------- Utility methods -------------------------

    // A date like "2026-11-05" becomes a LocalDate (400 if it is missing or has a wrong format)
    // Not private because RoomShare uses it for the dates of the search
    static LocalDate parseDate(String text, String name) {
        if (text == null || text.trim().isEmpty())
            throw new IllegalArgumentException("The " + name + " is required.");

        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("The " + name + " must have the format YYYY-MM-DD.");
        }
    }

    // A time like "18:00" becomes a LocalTime (null if there is no time)
    private static LocalTime parseTime(String text) {
        if (text == null || text.trim().isEmpty())
            return null;

        try {
            // Cut the seconds: the db keeps "HH:mm" only
            return LocalTime.parse(text.trim()).truncatedTo(ChronoUnit.MINUTES);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("The time must have the format HH:MM.");
        }
    }

    // Technical helper: stores a LocalTime as text "18:00" in the db
    @Converter
    public static class TimeConverter implements AttributeConverter<LocalTime, String> {

        private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

        // Java => db: 18:00 becomes "18:00"
        @Override
        public String convertToDatabaseColumn(LocalTime time) {
            return time == null ? null : time.format(FORMAT);
        }

        // db => Java: "18:00" becomes 18:00
        @Override
        public LocalTime convertToEntityAttribute(String text) {
            return text == null ? null : LocalTime.parse(text, FORMAT);
        }
    }
    

    // ------------------------- Overloads -------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        return id != null && id.equals(event.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}