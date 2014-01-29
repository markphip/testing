package com.atlassian.jira.plugins.dvcs.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.google.common.collect.ImmutableList;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FileDataTest
{
    final List<ChangesetFileDetail> fileDetails = ImmutableList.of(
            new ChangesetFileDetail(ChangesetFileAction.MODIFIED, "file1", 1, 0),
            new ChangesetFileDetail(ChangesetFileAction.MODIFIED, "file2", 1, 0)
    );

    final List<ChangesetFile> files = ImmutableList.of(
            new ChangesetFile(ChangesetFileAction.MODIFIED, "file3"),
            new ChangesetFile(ChangesetFileAction.MODIFIED, "file4")
    );

    final String filesJson = "{\"count\":2,\"files\":[{\"filename\":\"file3\",\"status\":\"modified\"},{\"filename\":\"file4\",\"status\":\"modified\"}]}";

    @Mock Changeset changeset;
    @Mock ChangesetMapping changesetMapping;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void toJsonShouldUseFileDataWhenNoDetailsAvailable() throws Exception
    {
        when(changeset.getAllFileCount()).thenReturn(2);
        when(changeset.getFiles()).thenReturn(files);
        when(changeset.getFileDetails()).thenReturn(null);

        assertThat(FileData.toJSON(changeset), equalTo(filesJson));
    }

    @Test
    public void toJsonShouldIgnoreFilesWhenFileDetailsAreAvailable() throws Exception
    {
        when(changeset.getAllFileCount()).thenReturn(2);
        when(changeset.getFiles()).thenReturn(files);
        when(changeset.getFileDetails()).thenReturn(fileDetails);

        assertThat(FileData.toJSON(changeset), equalTo("{\"count\":2}"));
    }

    @Test
    public void fromShouldPreferToReadFromFileDetailsRatherThanFileData() throws Exception
    {
        when(changesetMapping.getFilesData()).thenReturn(filesJson); // only count should be read
        when(changesetMapping.getFileDetailsJson()).thenReturn(ChangesetFileDetails.toJSON(fileDetails));

        assertThat(FileData.from(changesetMapping), equalTo(new FileData(ImmutableList.<ChangesetFile>copyOf(fileDetails), 2)));
    }

    @Test
    public void fromShouldUseFileDataIfNoDetailsAvailable() throws Exception
    {
        when(changesetMapping.getFilesData()).thenReturn(filesJson);
        when(changesetMapping.getFileDetailsJson()).thenReturn(null);

        assertThat(FileData.from(changesetMapping), equalTo(new FileData(files, 2)));
    }
}
