package com.gymcrm.aspect;

// constant class for pointcut strings for easier managment
public final class Pointcuts {

    public static final String SERVICE = "execution(public * com.gymcrm.service.*.*(..))";
    public static final String DAO = "execution(public * com.gymcrm.dao.impl.*.*(..))";
    public static final String FACADE = "execution(public * com.gymcrm.facade.*.*(..))";
    public static final String UTIL = "execution(public * com.gymcrm.util.impl.*.*(..))";
    public static final String CONTROLLER = "execution(public * com.gymcrm.controller.*.*(..))";

    private Pointcuts() {}
}
