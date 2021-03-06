package com.atlassian.jira.plugins.dvcs.sync.impl;

import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Martin Skurla
 */
public final class IssueKeyExtractor
{
    private static final String SEPARATOR = "[\\s\\p{Punct}]";
    private static final String KEY_PREFIX_REGEX  = "(?<=" + SEPARATOR + ")"; //zero-width positive lookbehind
    private static final String ISSUE_KEY_REGEX   = "(\\p{Lu}[\\p{Lu}\\p{Digit}]+-\\p{Digit}+)";
    private static final String KEY_POSTFIX_REGEX = "(?=" + SEPARATOR + ")";  //zero-width positive lookahead
    
    private static final String ISSUE_KEY_REGEX_INSIDE_LINE = KEY_PREFIX_REGEX + ISSUE_KEY_REGEX + KEY_POSTFIX_REGEX;
    
    private static final String ISSUE_KEY_REGEX_STARTING_LINE = "^" + ISSUE_KEY_REGEX + SEPARATOR;
    private static final String ISSUE_KEY_REGEX_ENDING_LINE   = SEPARATOR + ISSUE_KEY_REGEX + "$";
    private static final String ISSUE_KEY_ALONE_ON_LINE       = "^" + ISSUE_KEY_REGEX + "$";
    
    private IssueKeyExtractor() {}
    
    
    public static Set<String> extractIssueKeys(String...messages)
    {       
        Set<String> matches = new HashSet<String>();

        String[] ISSUE_KEY_REGEXES = {ISSUE_KEY_REGEX_STARTING_LINE,
                                      ISSUE_KEY_REGEX_INSIDE_LINE,
                                      ISSUE_KEY_REGEX_ENDING_LINE,
                                      ISSUE_KEY_ALONE_ON_LINE};
        for (String regex : ISSUE_KEY_REGEXES)
        {
            Pattern projectKeyPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            for (String message : messages)
            {
                if (StringUtils.isBlank(message))
                {
                    continue;
                }

                Matcher match = projectKeyPattern.matcher(message);

                while (match.find())
                {
                    // Get all groups for this match
                    for (int i = 1; i <= match.groupCount(); i++)
                    {
                        String issueKey = match.group(i);
                        matches.add(issueKey.toUpperCase());
                    }
                }
            }

        }

        return matches;
    }
}
