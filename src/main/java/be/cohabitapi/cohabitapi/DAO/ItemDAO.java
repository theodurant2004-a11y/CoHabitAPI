package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.ItemAlreadyExistsException;
import be.cohabitapi.cohabitapi.EXCEPTION.ItemNotFoundException;
import be.cohabitapi.cohabitapi.EXCEPTION.NoItemCheckedException;
import be.cohabitapi.cohabitapi.EXCEPTION.NoItemInTheListException;
import be.cohabitapi.cohabitapi.Models.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

@ApplicationScoped
public class ItemDAO extends DAO<Item> {

    @Override
    public void create(Item i) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                i.setProduct(em.getReference(Product.class, i.getProduct().getId()));
                i.setRoomShare(em.getReference(RoomShare.class, i.getRoomShare().getId()));

                // Here is like a INSERT in db but just "the preparation"
                em.persist(i);

                // The real INSERT in db is here, with the commit confirmation
                transaction.commit();
            }
            catch (RuntimeException e) {
                if (transaction.isActive())
                    transaction.rollback();

                // We check that this an actual uniqueness constraint violation

                // We create a variable to contain the cause,
                // and first it contains the exception concerned
                // by the catch (RuntimeException)
                Throwable cause = e;

                while (cause != null) {

                    // We check if this current cause is an object of these exception classes
                    if (cause instanceof org.hibernate.exception.ConstraintViolationException ||
                            cause instanceof java.sql.SQLIntegrityConstraintViolationException) {
                        // In this case, we know that this is really a uniqueness constraint violation
                        // So, we throw our own specific exception
                        throw new ItemAlreadyExistsException();
                    }

                    // And because this method will get
                    // at each loop the cause of the current cause,
                    // at some point, it will be null
                    // because the current cause is the origin
                    cause = cause.getCause();
                }

                // Maybe it's not at all an integrity constraint exception
                // So, we throw basically the general RuntimeException
                throw e;
            }
        }
    }

    public Item findById(int productId, int roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            ItemId id = new ItemId(productId, roomShareId);
            Item i = em.find(Item.class, id);
            if (i == null) throw new ItemNotFoundException();
            return i;
        }
    }

    public List<Item> findAll(Integer roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            List<Item> items = em.createQuery(
                            "SELECT i FROM Item i WHERE i.roomShare.id = :roomShareId", Item.class)
                    .setParameter("roomShareId", roomShareId)
                    .getResultList(); // Returns an empty list if nothing is found

            if (items.isEmpty()) throw new NoItemInTheListException();

            return items;
        }
    }

    // In the application, you can modify the quantity or the found status
    // We therefore receive a complete Item object that contains whether
    // a modification of the quantity or the found status
    // The process is identical for both actions
    // Then, a single method is used for these two "different" attributes
    public void update(Item i) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                em.merge(i);

                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }

    public void delete(Item i) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                ItemId id = new ItemId(i.getProduct().getId(), i.getRoomShare().getId());
                Item managedItem = em.getReference(Item.class, id);

                em.remove(managedItem);

                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }

    public void deleteCheckedItems(int roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                int deletedCount = em.createQuery(
                                "DELETE FROM Item i WHERE i.roomShare.id = :roomShareId AND i.status = true")
                        .setParameter("roomShareId", roomShareId)
                        .executeUpdate();

                if (deletedCount < 1) throw new NoItemCheckedException();

                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }
}
