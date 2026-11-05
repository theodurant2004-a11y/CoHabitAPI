package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.ProductNotFoundException;
import be.cohabitapi.cohabitapi.Models.Product;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

@ApplicationScoped
public class ProductDAO extends DAO<Product> {

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

    public Product findById(int id) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            Product p = em.find(Product.class, id);
            if (p == null) throw new ProductNotFoundException();
            return p;
        }
    }

    public Product findByNameAndCategory(String name, String category) {
        try (EntityManager em = JpaUtil.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Product p WHERE p.name = :name and p.category = :category", Product.class)
                    .setParameter("name", name)
                    .setParameter("category", category)
                    .getSingleResult();
        } catch (NoResultException e) {
            // getSingleResult throws an exception if no product has been found
            return null;
        }
    }

}
