package ai.enpasos.muzero.platform.agent.e_experience;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = "ai.enpasos.muzero.*")
public class TestApp implements CommandLineRunner {

    @Autowired
    private MuZeroConfig conf;

    public static void main(String[] args) {
        SpringApplication.run(TestApp.class, args);
    }

    @Override
    public void run(String... args) {
    }
}
