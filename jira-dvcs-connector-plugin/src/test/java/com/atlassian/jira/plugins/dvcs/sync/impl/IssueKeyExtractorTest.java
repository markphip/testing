package com.atlassian.jira.plugins.dvcs.sync.impl;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Martin Skurla
 */
public class IssueKeyExtractorTest {

    @DataProvider
    private Object[][] singleIssueKeyWithinMessageDataProvider()
    {
        return new Object[][]
        {
            {"ABC-123",                "ABC-123"}, // exactly the key
            {"ABC-123 text",           "ABC-123"}, // starting line with key
            {"message ABC-123",        "ABC-123"}, // ending line with key
            {"message ABC-123 text",   "ABC-123"}, // separated by whitespaces
            {"message\nABC-123\ntext", "ABC-123"}, // separated by newlines
            {"message\rABC-123\rtext", "ABC-123"}, // separated by carriage returns
            {"message.ABC-123.text",   "ABC-123"}, // separated by dots
            {"message:ABC-123:text",   "ABC-123"}, // separated by colons
            {"message,ABC-123,text",   "ABC-123"}, // separated by commas
            {"message;ABC-123;text",   "ABC-123"}, // separated by semicolons
            {"message&ABC-123&text",   "ABC-123"}, // separated by ampersands
            {"message=ABC-123=text",   "ABC-123"}, // separated by equal signs
            {"message?ABC-123?text",   "ABC-123"}, // separated by question marks
            {"message!ABC-123!text",   "ABC-123"}, // separated by exclamation marks
            {"message/ABC-123/text",   "ABC-123"}, // separated by slashes
            {"message\\ABC-123\\text", "ABC-123"}, // separated by back slashes
            {"message~ABC-123~text",   "ABC-123"}, // separated by tildas
            {"message_ABC-123_text",   "ABC-123"}, // separated by underscore
        };
    }

    @DataProvider
    private Object[][] singleIssueKeyWithinMessageDataProviderWithNumbers()
    {
        return new Object[][]
            {
                {"A1BC-123",                "A1BC-123"}, // exactly the key
                {"AB8C-123 text",           "AB8C-123"}, // starting line with key
                {"message A7BC-123",        "A7BC-123"}, // ending line with key
                {"message AB9C-123 text",   "AB9C-123"}, // separated by whitespaces
                {"message\nA2BC-123\ntext", "A2BC-123"}, // separated by newlines
                {"message\rABC0-123\rtext", "ABC0-123"}, // separated by carriage returns
                {"message.ABC789-123.text", "ABC789-123"}, // separated by dots
                {"message:A1BC-123:text",   "A1BC-123"}, // separated by colons
                {"message,A1BC-123,text",   "A1BC-123"}, // separated by commas
                {"message;A1BC-123;text",   "A1BC-123"}, // separated by semicolons
                {"message&AB1C-123&text",   "AB1C-123"}, // separated by ampersands
                {"message=A1BC-123=text",   "A1BC-123"}, // separated by equal signs
                {"message?A1BC-123?text",   "A1BC-123"}, // separated by question marks
                {"message!AB1C-123!text",   "AB1C-123"}, // separated by exclamation marks
                {"message/AB1C-123/text",   "AB1C-123"}, // separated by slashes
                {"message\\A1BC-123\\text", "A1BC-123"}, // separated by back slashes
                {"message~A1BC-123~text",   "A1BC-123"}, // separated by tildas
                {"message_A1BC-123_text",   "A1BC-123"}, // separated by underscore
            };
    }

    @DataProvider
    private Object[][] singleIssueKeyWithinMessageDataProviderWithUnderscore()
    {
        return new Object[][]
            {
                {"A_BC-123",                "A_BC-123"}, // exactly the key
                {"AB_C-123",                "AB_C-123"}, // exactly the key
                {"ABC_-123",                "ABC_-123"}, // exactly the key
                {"A_BC-123 text",           "A_BC-123"}, // starting line with key
                {"message A_BC-123",        "A_BC-123"}, // ending line with key
                {"message A_BC-123 text",   "A_BC-123"}, // separated by whitespaces
                {"message\nA_BC-123\ntext", "A_BC-123"}, // separated by newlines
                {"message\rA_BC-123\rtext", "A_BC-123"}, // separated by carriage returns
                {"message.A_BC-123.text",   "A_BC-123"}, // separated by dots
                {"message:A_BC-123:text",   "A_BC-123"}, // separated by colons
                {"message,A_BC-123,text",   "A_BC-123"}, // separated by commas
                {"message;A_BC-123;text",   "A_BC-123"}, // separated by semicolons
                {"message&A_BC-123&text",   "A_BC-123"}, // separated by ampersands
                {"message=A_BC-123=text",   "A_BC-123"}, // separated by equal signs
                {"message?A_BC-123?text",   "A_BC-123"}, // separated by question marks
                {"message!A_BC-123!text",   "A_BC-123"}, // separated by exclamation marks
                {"message/A_BC-123/text",   "A_BC-123"}, // separated by slashes
                {"message\\A_BC-123\\text", "A_BC-123"}, // separated by back slashes
                {"message~A_BC-123~text",   "A_BC-123"}, // separated by tildas
                {"message_A_BC-123_text",   "A_BC-123"}, // separated by underscore
            };
    }

   @DataProvider
    private Object[][] multipleIssueKeysWithinMessageDataProvider()
    {
        return new Object[][]
        {
            {"ABC-123 DEF-456",                 new String[] {"ABC-123", "DEF-456"}}, // exactly the keys
            {"message ABC-123 DEF-456 text",    new String[] {"ABC-123", "DEF-456"}}, // separated by whitespaces
            {"message\nABC-123\nDEF-456\ntext", new String[] {"ABC-123", "DEF-456"}}, // separated by newlines
            {"message\rABC-123\rDEF-456\rtext", new String[] {"ABC-123", "DEF-456"}}, // separated by carriage returns
            {"message.ABC-123.DEF-456.text",    new String[] {"ABC-123", "DEF-456"}}, // separated by dots
            {"message:ABC-123:DEF-456:text",    new String[] {"ABC-123", "DEF-456"}}, // separated by colons
            {"message,ABC-123,DEF-456,text",    new String[] {"ABC-123", "DEF-456"}}, // separated by commas
            {"message;ABC-123;DEF-456;text",    new String[] {"ABC-123", "DEF-456"}}, // separated by semicolons
            {"message&ABC-123&DEF-456&text",    new String[] {"ABC-123", "DEF-456"}}, // separated by ampersands
            {"message=ABC-123=DEF-456=text",    new String[] {"ABC-123", "DEF-456"}}, // separated by equal signs
            {"message?ABC-123?DEF-456?text",    new String[] {"ABC-123", "DEF-456"}}, // separated by question marks
            {"message!ABC-123!DEF-456!text",    new String[] {"ABC-123", "DEF-456"}}, // separated by exclamation marks
            {"message/ABC-123/DEF-456/text",    new String[] {"ABC-123", "DEF-456"}}, // separated by slashes
            {"message\\ABC-123\\DEF-456\\text", new String[] {"ABC-123", "DEF-456"}}, // separated by back slashes
            {"message~ABC-123~DEF-456~text",    new String[] {"ABC-123", "DEF-456"}}, // separated by tildas
            {"message_ABC-123_DEF-456_text",    new String[] {"ABC-123", "DEF-456"}}, // separated by underscore
        };
    }

   @DataProvider
    private Object[][] noIssueKeysWithinMessageDataProvider()
    {
        return new Object[][]
        {
            {"message without key"},
            {"message ABC-A text"},
            {"message M-123 invalid key"},
            {"message MES- invalid key"},
            {"message -123 invalid key"},
            {"message 1ABC-123 invalid key"},
            {"message 123-123 invalid key"},
            {"should not parse key0MES-123"},
            {"should not parse MES-123key"},
            {"MES-123k invalid char"},
            {"invalid char MES-123k"}
        };
    }

    @Test(dataProvider="singleIssueKeyWithinMessageDataProvider")
    public void extractorShouldExtractSingleIssueKeyCorrectly(String messageToExtract, String expectedExtractedKey)
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);

        assertThat(extractIssueKeys).hasSize(1);
        assertThat(extractIssueKeys).containsOnly(expectedExtractedKey);
    }

    @Test(dataProvider="singleIssueKeyWithinMessageDataProviderWithNumbers")
    public void extractorShouldExtractSingleIssueKeyWithNumbersCorrectly(String messageToExtract, String expectedExtractedKey)
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);

        assertThat(extractIssueKeys).hasSize(1);
        assertThat(extractIssueKeys).containsOnly(expectedExtractedKey);
    }

    @Test(dataProvider="singleIssueKeyWithinMessageDataProviderWithUnderscore")
    public void extractorShouldExtractSingleIssueKeyWithUnderscoreCorrectly(String messageToExtract, String expectedExtractedKey)
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);

        assertThat(extractIssueKeys).hasSize(1);
        assertThat(extractIssueKeys).containsOnly(expectedExtractedKey);
    }

    @Test(dataProvider="multipleIssueKeysWithinMessageDataProvider")
    public void extractorShouldExtractMultipleIssueKeysCorrectly(String messageToExtract, String[] expectedExtractedKeys)
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);

        assertThat(extractIssueKeys).hasSize(2);
        assertThat(extractIssueKeys).containsOnly(expectedExtractedKeys);
    }

    @Test(dataProvider="noIssueKeysWithinMessageDataProvider")
    public void extractorShouldExtractNoIssueKeysCorrectly(String messageToExtract)
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);

        assertThat(extractIssueKeys).isEmpty();
    }
}
