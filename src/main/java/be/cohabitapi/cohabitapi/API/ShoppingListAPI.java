package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.*;
import be.cohabitapi.cohabitapi.DTO.*;
import be.cohabitapi.cohabitapi.EXCEPTION.*;
import be.cohabitapi.cohabitapi.Models.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

import static be.cohabitapi.cohabitapi.API.APIResponse.error;

// For each of the functionalities in this endpoint,
// we retrieve the user id (normally a roomie)
@Path("/shopping-list")
public class ShoppingListAPI {

    // All the DAOs that we need
    @Inject private ItemDAO itemDAO;
    @Inject private ProductDAO productDAO;
    @Inject private PersonDAO personDAO;
    @Inject private RoomShareDAO roomShareDAO;
    @Inject private ExpenseDAO expenseDAO;

    // The method to get the shopping list
    @GET
    @Path("/{roomShareId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItems(@PathParam("roomShareId") int roomShareId) {
        try {
            // Recover a room share with all its items by its id
            RoomShare roomShare = RoomShare.findByIdWithItems(roomShareDAO, roomShareId);

            List<GetItemResponse> itemsDTO = new ArrayList<>();

            // For each item found, we create a DTO with all the information laid out
            for(Item i : roomShare.getItems())
            {
                GetItemResponse itemDTO = new GetItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getCategory(),
                        i.getQuantity(),
                        i.getStatus()
                );
                itemsDTO.add(itemDTO);
            }

            // A list of DTO items is returned as the response body
            // Even if the list is empty, it's returned because,
            // it's only in the js that it will be checked
            return Response.status(Response.Status.OK)
                    .entity(itemsDTO)
                    .build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.BAD_REQUEST, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    // The method to add a new item to the list
    @POST
    @Path("/{roomShareId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItem(@PathParam("roomShareId") int roomShareId, AddItemReq req) {
        // Check that the DTO in the request (the JSON sent) is not null
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid."); // Use of the shortcut method of 'APIResponse'

        try {
            RoomShare roomShare = RoomShare.findByIdWithItems(roomShareDAO, roomShareId);

            // Does the product requested to add exists ? In this case, we create it
            // And in all cases, we return the product
            Product product = Product.findOrCreate(productDAO, req.getName(), req.getCategory());

            // Create the item object by the room share and use the DAO method to create it in db
            roomShare.addItem(product, req.getQuantity(), itemDAO);

            // Use of 'CREATED' because we create in db a new item
            return Response.status(Response.Status.CREATED).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.BAD_REQUEST, "A null value was found");
        }
        catch (ItemAlreadyExistsException e) {
            return error(Response.Status.CONFLICT, e.getMessage());
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    // The method to update an item (its check status or its quantity)
    @PUT
    @Path("/{roomShareId}/{productId}") // The id of the product concerned
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("roomShareId") int roomShareId, @PathParam("productId") int productId, UpdateItemReq req) {
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            RoomShare roomShare = RoomShare.findByIdWithItems(roomShareDAO, roomShareId);

            roomShare.modifyItem(productId, req.getQuantity(), req.getStatus(), itemDAO);

            return Response.status(Response.Status.OK).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException |
               ItemNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.BAD_REQUEST, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    // The method to remove an item from the shopping list
    @DELETE
    @Path("/{roomShareId}/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeItem(@PathParam("roomShareId") int roomShareId, @PathParam("productId") int productId) {
        try {
            RoomShare roomShare = RoomShare.findByIdWithItems(roomShareDAO, roomShareId);

            // We delete the item (the combo of a specific product and a specific room share) in db but not the product
            roomShare.removeItem(productId, itemDAO);

            // 'NO_CONTENT' because, we return nothing but an action is made nonetheless
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException |
               ItemNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.BAD_REQUEST, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    // The method to add a purchase, and where a roomie paid the checked items of the list
    // It creates the expense shared between the roomies and removes the bought items from the list
    @POST
    @Path("/{roomShareId}/purchases")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPurchase(@PathParam("roomShareId") int roomShareId, AddPurchaseReq req) {
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            // The room share comes with its roomies and its items
            RoomShare roomShare = RoomShare.findWithRoomiesAndItems(roomShareDAO, roomShareId);

            // Check if there are checked items, create an expense and delete all the checked items
            roomShare.confirmShoppingListPurchase(req.getPaidBy(), req.getDescription(), req.getAmount(), expenseDAO, itemDAO);

            return Response.status(Response.Status.CREATED).build();
        }
        catch (RoomShareNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NoItemCheckedException e) {
            // Conflict because the state of the list is wrong
            return error(Response.Status.CONFLICT, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

}