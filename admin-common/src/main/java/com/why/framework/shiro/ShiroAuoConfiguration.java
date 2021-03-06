package com.why.framework.shiro;

import com.why.common.configuration.RememberMeCookie;
import com.why.common.configuration.ShiroProperties;
import com.why.common.utils.StringUtils;
import com.why.framework.shiro.realm.UserRealm;
import com.why.framework.shiro.session.OnlineSessionDAO;
import com.why.framework.shiro.session.OnlineSessionFactory;
import com.why.framework.shiro.web.filter.LogoutFilter;
import com.why.framework.shiro.web.filter.UserFilter;
import com.why.framework.shiro.web.filter.kickout.KickoutSessionFilter;
import com.why.framework.shiro.web.filter.online.OnlineSessionFilter;
import com.why.framework.shiro.web.filter.sync.SyncOnlineSessionFilter;
import com.why.framework.shiro.web.session.OnlineWebSessionManager;
import com.why.framework.shiro.web.session.SpringSessionValidationScheduler;
import com.why.framework.spring.ApplicationUtils;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.io.ResourceUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ??????????????????
 *
 * @author Crown
 */
@Configuration
@EnableConfigurationProperties({ShiroProperties.class})
public class ShiroAuoConfiguration implements WebMvcConfigurer {

    private final ShiroProperties properties;

    public ShiroAuoConfiguration(ShiroProperties properties) {
        this.properties = properties;
    }

    /**
     * ??????????????? ??????Ehcache??????
     */
    @Bean
    public EhCacheManager getEhCacheManager() {
        net.sf.ehcache.CacheManager cacheManager = net.sf.ehcache.CacheManager.getCacheManager("Crown2");
        EhCacheManager em = new EhCacheManager();
        if (StringUtils.isNull(cacheManager)) {
            em.setCacheManager(new net.sf.ehcache.CacheManager(getCacheManagerConfigFileInputStream()));
            return em;
        } else {
            em.setCacheManager(cacheManager);
            return em;
        }
    }

    /**
     * ????????????????????? ??????ehcache??????????????????????????????????????????????????????????????????
     */
    protected InputStream getCacheManagerConfigFileInputStream() {
        String configFile = "classpath:ehcache/ehcache-shiro.xml";
        try (InputStream inputStream = ResourceUtils.getInputStreamForPath(configFile)) {
            byte[] b = IOUtils.toByteArray(inputStream);
            return new ByteArrayInputStream(b);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Unable to obtain input stream for cacheManagerConfigFile [" + configFile + "]", e);
        }
    }

    /**
     * ?????????Realm
     */
    @Bean
    public UserRealm userRealm(EhCacheManager cacheManager) {
        UserRealm userRealm = new UserRealm();
        userRealm.setCacheManager(cacheManager);
        return userRealm;
    }

    /**
     * ?????????sessionDAO??????
     */
    @Bean
    public OnlineSessionDAO sessionDAO() {
        return new OnlineSessionDAO(properties.getSession().getDbSyncPeriod());
    }

    /**
     * ?????????sessionFactory??????
     */
    @Bean
    public OnlineSessionFactory sessionFactory() {
        return new OnlineSessionFactory();
    }

    /**
     * ???????????????
     */
    @Bean
    public OnlineWebSessionManager sessionManager() {
        OnlineWebSessionManager manager = new OnlineWebSessionManager();
        // ?????????????????????
        manager.setCacheManager(getEhCacheManager());
        // ???????????????session
        manager.setDeleteInvalidSessions(true);
        // ????????????session????????????
        manager.setGlobalSessionTimeout(properties.getSession().getExpireTime() * 60 * 1000);
        // ?????? JSESSIONID
        manager.setSessionIdUrlRewritingEnabled(false);
        // ???????????????????????????Session???????????????
        manager.setSessionValidationScheduler(ApplicationUtils.getBean(SpringSessionValidationScheduler.class));
        // ??????????????????session
        manager.setSessionValidationSchedulerEnabled(true);
        // ?????????SessionDao
        manager.setSessionDAO(sessionDAO());
        // ?????????sessionFactory
        manager.setSessionFactory(sessionFactory());
        return manager;
    }

    /**
     * ???????????????
     */
    @Bean
    public SecurityManager securityManager(UserRealm userRealm, SpringSessionValidationScheduler springSessionValidationScheduler) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // ??????realm.
        securityManager.setRealm(userRealm);
        // ?????????
        securityManager.setRememberMeManager(rememberMeManager());
        // ?????????????????????;
        securityManager.setCacheManager(getEhCacheManager());
        // session?????????
        securityManager.setSessionManager(sessionManager());
        return securityManager;
    }

    /**
     * ???????????????
     */
    public LogoutFilter logoutFilter() {
        LogoutFilter logoutFilter = new LogoutFilter();
        logoutFilter.setCacheManager(getEhCacheManager());
        logoutFilter.setLoginUrl(properties.getLoginUrl());
        return logoutFilter;
    }

    /**
     * UserFilter
     */
    public UserFilter userFilter() {
        return new UserFilter();
    }

    /**
     * Shiro???????????????
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        // Shiro?????????????????????,????????????????????????
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        // ??????????????????????????????????????????????????????
        shiroFilterFactoryBean.setLoginUrl(properties.getLoginUrl());
        // ?????????????????????????????????????????????
        shiroFilterFactoryBean.setUnauthorizedUrl(properties.getUnauthUrl());
        // Shiro??????????????????????????????????????????
        LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // ?????????????????????????????????
        filterChainDefinitionMap.put("/favicon.ico**", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/docs/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/img/**", "anon");
        filterChainDefinitionMap.put("/ajax/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/crown/**", "anon");
        // ?????? logout?????????shiro?????????session
        filterChainDefinitionMap.put("/logout", "logout");
        // ????????????????????????
        filterChainDefinitionMap.put("/login", "anon");
        //?????????
        filterChainDefinitionMap.put("/captcha", "anon");

        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("onlineSession", onlineSessionFilter());
        filters.put("syncOnlineSession", syncOnlineSessionFilter());
        filters.put("kickout", kickoutSessionFilter());
        // ???????????????????????????????????????
        filters.put("logout", logoutFilter());
        filters.put("user", userFilter());
        shiroFilterFactoryBean.setFilters(filters);

        // ????????????????????????
        filterChainDefinitionMap.put("/**", "user,kickout,onlineSession,syncOnlineSession");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);

        return shiroFilterFactoryBean;
    }

    /**
     * ????????????????????????????????????
     */
    @Bean
    public OnlineSessionFilter onlineSessionFilter() {
        OnlineSessionFilter onlineSessionFilter = new OnlineSessionFilter();
        onlineSessionFilter.setLoginUrl(properties.getLoginUrl());
        return onlineSessionFilter;
    }

    /**
     * ????????????????????????????????????
     */
    @Bean
    public SyncOnlineSessionFilter syncOnlineSessionFilter() {
        return new SyncOnlineSessionFilter();
    }

    /**
     * cookie ????????????
     */
    public SimpleCookie rememberMeCookie() {
        SimpleCookie cookie = new SimpleCookie("rememberMe");
        RememberMeCookie rememberMeCookie = properties.getRememberMeCookie();
        cookie.setDomain(rememberMeCookie.getDomain());
        cookie.setPath(rememberMeCookie.getPath());
        cookie.setHttpOnly(rememberMeCookie.isHttpOnly());
        cookie.setMaxAge(rememberMeCookie.getMaxAge() * 24 * 60 * 60);
        return cookie;
    }

    /**
     * ?????????
     */
    public CookieRememberMeManager rememberMeManager() {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        /**
         * Cipherkey byte length Must equal 16
         */
        cookieRememberMeManager.setCipherKey(Base64.decode("CrownKey==a12d/dakdad"));
        return cookieRememberMeManager;
    }

    /**
     * ????????????????????????????????????
     */
    public KickoutSessionFilter kickoutSessionFilter() {
        KickoutSessionFilter kickoutSessionFilter = new KickoutSessionFilter();
        kickoutSessionFilter.setCacheManager(getEhCacheManager());
        kickoutSessionFilter.setSessionManager(sessionManager());
        // ??????????????????????????????????????????-1??????????????????2????????????????????????????????????????????????????????????
        kickoutSessionFilter.setMaxSession(properties.getSession().getMaxSession());
        // ???????????????????????????????????????false?????????????????????????????????????????????????????????????????????
        kickoutSessionFilter.setKickoutAfter(properties.getSession().isKickoutAfter());
        // ????????????????????????????????????
        kickoutSessionFilter.setKickoutUrl("/login?kickout=1");
        return kickoutSessionFilter;
    }

    /**
     * ??????Shiro???????????????
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
            @Qualifier("securityManager") SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:" + properties.getIndexUrl());
    }

}

