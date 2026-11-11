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

    // ADD / UPDATE A TASK : builds a task from what the front sent
    // roomshare: the roomshare of the roomie, loaded with its roomies (by Roomie.findAcceptedMember)
    // Every value is checked here (400 if one is wrong)
    // The rule "no due date in the past" is in create and update: it depends on the action, not on the values
    public Task(String name, String description, String dueDate, String status, List<Integer> roomieIds, RoomShare roomshare){

        setName(name);
        setDescription(description);
        setDueDate(parseDueDate(dueDate));

        // No status sent means a new task is pending (it's the same logic in the db, by default a task is pending)
        setStatus((status == null || status.trim().isEmpty()) ? TaskStatus.PENDING : TaskStatus.fromLabel(status));

        // We set the roomshare before the roomies, if no roomshare it return an error 400
        setRoomshare(roomshare);

        // The roomies are found in the roomshare of the roomie who calls
        assignRoomies(roomieIds, roomshare);
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

    // Read only view: the roomies are only assigned when the task is created or updated
    public Set<Roomie> getRoomies() {
        return Collections.unmodifiableSet(roomies);
    }


    // ------------------------- Business methods -------------------------

    // ADD A TASK : the task built by the constructor saves itself in the db (the database gives the id)
    public void create(TaskDAO taskDAO){
        // A new task cannot already be late (400)
        if (dueDate != null && dueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The due date cannot be in the past.");

        taskDAO.create(this);
    }

    // UPDATE A TASK (PUT = everything is replaced)
    // Called on a task found with roomie.findTask, so we already know it is in his roomshare
    // changes: a task built by the constructor with the new values, so they are already checked
    public void update(Task changes, TaskDAO taskDAO) {
        // A NEW due date cannot be in the past, but keeping the same one is allowed (even if it is late)
        // We compare before setDueDate, while we still know the old date
        boolean dateChanged = changes.getDueDate() != null && !changes.getDueDate().equals(this.dueDate);
        if (dateChanged && changes.getDueDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The due date cannot be in the past.");

        // This task takes the new values
        setName(changes.getName());
        setDescription(changes.getDescription());
        setDueDate(changes.getDueDate());
        setStatus(changes.getStatus());

        // The old roomies are replaced by the new ones
        // Hibernate will only delete / insert the lines of Task_Roomie that really changed
        roomies.clear();
        roomies.addAll(changes.getRoomies());

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

    // True if this task is in this roomshare
    // We compare the ids: two different objects can represent the same roomshare (two EntityManagers)
    public boolean belongsTo(RoomShare roomshare){
        return roomshare != null && this.roomshare.getId().equals(roomshare.getId());
    }


    // ------------------------- Utility methods -------------------------

    // Finds the roomies in the roomshare and adds them to the task
    // findRoomie only gives an accepted roomie of the roomshare: 400 if the id is null or not one of them
    // Private: only the constructor uses it
    private void assignRoomies(List<Integer> roomieIds, RoomShare roomshare) {
        // 0..* in the class diagram: a task without roomie is allowed
        if (roomieIds == null || roomieIds.isEmpty())
            return;

        // A Set removes the duplicates.
        for(Integer id : new HashSet<>(roomieIds)){
            roomies.add(roomshare.findRoomie(id));
        }
    }

    // "2026-10-27" becomes a LocalDate. Nothing sent => no due date
    private LocalDate parseDueDate(String dueDate) {
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