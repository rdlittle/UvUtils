/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webfront.test;

import asjava.uniclientlibs.UniDynArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author rlittle
 */
public class LocateTest {
    
    UniDynArray arrayOne;
    
    
    public LocateTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        arrayOne = new UniDynArray();
        arrayOne.insert(1, 1, "1234");
        arrayOne.insert(1, 1, "3456");
        arrayOne.insert(1, 2, "9874");
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
