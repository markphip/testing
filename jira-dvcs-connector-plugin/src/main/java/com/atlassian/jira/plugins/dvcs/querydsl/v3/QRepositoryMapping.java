package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.path.BooleanPath;
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
public class QRepositoryMapping extends RelationalPathBase<QRepositoryMapping>
{

    private static final long serialVersionUID = 345376190L;

    public static final String AO_TABLE_NAME = "AO_E8B6CC_REPOSITORY_MAPPING";

    public static final QRepositoryMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QRepositoryMapping("REPOSITORY_MAPPING", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    // We have not yet built QueryDSL type support for java.util.Date getActivityLastSync()


    public final StringPath FORK_OF_NAME = createString("FORK_OF_NAME");

    public final StringPath FORK_OF_OWNER = createString("FORK_OF_OWNER");

    public final StringPath FORK_OF_SLUG = createString("FORK_OF_SLUG");

    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final StringPath LAST_CHANGESET_NODE = createString("LAST_CHANGESET_NODE");

    // We have not yet built QueryDSL type support for java.util.Date getLastCommitDate()


    public final StringPath LOGO = createString("LOGO");

    public final StringPath NAME = createString("NAME");

    public final NumberPath<Integer> ORGANIZATION_ID = createNumber("ORGANIZATION_ID", Integer.class);

    public final StringPath SLUG = createString("SLUG");

    public final BooleanPath DELETED = createBoolean("DELETED");

    public final BooleanPath LINKED = createBoolean("LINKED");

    public final com.mysema.query.sql.PrimaryKey<QRepositoryMapping> REPOSITORYMAPPING_PK = createPrimaryKey(ID);

    public QRepositoryMapping(String variable, String schema, String table)
    {
        super(QRepositoryMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata()
    {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */

        addMetadata(FORK_OF_NAME, ColumnMetadata.named("FORK_OF_NAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(FORK_OF_OWNER, ColumnMetadata.named("FORK_OF_OWNER").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(FORK_OF_SLUG, ColumnMetadata.named("FORK_OF_SLUG").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(LAST_CHANGESET_NODE, ColumnMetadata.named("LAST_CHANGESET_NODE").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..

        addMetadata(LOGO, ColumnMetadata.named("LOGO").ofType(Types.VARCHAR)); // .withSize(2147483647)); // until detect primitive types, int ..
        addMetadata(NAME, ColumnMetadata.named("NAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ORGANIZATION_ID, ColumnMetadata.named("ORGANIZATION_ID").ofType(Types.INTEGER)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(SLUG, ColumnMetadata.named("SLUG").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(DELETED, ColumnMetadata.named("DELETED").ofType(Types.BOOLEAN));
        addMetadata(LINKED, ColumnMetadata.named("LINKED").ofType(Types.BOOLEAN));
    }
}