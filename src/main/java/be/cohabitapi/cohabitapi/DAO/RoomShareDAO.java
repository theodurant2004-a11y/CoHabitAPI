package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.RoomShareNotFoundException;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
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
            RoomShare rs = em.find(RoomShare.class, id);
            if (rs == null) throw new RoomShareNotFoundException();
            return rs;
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
        EntityManager em = JpaUtil.createEntityManager();

        try {
            List<RoomShare> results = em.createQuery(
                            "SELECT r FROM RoomShare r WHERE r.invitationKey = :key", RoomShare.class)
                    .setParameter("key", key)
                    .getResultList();

            // The key is unique in the db, so there is at most one result
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    // Loads a roomshare with its roomies and all their expenses
    // 3 queries with the same em because Hibernate can't fetch two lists in one query
    public RoomShare findWithRoomies(Integer id) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
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
        } finally {
            em.close();
        }
    }
}
