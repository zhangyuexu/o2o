package com.zyx.o2o.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zyx.o2o.BaseTest;

public class AreaServiceTest extends BaseTest{
	@Autowired
	private AreaService areaService;
	@Test
	public void testGetAreaList() {
		//System.out.println(areaService.getAreaList().get(0).getAreaName());
		assertEquals("海淀", areaService.getAreaList().get(0).getAreaName());
	}

}
