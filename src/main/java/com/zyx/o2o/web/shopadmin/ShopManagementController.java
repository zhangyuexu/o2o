package com.zyx.o2o.web.shopadmin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyx.o2o.dto.ShopExecution;
import com.zyx.o2o.entity.Area;
import com.zyx.o2o.entity.LocalAuth;
import com.zyx.o2o.entity.PersonInfo;
import com.zyx.o2o.entity.Shop;
import com.zyx.o2o.entity.ShopCategory;
import com.zyx.o2o.enums.ProductCategoryStateEnum;
import com.zyx.o2o.enums.ShopStateEnum;
import com.zyx.o2o.service.AreaService;
import com.zyx.o2o.service.LocalAuthService;
import com.zyx.o2o.service.ShopCategoryService;
import com.zyx.o2o.service.ShopService;
import com.zyx.o2o.util.CodeUtil;
import com.zyx.o2o.util.FileUtil;
import com.zyx.o2o.util.HttpServletRequestUtil;
import com.zyx.o2o.util.ImageUtil;

@Controller
@RequestMapping("/shop")
public class ShopManagementController {
	@Autowired
	private ShopService shopService;
	@Autowired
	private ShopCategoryService shopCategoryService;
	@Autowired
	private AreaService areaService;
	@Autowired
	private LocalAuthService localAuthService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> list(HttpServletRequest request) {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		PersonInfo user = (PersonInfo) request.getSession()
				.getAttribute("user");
		long employeeId = user.getUserId();
		if (hasAccountBind(request, employeeId)) {
			modelMap.put("hasAccountBind", true);
		} else {
			modelMap.put("hasAccountBind", false);
		}
		List<Shop> list = new ArrayList<Shop>();
		try {
			ShopExecution shopExecution = shopService
					.getByEmployeeId(employeeId);
			list = shopExecution.getShopList();
			modelMap.put("shopList", list);
			modelMap.put("user", user);
			modelMap.put("success", true);
			// 列出店铺成功之后，将店铺放入session中作为权限验证依据，即该帐号只能操作它自己的店铺
			request.getSession().setAttribute("shopList", list);
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.put("success", false);
			modelMap.put("errMsg", e.toString());
		}
		return modelMap;
	}

	@RequestMapping(value = "/getshopbyid", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> getShopById(@RequestParam Long shopId,
			HttpServletRequest request) {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		if (shopId != null && shopId > -1) {
			Shop shop = shopService.getByShopId(shopId);
			shop.getShopCategory().setShopCategoryName(
					shopCategoryService.getShopCategoryById(
							shop.getShopCategory().getShopCategoryId())
							.getShopCategoryName());
			shop.getParentCategory().setShopCategoryName(
					shopCategoryService.getShopCategoryById(
							shop.getParentCategory().getShopCategoryId())
							.getShopCategoryName());
			modelMap.put("shop", shop);
			request.getSession().setAttribute("currentShop", "shop");
			List<Area> areaList = new ArrayList<Area>();
			try {
				areaList = areaService.getAreaList();
			} catch (IOException e) {
				modelMap.put("success", false);
				modelMap.put("errMsg", e.toString());
			}
			modelMap.put("areaList", areaList);
			modelMap.put("success", true);
		} else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "empty shopId");
		}
		return modelMap;
	}

	@RequestMapping(value = "/getshopinitinfo", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> getShopInitInfo() {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		List<ShopCategory> shopCategoryList = new ArrayList<ShopCategory>();
		List<Area> areaList = new ArrayList<Area>();
		try {
			shopCategoryList = shopCategoryService
					.getAllSecondLevelShopCategory();
			areaList = areaService.getAreaList();
		} catch (IOException e) {
			modelMap.put("success", false);
			modelMap.put("errMsg", e.toString());
		}
		modelMap.put("shopCategoryList", shopCategoryList);
		modelMap.put("areaList", areaList);
		modelMap.put("success", true);
		return modelMap;
	}

	@RequestMapping(value = "/modifyshop", method = RequestMethod.POST)
	@ResponseBody
	private Map<String, Object> modifyShop(HttpServletRequest request) {
		boolean statusChange = HttpServletRequestUtil.getBoolean(request,
				"statusChange");
		Map<String, Object> modelMap = new HashMap<String, Object>();
		if (!statusChange && !CodeUtil.checkVerifyCode(request)) {
			modelMap.put("success", false);
			modelMap.put("errMsg", "输入了错误的验证码");
			return modelMap;
		}
		ObjectMapper mapper = new ObjectMapper();
		Shop shop = null;
		String shopStr = HttpServletRequestUtil.getString(request, "shopStr");
		MultipartHttpServletRequest multipartRequest = null;
		CommonsMultipartFile shopImg = null;
		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		if (multipartResolver.isMultipart(request)) {
			multipartRequest = (MultipartHttpServletRequest) request;
			shopImg = (CommonsMultipartFile) multipartRequest
					.getFile("shopImg");
		}
		try {
			shop = mapper.readValue(shopStr, Shop.class);
		} catch (Exception e) {
			modelMap.put("success", false);
			modelMap.put("errMsg", e.toString());
			return modelMap;
		}
		Shop currentShop = (Shop) request.getSession().getAttribute(
				"currentShop");
		shop.setShopId(currentShop.getShopId());
		if (shop != null && shop.getShopId() != null) {
			filterAttribute(shop);
			try {
				ShopExecution se = shopService.modifyShop(shop, shopImg);
				if (se.getState() == ProductCategoryStateEnum.SUCCESS
						.getState()) {
					modelMap.put("success", true);
				} else {
					modelMap.put("success", false);
					modelMap.put("errMsg", se.getStateInfo());
				}
			} catch (RuntimeException e) {
				modelMap.put("success", false);
				modelMap.put("errMsg", e.toString());
				return modelMap;
			}

		} else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "请输入店铺信息");
		}
		return modelMap;
	}

	@RequestMapping(value = "/registershop", method = RequestMethod.POST)
	@ResponseBody
	private Map<String, Object> registerShop(HttpServletRequest request) {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		//1、接收并转换相应的参数，包括店铺信息和图片信息
		String shopStr=HttpServletRequestUtil.getString(request,"shopStr");
		ObjectMapper mapper = new ObjectMapper();
		Shop shop = null;
		try {
			shop=mapper.readValue(shopStr,Shop.class);
		}catch(Exception e) {
			modelMap.put("success", false);
			modelMap.put("errMsg", e.getMessage());
			return modelMap;
		}
		
		MultipartHttpServletRequest multipartRequest = null;
		CommonsMultipartFile shopImg = null;
		CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver(
				request.getSession().getServletContext());
		if (commonsMultipartResolver.isMultipart(request)) {
			multipartRequest = (MultipartHttpServletRequest) request;
			shopImg = (CommonsMultipartFile) multipartRequest
					.getFile("shopImg");
		} else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "上传图片不能为空");
			return modelMap;
		}
		
		//2、注册店铺
		if (shop != null && shopImg != null) {
//				PersonInfo user = (PersonInfo) request.getSession()
//						.getAttribute("user");
//				shop.setOwnerId(user.getUserId());
				PersonInfo owner=new PersonInfo();
				owner.setUserId(1L);
				shop.setOwnerId(owner.getUserId());
				File shopImgFile=new File(FileUtil.getImgBasePath()+FileUtil.getRandomFileName());
				shopImgFile.createNewFile();
				inputStreamToFile(shopImg.getInputStream(), shopImgFile);
				ShopExecution se = shopService.addShop(shop, shopImgFile);
				if (se.getState() == ShopStateEnum.CHECK.getState()) {
//					modelMap.put("success", true);
				}else {
					modelMap.put("success", false);
					modelMap.put("errMsg", se.getStateInfo());
				}
				return modelMap;
		}else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "请输入店铺信息");
			return modelMap;
		}
	}
				
		
//		MultipartHttpServletRequest multipartRequest = null;
//		CommonsMultipartFile shopImg = null;
//		CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver(
//				request.getSession().getServletContext());
//		if (multipartResolver.isMultipart(request)) {
//			multipartRequest = (MultipartHttpServletRequest) request;
//			shopImg = (CommonsMultipartFile) multipartRequest
//					.getFile("shopImg");
//		} else {
//			modelMap.put("success", false);
//			modelMap.put("errMsg", "上传图片不能为空");
//			return modelMap;
//		}
//		try {
//			shop = mapper.readValue(shopStr, Shop.class);
//		} catch (Exception e) {
//			modelMap.put("success", false);
//			modelMap.put("errMsg", e.toString());
//			return modelMap;
//		}
//		if (shop != null && shopImg != null) {
//			try {
//				PersonInfo user = (PersonInfo) request.getSession()
//						.getAttribute("user");
//				shop.setOwnerId(user.getUserId());
//				ShopExecution se = shopService.addShop(shop, shopImg);
//				if (se.getState() == ShopStateEnum.CHECK.getState()) {
//					modelMap.put("success", true);
//					// 若shop创建成功，则加入session中，作为权限使用
//					@SuppressWarnings("unchecked")
//					List<Shop> shopList = (List<Shop>) request.getSession()
//							.getAttribute("shopList");
//					if (shopList != null && shopList.size() > 0) {
//						shopList.add(se.getShop());
//						request.getSession().setAttribute("shopList", shopList);
//					} else {
//						shopList = new ArrayList<Shop>();
//						shopList.add(se.getShop());
//						request.getSession().setAttribute("shopList", shopList);
//					}
//				} else {
//					modelMap.put("success", false);
//					modelMap.put("errMsg", se.getStateInfo());
//				}
//			} catch (RuntimeException e) {
//				modelMap.put("success", false);
//				modelMap.put("errMsg", e.toString());
//				return modelMap;
//			}
//
//		} else {
//			modelMap.put("success", false);
//			modelMap.put("errMsg", "请输入店铺信息");
//		}
//		return modelMap;
//	}

	private void filterAttribute(Shop shop) {

	}

	private boolean hasAccountBind(HttpServletRequest request, long userId) {
		if (request.getSession().getAttribute("bindAccount") == null) {
			LocalAuth localAuth = localAuthService.getLocalAuthByUserId(userId);
			if (localAuth != null && localAuth.getUserId() != null) {
				request.getSession().setAttribute("bindAccount", localAuth);
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
	
	private static void inputStreamToFile(InputStream ins,File file) {
		FileOutputStream fos=new FileOutputStream(file);
		try {
			int byteRead=0;
			byte[] buffer=new byte[1024];
			while((byteRead=ins.read(buffer))!=-1) {
				fos.write(buffer, 0, byteRead);
			}
		}catch(IOException e) {
			throw new RuntimeException("调用inputStreamToFile产生异常"+e.getMessage());
		}finally {
			try {
				if(fos!=null) {
					fos.close();
				}
				if(ins!=null) {
					ins.close();
				}
			}catch(Exception e) {
				throw new RuntimeException("关闭输入输出流异常"+e.getMessage());
			}
		}
	}
}
