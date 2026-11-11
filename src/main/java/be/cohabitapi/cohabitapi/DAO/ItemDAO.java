package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.ItemAlreadyExistsException;
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
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }

    public Item findById(int productId, int roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            ItemId id = new ItemId(productId, roomShareId);
            return em.find(Item.class, id);
        }
    }

    public List<Item> findAll(Integer roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                    // JOIN FETCH load the products by the way !
                            "SELECT i FROM Item i JOIN FETCH i.product WHERE i.roomShare.id = :roomShareId", Item.class)
                    .setParameter("roomShareId", roomShareId)
                    .getResultList(); // Returns an empty list if nothing is found
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

    public long countCheckedItems(int roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                            "SELECT COUNT(i) FROM Item i WHERE i.roomShare.id = :roomShareId AND i.status = true", Long.class)
                    .setParameter("roomShareId", roomShareId)
                    .getSingleResult();
        }
    }

    public int deleteCheckedItems(int roomShareId) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                int deletedCount = em.createQuery(
                                "DELETE FROM Item i WHERE i.roomShare.id = :roomShareId AND i.status = true")
                        .setParameter("roomShareId", roomShareId)
                        .executeUpdate();

                transaction.commit();

                return deletedCount;
            } catch (RuntimeException e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }
}
