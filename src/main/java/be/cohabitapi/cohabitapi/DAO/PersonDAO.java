package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.EXCEPTION.UserNotFoundException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.MembershipStatus;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import be.cohabitapi.cohabitapi.Models.Person;
import jakarta.persistence.NoResultException;

import java.util.List;

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

    // Doit on le mettre dans un roomie dao ?
    public Roomie findById(int id){

        try (EntityManager em = JpaUtil.createEntityManager()) {
            Roomie r = em.find(Roomie.class, id);

            if (r == null) throw new UserNotFoundException();

            return r;
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
}
