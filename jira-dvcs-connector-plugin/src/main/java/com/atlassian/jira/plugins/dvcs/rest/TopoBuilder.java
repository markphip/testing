package com.atlassian.jira.plugins.dvcs.rest;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;

public class TopoBuilder
{
    public List<String> buildTopoOrder(final Collection<String> heads, final List<ChangesetHash> rawHashes)
    {

        Map<String, ChangesetHash> changeSets = new HashMap<String, ChangesetHash>(rawHashes.size());

        for (ChangesetHash rawHash : rawHashes)
        {
            changeSets.put(rawHash.getHash(), rawHash);
        }

        populateAncestors(heads, changeSets);

        return topoerise(heads, changeSets);
    }

    /**
     * A shrubbery?
     */
    public List<String> topoerise(final Collection<String> heads, final Map<String, ChangesetHash> changeSets)
    {
        final List<String> result = new ArrayList<String>(changeSets.size());
        final Collection<ChangesetHash> candidatesTrans = Collections2.transform(heads, new Function<String, ChangesetHash>()
        {
            @Override
            public ChangesetHash apply(@Nullable final String input)
            {
                return changeSets.get(input);
            }
        });

        final List<ChangesetHash> candidates = new ArrayList<ChangesetHash>(candidatesTrans);

        while (!candidates.isEmpty())
        {
            ChangesetHash candidate = pickCandidate(result, candidates);
            result.add(candidate.getHash());
            candidates.remove(candidate);
//            Set<String> candidateParents = new HashSet<String>(candidate.getParents());
//            candidateParents.retainAll(result);
            candidates.addAll(Collections2.transform(candidate.getParents(), new Function<String, ChangesetHash>()
            {
                @Override
                public ChangesetHash apply(@Nullable final String input)
                {
                    return changeSets.get(input);
                }
            }));
        }

        return result;
    }

    public ChangesetHash pickCandidate(final List<String> result, final Collection<ChangesetHash> candidates)
    {
        for (ChangesetHash candidate : candidates)
        {
            if (result.containsAll(candidate.getChildren()))
            {
                return candidate;
            }
        }
        // oh dear we have no candidates who are safe to add, fail
        throw new IllegalArgumentException("No eligible candidate found!!");
    }

    public void populateAncestors(final Collection<String> heads, final Map<String, ChangesetHash> changeSets)
    {
        Queue<String> nodesToProcess = new ArrayDeque<String>();
        nodesToProcess.addAll(heads);

        while (!nodesToProcess.isEmpty())
        {
            String currentNodeHash = nodesToProcess.poll();
            ChangesetHash currentChangeSet = changeSets.get(currentNodeHash);

            for (String parent : currentChangeSet.getParents())
            {
                changeSets.get(parent).addChild(currentChangeSet.getHash());
                nodesToProcess.add(parent);
            }
        }
    }
}
