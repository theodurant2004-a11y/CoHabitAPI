package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import be.cohabitapi.cohabitapi.Models.Person;
import java.util.List;
import java.util.Locale;

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
        EntityManager em = JpaUtil.createEntityManager();

        try {
            List<Person> results = em.createQuery(
                            "SELECT p FROM Person p WHERE p.email = :email", Person.class)
                    .setParameter("email", email)
                    // i use getResultList beceause it's  easier if a mistake is made
                    .getResultList();

            return results.isEmpty() ? null : results.get(0);
        } finally {
            em.close();
        }
    }

    // Dao chooses the role of the suser when he signs up
    public Person chooseRole(String role, String lastName, String firstname, String email, String password){
        String cleanRole = role == null ? "" : role.trim().toLowerCase(Locale.ROOT);

        if("owner".equals(cleanRole)){
            return new Owner(lastName, firstname, email, password);
        }
        if("roomie".equals(cleanRole)){
            return new Roomie(lastName, firstname, email, password);
        }
        throw new IllegalArgumentException("Invalid role.");

    }
}
