package com.example.springbatch5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

//@ExtendWith(MockitoExtension.class)
class SpringBatch5ApplicationTests {

	@Test
	void contextLoads() {

		final int count = Runtime.getRuntime().availableProcessors();
		System.out.println("count: " + count);
	}

}