package com.campaignworkbench.adobecampaignapi;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;

public final class KeystorePasswordProvider {

    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;

    // App-specific salt (keep constant, but not trivial)
    private static final byte[] SALT = "campaign-workbench-salt-v1".getBytes(StandardCharsets.UTF_8);

    public static char[] getPassword() {
        try {
            String seed = buildSeed();
            byte[] derived = deriveKey(seed, SALT);
            return toCharArray(derived);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive keystore password", e);
        }
    }

    private static String buildSeed() {
        String username = System.getProperty("user.name", "unknown");

        String machineId = getMachineId();

        // Normalise to avoid OS inconsistencies
        return (username + "|" + machineId)
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static byte[] deriveKey(String seed, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);

        KeySpec spec = new PBEKeySpec(
                seed.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        return factory.generateSecret(spec).getEncoded();
    }

    private static char[] toCharArray(byte[] bytes) {
        // Convert to hex (stable, printable, safe for KeyStore password)
        char[] hexChars = new char[bytes.length * 2];
        final char[] hexArray = "0123456789abcdef".toCharArray();

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }

        return hexChars;
    }

    private static String getMachineId() {
        try {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

            if (os.contains("win")) {
                return getWindowsMachineId();
            } else if (os.contains("mac")) {
                return getMacMachineId();
            } else {
                return getLinuxMachineId();
            }
        } catch (Exception e) {
            // Fallback (still deterministic-ish)
            return System.getProperty("user.home", "fallback");
        }
    }

    private static String getLinuxMachineId() throws IOException {
        Path path = Path.of("/etc/machine-id");
        if (Files.exists(path)) {
            return Files.readString(path).trim();
        }
        return System.getProperty("user.home");
    }

    private static String getMacMachineId() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
                .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        for (String line : output.split("\n")) {
            if (line.contains("IOPlatformUUID")) {
                return line.split("\"")[3];
            }
        }

        return System.getProperty("user.home");
    }

    private static String getWindowsMachineId() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("reg", "query",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid")
                .start();

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();

        for (String line : output.split("\n")) {
            if (line.contains("MachineGuid")) {
                return line.trim().split("\\s+")[2];
            }
        }

        return System.getProperty("user.home");
    }
}