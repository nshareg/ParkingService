package configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/*
    Created by anshanyan
    on 22.06.26
*/
@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@Getter
@Setter
public class DBproperties {
    private String url;
    private String password;
    private String username;
}
