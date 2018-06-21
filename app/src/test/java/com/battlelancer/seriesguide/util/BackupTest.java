package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BackupTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private DataLiberationFragment dataLiberationFragment = mock(DataLiberationFragment.class);

    @Test
    public void testBackup() {
        dataLiberationFragment.doDataLiberationAction(1); //1 = EXPORT backup.
        verify(dataLiberationFragment).doDataLiberationAction(1);
    }

}
