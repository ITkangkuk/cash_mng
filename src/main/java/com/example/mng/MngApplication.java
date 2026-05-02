package com.example.mng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class MngApplication {

	public static void main(String[] args) {
		SpringApplication.run(MngApplication.class, args);

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

//System.out.println("==1== " +encoder.encode("kk0618"));
//System.out.println("==2== " +encoder.encode("yj0917"));
	}

}
