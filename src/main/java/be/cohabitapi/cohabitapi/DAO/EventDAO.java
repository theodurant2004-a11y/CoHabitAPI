package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.Event;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

// Saves the events in the db
// The events are read through the list of their roomshare
// @ApplicationScoped : one single instance for the whole application, injected with @Inject in the API
@ApplicationScoped
public class EventDAO extends DAO<Event> {

    // ADD AN EVENT : saves the event in the table Event
    @Override
    public void create(Event event) {
        // One em per operation, closed in the finally
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Prepare the insert
                em.persist(event);

                // The real insertion
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    // If an error happens we rollback so nothing is saved
                    transaction.rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }
}