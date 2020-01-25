package com.fahlberg.repository;

import com.fahlberg.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AthleteRepository extends JpaRepository<Athlete, Integer> {
}
