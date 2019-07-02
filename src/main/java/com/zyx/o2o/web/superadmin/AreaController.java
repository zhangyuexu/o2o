package com.zyx.o2o.web.superadmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zyx.o2o.service.AreaService;


import com.zyx.o2o.entity.Area;
@Controller
@RequestMapping("/superadmin")
public class AreaController {
	Logger logger=LoggerFactory.getLogger(AreaController.class);
	
	@Autowired
	private AreaService areaService;
	
	@RequestMapping(value="/listarea",method=RequestMethod.GET)
	//自动让返回给前端的内容转换为json格式
	@ResponseBody
	private Map<String,Object> listArea(){
		logger.info("=========start============");
		long startTime=System.currentTimeMillis();
		Map<String,Object> map=new HashMap<>();
		List<Area> list=new ArrayList<>();
		
		try {
			list=areaService.getAreaList();
			map.put("rows", list);
			map.put("total", list.size());
		}catch(Exception e) {
			map.put("success", false);
			map.put("errMsg", e.toString());
		}
		long endTime=System.currentTimeMillis();
		logger.debug("costTime:[{}ms]", endTime-startTime);
		logger.info("=========end============");
		logger.error("test error");
		return map;
	}
}
