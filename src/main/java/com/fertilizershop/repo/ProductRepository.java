package com.fertilizershop.repo;
import com.fertilizershop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProductRepository extends JpaRepository<Product,Long>{List<Product> findByActiveTrueOrderByNameAsc(); long countByActiveTrue(); Optional<Product> findByBarcode(String barcode);}
