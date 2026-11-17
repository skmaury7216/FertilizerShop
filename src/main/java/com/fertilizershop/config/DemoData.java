package com.fertilizershop.config;
import com.fertilizershop.model.*;
import com.fertilizershop.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
@Configuration
public class DemoData {
 @Bean CommandLineRunner demo(ProductRepository p,CustomerRepository c,SupplierRepository s,@Value("${shop.demo.data:false}") boolean demo){return args->{if(!demo)return;
   if(p.count()==0){seed(p,"Urea 45 KG","IFFCO","Fertilizer","Bag",265,310,75);seed(p,"DAP 50 KG","IFFCO","Fertilizer","Bag",1350,1450,35);seed(p,"NPK 12:32:16","Coromandel","Fertilizer","Bag",1250,1380,22);seed(p,"Zinc Sulphate 10 KG","Local","Micronutrient","Bag",540,650,18);seed(p,"Potash 50 KG","MOP","Fertilizer","Bag",1600,1770,8);}
   if(c.count()==0){Customer a=new Customer();a.name="Ram Kumar";a.phone="9999999999";a.village="Sample Village";c.save(a);Customer b=new Customer();b.name="Seema Devi";b.village="New Village";c.save(b);}
   if(s.count()==0){Supplier a=new Supplier();a.name="Demo Fertilizer Distributor";a.phone="0000000000";s.save(a);}
 };}
 private void seed(ProductRepository r,String name,String brand,String cat,String unit,int buy,int sell,int stock){Product p=new Product();p.name=name;p.brand=brand;p.category=cat;p.unit=unit;p.purchasePrice=BigDecimal.valueOf(buy);p.sellingPrice=BigDecimal.valueOf(sell);p.stock=stock;p.reorderLevel=12;p.description="Demo catalog entry — verify the actual price";r.save(p);}
}
