package be.cohabitapi.cohabitapi.Models;

import static be.cohabitapi.cohabitapi.Utils.VerificationOperation.normalizeStringAttribute;

import be.cohabitapi.cohabitapi.DAO.ProductDAO;
import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    // ------------------------- Attributes and JPA Annotations --------------------------------------------------

    // @Id specifies a primary key
    @Id
    // @GeneratedValue specifies the auto-increment constraint
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Column specifies which column of the table is involved
    @Column(name = "product_id")
    private Integer id;

    @Column(name = "product_name", length = 100, nullable = false)
    private String name;

    @Column(name = "product_category", length = 100, nullable = false)
    private String category;


    // ------------------------- Getters and Setters --------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalizeStringAttribute(name, "name", 100);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = normalizeStringAttribute(category, "category", 100);
    }


    // ------------------------- Constructors --------------------------------------------------

    protected Product() {}

    public Product(String name, String category) {
        setName(name);
        setCategory(category);
    }


    // ------------------------- DAO methods --------------------------------------------------

    // Search a product and if it exists, it returns it but if not, it creates it
    public static Product findOrCreate(ProductDAO dao, String name, String category) {
        Product product = dao.findByNameAndCategory(name.trim(), category.trim());
        if (product == null) {
            product = new Product(name, category);
            dao.create(product);
        }
        return product;
    }


// ------------------------- Overloads --------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        return id != null && id.equals(product.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}