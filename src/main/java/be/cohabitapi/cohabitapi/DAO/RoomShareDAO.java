package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.RoomShare;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

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
}
