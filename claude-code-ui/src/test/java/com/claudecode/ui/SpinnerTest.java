package com.claudecode.ui;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Spinner progress indicator.
 */
class SpinnerTest {

    @Test
    void spinnerStartAndStop() throws InterruptedException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw, "Loading...");

        assertFalse(spinner.isRunning());

        spinner.start();
        assertTrue(spinner.isRunning());

        // Let it animate a few frames
        Thread.sleep(200);

        spinner.stop();
        assertFalse(spinner.isRunning());

        String output = sw.toString();
        assertTrue(output.contains("Loading..."), "Should contain the status message");
    }

    @Test
    void spinnerStopWithMessage() throws InterruptedException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw);

        spinner.start("Working...");
        Thread.sleep(100);
        spinner.stop("Done!");

        assertFalse(spinner.isRunning());
        String output = sw.toString();
        assertTrue(output.contains("Done!"), "Should contain final message");
    }

    @Test
    void spinnerUpdateMessage() throws InterruptedException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw);

        spinner.start("Step 1");
        Thread.sleep(100);
        spinner.setMessage("Step 2");
        Thread.sleep(100);
        spinner.stop();

        String output = sw.toString();
        assertTrue(output.contains("Step 1") || output.contains("Step 2"),
                "Should contain at least one of the messages");
    }

    @Test
    void spinnerDoubleStartIsIdempotent() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw);

        spinner.start("test");
        spinner.start("test2"); // Should not create a second thread
        assertTrue(spinner.isRunning());

        spinner.stop();
        assertFalse(spinner.isRunning());
    }

    @Test
    void spinnerDoubleStopIsIdempotent() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw);

        spinner.start("test");
        spinner.stop();
        spinner.stop(); // Should not throw
        assertFalse(spinner.isRunning());
    }

    @Test
    void spinnerStopWithoutStartIsNoOp() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Spinner spinner = new Spinner(pw);

        spinner.stop(); // Should not throw
        assertFalse(spinner.isRunning());
    }
}
