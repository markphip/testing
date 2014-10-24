package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.sql.ColumnMetadata;
import com.mysema.query.sql.RelationalPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import java.sql.Types;

import static com.mysema.query.types.PathMetadataFactory.forVariable;

/**
 * Generated by https://bitbucket.org/atlassian/querydsl-ao-code-gen
 */
public class QOrganizationMapping extends RelationalPathBase<QOrganizationMapping>
{

    private static final long serialVersionUID = -1614992075L;

    public static final String AO_TABLE_NAME = "AO_E8B6CC_ORGANIZATION_MAPPING";

    public static final QOrganizationMapping withSchema(SchemaProvider schemaProvider)
    {
        String schema = schemaProvider.getSchema(AO_TABLE_NAME);
        return new QOrganizationMapping("ORGANIZATION_MAPPING", schema, AO_TABLE_NAME);
    }

    /**
     * Database Columns
     */
    public final StringPath ACCESS_TOKEN = createString("ACCESS_TOKEN");

    public final StringPath ADMIN_PASSWORD = createString("ADMIN_PASSWORD");

    public final StringPath ADMIN_USERNAME = createString("ADMIN_USERNAME");

    public final StringPath DEFAULT_GROUPS_SLUGS = createString("DEFAULT_GROUPS_SLUGS");

    public final StringPath DVCS_TYPE = createString("DVCS_TYPE");

    public final StringPath HOST_URL = createString("HOST_URL");

    public final NumberPath<Integer> ID = createNumber("ID", Integer.class);

    public final StringPath NAME = createString("NAME");

    public final StringPath OAUTH_KEY = createString("OAUTH_KEY");

    public final StringPath OAUTH_SECRET = createString("OAUTH_SECRET");


    public final com.mysema.query.sql.PrimaryKey<QOrganizationMapping> ORGANIZATIONMAPPING_PK = createPrimaryKey(ID);

    public QOrganizationMapping(String variable, String schema, String table)
    {
        super(QOrganizationMapping.class, forVariable(variable), schema, table);
        addMetadata();
    }

    private void addMetadata()
    {
        /**
         * Database Metadata is not yet used by QueryDSL but it might one day.
         */
        addMetadata(ACCESS_TOKEN, ColumnMetadata.named("ACCESS_TOKEN").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ADMIN_PASSWORD, ColumnMetadata.named("ADMIN_PASSWORD").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ADMIN_USERNAME, ColumnMetadata.named("ADMIN_USERNAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(DEFAULT_GROUPS_SLUGS, ColumnMetadata.named("DEFAULT_GROUPS_SLUGS").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(DVCS_TYPE, ColumnMetadata.named("DVCS_TYPE").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(HOST_URL, ColumnMetadata.named("HOST_URL").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(ID, ColumnMetadata.named("ID").ofType(Types.INTEGER)); // .withSize(0).withNotNull()); // until detect primitive types, int ..
        addMetadata(NAME, ColumnMetadata.named("NAME").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(OAUTH_KEY, ColumnMetadata.named("OAUTH_KEY").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
        addMetadata(OAUTH_SECRET, ColumnMetadata.named("OAUTH_SECRET").ofType(Types.VARCHAR)); // .withSize(0)); // until detect primitive types, int ..
    }
}