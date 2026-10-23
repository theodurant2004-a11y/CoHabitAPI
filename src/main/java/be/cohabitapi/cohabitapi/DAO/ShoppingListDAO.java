package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.Item;
import be.cohabitapi.cohabitapi.Models.Person;
import be.cohabitapi.cohabitapi.Models.Product;
import be.cohabitapi.cohabitapi.Models.ShoppingList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import javax.swing.text.html.parser.Entity;
import java.util.List;

@ApplicationScoped
public class ShoppingListDAO extends DAO<ShoppingList> {

    @Override
    public void create(ShoppingList sl) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Here is like a INSERT in db but just "the preparation"
                em.persist(sl);

                // The real INSERT in db is here, with the commit confirmation
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive())
                    transaction.rollback();

                throw e;
            }
        }
    }

    public void create(Item i) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Here is like a INSERT in db but just "the preparation"
                em.persist(i);

                // The real INSERT in db is here, with the commit confirmation
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive())
                    transaction.rollback();

                throw e;
            }
        }
    }

    public void create(Product p) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // Here is like a INSERT in db but just "the preparation"
                em.persist(p);

                // The real INSERT in db is here, with the commit confirmation
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive())
                    transaction.rollback();

                throw e;
            }
        }
    }

    public Product findByNameAndCategory(String category, String name) {

        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Product p WHERE p.name = :name and p.category = :category", Product.class)
                    .setParameter("name", name)
                    .setParameter("category", category)
                    .getSingleResult();
        } catch (NoResultException e) {
            // getSingleResult throws an exception if no product has been found
            return new Product(name, category);
        }
    }
}
