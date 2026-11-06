package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.TaskDAO;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "Task")
public class Task {
    // ------------------------- Attributes and JPA Annotations -------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer id; // Integer because the id is null before the insert

    @Column(name = "task_name", length = 100, nullable = false)
    private String name;

    // Nullable in the db, so it can be null here too
    @Column(name = "task_description", length = 255)
    private String description;

    // LocalDate matches the SQL DATE type (same as the creation date of RoomShare)
    @Column(name = "task_due_date")
    private LocalDate dueDate;

    // Stored as a number (1 = PENDING, 2 = COMPLETED)
    @Convert(converter = TaskStatus.DbConverter.class)
    @Column(name = "task_status", nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    // a task belongs to one roomshare
    // This side fills the roomshare_id column
    @ManyToOne(optional = false)
    @JoinColumn(name = "roomshare_id", nullable = false)
    private RoomShare roomshare;

    // The roomies who must do the task (table Task_Roomie)
    // A Set because the same roomie cannot be assigned twice (primary key task_id + user_id)
    @ManyToMany
    @JoinTable(
            name = "Task_Roomie",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<Roomie> roomies = new HashSet<>();


    // ------------------------- Constructors -------------------------

    // Not public: only Hibernate uses it to load a task from the database
    protected Task() {
    }

    // The setters check every value
    public Task(String name, String description, LocalDate dueDate, TaskStatus status, RoomShare roomshare) {
        setName(name);
        setDescription(description);
        setDueDate(dueDate);
        setStatus(status);
        setRoomshare(roomshare);
    }


    // ------------------------- Getters and Setters -------------------------

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    // The name is required and maximum 100 characters
    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("The task name is required.");

        String trimmed = name.trim();
        if (trimmed.length() > 100)
            throw new IllegalArgumentException("The task name cannot be longer than 100 characters.");

        this.name = trimmed;
    }

    public String getDescription() {
        return description;
    }

    // The description is optional: null or only spaces means no description
    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            this.description = null;
            return;
        }

        String trimmed = description.trim();
        if (trimmed.length() > 255)
            throw new IllegalArgumentException("The task description cannot be longer than 255 characters.");

        this.description = trimmed;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    // The due date is optional (null = no deadline)
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        if (status == null)
            throw new IllegalArgumentException("The status is required.");
        this.status = status;
    }

    public RoomShare getRoomshare() {
        return roomshare;
    }

    // Private: a task never moves to another roomshare (composition)
    private void setRoomshare(RoomShare roomshare) {
        if (roomshare == null)
            throw new IllegalArgumentException("The roomshare cannot be null.");
        this.roomshare = roomshare;
    }

    // Read only view: the only way to add a roomie is assignRoomie (which checks the rules)
    public Set<Roomie> getRoomies() {
        return Collections.unmodifiableSet(roomies);
    }


    // ------------------------- Business methods -------------------------

    // ADD A TASK
    // Static because it creates a new task. The creator was already checked by Roomie.findAcceptedMember,
    // so his roomshare is loaded with its roomies
    public static Task create(Roomie creator, String name, String description, String dueDate,
                              String status, List<Integer> roomieIds, TaskDAO taskDAO) {

        // 1. Clean the inputs
        LocalDate cleanDueDate = parseDueDate(dueDate);

        // A new task cannot already be late (not in the setter, so an old task can still be updated later)
        if (cleanDueDate != null && cleanDueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The due date cannot be in the past.");

        // No status sent => a new task is pending (same default as the db)
        TaskStatus cleanStatus = (status == null || status.trim().isEmpty())
                ? TaskStatus.PENDING
                : TaskStatus.fromLabel(status);

        // 2. Build the task: its roomshare is the roomshare of the creator (an object, not an id)
        // The setters check the name and the description
        Task task = new Task(name, description, cleanDueDate, cleanStatus, creator.getRoomshare());

        // 3. Assign the roomies, found in the roomshare of the creator
        task.assignRoomies(roomieIds, creator.getRoomshare());

        // 4. Save it in the db (the database gives the id)
        taskDAO.create(task);

        return task;
    }

    // A task can only be given to an accepted roomie of the same roomshare
    // We ask the roomie: he knows his own status and roomshare
    public void assignRoomie(Roomie roomie) {
        if (roomie == null)
            throw new IllegalArgumentException("The roomie cannot be null.");

        if (!roomie.isAcceptedMemberOf(this.roomshare))
            throw new IllegalArgumentException("A task can only be assigned to a roomie of the roomshare.");

        roomies.add(roomie);
    }

    // UPDATE A TASK (PUT = everything is replaced)
    // Called on a task found with roomie.findTask, so we already know it is in his roomshare
    // roomshare: the roomshare of the roomie, loaded with its roomies (the one of the task is not)
    public void update(String name, String description, String dueDate, String status,
                       List<Integer> roomieIds, RoomShare roomshare, TaskDAO taskDAO) {

        // 1. Clean the inputs
        LocalDate cleanDueDate = parseDueDate(dueDate);

        // 2. A NEW due date cannot be in the past, but keeping the same one is allowed (even if it is late)
        // We compare before setDueDate, while we still know the old date
        boolean dateChanged = cleanDueDate != null && !cleanDueDate.equals(this.dueDate);
        if (dateChanged && cleanDueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The due date cannot be in the past.");

        // Required for a PUT (fromLabel refuses null)
        TaskStatus cleanStatus = TaskStatus.fromLabel(status);

        // 3. The setters check every value
        setName(name);
        setDescription(description);
        setDueDate(cleanDueDate);
        setStatus(cleanStatus);
        replaceRoomies(roomieIds, roomshare);

        // 4. Save it
        taskDAO.update(this);
    }

    // CHECK / UNCHECK A TASK : only the status changes ("completed" or "pending")
    public void changeStatus(String status, TaskDAO taskDAO) {
        // 400 if missing or unknown
        setStatus(TaskStatus.fromLabel(status));
        taskDAO.update(this);
    }

    // DELETE A TASK : its lines in Task_Roomie are deleted too, the roomies are not touched
    public void delete(TaskDAO taskDAO) {
        taskDAO.delete(this);
    }


    // ------------------------- Utility methods -------------------------

    // Finds the roomies in the roomshare and assigns them one by one
    // Private: only create and update use it
    private void assignRoomies(List<Integer> roomieIds, RoomShare roomshare) {
        // 0..* in the class diagram: a task without roomie is allowed
        if (roomieIds == null || roomieIds.isEmpty())
            return;

        // A Set removes the duplicates: [3, 3, 5] becomes [3, 5]
        for (Integer id : new HashSet<>(roomieIds)) {
            // findRoomie refuses a null id and an id that is not an accepted roomie of the roomshare (400)
            assignRoomie(roomshare.findRoomie(id));
        }
    }

    // PUT replaces everything: we empty the roomies, then assign the new ones
    // Hibernate will only delete / insert the lines of Task_Roomie that really changed
    private void replaceRoomies(List<Integer> roomieIds, RoomShare roomshare) {
        roomies.clear();
        assignRoomies(roomieIds, roomshare);
    }

    // "2026-10-27" becomes a LocalDate. Nothing sent => no due date
    private static LocalDate parseDueDate(String dueDate) {
        if (dueDate == null || dueDate.trim().isEmpty())
            return null;

        try {
            // LocalDate.parse reads the ISO format YYYY-MM-DD
            return LocalDate.parse(dueDate.trim());
        } catch (DateTimeParseException e) {
            // We turn it into an IllegalArgumentException so the API answers 400 with a clear message
            throw new IllegalArgumentException("The due date must have the format YYYY-MM-DD.");
        }
    }
}