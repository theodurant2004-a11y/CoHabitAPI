package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import be.cohabitapi.cohabitapi.Models.Owner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

@ApplicationScoped
public class RoomShareDAO extends DAO<RoomShare> {
    // CREATE A ROOMSHARE : Create the room share by the EM persist method
    @Override
    public void create(RoomShare rs) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                em.persist(rs);

                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive())
                    transaction.rollback();

                throw e;
            }
        }
    }

    public RoomShare findById(Integer id){
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.find(RoomShare.class, id);
        }
    }

    // LOGIN : the roomshare created by an owner (null if he has not created it yet)
    public RoomShare findByOwner(Owner owner) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            List<RoomShare> found = em.createQuery(
                            "SELECT r FROM RoomShare r WHERE r.owner.idPerson = :ownerId ORDER BY r.id", RoomShare.class)
                    .setParameter("ownerId", owner.getIdPerson())
                    .setMaxResults(1)
                    .getResultList();

            return found.isEmpty() ? null : found.get(0);
        }
    }

    public RoomShare findByIdWithItems(Integer id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return loadWithItems(em, id);
        }
    }

    public RoomShare findWithRoomies(Integer id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return loadWithRoomies(em, id);
        }
    }

    // We separate the find part and the load part
    // to use the same room share for the two methods called here
    // because, we create only one Entity Manager
    public RoomShare findWithRoomiesAndItems(Integer id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            RoomShare roomShare = loadWithRoomies(em, id);

            if (roomShare != null)
                loadWithItems(em, id);

            return roomShare;
        }
    }

    // EXPENSES OF THE OWNER : the roomshare with its roomies, their expenses and what its owner has to receive
    // Same em for the two parts, so the owner filled by the last query is the owner of the roomshare
    public RoomShare findWithCharges(Integer id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            RoomShare roomShare = loadWithRoomies(em, id);

            if (roomShare != null) {
                // The parts of the rents and the bills (they fill the list of the owner loaded with the roomshare)
                em.createQuery(
                                "SELECT DISTINCT o FROM Owner o LEFT JOIN FETCH o.expensesToReceive WHERE o.idPerson = :ownerId",
                                Owner.class)
                        .setParameter("ownerId", roomShare.getOwner().getIdPerson())
                        .getResultList();
            }

            return roomShare;
        }
    }

    // CREATE A ROOMSHARE : Checks if the invitation key is already taken
    public boolean keyIsAlreadyUsed(String key){

        try (EntityManager em = JpaUtil.createEntityManager()) {
            Long count = em.createQuery("SELECT COUNT(r) FROM RoomShare r WHERE r.invitationKey = :key", Long.class)
                    .setParameter("key", key)
                    .getSingleResult();
            return count > 0;
        }
    }

    // This method is to join a roomshare : it finds a roomshare with its invitation key (null if no roomshare uses it)
    public RoomShare findByInvitationKey(String key) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            List<RoomShare> results = em.createQuery(
                            "SELECT r FROM RoomShare r WHERE r.invitationKey = :key", RoomShare.class)
                    .setParameter("key", key)
                    .getResultList();

            // The key is unique in the db, so there is at most one result
            return results.isEmpty() ? null : results.get(0);
        }
    }

    private RoomShare loadWithItems(EntityManager em, Integer id) {
        List<RoomShare> results = em.createQuery(
                        "SELECT DISTINCT rs FROM RoomShare rs LEFT JOIN FETCH rs.items i LEFT JOIN FETCH i.product WHERE rs.id = :id", RoomShare.class)
                .setParameter("id", id)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    // Loads a roomshare with its roomies and all their expenses
    // 3 queries with the same em because Hibernate can't fetch two lists in one query
    public RoomShare loadWithRoomies(EntityManager em, Integer id) {
        // The roomshare and its roomies
        List<RoomShare> found = em.createQuery(
                        "SELECT DISTINCT rs FROM RoomShare rs LEFT JOIN FETCH rs.roomies WHERE rs.id = :id",
                        RoomShare.class)
                .setParameter("id", id)
                .getResultList();

        if (found.isEmpty())
            return null;

        // The expenses the roomies have to pay (they fill the lists of the roomies loaded above)
        em.createQuery(
                        "SELECT DISTINCT r FROM Roomie r LEFT JOIN FETCH r.expensesToPay WHERE r.roomshare.id = :id",
                        Roomie.class)
                .setParameter("id", id)
                .getResultList();

        // The expenses the roomies have to receive
        em.createQuery(
                        "SELECT DISTINCT r FROM Roomie r LEFT JOIN FETCH r.expensesToReceive WHERE r.roomshare.id = :id",
                        Roomie.class)
                .setParameter("id", id)
                .getResultList();

        return found.get(0);
    }
}
