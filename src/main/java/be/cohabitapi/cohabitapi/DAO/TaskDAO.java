package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Task;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

// Saves and reads the tasks in the database (only persistence, no business rule here)
@ApplicationScoped
public class TaskDAO extends DAO<Task> {

    // saves the task and the roomies assigned to it to add a task
    @Override
    public void create(Task task) {
        EntityManager em = JpaUtil.createEntityManager();

        try{
            EntityTransaction transaction = em.getTransaction();
            try{
                transaction.begin();

                // Prepare the insert
                em.persist(task);

                // the real insertion
                transaction.commit();
            } catch (RuntimeException e){
                if(transaction.isActive()){
                    //if an error happens we rollback
                    transaction.rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }

    // Find all the tasks of a roomshare
    public List<Task> findByRoomshare(RoomShare roomShare){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery(
                    "select distinct t from Task t " +
                            "left join fetch t.roomies " +
                            "where t.roomshare.id = :roomshareId " +
                            "order by t.dueDate asc nulls last, t.id asc", Task.class)
                    .setParameter("roomshareId", roomShare.getId())
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
