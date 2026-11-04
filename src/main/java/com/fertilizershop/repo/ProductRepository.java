package com.fertilizershop.repo;
import com.fertilizershop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product,Long>{List<Product> findByActiveTrueOrderByNameAsc(); long countByActiveTrue();}
