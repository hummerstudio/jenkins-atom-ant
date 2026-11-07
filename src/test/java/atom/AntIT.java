package atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jenkinschina.atom.sdk.testing.Atom;
import com.jenkinschina.atom.sdk.testing.AtomResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The ant atom, started the way the runner starts it, against a fake {@code ant} on PATH: no Jenkins, no toolchain.
 *
 * <p>These are the checks {@code tools/smoke.sh} made — the targets and options that reach the tool, the workspace it
 * runs in, a non-zero exit both failing and being tolerated, and the PATH fallback for a home that is not a
 * distribution.
 */
class AntIT {

    private static final String FAKE_ANT = """
            #!/bin/sh
            echo "fake ant called with: $*"
            echo "workspace=$PWD"
            case "$*" in
              *explode*) exit 7 ;;
            esac
            exit 0
            """;

    private Atom atom;

    @BeforeEach
    void setUp() throws Exception {
        atom = Atom.of();
    }

    @Test
    @DisplayName("targets and options reach the tool, the workspace is the job workspace, exit 0 is success")
    void passesArgumentsAndWorkspace() throws Exception {
        AtomResult run = atom.env("ANT_HOME", "")
                .input("goals", "all -Dbuild=${BUILD_NUMBER}")
                .input("options", "-Pci")
                .input("home", "")
                .input("failOnError", "true")
                .input("BUILD_NUMBER", "7")
                .bin("ant", FAKE_ANT)
                .run();

        assertEquals(0, run.exit(), run.console());
        assertTrue(run.console().contains("fake ant called with: all -Dbuild=7 -Pci"),
                "arguments did not reach the tool:\n" + run.console());
        assertTrue(run.console().contains("workspace=" + run.work().toRealPath()),
                "tool did not run in the workspace:\n" + run.console());
        assertEquals("success", run.status(), run.toString());
        assertEquals("0", run.field("exitCode"), "exit code missing:\n" + run.toString());
    }

    @Test
    @DisplayName("a non-zero exit fails the atom and is reported with the code")
    void failsOnNonZeroExit() throws Exception {
        AtomResult run = atom.env("ANT_HOME", "")
                .input("goals", "explode")
                .input("options", "")
                .input("home", "")
                .input("failOnError", "true")
                .bin("ant", FAKE_ANT)
                .run();

        assertNotEquals(0, run.exit(), "exit 7 must fail the atom:\n" + run.console());
        assertEquals("failure", run.status(), run.toString());
        assertTrue(run.field("message").contains("ant exited with 7"),
                "exit code is not in the message:\n" + run.toString());
        assertEquals("7", run.field("exitCode"), run.toString());
    }

    @Test
    @DisplayName("failOnError=false turns a non-zero exit into a success with the code still reported")
    void toleratesNonZeroExitWhenAsked() throws Exception {
        AtomResult run = atom.env("ANT_HOME", "")
                .input("goals", "explode")
                .input("options", "")
                .input("home", "")
                .input("failOnError", "false")
                .bin("ant", FAKE_ANT)
                .run();

        run.assertOk();
        assertEquals("7", run.field("exitCode"), "exit code lost:\n" + run.toString());
    }

    @Test
    @DisplayName("an unknown home falls back to PATH instead of crashing")
    void fallsBackToPathForAnUnknownHome() throws Exception {
        AtomResult run = atom.input("goals", "all")
                .input("options", "")
                .input("home", "/definitely/not/here")
                .input("failOnError", "true")
                .bin("ant", FAKE_ANT)
                .run();

        run.assertOk();
        assertTrue(run.console().contains("fake ant called"),
                "fallback to PATH did not happen:\n" + run.console());
    }
}
