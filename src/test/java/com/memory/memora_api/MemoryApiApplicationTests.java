package com.memory.memora_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(properties = {
		"gemini.api.key=dummy-test-key",
		"spring.data.mongodb.uri=mongodb://localhost:27017/test_db"
})
class MemoryApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
