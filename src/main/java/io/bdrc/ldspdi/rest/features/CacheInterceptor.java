package io.bdrc.ldspdi.rest.features;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import io.bdrc.auth.AuthProps;
import io.bdrc.auth.rdf.RdfAuthModel;
import io.bdrc.ldspdi.service.ServiceConfig;

@Component
public class CacheInterceptor implements HandlerInterceptor {

	private final static String MAX_AGE = "max-age=";
	private final static String NO_CACHE = "no-cache";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String header = "";
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		SpringCacheControl methodAnnotation = handlerMethod.getMethodAnnotation(SpringCacheControl.class);
		if (methodAnnotation == null) {
			return false;
		}
		if (methodAnnotation.noCache()) {
			header = NO_CACHE;
		} else {
			long seconds = methodAnnotation.maxAge();
			header = MAX_AGE + Long.toString(seconds);
		}
		if (ServiceConfig.useAuth()) {
			String appId = AuthProps.getProperty("appId");
			String path = request.getServletPath();
			if (!RdfAuthModel.isSecuredEndpoint(appId, path)) {
				response.setHeader(HttpHeaders.CACHE_CONTROL, "public," + header);
			} else {
				response.setHeader(HttpHeaders.CACHE_CONTROL, "private," + header);
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
		// TODO Auto-generated method stub
		HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
	}

}
