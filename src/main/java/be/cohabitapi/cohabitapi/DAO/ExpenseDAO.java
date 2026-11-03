package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.Models.Expense;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

// This class is the only one that talks to the db for the expenses
@ApplicationScoped
public class ExpenseDAO extends DAO<Expense> {

    // Required by the mother class DAO: saves one expense
    @Override
    public void create(Expense expense) {
        createAll(List.of(expense));
    }

    // This method saves several expenses in one transaction, an expense shared by 3 roomies = 2 rows
    // We do this because if one row fails, nothing must be saved
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
                // ifsomething went wrong we cancel everything that was done in the transaction
                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        } finally {
            // We always close the EntityManager, even if there is an error
            em.close();
        }
    }

    // This method gets all the debts of the roomies of a roomshare, the newest first
    // A debt belongs to a roomshare through its debtor (debtor.roomshare)
    public List<Expense> findByRoomshare(Integer roomshareId) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT x FROM Expense x WHERE x.debtor.roomshare.id = :id "
                                    + "ORDER BY x.creationDate DESC, x.id DESC", Expense.class)
                    .setParameter("id", roomshareId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}