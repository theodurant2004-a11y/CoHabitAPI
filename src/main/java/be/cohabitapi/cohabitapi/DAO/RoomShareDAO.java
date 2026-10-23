package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.RoomShare;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.util.List;

@ApplicationScoped
public class RoomShareDAO extends DAO<RoomShare> {
    // CREATE A ROOMSHARE : Create the room share by the EM persist method
    @Override
    public void create(RoomShare rs) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Here is like a INSERT in db but just "the preparation"
                em.persist(rs);

                // The real INSERT in db is here, with the commit confirmation
                transaction.commit();
            } catch (RuntimeException e) {
                if(transaction.isActive()){
                    transaction.rollback();
                }

                throw e;
            }
        } finally {
            em.close();
        }
    }

    // CREATE A ROOMSHARE : Checks if the invitation key is already taken
    public boolean keyIsAlreadyUsed(String key){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            Long count = em.createQuery( "SELECT COUNT(r) FROM RoomShare r WHERE r.invitationKey = :key", Long.class)
                    .setParameter("key", key)
                    .getSingleResult();

            return count > 0;
        } finally {
            em.close();
        }
    }

    // This method is to join a roomshare : it finds a roomshare with its invitation key (null if no roomshare uses it)
    public RoomShare findByInvitationKey(String key) {
        // One EntityManager per call
        EntityManager em = JpaUtil.createEntityManager();

        try {
            // Here we work on the Java class and not the table: so RoomShare is the class, r.invitationKey is the attribute
            // :key is a parameter, so there is no sql injection
            // And I use getResultList instead of getSingleResult so there is no exception when nothing is found
            List<RoomShare> results = em.createQuery(
                            "SELECT r FROM RoomShare r WHERE r.invitationKey = :key", RoomShare.class)
                    .setParameter("key", key)
                    .getResultList();

            // The key is unique in the db, so there is at most one result
            return results.isEmpty() ? null : results.get(0);
        } finally {
            // We always need to close the EntityManager, even if there is an error
            em.close();
        }
    }
}
