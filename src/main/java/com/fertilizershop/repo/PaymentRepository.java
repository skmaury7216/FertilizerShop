package com.fertilizershop.repo;
import com.fertilizershop.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long>{List<Payment> findBySaleIdOrderByPaidAtDesc(Long saleId); List<Payment> findAllByOrderByPaidAtDesc();}
