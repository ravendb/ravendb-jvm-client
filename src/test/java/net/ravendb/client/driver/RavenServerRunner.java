package net.ravendb.client.driver;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public abstract class RavenServerRunner {

    public static Process run(RavenServerLocator locator) throws IOException {
        ProcessStartInfo processStartInfo = getProcessStartInfo(locator);

        List<String> arguments = new ArrayList<>();
        arguments.add(processStartInfo.getCommand());
        arguments.addAll(Arrays.asList(processStartInfo.getArguments()));

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        return processBuilder.start();
    }

    private static ProcessStartInfo getProcessStartInfo(RavenServerLocator locator) throws IOException {
        File serverPath = new File(locator.getServerPath());
        if (!serverPath.exists()) {
            throw new FileNotFoundException("Server file was not found: " + locator.getServerPath());
        }

        String[] commandArguments = new String[] {
                "--RunInMemory=true",
                "--License.Eula.Accepted=true",
                "--Setup.Mode=None",
                "--Testing.ParentProcessId=" + getProcessId("0")
        };


        commandArguments = ArrayUtils.addAll(locator.getCommandArguments(), commandArguments);

        ProcessStartInfo processStartInfo = new ProcessStartInfo();
        processStartInfo.setCommand(locator.getCommand());
        processStartInfo.setArguments(commandArguments);

        return processStartInfo;
    }

    private static String getProcessId(final String fallback) {
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            return fallback;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }

    private static class ProcessStartInfo {
        private String command;
        private String[] arguments;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String[] getArguments() {
            return arguments;
        }

        public void setArguments(String[] arguments) {
            this.arguments = arguments;
        }
    }
}
