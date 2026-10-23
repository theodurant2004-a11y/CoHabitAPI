package be.cohabitapi.cohabitapi.API;

import be.cohabitapi.cohabitapi.DAO.PersonDAO;
import be.cohabitapi.cohabitapi.DAO.RoomShareDAO;
import be.cohabitapi.cohabitapi.DAO.ShoppingListDAO;
import be.cohabitapi.cohabitapi.DTO.AddItemReq;
import be.cohabitapi.cohabitapi.DTO.AddItemResp;
import be.cohabitapi.cohabitapi.EXCEPTION.RoomShareNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.UnauthorizedUserException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;

import static be.cohabitapi.cohabitapi.API.APIResponse.error;
import static be.cohabitapi.cohabitapi.Models.Roomie.checkRightRoomie;

@Path("/shoppinglist")
public class ShoppingListAPI {

    @Inject private ShoppingListDAO shoppingListDAO;
    @Inject private PersonDAO personDAO;
    @Inject private RoomShareDAO roomShareDAO;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addItem(AddItemReq req) {
        if (req == null) return error(Response.Status.BAD_REQUEST, "JSON invalid.");
        if (req.getRoomshareId() == null) return error(Response.Status.BAD_REQUEST, "Room share ID invalid.");

        try {
            // Verifies that it is a roomie
            Roomie roomie = checkRightRoomie(req.getRoomieEmail(), personDAO);
            RoomShare roomShare = RoomShare.findById(roomShareDAO, req.getRoomshareId());

            // Does the product requested to add not exist ?
            Product product = Product.findByNameAndCategory(shoppingListDAO, req.getName(), req.getCategory());
            if (product == null) {
                product = new Product(req.getName(), req.getCategory());
                product.create(shoppingListDAO);
            }

            if (roomShare.getShoppingList() == null ) {
                ShoppingList shoppingList = new ShoppingList();
                roomShare.setShoppingList(shoppingList); // Add in the same time the roomshare to the shopping list
                shoppingList.create(shoppingListDAO);
            }
            Item item = new Item(product, roomShare.getShoppingList(), req.getQuantity());
            item.create(shoppingListDAO);

            // Build of the response using a DTO and not the real model
            AddItemResp resp = new AddItemResp(item.getProduct().getId(), item.getShoppingList().getId());

            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("item", resp))
                    .build();
        }
        catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e.getMessage());
        }
        catch (UserNotFoundException | RoomShareNotFoundException e) {
            return error(Response.Status.UNAUTHORIZED, e.getMessage());
        }
        catch (UnauthorizedUserException e) {
            return error(Response.Status.FORBIDDEN, e.getMessage());
        }
        catch(RuntimeException e){
            return error(Response.Status.INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createShoppingList() {}

}
