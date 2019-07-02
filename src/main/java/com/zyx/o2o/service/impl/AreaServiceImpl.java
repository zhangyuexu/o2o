package com.zyx.o2o.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zyx.o2o.service.AreaService;
import com.zyx.o2o.dao.AreaDao;
import com.zyx.o2o.entity.Area;
@Service
public class AreaServiceImpl implements AreaService {
	//service层依赖dao层的
	@Autowired
	private AreaDao areaDao;

	@Override
	public List<Area> getAreaList(){
		return areaDao.queryArea();
	}
	
	
}
