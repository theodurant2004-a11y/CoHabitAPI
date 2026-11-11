package be.cohabitapi.cohabitapi.DAO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "CoHabitPU";
    private static EntityManagerFactory factory;

    private static EntityManagerFactory getEntityManagerFactory() {
        if (factory == null || !factory.isOpen()) {
            Map<String, String> props = new HashMap<>();
            props.put("jakarta.persistence.jdbc.url", System.getenv("db_url"));
            props.put("jakarta.persistence.jdbc.user", System.getenv("db_user"));
            props.put("jakarta.persistence.jdbc.password", System.getenv("db_mdp"));
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, props);
        }
        return factory;
    }

    public static EntityManager createEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static void close() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
    }
}
