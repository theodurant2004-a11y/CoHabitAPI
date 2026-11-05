package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.Models.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.*;

@ApplicationScoped
public class PersonDAO extends DAO<Person>{

    @Override
    public void create(Person person){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                em.persist(person);

                transaction.commit();
            } catch (RuntimeException e) {
                if(transaction.isActive()){
                    transaction.rollback();
                }

                // If the insert failed it's maybe because two persons were trying to sign up at the same time
                if(existsByEmail(person.getEmail())){
                    throw new EmailAlreadyUsedException();
                }

                throw e;
            }
        } finally {
            em.close();
        }
    }

    public boolean existsByEmail(String email){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            Long count = em.createQuery( "SELECT COUNT(p) FROM Person p WHERE p.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();

            return count > 0;
        } finally {
            em.close();
        }
    }

    //LOGIN: Find existing email and take information
    public Person findByEmail(String email){
        // An email that is null can't be in the db
        if (email == null)
            return null;

        EntityManager em = JpaUtil.createEntityManager();

        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.email = :email", Person.class)
                    .setParameter("email", email)
                    .getResultList();

            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    // Saves the changes made on an existing person (for example a roomie who asks to join a roomshare)
    // update() is for a person that is already in the database.
    public void update(Person person) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // merge because the person was loaded by another EntityManager (detached)
                em.merge(person);

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

    // TASKS

    // ADD / UPDATE A TASK : finds the roomies whose id is in the collection (one query for all of them)
    // "Roomie" is the class: an owner id is never returned
    public List<Roomie> findRoomiesByIds(Collection<Integer> ids){
        //Oracle refuses a IN() with a empty list
        if(ids == null || ids.isEmpty()){
            return new ArrayList<>();
        }

        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery("select r from Roomie r where r.idPerson IN :ids", Roomie.class)
                    .setParameter("ids", ids)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // TASKS LIST : finds the accepted roomies of a roomshare (the ones a task can be assigned to)
    // We pass the enum value: Hibernate uses the DbConverter to turn ACCEPTED into 3
    // Sorted by name so the front can display them directly
    public List<Roomie> findAcceptedRoomies(RoomShare roomShare){
        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery(
                            "select r from Roomie r " +
                                    "where r.roomshare.id = :roomshareId and r.statusMember = :status " +
                                    "order by r.firstName, r.lastName", Roomie.class)
                    .setParameter("roomshareId", roomShare.getId())
                    .setParameter("status", MembershipStatus.ACCEPTED)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
