package com.gabrielflorencco.hotelimpact;

import com.gabrielflorencco.hotelimpact.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.seed-demo=true")
class HotelimpactApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextoSobeComBancoEConstraints() {
    }
}
