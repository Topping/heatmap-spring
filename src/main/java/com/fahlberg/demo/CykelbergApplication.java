package com.fahlberg.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.fahlberg"})
public class CykelbergApplication {


	public static void main(String[] args) {
		SpringApplication.run(CykelbergApplication.class, args);
	}

}
