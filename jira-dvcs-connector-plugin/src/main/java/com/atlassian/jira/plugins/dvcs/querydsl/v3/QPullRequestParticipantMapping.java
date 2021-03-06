package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import com.mysema.query.types.Path;

import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;

/**
 *
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 */
public class QPullRequestParticipantMapping extends RelationalPathBase<QPullRequestParticipantMapping> {

    private static final long serialVersionUID = -776596911L;

    public static final String AO_TABLE_NAME  = "AO_E8B6CC_PR_PARTICIPANT";

    public static final QPullRequestParticipantMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QPullRequestParticipantMapping("PR_PARTICIPANT", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    // We have not yet built QueryDSL type support for com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping getPullRequest()


    public final StringPath ROLE = createString("ROLE");

    public final StringPath USERNAME = createString("USERNAME");

    public final BooleanPath APPROVED = createBoolean(PullRequestParticipantMapping.APPROVED);

    public final NumberPath<Integer> PULL_REQUEST_ID = createNumber(PullRequestParticipantMapping.PULL_REQUEST_ID, Integer.class);

    public final com.mysema.query.sql.PrimaryKey<QPullRequestParticipantMapping> PR_PARTICIPANT_PK = createPrimaryKey(ID);

    public QPullRequestParticipantMapping(String variable, String schema, String table) {
        super(QPullRequestParticipantMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata() {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        
        addMetadata(ROLE, ColumnMetadata.named("ROLE").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(USERNAME, ColumnMetadata.named("USERNAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..

        addMetadata(PULL_REQUEST_ID, ColumnMetadata.named(PullRequestParticipantMapping.PULL_REQUEST_ID).ofType(Types.VARCHAR));
        addMetadata(APPROVED, ColumnMetadata.named(PullRequestParticipantMapping.APPROVED).ofType(Types.BOOLEAN));
    }
}