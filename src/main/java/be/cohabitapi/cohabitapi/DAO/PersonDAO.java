package be.cohabitapi.cohabitapi.DAO;

import be.cohabitapi.cohabitapi.EXCEPTION.EmailAlreadyUsedException;
import be.cohabitapi.cohabitapi.Models.Owner;
import be.cohabitapi.cohabitapi.Models.MembershipStatus;
import be.cohabitapi.cohabitapi.Models.RoomShare;
import be.cohabitapi.cohabitapi.Models.Roomie;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import be.cohabitapi.cohabitapi.Models.Person;
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

    //--------------- ACCEPT A ROOMIE -------------------

    // Finds a person with his id (null if nobody has this id)
    // Used when the owner answers a request: the front only sends the id of the roomie
    public Person findById(Integer id) {
        // One EntityManager per call, like the other methods
        EntityManager em = JpaUtil.createEntityManager();

        try {
            // find() searches with the primary key, no need to write a query
            // We ask a Person, but with the JOINED inheritance JPA gives back the real subclass (Owner or Roomie)
            // The roomshare of the roomie and the owner of the roomshare are @ManyToOne,
            // so they are loaded at the same time (EAGER): we can use them after em.close()
            return em.find(Person.class, id);
        } finally {
            // We always need to close the EntityManager, even if there is an error
            em.close();
        }
    }

    // Finds all the roomies waiting for an answer in the roomshare of this owner
    // Used to show the "Waiting for approval" list of the Members page
    public List<Roomie> findPendingRoomiesByOwner(Integer ownerId) {
        // One EntityManager per call
        EntityManager em = JpaUtil.createEntityManager();

        try {
            // Here we work on the Java classes and not the tables:
            // r.roomshare.owner.idPerson follows the links between the objects, JPA writes the SQL joins for us
            // :ownerId and :status are parameters, so there is no sql injection
            return em.createQuery(
                            "SELECT r FROM Roomie r WHERE r.roomshare.owner.idPerson = :ownerId AND r.statusMember = :status",
                            Roomie.class)
                    .setParameter("ownerId", ownerId)
                    // We give the enum directly: the converter of MembershipStatus turns PENDING into 2 for the db
                    .setParameter("status", MembershipStatus.PENDING)
                    // getResultList: an empty list if there is no request (no exception)
                    .getResultList();
        } finally {
            em.close();
        }
    }


    // This method is used to get the roomies that are in a roomshare
    // They are always in the same order (by id), so the colors of the roomies never change
    // We use a query (and not roomshare.getRoomies()) because that list is lazy so it cannot be read here
    public List<Roomie> findAcceptedRoomies(RoomShare roomshare) {
        EntityManager em = JpaUtil.createEntityManager();

        try {
            return em.createQuery(
                            "SELECT r FROM Roomie r WHERE r.roomshare.id = :id AND r.statusMember = :status "
                                    + "ORDER BY r.idPerson", Roomie.class)
                    .setParameter("id", roomshare.getId())
                    .setParameter("status", MembershipStatus.ACCEPTED)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    //--------------- MEMBERS OF A ROOMSHARE -------------------

    // Finds all the roomies who are real members (ACCEPTED) of the roomshare of this owner
    // Same query as findPendingRoomiesByOwner, only the status changes
    public List<Roomie> findAcceptedRoomiesByOwner(Integer ownerId) {
        // One EntityManager per call
        EntityManager em = JpaUtil.createEntityManager();

        try {
            // r.roomshare.owner.idPerson follows the links between the objects, JPA writes the SQL joins for us
            // ORDER BY: the roomies are always shown in the same order (by first name)
            return em.createQuery(
                            "SELECT r FROM Roomie r WHERE r.roomshare.owner.idPerson = :ownerId AND r.statusMember = :status ORDER BY r.firstName",
                            Roomie.class)
                    .setParameter("ownerId", ownerId)
                    // The converter of MembershipStatus turns ACCEPTED into 3 for the db
                    .setParameter("status", MembershipStatus.ACCEPTED)
                    // getResultList: an empty list if there is no roomie yet (no exception)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}