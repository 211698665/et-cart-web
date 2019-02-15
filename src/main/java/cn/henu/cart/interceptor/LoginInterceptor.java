package cn.henu.cart.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.henu.common.utils.CookieUtils;
import cn.henu.common.utils.EtResult;
import cn.henu.pojo.TbUser;
import cn.henu.sso.service.TokenService;

//用户登录处理的拦截器
public class LoginInterceptor implements HandlerInterceptor {

	@Autowired
	private TokenService tokenService;
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// 前处理，在执行handler（就是controller里面的方法）之前执行此方法，此方法返回true则放行，否则拦截
		//1.先从cookie中取token
		String token = CookieUtils.getCookieValue(request, "token");
		//2.如果没有token，未登陆，直接放行
		if(StringUtils.isBlank(token)) {
			return true;
		}
		//3.取到token，调用sso系统的服务，根据token取出用户信息
		EtResult result = tokenService.getUserBytoken(token);
		//4.没有取到用户信息，登陆过期，直接放行
		if(result.getStatus()!=200) {
			return true;
		}
		//5,取到用户信息，
		 TbUser user=(TbUser)result.getData();
		//6.把用户信息放到request，只需要在controller中判定request中是否包含user信息
		 request.setAttribute("user", user);
		 //放行
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// handler执行之后，返回modelandview之前，

	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// 完成处理，返回modelandview之后，这个一般用来处理异常

	}

}
