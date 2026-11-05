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
import java.util.Collections;
import java.util.List;

import static be.cohabitapi.cohabitapi.API.APIResponse.error;
import static be.cohabitapi.cohabitapi.Models.Roomie.checkRightRoomie;
import static be.cohabitapi.cohabitapi.Models.Roomie.findById;

@Path("/users/{userId}/shopping-items")
public class ShoppingListAPI {

    @Inject private ItemDAO itemDAO;
    @Inject private ProductDAO productDAO;
    @Inject private PersonDAO personDAO;
    @Inject private RoomShareDAO roomShareDAO;
    @Inject private ExpenseDAO expenseDAO;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getShoppingList(@PathParam("userId") int userId) {
        try {
            RoomShare roomShare = RoomShare.findRoomShareByRoomie(personDAO, roomShareDAO, userId);

            List<GetItemResponse> itemsDTO = new ArrayList<>();

            List<Item> items = Item.findAll(itemDAO, roomShare.getId());

            for(Item i : items)
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

            return Response.status(Response.Status.OK)
                    .entity(itemsDTO)
                    .build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NotMemberOfRoomShareException | ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException | NoItemInTheListException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.NOT_FOUND, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItem(@PathParam("userId") int userId, AddItemReq req) {
        if (req == null ||
                req.getName() == null ||
                req.getCategory() == null ||
                req.getQuantity() == null
        ) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            RoomShare roomShare = RoomShare.findRoomShareByRoomie(personDAO, roomShareDAO, userId);

            // Does the product requested to add not exist ?
            Product product = Product.findByNameAndCategory(productDAO, req.getName(), req.getCategory());
            if (product == null) {
                product = new Product(req.getName(), req.getCategory());
                product.create(productDAO);
            }

            Item item = new Item(product, roomShare, req.getQuantity());
            item.create(itemDAO);

            return Response.status(Response.Status.CREATED).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NotMemberOfRoomShareException | ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.NOT_FOUND, "A null value was found");
        }
        catch (ItemAlreadyExistsException e) {
            return error(Response.Status.CONFLICT, e.getMessage());
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    @PUT
    @Path("/{productId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("userId") int userId, @PathParam("productId") int productId, UpdateItemReq req) {
        if (req == null || req.getQuantity() == null || req.getStatus() == null)
            return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            RoomShare roomShare = RoomShare.findRoomShareByRoomie(personDAO, roomShareDAO, userId);

            Product product = Product.findById(productDAO, productId);

            Item item = Item.findById(itemDAO, product.getId(), roomShare.getId());
            item.setStatus(req.getStatus());
            item.setQuantity(req.getQuantity());
            item.update(itemDAO); // Update in db

            return Response.status(Response.Status.OK).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException |
               ItemNotFoundException | ProductNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NotMemberOfRoomShareException | ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.NOT_FOUND, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    @DELETE
    @Path("/{productId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteItem(@PathParam("userId") int userId, @PathParam("productId") int productId) {
        try {
            RoomShare roomShare = RoomShare.findRoomShareByRoomie(personDAO, roomShareDAO, userId);
            Product product = Product.findById(productDAO, productId);
            Item item = Item.findById(itemDAO, product.getId(), roomShare.getId());
            item.delete(itemDAO);

            return Response.status(Response.Status.OK).build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException |
               ItemNotFoundException | ProductNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NotMemberOfRoomShareException | ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.NOT_FOUND, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error.");
        }
    }

    @POST
    @Path("/confirm-purchases")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response confirmPurchases(@PathParam("userId") int userId, ConfirmPurchasesReq req) {
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");

        try {
            Roomie buyer = checkRightRoomie(personDAO, userId);
            Roomie loadedBuyer = buyer.loadRoomshare(roomShareDAO);
            RoomShare roomShare = loadedBuyer.getRoomshare();
            List<Roomie> allRoomies = roomShare.getAcceptedRoomies();
            List<Integer> allRoomiesIds = new ArrayList<>();
            for (Roomie roomie : allRoomies) {
                allRoomiesIds.add(roomie.getIdPerson());
            }

            loadedBuyer.addExpense(
                    req.getDescription(),
                    req.getAmount(),
                    java.time.LocalDate.now().toString(),
                    "food", // TODO : To replace when the catgeory "groceries" will be added
                    buyer.getIdPerson(),
                    allRoomiesIds,
                    expenseDAO
            );

            Item.deleteCheckedItems(itemDAO, roomShare.getId());

            return Response.status(Response.Status.OK)
                    .build();
        }
        catch (UserNotFoundException | RoomShareNotFoundException |
               ItemNotFoundException | ProductNotFoundException e) {
            return error(Response.Status.NOT_FOUND, e.getMessage());
        }
        catch (NotInRoomshareException | NotMemberOfRoomShareException | ForbiddenRoleException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch (IllegalArgumentException | NoItemCheckedException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (NullPointerException e) {
            return error(Response.Status.NOT_FOUND, "A null value was found.");
        }
        catch (RuntimeException e) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}