package com.fertilizershop.repo;
import com.fertilizershop.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CustomerRepository extends JpaRepository<Customer,Long>{List<Customer> findAllByOrderByNameAsc();}
