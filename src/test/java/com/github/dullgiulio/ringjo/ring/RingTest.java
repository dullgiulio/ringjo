package com.github.dullgiulio.ringjo.ring;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		RingWriterTest.class,
		RingReaderTest.class,
		ExecutorTest.class
})
public class RingTest {
}