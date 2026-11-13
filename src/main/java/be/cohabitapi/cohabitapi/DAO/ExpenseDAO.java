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

    // Finds one expense with its id and returns null if it does not exist:
    public Expense findById(Integer id) {
        // An id that is null can't be in the db
        if (id == null) {
            return null;
        }

        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.find(Expense.class, id);
        } finally {
            em.close();
        }
    }

    // Saves the changes made on an existing expense so when it is settled
    public void update(Expense expense) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();

                // The expense was loaded by another em ( findById), so we use detached
                // merge() copies its state into the db row
                em.merge(expense);

                transaction.commit();
            } catch (RuntimeException e) {
                // If something fails we cancel everything so the db stays the same as before
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