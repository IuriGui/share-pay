package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.jwt.secret=dGVzdC1zZWNyZXQtd2l0aC1hdC1sZWFzdC0yNTYtYml0cyE=")
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
