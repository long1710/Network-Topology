package simulator.fatTreeMachine.JUnit;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import simulator.Job;
import simulator.allocator.AllocInfo;
import simulator.fatTreeMachine.FatTree;
import simulator.fatTreeMachine.FatTreeAllocInfo;
import simulator.fatTreeMachine.FatTreeLocation;
import simulator.fatTreeMachine.PollardAllocator;

public class TestAllocator{
    FatTree tree;
    PollardAllocator alloc;
    Job canAllocate, cannotAllocate, small, medium, large, large2;
    @Before
    public void setUp(){
        tree = new FatTree(4);
        alloc = new PollardAllocator(tree);   
        canAllocate = new Job(0, 10, 200, 300);
        cannotAllocate = new Job(0, 17, 200, 300); 
        small = new Job(0, 1, 200, 300);
        medium = new Job(0, 3, 300, 400);
        large = new Job(0, 8, 400, 500);
        large2 = new Job(0, 6, 300, 400);
    }

    //helper method that classified how many tier 1, 2 ,3 job presence in tree
    public int[] getEachType(FatTree tree){
        int[] type = new int[4];
        for(FatTreeLocation x: tree.usedProcessors()){

            type[x.type]++;
        }
        return type;
    }

    @Test
    public void testAllocate_CanAllocate(){
        //job with more procs than max capacity 
        assertEquals(alloc.canAllocate(canAllocate), true);
        assertEquals(alloc.canAllocate(cannotAllocate), false);

        tree.allocate(alloc.allocate(canAllocate));
        //max capacity ( canAllocate= 10 + big = 8 > 16)
        assertEquals(alloc.canAllocate(large), false);
        tree.reset();
        
        //2 tier 3 job
        tree.allocate(alloc.allocate(canAllocate));
        assertEquals(alloc.canAllocate(large2), false);

        tree.reset();
    }

    @Test
    public void testAllocate_smallAllocate(){
        tree.allocate(alloc.allocate(small));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 1);

        //test type of procs
        assertEquals(getEachType(tree)[1], 1);

        tree.reset();
    }

    @Test
    public void testAllocate_mediumAllocate(){
        tree.allocate(alloc.allocate(medium));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 3);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[2], 3);
        
        tree.reset();
    }

    @Test
    public void testAllocate_largeAllocate(){
        tree.allocate(alloc.allocate(large));
        //test allocte procs
        assertEquals(tree.usedProcessors().size(), 8);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[3], 8);
        
        tree.reset();
    }

    @Test
    public void testAllocate_small_medium_Allocate(){
        tree.allocate(alloc.allocate(small));
        tree.allocate(alloc.allocate(medium));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 4);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[1], 1);
        assertEquals(type[2], 3);

        tree.reset();
    }

    @Test
    public void testAllocate_small_large_Allocate(){
        tree.allocate(alloc.allocate(large));
        tree.allocate(alloc.allocate(small));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 9);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[1], 1);
        assertEquals(type[3], 8);

        tree.reset();
    }

    @Test
    public void testAllocate_medium_large_Allocate(){
        tree.allocate(alloc.allocate(large));
        tree.allocate(alloc.allocate(medium));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 11);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[2], 3);
        assertEquals(type[3], 8);

        tree.reset();
    }

    @Test
    public void testAllocate_small_medium_large_Allocate(){
        tree.allocate(alloc.allocate(large));
        tree.allocate(alloc.allocate(medium));
        tree.allocate(alloc.allocate(small));
        //test allocate procs
        assertEquals(tree.usedProcessors().size(), 12);

        //test type of procs
        int[] type = getEachType(tree);
        assertEquals(type[1], 1);
        assertEquals(type[2], 3);
        assertEquals(type[3], 8);

        tree.reset();
    }

}