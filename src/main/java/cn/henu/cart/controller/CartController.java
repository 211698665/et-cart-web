package cn.henu.cart.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
/**
 * 处理购物车
 * @author syw
 *
 */
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.henu.cart.service.CartService;
import cn.henu.common.utils.CookieUtils;
import cn.henu.common.utils.EtResult;
import cn.henu.common.utils.JsonUtils;
import cn.henu.pojo.TbItem;
import cn.henu.pojo.TbUser;
import cn.henu.service.ItemService;
@Controller
public class CartController {

	@Autowired
	private ItemService itemService;
	
	@Autowired
	private CartService cartService;
	@Value("${COOKIE_CART_EXPIRE}")
	private Integer COOKIE_CART_EXPIRE;
	@RequestMapping("/cart/add/{itemId}")
	public String addCart(@PathVariable Long itemId,@RequestParam(defaultValue="1")Integer num,
			HttpServletRequest request,HttpServletResponse response) {
		//判断用户是否登录，如果登录，把购物车写入redis
		TbUser tbUser = (TbUser) request.getAttribute("user");
		if(tbUser!=null) {
			//保存到服务端
			cartService.addCart(tbUser.getId(), itemId, num);
			//返回一个逻辑视图
			return "cartSuccess";
		}
		//如果未登陆，执行下面的操作
		//1.从cookie中取购物车列表,使用CookieUtils工具类
		List<TbItem> list = getCartListFromCookie(request);
		//2.判断商品在商品列表中是否存在
		boolean flag=false;
		for (TbItem tbItem : list) {
			//注意包装数据类型不能直接等
			if(tbItem.getId()==itemId.longValue()) {
				flag=true;
				//3.如果存在，则把数量相加
				tbItem.setNum(tbItem.getNum()+num);
				break;
			}
		}
		//4.如果不存在，根据商品ID查询商品信息
		if(!flag) {
			TbItem tbItem = itemService.getItemById(itemId);
			//设置商品的数量，不能把库存放进去，这里只是借用库存这个字段来放商品数量
			tbItem.setNum(num);
			//取一张图片,用于展示
			String image = tbItem.getImage();
			if(StringUtils.isNotBlank(image)) {
				tbItem.setImage(image.split(",")[0]);
			}
			//5.把商品添加到商品列表
			list.add(tbItem);
		}
		
		//6.写入cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), COOKIE_CART_EXPIRE,true);
		//7.返回添加成功界面
		return "cartSuccess";
	}
	//从cookie中取出购物车列表
	private List<TbItem> getCartListFromCookie(HttpServletRequest request){
		String json = CookieUtils.getCookieValue(request, "cart", true);
		if(StringUtils.isBlank(json)) {
			//如果为空则返回一个空的list，这里不返回null是为了避免空指针异常
			return new ArrayList();
		}else {
			//不为空时候，把json装换为商品列表
			List<TbItem> list = JsonUtils.jsonToList(json, TbItem.class);
			return list;
		}
	}
	//展示购物车列表
	@RequestMapping("/cart/cart")
	public String showCartList(HttpServletRequest request,HttpServletResponse response) {
		//1.从cookie中取出购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		//判断用户是否登录
		TbUser user = (TbUser) request.getAttribute("user");
		//如果是登录状态
		if(user!=null) {
			//从cookie中取出购物车列表
			//如果不为空,把cookie中的商品和服务器中的商品进行合并
			cartService.mergeCart(user.getId(), list);
			//把cookie中的购物车删除
			CookieUtils.deleteCookie(request, response, "cart");
			//从服务端取出购物车列表
			list = cartService.getCartList(user.getId());
		}
		//未登录时候进行如下操作
		//2.把列表传递给页面
		request.setAttribute("cartList", list);
		//3.返回逻辑视图
		return "cart";
	}
	//更新购物车数量
	@RequestMapping("cart/update/num/{itemId}/{num}")
	@ResponseBody //注意，使用responseBody这个注解返回json数据，必须要有jackson或fastjson等的包，否则会出现406错误
	public EtResult updateCartNum(@PathVariable Long itemId,@PathVariable Integer num,
			HttpServletRequest request,HttpServletResponse response) {
		//判断用户是否为登录状态
		TbUser user = (TbUser) request.getAttribute("user");
		if(user!=null) {
			cartService.updateCartNum(user.getId(), itemId, num);
			return EtResult.ok();
		}
		//1.从cookie中获取购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		//2.遍历商品列表找到对应的商品
		for (TbItem tbItem : list) {
			if(tbItem.getId().longValue()==itemId) {
				//3.更新数量
				tbItem.setNum(num);
				break;
			}
		}
		//4.把购物车列表写回cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), COOKIE_CART_EXPIRE,true);
		//5.返回成功
		return EtResult.ok();
	}
	
	@RequestMapping("/cart/delete/{itemId}")
	public String deleteCartItem(@PathVariable Long itemId, HttpServletRequest request,HttpServletResponse response) {
		//判断用户是否为登录状态
		TbUser user = (TbUser) request.getAttribute("user");
		if(user!=null) {
			cartService.deleteCartItem(user.getId(), itemId);
			return "redirect:/cart/cart.html";//注意这里需要加上完整的请求
		}
		//从cookie获取购物车列表
		List<TbItem> list = getCartListFromCookie(request);
		//遍历列表找到要删除的商品
		for (TbItem tbItem : list) {
			if(tbItem.getId().longValue()==itemId) {
				//删除商品
				list.remove(tbItem);
				break;
			}
		}
		//把购物车列表写入cookie
		CookieUtils.setCookie(request, response, "cart", JsonUtils.objectToJson(list), COOKIE_CART_EXPIRE,true);
		//返回逻辑视图
		return "redirect:/cart/cart.html";//注意这里需要加上完整的请求
	}
}
