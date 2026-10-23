package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.RoomShareNotFoundException;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class RoomShareDAO extends DAO<RoomShare> {
    // CREATE A ROOMSHARE : Create the room share by the EM persist method
    @Override
    public void create(RoomShare rs) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Here is like a INSERT in db but just "the preparation"
                em.persist(rs);

                // The real INSERT in db is here, with the commit confirmation
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
            return em.createQuery(
                            "SELECT r FROM RoomShare r WHERE r.id = :id", RoomShare.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new RoomShareNotFoundException();
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
}
