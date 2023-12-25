package com.example.springdemo.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Manual test")
public class MapTests {
    private static final int MAP_SIZE = 3000;
    public static int iterations = 100000;

    private long totalDuration = 0;
    private int count = 0;

    public static LinkedHashSet<String> controlSet = new LinkedHashSet<>();

    @BeforeAll
    public static void initOnce() {
        for (int i = 0; i < MAP_SIZE; i++) {
            controlSet.add(RandomStringUtils.random(10, true, false));
        }
    }

    @BeforeEach
    public void setup() {
        totalDuration = 0;
        count = 0;
    }

    @Test
    public void testLinkedHashSetUsingStream() {

        long start = System.nanoTime();
        for (int reps = 0; reps < iterations; reps++) {
            LinkedHashSet<String> modSet = new LinkedHashSet<>(controlSet);
            for (int i = 0; i < 25; i++) {
                modSet.add(RandomStringUtils.random(10, true, false));
            }

            if (modSet.size() > controlSet.size()) {
                Set<String> newEntries = modSet.stream()
                        .skip(controlSet.size()) // the offset
                        .collect(Collectors.toSet());
                Assertions.assertThat(newEntries).hasSize(25);

            }
            count++;
        }
        totalDuration += (System.nanoTime() - start);

        System.out
                .println("stream skip duration = " + totalDuration / 1000000000 + "s avg = " + (totalDuration / count) / 1000);
    }

    @Test
    public void testLinkedHashSetUsingArrayCopyOfRange() {

        long start = System.nanoTime();
        for (int reps = 0; reps < iterations; reps++) {
            LinkedHashSet<String> modSet = new LinkedHashSet<>(controlSet);
            for (int i = 0; i < 25; i++) {
                modSet.add(RandomStringUtils.random(10, true, false));
            }

            if (modSet.size() > controlSet.size()) {
                String[] entries = modSet.toArray(new String[0]);
                String[] newList = Arrays.copyOfRange(entries, controlSet.size(), entries.length);
                Assertions.assertThat(newList).hasSize(25);
            }
            count++;
        }
        totalDuration += (System.nanoTime() - start);

        System.out
                .println("toArrayCopyOfRange duration = " + totalDuration / 1000000000 + "s avg = "
                        + (totalDuration / count) / 1000);
    }

    @Test
    public void testHashSetRemoveAll() {
        HashSet<String> controlHashSet = new HashSet<>(controlSet);

        long start = System.nanoTime();
        for (int reps = 0; reps < iterations; reps++) {
            HashSet<String> modSet = new HashSet<>(controlHashSet);
            for (int i = 0; i < 25; i++) {
                modSet.add(RandomStringUtils.random(10, true, false));
            }

            if (modSet.size() > controlHashSet.size()) {
                modSet.removeAll(controlHashSet);
                Assertions.assertThat(modSet).hasSize(25);
            }
            count++;
        }
        totalDuration += (System.nanoTime() - start);

        System.out
                .println("removeAll duration = " + totalDuration / 1000000000 + "s avg = " + (totalDuration / count) / 1000);
    }

    @Test
    public void testHashSetForEachAdd() {
        HashSet<String> controlHashSet = new HashSet<>(controlSet);

        long start = System.nanoTime();
        for (int reps = 0; reps < iterations; reps++) {
            HashSet<String> modSet = new HashSet<>(controlHashSet);
            for (int i = 0; i < 25; i++) {
                modSet.add(RandomStringUtils.random(10, true, false));
            }

            if (modSet.size() > controlSet.size()) {
                Set<String> newSet = new HashSet<>();
                for (String s : modSet) {
                    if (!controlSet.contains(s)) {
                        newSet.add(s);
                    }
                }
                Assertions.assertThat(newSet).hasSize(25);

            }
            count++;
        }
        totalDuration += (System.nanoTime() - start);

        System.out
                .println("foreach duration = " + totalDuration / 1000000000 + "s avg = " + (totalDuration / count) / 1000);
    }
}
