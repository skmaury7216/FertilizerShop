package com.fertilizershop;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
public class FertilizerShopApplication {
 public static void main(String[] args) {
  try {java.nio.file.Files.createDirectories(java.nio.file.Path.of("data"));}
  catch(java.io.IOException e){throw new IllegalStateException("Cannot create database directory",e);}
  SpringApplication.run(FertilizerShopApplication.class,args);
 }
}
