package simulator.fatTreeMachine.JUnit;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import simulator.Job;
import simulator.allocator.AllocInfo;
import simulator.fatTreeMachine.FatTree;
import simulator.fatTreeMachine.FatTreeAllocInfo;
import simulator.fatTreeMachine.FatTreeLocation;

public class TestFatTreeBasic
{
    //helper method that translate job to allocinfo and allocate to tree
    public void helper(FatTree tree, FatTreeAllocInfo test_allocate, int type){
        int num_procs = test_allocate.processors.length;
        for(int i = 0; i < num_procs; i++){
            FatTreeLocation test_location = tree.freeProcessors().get(i);
            test_location.type = type;
            test_allocate.processors[i] = test_location;
        }
        tree.allocate(test_allocate);
    }

    @Test
    public void testFatTree_Constructor()
    {
        FatTree tree1 = new FatTree(0);
        FatTree tree2 = new FatTree(4);
        //test tree 1 with 0 port
        assertEquals(tree1.freeProcessors().size(), 0);
        //test tree 2 with 4 port
        assertEquals(tree2.freeProcessors().size(), 16);
    }

    @Test
    public void testFatTree_Allocate_and_Deallocate()
    {
        FatTree tree1 = new FatTree(2);
        Job test = new Job(0, 1, 290, 300);
        FatTreeAllocInfo test_allocate = new FatTreeAllocInfo(test);
        //Test first allocation
        helper(tree1, test_allocate, 3);
        assertEquals(tree1.freeProcessors().size(), 1);
        assertEquals(tree1.usedProcessors().size(), 1);
        //test deallocate
        tree1.deallocate(test_allocate);
        assertEquals(tree1.freeProcessors().size(), 2);
        assertEquals(tree1.usedProcessors().size(), 0);
    }

    @Test
    //check edge case where job is empty or maximum capacity and normal case
    public void testFatTree_numOfProcs(){
        FatTree tree = new FatTree(4);
        Job test1 = new Job(0, 1, 290, 300);
        Job test2 = new Job(0, 3, 300, 500);
        Job test4 = new Job(0, 0, 0, 0);
        Job test5 = new Job(0, 16, 300, 800);

        FatTreeAllocInfo test_allocate = new FatTreeAllocInfo(test1);
        FatTreeAllocInfo test_allocate2 = new FatTreeAllocInfo(test2);
        FatTreeAllocInfo test_allocate4 = new FatTreeAllocInfo(test4);
        FatTreeAllocInfo test_allocate5 = new FatTreeAllocInfo(test5);
        //test allocate 1 procs and reset
        helper(tree, test_allocate, 3);
        assertEquals(tree.usedProcessors().size(), 1);
        tree.deallocate(test_allocate);
        //test allocate 3 procs and reset
        helper(tree, test_allocate2, 3);
        assertEquals(tree.usedProcessors().size(), 3);
        tree.deallocate(test_allocate2);

        //test allocate 1 procs then 3 procs and reset 
        helper(tree, test_allocate, 3);
        helper(tree, test_allocate2, 3);
        assertEquals(tree.usedProcessors().size(), 4);
        tree.deallocate(test_allocate);
        tree.deallocate(test_allocate2);

        //test allocate 0 procs
        helper(tree, test_allocate4, 3);
        assertEquals(tree.usedProcessors().size(), 0);
        tree.deallocate(test_allocate4);

        //test allocate 16 ( all ) procs
        helper(tree, test_allocate5, 3);
        assertEquals(tree.usedProcessors().size(), 16);
        tree.deallocate(test_allocate5);

    }

    @Test
    public void testFatTree_Reset(){
        FatTree tree = new FatTree(5);
        Job test1 = new Job(0, 4, 290, 300);
        Job test2 = new Job(0, 7, 300, 500);
        Job test3 = new Job(0, 10, 300, 800);
        
        FatTreeAllocInfo test_allocate1 = new FatTreeAllocInfo(test1);
        FatTreeAllocInfo test_allocate2 = new FatTreeAllocInfo(test2);
        FatTreeAllocInfo test_allocate3 = new FatTreeAllocInfo(test3);

        helper(tree, test_allocate1, 3);
        helper(tree, test_allocate2, 3);
        assertEquals(tree.usedProcessors().size(), 11);
        tree.reset();
        assertEquals(tree.usedProcessors().size(), 0);
        helper(tree, test_allocate3, 3);
        assertEquals(tree.usedProcessors().size(), 10);
    }    
}