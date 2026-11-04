package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.TaskDAO;
import be.cohabitapi.cohabitapi.DTO.*;
import be.cohabitapi.cohabitapi.EXCEPTION.ForbiddenRoleException;
import be.cohabitapi.cohabitapi.EXCEPTION.NotInRoomshareException;
import be.cohabitapi.cohabitapi.EXCEPTION.TaskNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Roomie;
import be.cohabitapi.cohabitapi.Models.Task;
import static be.cohabitapi.cohabitapi.API.APIResponse.error;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

// Every route about the tasks of a roomshare
// The full URL is /api/tasks
@Path("/tasks")
public class TaskAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private TaskDAO taskDAO;

    // ADD A TASK : POST /api/tasks
    // Only an accepted roomie can add a task, in his own roomshare
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTask(TaskCreateRequest req) {

        // No body at all: 400
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The model does all the work (checks, roomies, save)
            Task task = Task.create(
                    req.getRoomieEmail(),
                    req.getName(),
                    req.getDescription(),
                    req.getDueDate(),
                    req.getStatus(),
                    req.getRoomieIds(),
                    personDAO,
                    taskDAO);

            // We answer with a DTO, never with the real model
            TaskResponse resp = new TaskResponse(task);

            // 201 CREATED: a new task exists in the db
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("task", resp))
                    .build();

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot add a task
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403: the roomie has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400: a business rule is broken in the model (name, date, status, roomies...)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected, the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    // TASKS LIST : GET /api/tasks?roomieEmail=...
    // Returns the tasks of the roomshare + the roomies a task can be assigned to
    // A GET has no body, so the email comes in the URL
    // TO DO : remove roomieEmail when the session system is efficient
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTasks(@QueryParam("roomieEmail") String roomieEmail) {

        try {
            // 1. The one calling must be an accepted roomie (checked once)
            Roomie roomie = Roomie.checkAcceptedRoomie(roomieEmail, personDAO);

            // 2. Ask the model: the tasks of his roomshare, and who can be assigned
            List<Task> tasks = Task.findAllForRoomie(roomie, taskDAO);
            List<Roomie> roomies = Task.findAssignableRoomies(roomie, personDAO);

            // 3. We answer with a DTO, never with the real model
            return Response.ok(new TaskListResponse(tasks, roomies)).build();

        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot see the tasks
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403: the roomie has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400: the email is missing or empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected, the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }


    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTask(@PathParam("id") Integer id, TaskUpdateRequest req){
        if(req == null){
            return error(Response.Status.BAD_REQUEST, "JSON invalid.");
        }

        try {
            Task task = Task.update(
                    req.getRoomieEmail(),
                    id,
                    req.getName(),
                    req.getDescription(),
                    req.getDueDate(),
                    req.getStatus(),
                    req.getRoomieIds(),
                    personDAO,
                    taskDAO);

            TaskResponse resp = new TaskResponse(task);

            return Response.status(Response.Status.OK)
                    .entity(Collections.singletonMap("task", resp))
                    .build();
        } catch (UserNotFoundException e) {
            // 401: neutral message, so nobody can find out which emails have an account
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot update a task
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403: the roomie has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (TaskNotFoundException e) {
            // 404: the task does not exist or is not in his roomshare
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400: a business rule is broken in the model (name, date, status, roomies...)
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected, the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeStatus(@PathParam("id") Integer id, TaskStatusRequest req){
        if(req == null){
            return error(Response.Status.BAD_REQUEST, "JSON invalid.");
        }

        try {
            // The model checks the roomie, the task and the status, then saves
            Task task = Task.changeStatus(
                    req.getRoomieEmail(),
                    id,
                    req.getStatus(),
                    personDAO,
                    taskDAO);

            // 200 OK with the updated task
            return Response.ok(Collections.singletonMap("task", new TaskResponse(task))).build();

        } catch (UserNotFoundException e) {
            // 401: neutral message
            return error(Response.Status.UNAUTHORIZED, "Authentication required.");

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot check a task
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403: the roomie has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (TaskNotFoundException e) {
            // 404: the task does not exist or is not in his roomshare
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400: the status is missing or unknown, or the email is empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

}