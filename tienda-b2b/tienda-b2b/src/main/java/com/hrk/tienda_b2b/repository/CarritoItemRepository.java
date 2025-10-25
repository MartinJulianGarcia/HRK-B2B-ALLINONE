package com.hrk.tienda_b2b.repository;



import com.hrk.tienda_b2b.model.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> { }