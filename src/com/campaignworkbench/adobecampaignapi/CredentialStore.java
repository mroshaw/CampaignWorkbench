package com.campaignworkbench.adobecampaignapi;

import com.campaignworkbench.ide.AppSettings;
import com.campaignworkbench.ide.CampaignWorkbenchIDE;
import com.campaignworkbench.workspace.Workspace;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Optional;

public class CredentialStore {

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_FILE = ".campaign-workbench-%s.p12";

    private final String instanceId;
    private final Path keystorePath;
    private final KeyStore keyStore;

    private char[] credentialPassword;

    public CredentialStore(String instanceId) {
        this.instanceId = instanceId;
        this.keystorePath = AppSettings.appSettingsPath.resolve(KEYSTORE_FILE.formatted(instanceId));
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise keystore", e);
        }
    }

    public void unlock(char[] credentialPassword) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        this.credentialPassword = credentialPassword;
        loadOrCreate();
    }

    public void lock() {
        if (credentialPassword != null) {
            Arrays.fill(credentialPassword, '\0');
            credentialPassword = null;
        }
    }

    private void checkUnlocked() {
        if (credentialPassword == null) {
            throw new ApiException("CredentialStore is locked", null);
        }
    }

    private void loadOrCreate() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException  {

        if (Files.exists(keystorePath)) {
            try (InputStream is = Files.newInputStream(keystorePath)) {
                keyStore.load(is, credentialPassword);
            }
        } else {
            keyStore.load(null, credentialPassword);
            persist();
        }
    }

    private void persist() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException  {
        try (OutputStream os = Files.newOutputStream(keystorePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            keyStore.store(os, credentialPassword);
        }
    }

    private String key(String field) {
        return instanceId + "_" + field;
    }

    private SecretKey toSecretKey(String value) {
        return new SecretKeySpec(value.getBytes(StandardCharsets.UTF_8), "AES");
    }

    private String fromSecretKey(SecretKey key) {
        return new String(key.getEncoded(), StandardCharsets.UTF_8);
    }

    private void set(String alias, String value) throws Exception {
        SecretKeyEntry entry = new SecretKeyEntry(toSecretKey(value));
        keyStore.setEntry(alias, entry, new PasswordProtection(credentialPassword));
        persist();
    }

    private Optional<String> get(String alias) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException {
        if (!keyStore.containsAlias(alias)) {
            return Optional.empty();
        }
        SecretKeyEntry entry = (SecretKeyEntry)
                keyStore.getEntry(alias, new PasswordProtection(credentialPassword));

        return Optional.ofNullable(entry)
                .map(SecretKeyEntry::getSecretKey)
                .map(this::fromSecretKey);
    }

    private void delete(String alias) throws IOException, KeyStoreException,  CertificateException, NoSuchAlgorithmException {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias);
            persist();
        }
    }

    // === Public API (unchanged) ===

    public void save(String clientId, String clientSecret) throws ApiException {
        checkUnlocked();
        try {
            set(key("client_id"), clientId);
            set(key("client_secret"), clientSecret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save credentials", e);
        }
    }

    public Optional<String> getClientId() throws ApiException {
        checkUnlocked();
        try {
            return get(key("client_id"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get clientId", e);
        }
    }

    public Optional<String> getClientSecret() throws ApiException {
        checkUnlocked();
        try {
            return get(key("client_secret"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get clientSecret", e);
        }
    }

    public void clear() throws ApiException{
        try {
            delete(key("client_id"));
            delete(key("client_secret"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear credentials", e);
        }
    }
}