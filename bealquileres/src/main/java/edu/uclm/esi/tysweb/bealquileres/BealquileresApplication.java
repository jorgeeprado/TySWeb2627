package edu.uclm.esi.tysweb.bealquileres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BealquileresApplication {

	public static void main(String[] args) {
		/*Console console = System.console();
		if (console == null) 
            throw new IllegalStateException("La aplicación debe ejecutarse desde una consola");

        String username = console.readLine("Usuario MySQL: ");
        char[] password = console.readPassword("Password MySQL: ");

        System.setProperty("spring.datasource.username", username);
        System.setProperty("spring.datasource.password", new String(password));*/

		SpringApplication.run(BealquileresApplication.class, args);
	}

}
