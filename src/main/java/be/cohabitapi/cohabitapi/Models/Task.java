package be.cohabitapi.cohabitapi.Models;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.TaskDAO;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.TaskNotFoundException;
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
    private Integer idTask; // Integer because the id is null before the insert

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
    private TaskStatus statusTask = TaskStatus.PENDING;

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
    public Task(String name, String description, LocalDate dueDate, TaskStatus statusTask, RoomShare roomshare) {
        setName(name);
        setDescription(description);
        setDueDate(dueDate);
        setStatusTask(statusTask);
        setRoomshare(roomshare);
    }


    // ------------------------- Getters and Setters -------------------------

    public Integer getIdTask() {
        return idTask;
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

    public TaskStatus getStatusTask() {
        return statusTask;
    }

    public void setStatusTask(TaskStatus status) {
        if (status == null)
            throw new IllegalArgumentException("The status is required.");
        this.statusTask = status;
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

    // ADD A TASK : the whole use case. The API only calls this method
    // It receives the raw data of the front and the DAOs as parameters
    public static Task create(String roomieEmail, String name, String description, String dueDate,
                              String status, List<Integer> roomieIds,
                              PersonDAO personDAO, TaskDAO taskDAO) {

        // 1. Who creates the task? He must be an accepted roomie
        Roomie creator = Roomie.checkAcceptedRoomie(roomieEmail, personDAO);

        // 2. Clean the inputs
        LocalDate cleanDueDate = parseDueDate(dueDate);

        // A new task cannot already be late (not in the setter, so an old task can still be updated later)
        if (cleanDueDate != null && cleanDueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The due date cannot be in the past.");

        // No status sent => a new task is pending (same default as the db)
        TaskStatus cleanStatus = (status == null || status.trim().isEmpty())
                ? TaskStatus.PENDING
                : TaskStatus.fromLabel(status);

        // 3. Build the task: its roomshare is the roomshare of the creator (an object, not an id)
        // The setters check the name and the description
        Task task = new Task(name, description, cleanDueDate, cleanStatus, creator.getRoomshare());

        // 4. Assign the roomies (each one is checked by assignRoomie)
        task.assignRoomies(roomieIds, personDAO);

        // 5. Save it in the db (the database gives the id)
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

    // TASKS LIST

    // All the tasks of a roomshare
    public static List<Task> findAllForRoomie(Roomie roomie, TaskDAO taskDAO) {
        requireAcceptedMember(roomie);
        return taskDAO.findByRoomshare(roomie.getRoomshare());
    }

    // Find roomies with accepted status of the roomshare so the assignable ones
    public static List<Roomie> findAssignableRoomies(Roomie roomie, PersonDAO personDAO) {
        requireAcceptedMember(roomie);
        return personDAO.findAcceptedRoomies(roomie.getRoomshare());
    }

    // UPDATE A TASK : the whole use case (PUT = everything is replaced)
    // Same checks as create: the setters check every value, assignRoomie checks every roomie
    public static Task update(String roomieEmail, Integer taskId,String name, String description,
                              String dueDate, String status, List<Integer> roomieIds,
                              PersonDAO personDAO, TaskDAO taskDAO){
        // 1. we search for the Roomie who wants to change the task
        Roomie roomie = Roomie.checkAcceptedRoomie(roomieEmail, personDAO);

        // 2. we search the task
        Task task = findTaskOfRoomie(taskId, roomie, taskDAO);

        // 3. we clean the datas
        LocalDate cleanDueDate = parseDueDate(dueDate);

        // 4. A NEW due date cannot be in the past, but keeping the same one is allowed (even if it is late)
        // We compare before setDueDate, while we still know the old date
        boolean dateChanged = cleanDueDate != null && !cleanDueDate.equals(task.getDueDate());
        if(dateChanged && cleanDueDate.isBefore(LocalDate.now())){
            throw new IllegalArgumentException("The due date cannot be in the past.");
        }

        TaskStatus cleanStatus = TaskStatus.fromLabel(status);

        // 5. we use the setters to update the object
        task.setName(name);
        task.setDescription(description);
        task.setDueDate(cleanDueDate);
        task.setStatusTask(cleanStatus);
        task.replaceRoomies(roomieIds, personDAO);

        // 6. we send the object to the dao to update it the database
        taskDAO.update(task);

        return task;
    }

    // CHECK / UNCHECK A TASK : only the status changes ("completed" or "pending")
    // Every accepted roomie of the roomshare can check or uncheck a task of his roomshare
    public static Task changeStatus(String roomieEmail, Integer taskId, String status,
                                    PersonDAO personDAO, TaskDAO taskDAO) {

        // 1. Who is calling? He must be an accepted roomie
        Roomie roomie = Roomie.checkAcceptedRoomie(roomieEmail, personDAO);

        // 2. The task must exist and be in his roomshare (404 otherwise)
        Task task = findTaskOfRoomie(taskId, roomie, taskDAO);

        // 3. Only the status changes (400 if missing or unknown)
        task.setStatusTask(TaskStatus.fromLabel(status));

        // 4. Save it
        taskDAO.update(task);

        return task;
    }

    // DELETE A TASK : the whole use case
    // Every accepted roomie of the roomshare can delete a task of his roomshare
    public static void delete(String roomieEmail, Integer taskId, PersonDAO personDAO, TaskDAO taskDAO){

        // 1. Who is calling? He must be an accepted roomie (401 / 403)
        Roomie roomie = Roomie.checkAcceptedRoomie(roomieEmail, personDAO);

        // 2. The task must exist and be in his roomshare (404 otherwise)
        Task task = findTaskOfRoomie(taskId, roomie, taskDAO);

        // 3. Delete it (its lines in Task_Roomie are deleted too, the roomies are not touched)
        taskDAO.delete(task);
    }

    // -------------Utility methods

    // Loads the roomies from their ids and assigns them one by one
    // Private: only create and update use it
    private void assignRoomies(List<Integer> roomieIds, PersonDAO personDAO) {
        // 0..* in the class diagram: a task without roomie is allowed
        if (roomieIds == null || roomieIds.isEmpty())
            return;

        // A Set removes the duplicates: [3, 3, 5] becomes [3, 5]
        Set<Integer> uniqueIds = new HashSet<>(roomieIds);
        if (uniqueIds.contains(null))
            throw new IllegalArgumentException("Invalid roomie id.");

        List<Roomie> found = personDAO.findRoomiesByIds(uniqueIds);

        // Less roomies than ids: an id does not exist or it is the id of an owner
        if (found.size() != uniqueIds.size())
            throw new IllegalArgumentException("Unknown roomie.");

        for (Roomie roomie : found) {
            assignRoomie(roomie);
        }
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

    // Only an accepted member of a roomshare can see its tasks (403 otherwise)
    // We reuse isAcceptedMemberOf so the rule stays written only once, in Roomie
    private static void requireAcceptedMember(Roomie roomie) {
        if (roomie == null || !roomie.isAcceptedMemberOf(roomie.getRoomshare()))
            throw new NotInRoomshareException();
    }

    // Finds a task of the roomshare of this roomie
    // Does not exist, or is in another roomshare: same exception (404), so nobody learns that it exists elsewhere
    // Used by update, changeStatus and delete
    private static Task findTaskOfRoomie(Integer taskId, Roomie roomie, TaskDAO taskDAO){
        Task task = taskDAO.findById(taskId);

        if(task == null || !roomie.isAcceptedMemberOf(task.getRoomshare())){
            throw new TaskNotFoundException();
        }

        return task;
    }


    // PUT replaces everything: we empty the roomies, then assign the new ones
    // assignRoomies checks each one again (accepted, same roomshare)
    // Hibernate will only delete / insert the lines of Task_Roomie that really changed
    private void replaceRoomies(List<Integer> roomieIds, PersonDAO personDAO){
        roomies.clear();
        assignRoomies(roomieIds, personDAO);
    }
}
