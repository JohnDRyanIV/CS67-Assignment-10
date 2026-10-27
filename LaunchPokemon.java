import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Starts Pokemon Explorer on Windows, macOS, or Linux using an installed JDK 17+.
 * This launcher itself works with Java 8+, so older default Java associations
 * can still find and launch a newer JDK installed elsewhere on the computer.
 */
public class LaunchPokemon {
    private static final String MAVEN_VERSION = "3.9.11";
    private static final String MAVEN_FOLDER = "apache-maven-" + MAVEN_VERSION;
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).startsWith("windows");
    private static final String JAVA = WINDOWS ? "java.exe" : "java";
    private static final String JAVAC = WINDOWS ? "javac.exe" : "javac";

    /**
     * This function detects the environment, prepares Maven, and starts the GUI
     * @param args - optional --check to inspect setup or --build-only to compile
     */
    public static void main(String[] args) {
        Path logFile = null;
        try {
            boolean checkOnly = Arrays.asList(args).contains("--check");
            boolean buildOnly = Arrays.asList(args).contains("--build-only");
            for (String argument : args) {
                if (!argument.equals("--check") && !argument.equals("--build-only")) {
                    throw new IllegalArgumentException("Usage: java -jar LaunchPokemon.jar [--check | --build-only]");
                }
            }

            Path project = findProject();
            Path tools = project.resolve(".tools");
            Files.createDirectories(tools);
            logFile = tools.resolve("launcher.log");
            Files.write(logFile, new byte[0]);

            try (PrintStream log = new PrintStream(
                    Files.newOutputStream(logFile, StandardOpenOption.APPEND), true, "UTF-8")) {
                try {
                    report(log, "Pokemon Explorer launcher");
                    report(log, "System: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
                    report(log, "Project: " + project);
                    Path jdk = findJdk();
                    report(log, "JDK: " + jdk);
                    Path maven = findMaven(tools);
                    if (checkOnly) {
                        report(log, "Maven: " + (maven == null ? "will download on first launch" : maven));
                        report(log, "Environment check passed. A JDK 17+ is available.");
                        return;
                    }
                    if (maven == null) {
                        report(log, "Downloading Maven " + MAVEN_VERSION + " (first launch only)...");
                        maven = downloadMaven(tools);
                    }
                    report(log, "Maven: " + maven);
                    report(log, "Building Pokemon Explorer. First-run dependency downloads may take a few minutes.");
                    report(log, "Build output: " + logFile);

                    // Launch Maven with Java directly, avoiding OS-specific shell quoting
                    // and executable-bit requirements on downloaded scripts.
                    List<String> command = new ArrayList<String>();
                    command.add(jdk.resolve("bin").resolve(JAVA).toString());
                    command.add("-Dmaven.home=" + maven);
                    command.add("-Dmaven.multiModuleProjectDirectory=" + project);
                    command.add("-Dclassworlds.conf=" + maven.resolve("bin/m2.conf"));
                    command.add("-classpath");
                    command.add(findBootJar(maven).toString());
                    command.add("org.codehaus.plexus.classworlds.launcher.Launcher");
                    command.add("--batch-mode");
                    command.add("--no-transfer-progress");
                    command.add("-Dmaven.repo.local=" + tools.resolve("repository"));
                    command.add("compile");
                    if (!buildOnly) {
                        command.add("javafx:run");
                    }
                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.directory(project.toFile());
                    builder.environment().put("JAVA_HOME", jdk.toString());
                    String existingPath = builder.environment().get("PATH");
                    builder.environment().put("PATH", jdk.resolve("bin") + java.io.File.pathSeparator
                            + (existingPath == null ? "" : existingPath));
                    builder.redirectErrorStream(true);
                    builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
                    int exitCode = builder.start().waitFor();
                    if (exitCode != 0) {
                        throw new IOException("The build or application exited with code " + exitCode
                                + ". See the build output above for details.");
                    }
                    report(log, buildOnly ? "Build completed successfully." : "Pokemon Explorer closed normally.");
                } catch (Exception e) {
                    log.println("\nLAUNCH FAILED: " + e.getMessage());
                    e.printStackTrace(log);
                    throw e;
                }
            }
        } catch (Exception e) {
            System.err.println("Could not launch Pokemon Explorer: " + e.getMessage());
            try {
                if (logFile == null) {
                    logFile = Files.createTempFile("pokemon-launch-error-", ".log");
                    Files.write(logFile, Arrays.asList("Could not launch Pokemon Explorer.", e.toString()),
                            StandardCharsets.UTF_8);
                }
                System.err.println("Details: " + logFile);
                // Double-click launches may have no console; show the error log
                // in the system's text editor rather than failing silently.
                if (System.console() == null && !GraphicsEnvironment.isHeadless()
                        && Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(logFile.toFile());
                }
            } catch (Exception logError) {
                System.err.println("Could not open the error log: " + logError.getMessage());
            }
            System.exit(1);
        }
    }

    private static Path findProject() throws Exception {
        URL location = LaunchPokemon.class.getProtectionDomain().getCodeSource() == null
                ? null : LaunchPokemon.class.getProtectionDomain().getCodeSource().getLocation();
        if (location != null && location.getProtocol().equals("file")) {
            Path source = Paths.get(location.toURI()).toAbsolutePath();
            Path found = projectAbove(Files.isDirectory(source) ? source : source.getParent());
            if (found != null) {
                return found;
            }
        }
        Path found = projectAbove(Paths.get("").toAbsolutePath());
        if (found == null) {
            throw new IOException("Keep LaunchPokemon.jar beside pom.xml and the src folder in the project root.");
        }
        return found;
    }

    private static Path projectAbove(Path directory) {
        for (Path current = directory; current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isRegularFile(current.resolve("src/PokemonGUI.java"))) {
                return current.normalize();
            }
        }
        return null;
    }

    private static Path findJdk() throws IOException {
        Set<Path> candidates = new LinkedHashSet<Path>();
        addEnvironmentPath(candidates, "JAVA_HOME");
        candidates.add(Paths.get(System.getProperty("java.home")));
        candidates.add(Paths.get(System.getProperty("java.home")).getParent());
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                if (entry.isEmpty()) {
                    continue;
                }
                Path compiler = Paths.get(entry).resolve(JAVAC);
                if (Files.isRegularFile(compiler)) {
                    candidates.add(compiler.toRealPath().getParent().getParent());
                }
            }
        }

        Path home = Paths.get(System.getProperty("user.home"));
        addInstallations(candidates, home.resolve(".jdks"));
        addInstallations(candidates, home.resolve(".sdkman/candidates/java"));
        addInstallations(candidates, home.resolve("Library/Java/JavaVirtualMachines"));
        addVsCodeJdks(candidates, home.resolve(".vscode/extensions"));
        addVsCodeJdks(candidates, home.resolve(".vscode-insiders/extensions"));
        if (WINDOWS) {
            for (String variable : Arrays.asList("ProgramFiles", "ProgramFiles(x86)")) {
                String programFiles = System.getenv(variable);
                if (programFiles != null) {
                    Path programs = Paths.get(programFiles);
                    for (String folder : Arrays.asList("Java", "Eclipse Adoptium", "Microsoft", "JetBrains")) {
                        addInstallations(candidates, programs.resolve(folder));
                    }
                }
            }
        } else {
            for (String folder : Arrays.asList("/Library/Java/JavaVirtualMachines", "/usr/lib/jvm",
                    "/usr/java", "/opt/java", "/opt/homebrew/opt", "/usr/local/opt")) {
                addInstallations(candidates, Paths.get(folder));
            }
        }
        for (Path candidate : candidates) {
            if (isJdk(candidate)) {
                return candidate.toRealPath();
            }
        }
        throw new IOException("No JDK 17 or newer was found. Install a JDK 17+ or set JAVA_HOME to its folder, then try again.");
    }

    private static void addEnvironmentPath(Set<Path> candidates, String variable) {
        String value = System.getenv(variable);
        if (value != null && !value.trim().isEmpty()) {
            candidates.add(Paths.get(value));
        }
    }

    private static void addInstallations(Set<Path> candidates, Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path child : children) {
                if (Files.isDirectory(child)) {
                    candidates.add(child);
                    candidates.add(child.resolve("Contents/Home"));
                    candidates.add(child.resolve("jbr"));
                    candidates.add(child.resolve("libexec/openjdk.jdk/Contents/Home"));
                }
            }
        } catch (IOException ignored) {
            // An inaccessible optional installation directory should not stop detection.
        }
    }

    private static void addVsCodeJdks(Set<Path> candidates, Path extensions) {
        if (!Files.isDirectory(extensions)) {
            return;
        }
        try (DirectoryStream<Path> children = Files.newDirectoryStream(extensions, "redhat.java-*")) {
            for (Path extension : children) {
                addInstallations(candidates, extension.resolve("jre"));
            }
        } catch (IOException ignored) {
            // Continue with the other JDK locations.
        }
    }

    private static boolean isJdk(Path directory) {
        if (directory == null || !Files.isRegularFile(directory.resolve("bin").resolve(JAVA))
                || !Files.isRegularFile(directory.resolve("bin").resolve(JAVAC))) {
            return false;
        }
        try (InputStream input = Files.newInputStream(directory.resolve("release"))) {
            Properties release = new Properties();
            release.load(input);
            String version = release.getProperty("JAVA_VERSION", "0").replace("\"", "");
            return Integer.parseInt(version.split("[.\\-+]")[0]) >= 17;
        } catch (IOException | NumberFormatException e) {
            return false;
        }
    }

    private static Path findMaven(Path tools) throws IOException {
        List<Path> candidates = new ArrayList<Path>();
        candidates.add(tools.resolve(MAVEN_FOLDER));
        for (String variable : Arrays.asList("MAVEN_HOME", "M2_HOME")) {
            String value = System.getenv(variable);
            if (value != null && !value.trim().isEmpty()) {
                candidates.add(Paths.get(value));
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String entry : path.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                if (!entry.isEmpty()) {
                    Path script = Paths.get(entry).resolve(WINDOWS ? "mvn.cmd" : "mvn");
                    if (Files.isRegularFile(script)) {
                        candidates.add(script.toRealPath().getParent().getParent());
                    }
                }
            }
        }
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate.resolve("bin/m2.conf")) && findBootJar(candidate) != null) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static Path findBootJar(Path maven) throws IOException {
        Path boot = maven.resolve("boot");
        if (Files.isDirectory(boot)) {
            try (DirectoryStream<Path> jars = Files.newDirectoryStream(boot, "plexus-classworlds-*.jar")) {
                for (Path jar : jars) {
                    return jar;
                }
            }
        }
        return null;
    }

    private static Path downloadMaven(Path tools) throws Exception {
        String address = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/"
                + MAVEN_VERSION + "/" + MAVEN_FOLDER + "-bin.zip";
        Path archive = tools.resolve(MAVEN_FOLDER + "-bin.zip");
        try (InputStream download = openDownload(address)) {
            Files.copy(download, archive, StandardCopyOption.REPLACE_EXISTING);
        }
        String expected;
        try (InputStream checksum = openDownload(address + ".sha512")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = checksum.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            expected = new String(bytes.toByteArray(), StandardCharsets.UTF_8).trim().split("\\s+")[0];
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (InputStream input = Files.newInputStream(archive)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        StringBuilder actual = new StringBuilder();
        for (byte value : digest.digest()) {
            actual.append(String.format("%02x", value & 0xff));
        }
        if (!actual.toString().equalsIgnoreCase(expected)) {
            throw new IOException("Maven's download failed its SHA-512 check. Please try again.");
        }

        Path maven = tools.resolve(MAVEN_FOLDER).toAbsolutePath().normalize();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path destination = tools.resolve(entry.getName()).toAbsolutePath().normalize();
                if (!destination.startsWith(maven)) {
                    throw new IOException("The Maven archive contains an invalid file path.");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
        if (!Files.isRegularFile(maven.resolve("bin/m2.conf")) || findBootJar(maven) == null) {
            throw new IOException("Maven could not be extracted. Please try again.");
        }
        return maven;
    }

    private static InputStream openDownload(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "PokemonExplorerLauncher/1.0");
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("Could not download Maven. Check your internet connection and try again.");
        }
        return connection.getInputStream();
    }

    private static void report(PrintStream log, String message) {
        log.println(message);
        System.out.println(message);
    }
}
