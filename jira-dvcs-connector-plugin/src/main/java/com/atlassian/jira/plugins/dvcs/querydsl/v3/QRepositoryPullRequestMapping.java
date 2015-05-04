package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import java.sql.Types;
import java.util.Date;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

/**
 *
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 */
public class QRepositoryPullRequestMapping extends RelationalPathBase<QRepositoryPullRequestMapping> {

    private static final long serialVersionUID = -1102727614L;

    public static final String AO_TABLE_NAME  = "AO_E8B6CC_PULL_REQUEST";

    public static final QRepositoryPullRequestMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QRepositoryPullRequestMapping("PULL_REQUEST", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    public final StringPath AUTHOR = createString("AUTHOR");

    public final NumberPath<Integer> COMMENT_COUNT = createNumber("COMMENT_COUNT", Integer.class);

    // We have not yet built QueryDSL type support for com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping[] getCommits()

    public final DateTimePath<Date> CREATED_ON = createDateTime("CREATED_ON", Date.class);
    // We have not yet built QueryDSL type support for java.util.Date getCreatedOn()


    public final StringPath DESTINATION_BRANCH = createString("DESTINATION_BRANCH");

    public final StringPath EXECUTED_BY = createString("EXECUTED_BY");

    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final StringPath LAST_STATUS = createString("LAST_STATUS");

    public final StringPath NAME = createString("NAME");

    // We have not yet built QueryDSL type support for com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping[] getParticipants()


    public final NumberPath<Long> REMOTE_ID = createNumber("REMOTE_ID", Long.class);

    public final StringPath SOURCE_BRANCH = createString("SOURCE_BRANCH");

    public final StringPath SOURCE_REPO = createString("SOURCE_REPO");

    public final NumberPath<Integer> TO_REPOSITORY_ID = createNumber("TO_REPOSITORY_ID", Integer.class);

    public final DateTimePath<Date> UPDATED_ON = createDateTime("UPDATED_ON", Date.class);

    public final StringPath URL = createString("URL");

    public final com.mysema.query.sql.PrimaryKey<QRepositoryPullRequestMapping> PULL_REQUEST_PK = createPrimaryKey(ID);

    public QRepositoryPullRequestMapping(String variable, String schema, String table) {
        super(QRepositoryPullRequestMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata() {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */
        addMetadata(AUTHOR, ColumnMetadata.named("AUTHOR").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(COMMENT_COUNT, ColumnMetadata.named("COMMENT_COUNT").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
        
        
        addMetadata(DESTINATION_BRANCH, ColumnMetadata.named("DESTINATION_BRANCH").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(EXECUTED_BY, ColumnMetadata.named("EXECUTED_BY").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(LAST_STATUS, ColumnMetadata.named("LAST_STATUS").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(NAME, ColumnMetadata.named("NAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        
        addMetadata(REMOTE_ID, ColumnMetadata.named("REMOTE_ID").ofType(Types.REAL)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(SOURCE_BRANCH, ColumnMetadata.named("SOURCE_BRANCH").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(SOURCE_REPO, ColumnMetadata.named("SOURCE_REPO").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(TO_REPOSITORY_ID, ColumnMetadata.named("TO_REPOSITORY_ID").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
        
        addMetadata(URL, ColumnMetadata.named("URL").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..

        addMetadata(CREATED_ON, ColumnMetadata.named("CREATED_ON").ofType(Types.TIMESTAMP));
        addMetadata(UPDATED_ON, ColumnMetadata.named("UPDATED_ON").ofType(Types.TIMESTAMP));
    }
}