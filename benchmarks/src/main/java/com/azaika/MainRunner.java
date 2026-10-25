package com.azaika;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MainRunner {

    @Benchmark
    public void run() {
        Main.main(new String[]{});
    }

}
