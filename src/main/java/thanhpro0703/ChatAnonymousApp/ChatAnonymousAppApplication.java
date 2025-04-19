package thanhpro0703.ChatAnonymousApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChatAnonymousAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatAnonymousAppApplication.class, args);
	}

}
