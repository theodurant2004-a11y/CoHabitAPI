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

    // ADD A TASK : saves the task and the roomies assigned to it (Task + Task_Roomie)
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

    // TASKS LIST : finds all the tasks of a roomshare, with their roomies
    public List<Task> findByRoomshare(RoomShare roomShare){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery(
                    "select distinct t from Task t " +
                            "left join fetch t.roomies " +
                            "where t.roomshare.id = :roomshareId " +
                            "order by t.dueDate asc nulls last, t.idTask asc", Task.class)
                    .setParameter("roomshareId", roomShare.getId())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // UPDATE / DELETE : finds one task with its roomies (null if it does not exist)
    public Task findById(Integer id){
        if(id == null){
            return null;
        }

        EntityManager em = JpaUtil.createEntityManager();

        try {
            List<Task> results = em.createQuery(
                    "select t from Task t " +
                            "left join fetch t.roomies " +
                            "where t.idTask = :id", Task.class)
                    .setParameter("id", id)
                    .getResultList();
            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    // UPDATE A TASK : saves the changes made on an existing task (and its roomies)
    public void update(Task task){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                em.merge(task);

                transaction.commit();
            } catch (RuntimeException e){
                if(transaction.isActive()){
                    transaction.rollback();
                }
                throw e;
            }
        } finally {
            em.close();
        }
    }

    // DELETE A TASK : deletes a task (and its lines in Task_Roomie)
    public void delete(Task task){
        EntityManager em = JpaUtil.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // The same task, but managed by this EntityManager
                Task managed = em.find(Task.class, task.getIdTask());

                // null: already deleted meanwhile (by another roomie), nothing left to do
                if (managed != null) {
                    // Hibernate first deletes the lines of Task_Roomie, then the task
                    // The roomies themselves are never deleted (no cascade to them)
                    em.remove(managed);
                }

                // The real DELETEs are sent here
                transaction.commit();
            } catch (RuntimeException e){
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