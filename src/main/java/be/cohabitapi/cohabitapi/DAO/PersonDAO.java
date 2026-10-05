package be.cohabitapi.cohabitapi.DAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import be.cohabitapi.cohabitapi.Models.Person;

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
}
