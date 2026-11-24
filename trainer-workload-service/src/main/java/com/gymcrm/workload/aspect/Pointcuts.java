package com.gymcrm.workload.aspect;

//mirrors gym-crm's aspect.Pointcuts
public final class Pointcuts {

    public static final String SERVICE = "execution(public * com.gymcrm.workload.service.*.*(..))";
    public static final String REPOSITORY = "execution(public * com.gymcrm.workload.repository.*.*(..))";
    public static final String CONTROLLER = "execution(public * com.gymcrm.workload.controller.*.*(..))";

    private Pointcuts() {
    }
}
