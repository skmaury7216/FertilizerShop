package com.fertilizershop.repo;
import com.fertilizershop.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SaleRepository extends JpaRepository<Sale,Long>{List<Sale> findAllByOrderByCreatedAtDesc(); List<Sale> findByCustomerIdOrderByCreatedAtDesc(Long customerId);}
