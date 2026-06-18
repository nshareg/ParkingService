package configs;

import com.parkingsystem.contract.ParkingRepository;
import com.parkingsystem.contract.ParkingService;
import com.parkingsystem.contract.ParkingSessionRepository;
import com.parkingsystem.impl.ParkingRepositoryimpl;
import com.parkingsystem.impl.ParkingServiceImpl;
import com.parkingsystem.impl.ParkingSessionRepositoryimpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/*
    Created by anshanyan
    on 18.06.26
*/
@Configuration
public class ParkingServiceConfiguration {
    @Bean
    public ParkingRepository parkingRepositoryFactory(DataSource dataSource){
        return new ParkingRepositoryimpl(dataSource);
    }

    @Bean
    public ParkingSessionRepository parkingSessionRepository(DataSource dataSource){
        return new ParkingSessionRepositoryimpl(dataSource);
    }

    @Bean
    public ParkingService parkingService(ParkingRepository parkingRepository, ParkingSessionRepository parkingSessionRepository){
        return new ParkingServiceImpl(parkingRepository, parkingSessionRepository);
    }
}
