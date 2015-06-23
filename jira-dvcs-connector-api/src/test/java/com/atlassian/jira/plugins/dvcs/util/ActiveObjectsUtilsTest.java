package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.base.Splitter;
import net.java.ao.Entity;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActiveObjectsUtilsTest
{
    @Mock
    private ActiveObjects activeObjects;

    @BeforeMethod
    public void initializeMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDeletion()
    {
        final List<Entity> entities = sampleEntitites();
        setupAoStream(entities);
        setupMockAoFind(entities);

        ActiveObjectsUtils.delete(activeObjects, Entity.class, Mockito.mock(Query.class));


        Entity[] entitiesAsArray = entities.toArray(new Entity[entities.size()]);
        verify(activeObjects, atLeastOnce()).delete(entitiesAsArray);
    }

    private void setupAoStream(List<Entity> entities){
        doAnswer(invocation -> {
            EntityStreamCallback<Entity, Integer> callback = (EntityStreamCallback<Entity, Integer>)invocation.getArguments()[2];
            for (Entity entity : entities)
            {
                callback.onRowRead(entity);
            }
            return null;
        }).when(activeObjects).stream(any(Class.class), any(Query.class), any(EntityStreamCallback.class));
    }


    private void setupMockAoFind(List<Entity> entities){

        when(activeObjects.find(any(Class.class), anyString(), Mockito.<Object>anyVararg())).then(invocation -> {
            String criteria = (String) invocation.getArguments()[1];
            List<Entity> result = new ArrayList<Entity>();
            int i = 2;
            for (String param : Splitter.on(",").split(criteria.replaceAll("ID IN \\((.+)\\)", "$1")))
            {
                if (param.trim().equals("?"))
                {
                    Integer parameter = (Integer) invocation.getArguments()[i++];
                    result.add(entities.get(parameter));
                }
                else
                {
                    result.add(entities.get(Integer.valueOf(param.trim())));
                }
            }

            return result.toArray(new Entity[result.size()]);
        }
        );
    }

    private List<Entity> sampleEntitites(){
        List<Entity> entities = new ArrayList<Entity>();
        for (int id = 0; id < 100 ; id++)
        {
            entities.add(createEntity(id));
        }

        return entities;
    }


    private Entity createEntity(int id)
    {
        Entity entity = Mockito.mock(Entity.class);
        when(entity.getID()).thenReturn(id);
        return entity;
    }

}
