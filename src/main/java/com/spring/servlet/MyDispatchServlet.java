package com.spring.servlet;

import com.google.common.collect.Lists;
import com.spring.stereotype.*;
import lombok.Data;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class MyDispatchServlet extends HttpServlet {

    /** 配置 */
    private final static Properties properties = new Properties();

    private final static String SEPARATE = ".";

    /** 模拟ioc容器 */
    private final static Map<String,Object> ioc = new HashMap<>();

    /** handlermapping */
    private final static List<Handler> handlerMapping = Lists.newArrayList();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatcher(req,resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);

        // 1、加载配置
        doLoadConfig(config);

        // 2、扫描包
        doScanner(properties.getProperty("scan.package"));

        // 3、实例化
        doInstance();

        // 4、自动注入
        doAutowired();

        // 5、初始化映射
        initHandlerMapping();
    }

    /**
     * 初始化映射
     */
    private void initHandlerMapping() {
        
        try {

            for(Map.Entry<String,Object> entry : ioc.entrySet()) {
                
                Class clzz = Class.forName(entry.getKey());
                
                if(clzz.isAnnotationPresent(MyController.class)) {

                    final Method[] declaredMethods = clzz.getDeclaredMethods();
                    
                    for(Method method : declaredMethods) {

                        String path = ioc.get(entry.getKey()).getClass().getAnnotation(MyRequestMapping.class).value()[0] 
                                + method.getAnnotation(MyRequestMapping.class).value()[0];

                        handlerMapping.add(new Handler(ioc.get(entry.getKey()),
                                method,
                                Pattern.compile(path)));
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("initHandlerMapping失败：",e);
        }
    }

    /**
     * 自动注入
     */
    private void doAutowired() {

        try {

            for(Map.Entry<String,Object> entry : ioc.entrySet()) {

                Class<?> clzz = Class.forName(entry.getKey());
                
                if(clzz.isAnnotationPresent(MyController.class) ||
                        clzz.isAnnotationPresent(MyService.class) || 
                        clzz.isAnnotationPresent(MyRepository.class)) {

                    final Field[] declaredFields = clzz.getDeclaredFields();
                    
                    for(Field field : declaredFields) {
                        
                        if(field.isAnnotationPresent(MyAutowired.class)) {
                            
                            if(!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            
                            field.set(ioc.get(entry.getKey()),ioc.get(field.getType().getName()));
                        }
                    }
                } 
            }
            
        } catch (Exception e) {
            throw new RuntimeException("doAutowired失败：",e);
        }
    }

    /**
     * 实例化
     */
    private void doInstance() {

        try {

            for(Map.Entry<String,Object> entry : ioc.entrySet()) {

                final Class<?> clzz = Class.forName(entry.getKey());
                
                if(clzz.isAnnotationPresent(MyController.class)) {

                    ioc.put(entry.getKey(),clzz.newInstance());

                } else if(clzz.isAnnotationPresent(MyService.class) || clzz.isAnnotationPresent(MyRepository.class)) {

                    ioc.put(entry.getKey(),clzz.newInstance());

                    final Class<?>[] interfaces = clzz.getInterfaces();

                    for(Class i : interfaces) {
                        ioc.put(i.getName(),ioc.get(entry.getKey()));
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("doInstance失败：",e);
        }
    }

    /**
     * 扫描包
     */
    private void doScanner(String scanPackage) {

        try {

            final URL resourceUrl = this.getClass().getClassLoader().getResource("/");

            final String filePath = (resourceUrl.getPath() + scanPackage.replaceAll("\\.", "/")).substring(1);

            File file = new File(filePath);

            final File[] files = file.listFiles();

            for(File f : files) {

                if(f.isDirectory()) {
                    doScanner(scanPackage + SEPARATE + f.getName());
                } else {

                    if(f.getName().endsWith(".class")) {
                        ioc.put(scanPackage + SEPARATE + f.getName().replaceAll(".class",""),null);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("doScanner失败",e);
        }
    }

    /**
     * 加载配置
     * @param config
     */
    private void doLoadConfig(ServletConfig config) {
        try {
            final String contextConfigLocation = config.getInitParameter("contextConfigLocation");
            final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation.replaceAll("classpath:",""));
            properties.load(resourceAsStream);
        } catch (Exception e) {
            throw new RuntimeException("doLoadConfig失败：",e);
        }
    }

    /**
     * 委派处理
     * @param req
     * @param resp
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) {

        try {
            final Handler handler = getHandler(req);

            if(null == handler) {
                resp.getWriter().write("404 Not Found");
                return;
            }
            
            Object[] params = new Object[handler.getParamIdxMap().size()];

            final Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();

            for(Map.Entry<String,Integer> entry : handler.getParamIdxMap().entrySet()) {

                if(entry.getKey().equals(HttpServletRequest.class.getName())) {
                    params[entry.getValue()] = req;
                } else if(entry.getKey().equals(HttpServletResponse.class.getName())) {
                    params[entry.getValue()] = resp;
                } else {
                    params[entry.getValue()] = cover(req.getParameter(entry.getKey()),parameterTypes[entry.getValue()]);
                }
            }
            
            // 反射invoke
            handler.getMethod().invoke(handler.getController(),params);
            
        } catch (Exception e) {
            throw new RuntimeException("doDispatcher失败：",e);
        }
    }

    /**
     * 转换参数类型
     * @param param 参数
     * @param parameterType 参数类型
     * @return
     */
    private Object cover(String param, Class<?> parameterType) {

        if(parameterType == java.lang.Integer.class) {
            return null == param || "".equals(param) ? 0 : Integer.parseInt(param);
        }

        if(parameterType == java.lang.Double.class) {
            return null == param || "".equals(param) ? 0 : Double.parseDouble(param);
        }

        // TODO:其他类型的依次添加

        return param;
    }

    /**
     * 获取映射Handler
     * @param req
     * @return
     */
    private Handler getHandler(HttpServletRequest req) {

        final String requestUrl = req.getRequestURI();
        
        for(Handler handler : handlerMapping) {
            
            if(handler.getPattern().matcher(requestUrl).find()) {
                return handler;
            }
        }
        
        return null;
    }
    
    @Data
    private static class Handler {
        
        private Object controller;
        
        private Method method;
        
        private Pattern pattern;
        
        private Map<String,Integer> paramIdxMap = new HashMap<>();

        Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            
            initParamParamIdx();
        }

        /**
         * 处理方法参数索引位置
         */
        private void initParamParamIdx() {

            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();

            for(int i = 0; i < parameterAnnotations.length;i++) {

                for(Annotation annotation : parameterAnnotations[i]) {
                    if(annotation instanceof MyRequestParam) {
                        paramIdxMap.put(((MyRequestParam) annotation).value(),i);
                    }
                }
            }

            final Class<?>[] parameterTypes = this.method.getParameterTypes();

            for(int j = 0;j < parameterTypes.length;j++) {

                if(parameterTypes[j] == HttpServletRequest.class || parameterTypes[j] == HttpServletResponse.class ||
                        parameterTypes[j] == HttpSession.class) {
                    paramIdxMap.put(parameterTypes[j].getName(),j);
                }
            }
        }
    }
}
