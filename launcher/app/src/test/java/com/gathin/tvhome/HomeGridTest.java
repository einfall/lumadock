package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeGridTest {
    @Test public void fiveDockSlotsAndSixColumnsUseNearestVerticalNeighbour() {
        HomeGrid grid=new HomeGrid(5,6);
        assertEquals(5,grid.down(0,20));assertEquals(8,grid.down(2,20));assertEquals(10,grid.down(4,20));
        assertEquals(0,grid.up(5));assertEquals(2,grid.up(8));assertEquals(4,grid.up(10));
        assertEquals(5,grid.up(11));assertEquals(11,grid.down(5,20));
    }
    @Test public void allSupportedSizesStayInTheNextRowAndInBounds() {
        for(int dock=4;dock<=7;dock++)for(int cols=4;cols<=7;cols++)for(int count=1;count<=40;count++){
            HomeGrid grid=new HomeGrid(dock,cols);
            for(int position=0;position<count;position++){
                int down=grid.down(position,count);assertTrue(down>=0&&down<count);
                assertTrue(down==position||grid.rowStart(down)==(position<dock?dock:grid.rowStart(position)+cols));
                int up=grid.up(position);assertTrue(up<position);
                if(position<dock)assertEquals(-1,up);else assertTrue(up>=0);
                assertTrue(grid.toDock(position)>=0&&grid.toDock(position)<dock);
            }
        }
    }
    @Test public void horizontalEdgesDoNotWrapAfterTheShorterDock() {
        HomeGrid grid=new HomeGrid(5,6);
        assertTrue(grid.edge(4,20,false));assertTrue(grid.edge(5,20,true));
        assertFalse(grid.edge(5,20,false));assertTrue(grid.edge(10,20,false));
        assertTrue(grid.edge(19,20,false));assertEquals(17,grid.rowStart(19));
    }
}
