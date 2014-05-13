package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.jira.project.ProjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * PasswordReEncryptor decrypts the password using old key (projectId + repositoryUrl) 
 * and reencrypts using new key (organisation name + host url) 
 */
class PasswordReEncryptor
{
    private static final Logger log = LoggerFactory.getLogger(PasswordReEncryptor.class);
    private final ProjectManager projectManager;

    public PasswordReEncryptor(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    String reEncryptPassword(String password, String projectKey, String repositoryUrl, String organizationName, String hostUrl)
    {
        String unencrypted = decrypt(password, projectKey, repositoryUrl);
        String newHash = encrypt(unencrypted, organizationName, hostUrl);
        log.debug("repoUrl: " + repositoryUrl + " [" + password + "->" + newHash + "]");
        return newHash;
    }

    /**
     * @param password
     * @param projectKey
     * @param repoURL
     * @return
     */
    private String decrypt(String password, String projectKey, String repoURL)
    {
        if (password == null) 
            return null; 
                
        try
        {
            byte[] ciphertext = hexStringToByteArray(password);
            String projectID = projectManager.getProjectObjByKey(projectKey).getId().toString();

            // Get the Key
            byte[] key = (projectID + repoURL).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] original = cipher.doFinal(ciphertext);
            String originalString = new String(original);
            return originalString;

        }
        catch (Exception e)
        {
            log.debug("error encrypting",e);
        }
        return "";
    }


    /**
     * Encrypt the input into a hex encoded string;
     * @param input the input to encrypt
     * @param organizationName the project key
     * @param hostUrl the repository url
     * @return the encrypted string
     */
    private String encrypt(String input, String organizationName, String hostUrl)
    {
        byte[] encrypted;
        try
        {
            byte[] key = (organizationName + hostUrl).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            encrypted = cipher.doFinal((input).getBytes());
        }
        catch (Exception e)
        {
            log.debug("error encrypting",e);
            encrypted = new byte[0];
        }

        BigInteger bi = new BigInteger(1, encrypted);
        return String.format("%0" + (encrypted.length << 1) + "X", bi);
    }
    
    private static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
}
