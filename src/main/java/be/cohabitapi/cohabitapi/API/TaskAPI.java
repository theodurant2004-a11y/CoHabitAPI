package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
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
// Each route starts with Roomie.findAcceptedMember: it checks who is calling (401 / 403)
// and gives back the roomie with his roomshare and its roomies loaded
@Path("/tasks")
public class TaskAPI {

    @Inject
    private PersonDAO personDAO;
    @Inject
    private RoomShareDAO roomShareDAO;
    @Inject
    private TaskDAO taskDAO;

    // ADD A TASK : POST /api/tasks
    // Only an accepted roomie can add a task, in his own roomshare
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTask(TaskRequest req) {

        // No body at all: 400
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // 1. Who is calling? He must be an accepted roomie
            Roomie roomie = Roomie.findAcceptedMember(req.getRoomieEmail(), personDAO, roomShareDAO);

            // 2. The model checks the values, assigns the roomies and saves
            Task task = Task.create(
                    roomie,
                    req.getName(),
                    req.getDescription(),
                    req.getDueDate(),
                    req.getStatus(),
                    req.getRoomieIds(),
                    taskDAO);

            // 201 CREATED: a new task exists in the db. We answer with a DTO, never with the real model
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("task", new TaskResponse(task)))
                    .build();

        } catch (UserNotFoundException e) {
            // 401: unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

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
            // 1. Who is calling? He must be an accepted roomie
            Roomie roomie = Roomie.findAcceptedMember(roomieEmail, personDAO, roomShareDAO);

            // 2. The tasks of his roomshare, and the accepted roomies (already loaded with the roomshare)
            List<Task> tasks = roomie.findTasks(taskDAO);
            List<Roomie> roomies = roomie.getRoomshare().getAcceptedRoomies();

            // 3. We answer with a DTO, never with the real model
            return Response.ok(new TaskListResponse(tasks, roomies)).build();

        } catch (UserNotFoundException e) {
            // 401: unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

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

    // UPDATE A TASK : PUT /api/tasks/{id}
    // PUT = everything is replaced, the front sends the whole task
    // {id} is read from the URL by @PathParam (/api/tasks/abc gives a 404 automatically)
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTask(@PathParam("id") Integer id, TaskRequest req) {
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "JSON invalid.");
        }

        try {
            // 1. Who is calling? He must be an accepted roomie
            Roomie roomie = Roomie.findAcceptedMember(req.getRoomieEmail(), personDAO, roomShareDAO);

            // 2. The task must be in his roomshare (404 otherwise)
            Task task = roomie.findTask(id, taskDAO);

            // 3. The model checks the new values and saves
            task.update(
                    req.getName(),
                    req.getDescription(),
                    req.getDueDate(),
                    req.getStatus(),
                    req.getRoomieIds(),
                    roomie.getRoomshare(),
                    taskDAO);

            return Response.ok(Collections.singletonMap("task", new TaskResponse(task))).build();

        } catch (UserNotFoundException e) {
            // 401: unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

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

    // CHECK / UNCHECK A TASK : PUT /api/tasks/{id}/status
    // Only the status changes, so the front only sends the status (and the email for now)
    // {id} is read from the URL by @PathParam (/api/tasks/abc gives a 404 automatically)
    @PUT
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeStatus(@PathParam("id") Integer id, TaskStatusRequest req) {
        if (req == null) {
            return error(Response.Status.BAD_REQUEST, "JSON invalid.");
        }

        try {
            // 1. Who is calling? He must be an accepted roomie
            Roomie roomie = Roomie.findAcceptedMember(req.getRoomieEmail(), personDAO, roomShareDAO);

            // 2. The task must be in his roomshare (404 otherwise)
            Task task = roomie.findTask(id, taskDAO);

            // 3. Only the status changes (400 if missing or unknown)
            task.changeStatus(req.getStatus(), taskDAO);

            return Response.ok(Collections.singletonMap("task", new TaskResponse(task))).build();

        } catch (UserNotFoundException e) {
            // 401: unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

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

    // DELETE A TASK : DELETE /api/tasks/{id}?roomieEmail=...
    // No body (a DELETE with a body is badly supported), so the email comes in the URL like for the GET
    // Every accepted roomie of the roomshare can delete a task of his roomshare
    // TO DO : remove roomieEmail when the session system is efficient
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteTask(@PathParam("id") Integer id, @QueryParam("roomieEmail") String roomieEmail) {
        try {
            // 1. Who is calling? He must be an accepted roomie
            Roomie roomie = Roomie.findAcceptedMember(roomieEmail, personDAO, roomShareDAO);

            // 2. The task must be in his roomshare (404 otherwise), then it is deleted
            roomie.findTask(id, taskDAO).delete(taskDAO);

            return Response.noContent().build();

        } catch (UserNotFoundException e) {
            // 401: unknown email
            return error(Response.Status.UNAUTHORIZED, e.getMessage());

        } catch (ForbiddenRoleException e) {
            // 403: an owner cannot delete a task
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (NotInRoomshareException e) {
            // 403: the roomie has no roomshare or his request is still pending
            return error(Response.Status.FORBIDDEN, e.getMessage());

        } catch (TaskNotFoundException e) {
            // 404: the task does not exist or is not in his roomshare
            return error(Response.Status.NOT_FOUND, e.getMessage());

        } catch (IllegalArgumentException e) {
            // 400: the email is missing or empty
            return error(Response.Status.BAD_REQUEST, e.getMessage());

        } catch (RuntimeException e) {
            // 500: anything unexpected, the client only gets a generic message
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }
}