package com.fertilizershop.repo;
import com.fertilizershop.model.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StockPurchaseRepository extends JpaRepository<StockPurchase,Long>{List<StockPurchase> findAllByOrderByPurchasedAtDesc();}
