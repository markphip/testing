package com.atlassian.jira.plugins.dvcs.rest;

import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TopoBuilderPickCandidateTest
{
    private static final String HASH_1_HEAD = "A";
    private static final String HASH_2 = "B";
    private static final String HASH_3 = "C";

    private TopoBuilder topoBuilder = new TopoBuilder();

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void testNoValues()
    {
        topoBuilder.pickCandidate(new LinkedList<String>(), new LinkedList<ChangesetHash>());
    }

    @Test
    public void testNoChildren()
    {
        ChangesetHash hash = new ChangesetHash(HASH_1_HEAD);
        ChangesetHash candidate = topoBuilder.pickCandidate(new LinkedList<String>(), ImmutableList.of(hash));
        assertThat(candidate, is(hash));
    }

    @Test
    public void testChildrenProcessed()
    {
        ChangesetHash head = new ChangesetHash(HASH_1_HEAD);
        ChangesetHash parent = new ChangesetHash(HASH_2);
        head.addParent(parent.getHash());
        parent.addChild(head.getHash());
        ChangesetHash candidate = topoBuilder.pickCandidate(ImmutableList.of(head.getHash()), ImmutableList.of(parent));
        assertThat(candidate, is(parent));
    }

    @Test
    public void testChildrenNotProcessed()
    {
        ChangesetHash head = new ChangesetHash(HASH_1_HEAD);
        ChangesetHash parent = new ChangesetHash(HASH_2);
        head.addParent(parent.getHash());
        parent.addChild(head.getHash());
        ChangesetHash candidate = topoBuilder.pickCandidate(new LinkedList<String>(), ImmutableList.of(parent, head));
        assertThat(candidate, is(head));
    }

    @Test
    public void testTwoChildParent()
    {
        final Map<String, ChangesetHash> map = new HashMap<String, ChangesetHash>();

        ChangesetHash head = new ChangesetHash(HASH_1_HEAD);
        head.addParent(HASH_2);
        head.addParent(HASH_3);
        map.put(HASH_1_HEAD, head);
        final ChangesetHash parent = new ChangesetHash(HASH_2);
        map.put(HASH_2, parent);
        final ChangesetHash parent2 = new ChangesetHash(HASH_3);
        map.put(HASH_3, parent2);

        final Collection<String> heads = new ArrayList<String>();
        heads.add(HASH_1_HEAD);
        topoBuilder.populateAncestors(heads, map);

        assertThat(parent.getChildren().iterator().next(), is(HASH_1_HEAD));
        assertThat(parent2.getChildren().iterator().next(), is(HASH_1_HEAD));
    }
}
