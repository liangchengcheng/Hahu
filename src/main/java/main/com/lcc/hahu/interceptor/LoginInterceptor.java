package main.com.lcc.hahu.interceptor;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Created by liangchengcheng on 2017/7/3.
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JedisPool jedisPool;

    private List<String> excludedUrls;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {

        String requestUri = httpServletRequest.getRequestURI();
        //是否需要拦截
        for (String s : excludedUrls){
            if (requestUri.endsWith(s)){
                return true;
            }
        }

        String loginToken = null;
        //是否含有cookie
        Cookie[] cookies = httpServletRequest.getCookies();
        if (ArrayUtils.isEmpty(cookies)){
            httpServletRequest.getRequestDispatcher("toLogin").forward(
                    httpServletRequest,httpServletResponse
            );
            return false;
        }else {
            for (Cookie cookie : cookies){
                if (cookie.getName().equals("loginToken")){
                    loginToken = cookie.getValue();
                    break;
                }
            }
        }

        //cookie 中含有loginToken
        if (StringUtils.isEmpty(loginToken)){
            httpServletRequest.getRequestDispatcher("toLogin").forward(
                    httpServletRequest,httpServletResponse
            );
            return false;
        }

        //jedis
        Jedis jedis = jedisPool.getResource();
        String userId = jedis.get(loginToken);
        //根据loginToken 是否能从redis 中获取userId

        if (StringUtils.isEmpty(userId)){
            httpServletRequest.getRequestDispatcher("toLogin").forward(
                    httpServletRequest,httpServletResponse
            );
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }

    public List<String> getExcludedUrls() {
        return excludedUrls;
    }

    public void setExcludedUrls(List<String> excludedUrls) {
        this.excludedUrls = excludedUrls;
    }
}
