package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.Expense;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

@ApplicationScoped
public class ExpenseDAO extends DAO<Expense> {

    @Override
    public void create(Expense expense) {
        createAll(List.of(expense));
    }

    // Saves several expenses in one transaction: if one row fails, nothing is saved
    public void createAll(List<Expense> expenses) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                for (Expense expense : expenses) {
                    em.persist(expense);
                }

                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        } finally {
            em.close();
        }
    }
}