package com.fahlberg.demo.repository;

import com.fahlberg.demo.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteRepository extends JpaRepository<Athlete, Integer> {
}
