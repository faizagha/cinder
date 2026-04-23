package dev.cinder.note;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NoteServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoteServiceApplication.class, args);
	}

}
