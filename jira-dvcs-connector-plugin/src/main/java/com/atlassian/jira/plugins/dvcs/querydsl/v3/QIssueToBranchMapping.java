package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

/**
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 *
 * Changes made by hand:
 *    Map and FK mappings to integers
 *    Map Booleans to BooleanPaths
 *    Map Dates as DateTimePath<Date>
 *
 * Future approach is documented at https://extranet.atlassian.com/x/AAuQj
 */
public class QIssueToBranchMapping extends RelationalPathBase<QIssueToBranchMapping> implements IssueKeyedMapping
{

    private static final long serialVersionUID = 130563614L;

    public static final String AO_TABLE_NAME = "AO_E8B6CC_ISSUE_TO_BRANCH";

    public static final QIssueToBranchMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QIssueToBranchMapping("ISSUE_TO_BRANCH", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final StringPath ISSUE_KEY = createString("ISSUE_KEY");

    public final NumberPath<Integer> BRANCH_ID = createNumber("BRANCH_ID", Integer.class);

    public final com.mysema.query.sql.PrimaryKey<QIssueToBranchMapping> ISSUETOBRANCH_PK = createPrimaryKey(ID);

    public QIssueToBranchMapping(String variable, String schema, String table)
    {
        super(QIssueToBranchMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata()
    {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */

        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(ISSUE_KEY, ColumnMetadata.named("ISSUE_KEY").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(BRANCH_ID, ColumnMetadata.named("BRANCH_ID").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
    }

    @Override
    public SimpleExpression getIssueKeyExpression()
    {
        return ISSUE_KEY;
    }
}