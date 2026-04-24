package com.forex.order.order.repository;

import com.forex.order.order.entity.ForexOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForexOrderRepository extends JpaRepository<ForexOrder, Long> {

    List<ForexOrder> findAllByOrderByDateTimeDesc();
}
