package org.carlspring.commons.io;

import org.carlspring.commons.encryption.EncryptionAlgorithmsEnum;
import org.carlspring.commons.util.MessageDigestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mtodorov
 */
class MultipleDigestOutputStreamTest
{

    private static final Logger logger = LoggerFactory.getLogger(MultipleDigestOutputStreamTest.class);

    @BeforeEach
    void setUp()
            throws Exception
    {
        Path dir = Paths.get("target/test-resources").toAbsolutePath();
        if (Files.notExists(dir))
        {
            Files.createDirectories(dir);
        }
    }

    @Test
    void testWrite()
            throws IOException,
                   NoSuchAlgorithmException
    {
        String s = "This is a test.";

        Path filePath = Paths.get("target/test-resources/metadata.xml").toAbsolutePath();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MultipleDigestOutputStream mdos = new MultipleDigestOutputStream(filePath, baos);

        mdos.write(s.getBytes());
        mdos.flush();

        final String md5 = mdos.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.MD5.getAlgorithm());
        final String sha1 = mdos.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.SHA1.getAlgorithm());

        assertEquals("120ea8a25e5d487bf68b5f7096440019", md5, "Incorrect MD5 sum!");
        assertEquals("afa6c8b3a2fae95785dc7d9685a57835d703ac88", sha1, "Incorrect SHA-1 sum!");

        mdos.close();

        logger.debug("MD5:  " + md5);
        logger.debug("SHA1: " + sha1);

        Path md5File = filePath.resolveSibling(filePath.getFileName() + EncryptionAlgorithmsEnum.MD5.getExtension());
        Path sha1File = filePath.resolveSibling(filePath.getFileName() + EncryptionAlgorithmsEnum.SHA1.getExtension());

        assertTrue(Files.exists(md5File), "Failed to create md5 checksum filePath!");
        assertTrue(Files.exists(sha1File), "Failed to create sha1 checksum filePath!");

        String md5ReadIn = MessageDigestUtils.readChecksumFile(md5File.toString());
        String sha1ReadIn = MessageDigestUtils.readChecksumFile(sha1File.toString());

        assertEquals(md5, md5ReadIn, "MD5 checksum filePath contains incorrect checksum!");
        assertEquals(sha1, sha1ReadIn, "SHA-1 checksum filePath contains incorrect checksum!");
    }

    @Test
    void testConcatenatedWrites()
            throws IOException,
                   NoSuchAlgorithmException
    {
        String string = "This is a big fat super long text which has no meaning, but is good for the test.";

        ByteArrayInputStream bais1 = new ByteArrayInputStream(string.getBytes());
        ByteArrayInputStream bais2 = new ByteArrayInputStream(string.getBytes());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MultipleDigestOutputStream mdos = new MultipleDigestOutputStream(baos);

        int size = 32;
        byte[] bytes = new byte[size];

        int total = 0;
        int len;

        while ((len = bais1.read(bytes, 0, size)) != -1)
        {
            mdos.write(bytes, 0, len);

            total += len;
            if (total >= size)
            {
                break;
            }
        }

        mdos.flush();

        bytes = new byte[size];
        bais1.close();

        logger.debug("Read {} bytes", total);

        bais2.skip(total);

        logger.debug("Skipped {}/{} bytes.", total, string.getBytes().length);

        while ((len = bais2.read(bytes, 0, size)) != -1)
        {
            mdos.write(bytes, 0, len);

            total += len;
        }

        mdos.flush();

        logger.debug("Original:      {}", string);
        logger.debug("Read:          {}", new String(baos.toByteArray()));

        logger.debug("Read {}/{} bytes.", total, string.getBytes().length);

        mdos.close();

        final String md5 = mdos.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.MD5.getAlgorithm());
        final String sha1 = mdos.getMessageDigestAsHexadecimalString(EncryptionAlgorithmsEnum.SHA1.getAlgorithm());

        logger.debug("MD5:  {}", md5);
        logger.debug("SHA1: {}", sha1);

        assertEquals("693188a2fb009bf2a87afcbca95cfcd6", md5, "Incorrect MD5 sum!");
        assertEquals("6ed7c74babd1609cb11836279672ade14a8748c1", sha1, "Incorrect SHA-1 sum!");
    }

}
